package likelion13th.chat.controller;

import jakarta.validation.Valid;
import likelion13th.chat.dto.command.*;
import likelion13th.chat.dto.event.ChatMessageResponse;
import likelion13th.chat.dto.event.DeletedMessageEvent;
import likelion13th.chat.dto.event.WsEvent;
import likelion13th.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
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
    private final SimpMessagingTemplate broker;

    // ===== REST: 채팅 내역 조회 =====
    @GetMapping("/api/rooms/{roomId}/messages")
    public List<ChatMessageResponse> history(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return service.fetchRecent(roomId, limit);
    }

    // ===== STOMP: 전송 =====
    @MessageMapping("/send")
    public void send(@Valid SendMessageCommand cmd) {
        ChatMessageResponse res = service.send(cmd);
        broker.convertAndSend("/topic/rooms/" + res.getRoomId(), res);
    }

    // ===== STOMP: 수정 =====
    @MessageMapping("/edit")
    public void edit(@Valid EditMessageCommand cmd) {
        ChatMessageResponse res = service.edit(cmd);
        broker.convertAndSend("/topic/rooms/" + res.getRoomId(), res);
    }

    // ===== STOMP: 삭제 =====
    @MessageMapping("/delete")
    public void delete(@Valid DeleteMessageCommand cmd) {
        DeletedMessageEvent ev = service.delete(cmd);
        broker.convertAndSend("/topic/rooms/" + ev.getRoomId(), WsEvent.deleted(ev.getMessageId()));
    }

    // ===== STOMP: 방 입장 (상대 메시지 읽음 처리) =====
    @MessageMapping("/enter")
    public void enter(@Valid EnterRoomCommand cmd) {
        List<ChatMessageResponse> changed = service.enter(cmd);
        for (ChatMessageResponse res : changed) {
            broker.convertAndSend("/topic/rooms/" + res.getRoomId(), res);
        }
    }
}