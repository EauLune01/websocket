package likelion13th.chat.controller;

import jakarta.validation.Valid;
import likelion13th.chat.dto.command.*;
import likelion13th.chat.dto.event.ChatMessageResponse;
import likelion13th.chat.dto.event.DeletedMessageEvent;
import likelion13th.chat.dto.event.WsEvent;
import likelion13th.chat.service.ActiveUserService;
import likelion13th.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService service;
    //STOMP 브로커를 통해 서버 → 클라이언트로 메시지 브로드캐스트할 때 사용하는 객체
    private final SimpMessagingTemplate broker;
    private final ActiveUserService activeUserService;

    // ===== REST: 채팅 내역 조회 =====
    @GetMapping("/api/rooms/{roomId}/messages")
    public List<ChatMessageResponse> history(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return service.fetchRecent(roomId, limit);
    }

    // ===== STOMP: 실시간 채팅의 핵심 송신 엔드포인트 =====
    /**
     * 1. 클라이언트가 /app/send로 SendMessageCommand를 보냄.
     * 2. ChatService.send() 호출 → DB에 메시지 저장, 상태(SENT/READ) 결정
     * 3. 반환된 ChatMessageResponse를 브로커로 전송
     * 4. 해당 방을 구독 중인 모든 사용자에게 메시지를 브로드캐스트
     * */
    @MessageMapping("/send")
    public void send(@Valid SendMessageCommand cmd) {
        ChatMessageResponse res = service.send(cmd);
        broker.convertAndSend("/topic/rooms/" + res.getRoomId(), res);
    }

    // ===== STOMP: 실시간으로 “메시지 수정됨” 상태를 방 전체에 반영=====
    /**
     * 1. 클라이언트가 /app/edit으로 수정 요청(EditMessageCommand) 전송
     * 2. ChatService.edit() 실행 → 메시지 내용 수정 후 저장
     * 3. 수정된 메시지를 브로커로 재전송하여 해당 방 구독자(모든 클라이언트)의 UI가 갱신
     * */
    @MessageMapping("/edit")
    public void edit(@Valid EditMessageCommand cmd) {
        ChatMessageResponse res = service.edit(cmd);
        broker.convertAndSend("/topic/rooms/" + res.getRoomId(), res);
    }

    // ===== STOMP: 삭제 =====
    /**
     * 1. 클라이언트가 /app/delete로 삭제 요청 전송
     * 2. ChatService.delete() 실행 → 검증 후 메시지 삭제
     * 3. 삭제된 메시지의 ID로 WsEvent.deleted(messageId) 이벤트 생성
     * 4. 해당 채팅방 구독자들에게 브로드캐스트
     * */
    @MessageMapping("/delete")
    public void delete(@Valid DeleteMessageCommand cmd) {
        DeletedMessageEvent ev = service.delete(cmd);
        broker.convertAndSend("/topic/rooms/" + ev.getRoomId(), WsEvent.deleted(ev.getMessageId()));
    }

    // ===== STOMP: 방 입장 (사용자가 채팅방에 입장했을 때 호출되어 읽음 처리 및 접속 등록을 수행) =====
    /**
     * 1. 누가 어떤 방에 들어왔는지 STOMP 세션에 저장(이 정보는 나중에 StompEventListener에서 연결 종료 시 퇴장 처리할 때 사용)
     * 2. ActiveUserService에 현재 접속자 정보 등록(이후 상대방이 메시지를 보낼 때 “상대가 접속 중인지” 판별할 수 있음)
     * 3. 방 입장 시, 나 아닌 사용자가 보낸 읽지 않은 메시지들을 모두 READ 상태로 변경
     * 4. 변경된 메시지들의 상태(READ)를 방 안의 모든 구독자에게 브로드캐스트
     * */
    @MessageMapping("/enter")
    public void enter(@Valid EnterRoomCommand cmd, SimpMessageHeaderAccessor headerAccessor) {

        headerAccessor.getSessionAttributes().put("roomId", cmd.getRoomId());
        headerAccessor.getSessionAttributes().put("userUid", cmd.getUserUid());

        activeUserService.userJoined(cmd.getRoomId(), cmd.getUserUid());

        List<ChatMessageResponse> changed = service.enter(cmd);

        for (ChatMessageResponse res : changed) {
            broker.convertAndSend("/topic/rooms/" + res.getRoomId(), res);
        }
    }
}