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

        /**
         * StompHeaderAccessor: STOMP 메시지의 헤더를 다루는 도우미 클래스
         * event 안에는 끊긴 세션의 메시지 정보가 들어있음
         * wrap()을 통해 그 정보를 STOMP 형태로 쉽게 읽을 수 있도록 감싸주기
         * */
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // STOMP 세션에서 roomId와 userUid를 가져옵니다 (연결 시 저장했다고 가정)

        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes(); //연결 세션에 저장된 속성(Attribute)들을 가져옵
        //세션이 존재할 때, 저장된 roomId와 userUid를 꺼냄 (어떤 사용자가 어떤 방에서 나갔는지 확인)
        if (sessionAttributes != null) {
            String roomId = (String) sessionAttributes.get("roomId");
            String userUid = (String) sessionAttributes.get("userUid");
            //두 값이 모두 유효하면 activeUserService의 userLeft() 메서드를 호출
            if (roomId != null && userUid != null) {
                activeUserService.userLeft(roomId, userUid);
            }
        }
    }
}
