package likelion13th.chat.domain;
import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatLog {
    private UUID id;
    private String senderKey;
    private String senderDisplayName;
    private String content;
    private long timestamp;
}
