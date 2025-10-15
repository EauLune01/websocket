package likelion13th.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomId;   // 채팅방 구분 식별자

    @Column(nullable = false)
    private String senderUid; // 송신자의 고유 식별자

    @Column(nullable = false)
    private String content; // 메시지 본문

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status; //메시지의 상태 (SENT, DELIVERED, READ)

    @CreationTimestamp
    private Instant createdAt; //엔티티가 처음 저장될 때의 시각을 자동 기록

    @UpdateTimestamp
    private Instant updatedAt; //엔티티가 수정(update) 될 때마다 자동으로 현재 시간을 업데이트

    @Version
    private Long version; //JPA의 낙관적 락(Optimistic Locking) 을 위한 버전 필드
}
