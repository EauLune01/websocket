package likelion13th.chat.domain;

import lombok.*;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_uid", columnNames = "uid"),
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_key",nullable = false)
    private String key;

    @Column(nullable = false, length = 50)
    private String displayName; // "최성민" | "김도영"

    /** 전역 고유 ID (서버 내부 식별/방ID 구성용) */
    @Column(name = "uid", nullable = false, length = 36, unique = true)
    private String uid;

    @PrePersist
    public void prePersist() {
        if (this.uid == null) {
            this.uid = UUID.randomUUID().toString();
        }
    }
}