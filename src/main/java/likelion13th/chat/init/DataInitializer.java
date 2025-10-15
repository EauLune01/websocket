package likelion13th.chat.init;

import jakarta.annotation.PostConstruct;
import likelion13th.chat.domain.User;
import likelion13th.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    private final UserRepository userRepository;
    @PostConstruct
    public void init() {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder().key("Choi").displayName("최성민").build());
            userRepository.save(User.builder().key("Choi").displayName("최예나").build());
            userRepository.save(User.builder().key("Kim").displayName("김도영").build());
            userRepository.save(User.builder().key("Cho").displayName("조이현").build());
        }
    }
}
