package likelion13th.chat.service;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveUserService {
    /**
     * 채팅방별 접속자 목록을 저장하는 메모리 기반 자료구조
     * Key (String) → roomId (채팅방 ID)
     * Value (Set<String>) → 해당 방에 접속 중인 사용자들의 UID 집합
     */
    private final ConcurrentHashMap<String, Set<String>> activeUsersByRoom = new ConcurrentHashMap<>();

    /**
     * 사용자가 채팅방에 입장할 때 호출되는 메서드
     * 해당 roomId의 Set(접속자 목록)에 userUid를 추가
     * 없으면 새로운 Set을 생성해서 추가
     */
    public void userJoined(String roomId, String userUid) {
        activeUsersByRoom.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userUid);
    }

    /**
     * 사용자가 채팅방에서 나갈 때 호출
     * 해당 roomId의 Set에서 userUid를 제거
     */
    public void userLeft(String roomId, String userUid) {
        if (activeUsersByRoom.containsKey(roomId)) {
            activeUsersByRoom.get(roomId).remove(userUid);
        }
    }

    /**
     * 특정 채팅방에 현재 접속 중인 사용자 목록을 반환
     * 방이 없으면 빈 Set(비어 있는 목록)을 반환
     * */
    public Set<String> getUsersInRoom(String roomId) {
        return activeUsersByRoom.getOrDefault(roomId, ConcurrentHashMap.newKeySet());
    }
}
