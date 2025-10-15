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

    private ChatMessageResponse toResponseWithName(ChatMessage m) {
        String name = userRepository.findByUid(m.getSenderUid())
                .map(u -> u.getDisplayName())
                .orElse("Unknown");
        return ChatMessageResponse.from(m).withSenderName(name);
    }

    /** ✅ 메시지 전송 (상대방 접속 여부에 따라 READ 처리) */
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

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .roomId(cmd.getRoomId())
                .senderUid(senderUid)
                .content(cmd.getContent())
                .status(status) // ✅ 동적으로 상태 결정
                .build());
        return toResponseWithName(saved);
    }

    /** ✅ 메시지 수정 */
    @Transactional
    public ChatMessageResponse edit(EditMessageCommand cmd) {
        ChatMessage origin = chatMessageRepository.findById(cmd.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("message not found"));

        if (!origin.getSenderUid().equals(cmd.getEditorUid()))
            throw new IllegalStateException("다른 사용자의 메시지는 수정할 수 없습니다.");
        if (origin.getStatus() == MessageStatus.READ)
            throw new IllegalStateException("이미 READ된 메시지는 수정할 수 없습니다.");

        ChatMessage updated = origin.toBuilder()
                .content(cmd.getNewContent())
                .status(MessageStatus.SENT)
                .build();

        ChatMessage saved = chatMessageRepository.save(updated);
        return toResponseWithName(saved);
    }

    /** ✅ 메시지 삭제 (DeletedMessageEvent 반환) */
    @Transactional
    public DeletedMessageEvent delete(DeleteMessageCommand cmd) {
        ChatMessage m = chatMessageRepository.findById(cmd.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("message not found"));

        if (!m.getSenderUid().equals(cmd.getRequesterUid()))
            throw new IllegalStateException("다른 사용자의 메시지는 삭제할 수 없습니다.");
        if (m.getStatus() == MessageStatus.READ)
            throw new IllegalStateException("이미 READ된 메시지는 삭제할 수 없습니다.");

        chatMessageRepository.delete(m);

        return DeletedMessageEvent.builder()
                .messageId(m.getId())
                .roomId(m.getRoomId())
                .build();
    }

    /** ✅ 방 입장 시 상대 메시지들 READ로 변경 (변경된 목록 반환) */
    @Transactional
    public List<ChatMessageResponse> enter(EnterRoomCommand cmd) {
        String me = cmd.getUserUid();

        List<ChatMessage> toUpdate = chatMessageRepository.findByRoomId(cmd.getRoomId()).stream()
                .filter(m -> !m.getSenderUid().equals(me))
                .filter(m -> m.getStatus() == MessageStatus.SENT || m.getStatus() == MessageStatus.DELIVERED)
                .map(m -> m.toBuilder().status(MessageStatus.READ).build())
                .toList();

        List<ChatMessage> saved = chatMessageRepository.saveAll(toUpdate);

        return saved.stream()
                .map(this::toResponseWithName)
                .collect(Collectors.toList());
    }

    /** ✅ 최근 메시지 50개 불러오기 */
    @Transactional
    public List<ChatMessageResponse> fetchRecent(String roomId, int limit) {
        return chatMessageRepository.findTop50ByRoomIdOrderByIdDesc(roomId).stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .limit(limit)
                .map(this::toResponseWithName)
                .collect(Collectors.toList());
    }
}