package likelion13th.chat.config;

import likelion13th.chat.service.ActiveUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StompEventListener {

    private final ActiveUserService activeUserService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // STOMP 세션에서 roomId와 userUid를 가져옵니다 (연결 시 저장했다고 가정)
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            String roomId = (String) sessionAttributes.get("roomId");
            String userUid = (String) sessionAttributes.get("userUid");

            if (roomId != null && userUid != null) {
                activeUserService.userLeft(roomId, userUid);
            }
        }
    }
}
