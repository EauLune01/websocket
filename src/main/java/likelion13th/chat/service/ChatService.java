package likelion13th.chat.service;

import jakarta.transaction.Transactional;
import likelion13th.chat.domain.ChatMessage;
import likelion13th.chat.domain.MessageStatus;
import likelion13th.chat.dto.command.EditMessageCommand;
import likelion13th.chat.dto.command.EnterRoomCommand;
import likelion13th.chat.dto.command.SendMessageCommand;
import likelion13th.chat.dto.command.DeleteMessageCommand;
import likelion13th.chat.dto.event.ChatMessageResponse;
import likelion13th.chat.dto.event.DeletedMessageEvent;
import likelion13th.chat.repository.ChatMessageRepository;
import likelion13th.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ActiveUserService activeUserService;
    /**
     * ChatMessage 엔티티에서 senderUid를 이용해 사용자 이름(displayName) 을 조회
     * 이름이 존재하지 않으면 "Unknown"으로 처리
     * DTO(ChatMessageResponse)로 변환 후 senderName까지 채워서 반환
     * */
    private ChatMessageResponse toResponseWithName(ChatMessage m) {
        String name = userRepository.findByUid(m.getSenderUid())
                .map(u -> u.getDisplayName())
                .orElse("Unknown");
        return ChatMessageResponse.from(m).withSenderName(name);
    }
    /** ✅메시지 전송 (상대방 접속 여부에 따라 READ 처리) */
    @Transactional
    public ChatMessageResponse send(SendMessageCommand cmd) {
        // 상대방 UID 찾기 (A__B 형식의 roomId 가정)
        String[] uids = cmd.getRoomId().split("__");
        String senderUid = cmd.getSenderUid();
        String receiverUid = uids[0].equals(senderUid) ? uids[1] : uids[0];

        // 상대방이 현재 접속 중인지 확인
        boolean isReceiverActive = activeUserService.getUsersInRoom(cmd.getRoomId()).contains(receiverUid);

        // 접속 중이면 READ, 아니면 SENT
        MessageStatus status = isReceiverActive ? MessageStatus.READ : MessageStatus.SENT;

        //메시지를 DB에 저장 후 ChatMessageResponse로 변환하여 반환
        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .roomId(cmd.getRoomId())
                .senderUid(senderUid)
                .content(cmd.getContent())
                .status(status) // ✅ 동적으로 상태 결정
                .build());
        return toResponseWithName(saved);
    }
    /** ✅ 메시지 수정: 아직 읽히지 않은 메시지만 수정 가능하게 제한 */
    @Transactional
    public ChatMessageResponse edit(EditMessageCommand cmd) {

        //messageId로 DB에서 원본 메시지 조회
        ChatMessage origin = chatMessageRepository.findById(cmd.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("message not found"));
        //보낸 사람 UID와 편집자 UID가 다르면 → 수정 불가
        if (!origin.getSenderUid().equals(cmd.getEditorUid()))
            throw new IllegalStateException("다른 사용자의 메시지는 수정할 수 없습니다.");
        //이미 READ 상태라면 → 수정 불가
        if (origin.getStatus() == MessageStatus.READ)
            throw new IllegalStateException("이미 READ된 메시지는 수정할 수 없습니다.");
        //조건 통과 시 content를 새 내용으로 변경, status를 다시 SENT로 초기화
        ChatMessage updated = origin.toBuilder()
                .content(cmd.getNewContent())
                .status(MessageStatus.SENT)
                .build();
        // 변경 후 저장 및 ChatMessageResponse로 반환
        ChatMessage saved = chatMessageRepository.save(updated);
        return toResponseWithName(saved);
    }
    /** ✅ 메시지 삭제 (DeletedMessageEvent 반환) */
    @Transactional
    public DeletedMessageEvent delete(DeleteMessageCommand cmd) {
        // messageId로 메시지 조회
        ChatMessage m = chatMessageRepository.findById(cmd.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("message not found"));
        // 보낸 사람만 삭제 가능 (requesterUid 검증)
        if (!m.getSenderUid().equals(cmd.getRequesterUid()))
            throw new IllegalStateException("다른 사용자의 메시지는 삭제할 수 없습니다.");
        //이미 읽힌(READ) 메시지는 삭제 불가
        if (m.getStatus() == MessageStatus.READ)
            throw new IllegalStateException("이미 READ된 메시지는 삭제할 수 없습니다.");
        //Repository에서 삭제
        chatMessageRepository.delete(m);
        //삭제 후, DeletedMessageEvent(messageId, roomId)를 만들어 반환
        return DeletedMessageEvent.builder()
                .messageId(m.getId())
                .roomId(m.getRoomId())
                .build();
    }
    /** ✅ 방 입장 시 상대 메시지들 READ로 변경 (변경된 목록 반환): 입장과 동시에 “안 읽은 메시지들”이 자동으로 읽음 처리 */
    @Transactional
    public List<ChatMessageResponse> enter(EnterRoomCommand cmd) {
        //사용자가 방에 입장했을 때 실행
        String me = cmd.getUserUid();
        //DB에서 해당 roomId의 메시지를 가져온 뒤, 내가 보낸 게 아닌 메시지(senderUid != me) 중
        //아직 SENT 또는 DELIVERED 상태인 메시지를 → READ 상태로 변경
        List<ChatMessage> toUpdate = chatMessageRepository.findByRoomId(cmd.getRoomId()).stream()
                .filter(m -> !m.getSenderUid().equals(me))
                .filter(m -> m.getStatus() == MessageStatus.SENT || m.getStatus() == MessageStatus.DELIVERED)
                .map(m -> m.toBuilder().status(MessageStatus.READ).build())
                .toList();
        //수정된 메시지들을 saveAll()로 DB에 반영
        List<ChatMessage> saved = chatMessageRepository.saveAll(toUpdate);
        // 읽음 처리된 메시지들을 DTO로 변환하여 반환
        return saved.stream()
                .map(this::toResponseWithName)
                .collect(Collectors.toList());
    }
    /**
     * ✅ 최근 메시지 50개 불러오기
     * DB에서 findTop50ByRoomIdOrderByIdDesc()로 최신 50개 메시지를 가져옴 (내림차순)
     * 가져온 목록을 오름차순(.sorted(Comparator.comparing(ChatMessage::getId)))으로 다시 정렬
     * 요청한 개수(limit)까지만 잘라서 반환
     */
    @Transactional
    public List<ChatMessageResponse> fetchRecent(String roomId, int limit) {
        return chatMessageRepository.findTop50ByRoomIdOrderByIdDesc(roomId).stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .limit(limit)
                .map(this::toResponseWithName)
                .collect(Collectors.toList());
    }
}