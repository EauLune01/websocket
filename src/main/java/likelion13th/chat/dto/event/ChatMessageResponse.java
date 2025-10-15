package likelion13th.chat.dto.event;

import likelion13th.chat.domain.ChatMessage;
import likelion13th.chat.domain.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private String roomId;
    private String senderUid;
    private String senderName;
    private String content;
    private MessageStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static ChatMessageResponse from(ChatMessage entity) {
        return ChatMessageResponse.builder()
                .id(entity.getId())
                .roomId(entity.getRoomId())
                .senderUid(entity.getSenderUid())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ChatMessageResponse withSenderName(String name) {
        this.senderName = name;
        return this;
    }
}