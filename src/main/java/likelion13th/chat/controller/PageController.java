package likelion13th.chat.controller;

import likelion13th.chat.domain.User;
import likelion13th.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@Controller
@RequiredArgsConstructor
public class PageController {

    private final UserRepository userRepository;
    /**
     * 인덱스 페이지: 사용자 선택
     * 사용자 목록을 불러와 index.html에 전달
     * DB에서 모든 사용자(UserRepository.findAll())를 조회하고 Model에 담아 뷰로 전달
     * 뷰 엔진(Thymeleaf)이 templates/index.html을 렌더링하여 사용자 선택 화면을 표시
     */
    @GetMapping({"/", "/index"})
    public String index(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "index"; // templates/index.html
    }
    // 채팅방 입장 페이지(chat.html) 렌더링
    @GetMapping("/chat")
    public String chat(@RequestParam("userUid") String userUid,
                       @RequestParam("room") String roomUidPair,
                       Model model) {

        // URL 파라미터로 들어온 userUid를 기반으로 현재 로그인 사용자를 조회
        User me = userRepository.findByUid(userUid)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userUid));

        // roomUidPair 파싱
        // 예: "115f20f9-ef52-44a0-827c-2388a00605e2__aa8c1ac7-2e76-4950-a8ba-37c5d683b907"
        String[] uids = roomUidPair.split("__");
        if (uids.length != 2)
            throw new IllegalArgumentException("invalid room format: " + roomUidPair);

        // 두 UID를 실제 사용자 이름으로 변환하고, "room: A ↔ B" 형태의 방 제목(roomLabel)을 만들기
        User userA = userRepository.findByUid(uids[0])
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + uids[0]));
        User userB = userRepository.findByUid(uids[1])
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + uids[1]));
        String roomLabel = "room: " + userA.getDisplayName() + " ↔ " + userB.getDisplayName();

        // 모델 속성(용자 정보, 방 정보, 방 제목)을 전달 후 뷰 렌더링
        model.addAttribute("userUid", me.getUid());
        model.addAttribute("displayName", me.getDisplayName());
        model.addAttribute("room", roomUidPair);
        model.addAttribute("roomLabel", roomLabel);
        return "chat";
    }
}


