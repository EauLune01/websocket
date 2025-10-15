package likelion13th.chat.repository;

import likelion13th.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByRoomIdOrderByIdDesc(String roomId);
    List<ChatMessage> findByRoomId(String roomId);
}
