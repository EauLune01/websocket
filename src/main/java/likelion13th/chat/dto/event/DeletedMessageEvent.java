package likelion13th.chat.dto.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeletedMessageEvent {
    private final Long messageId;
    private final String roomId;
}
