package likelion13th.chat.dto.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsEvent {
    /** 예: "deleted", "read", "typing" 등 */
    private String type;
    /** 대상 메시지 ID (삭제/수정 등에서 사용) */
    private Long messageId;

    private String senderUid;

    // 편의 팩토리
    public static WsEvent deleted(Long messageId) {
        return WsEvent.builder().type("deleted").messageId(messageId).build();
    }
}

