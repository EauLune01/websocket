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
     */
    @GetMapping({"/", "/index"})
    public String index(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "index"; // templates/index.html
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("userUid") String userUid,
                       @RequestParam("room") String roomUidPair,
                       Model model) {

        // 현재 사용자 조회
        User me = userRepository.findByUid(userUid)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userUid));

        // roomUidPair 예: "115f20f9-ef52-44a0-827c-2388a00605e2__aa8c1ac7-2e76-4950-a8ba-37c5d683b907"
        String[] uids = roomUidPair.split("__");
        if (uids.length != 2)
            throw new IllegalArgumentException("invalid room format: " + roomUidPair);

        // UUID → 이름으로 변환
        User userA = userRepository.findByUid(uids[0])
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + uids[0]));
        User userB = userRepository.findByUid(uids[1])
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + uids[1]));

        // 사람 이름 기반 라벨 생성
        String roomLabel = "room: " + userA.getDisplayName() + " ↔ " + userB.getDisplayName();

        model.addAttribute("userUid", me.getUid());
        model.addAttribute("displayName", me.getDisplayName());
        model.addAttribute("room", roomUidPair);
        model.addAttribute("roomLabel", roomLabel);
        return "chat";
    }
}


