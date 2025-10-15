package likelion13th.chat.service;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveUserService {
    // Key: roomId, Value: Set of userUids in that room
    private final ConcurrentHashMap<String, Set<String>> activeUsersByRoom = new ConcurrentHashMap<>();

    /** 방에 사용자가 입장 */
    public void userJoined(String roomId, String userUid) {
        activeUsersByRoom.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userUid);
    }

    /** 방에서 사용자가 퇴장 */
    public void userLeft(String roomId, String userUid) {
        if (activeUsersByRoom.containsKey(roomId)) {
            activeUsersByRoom.get(roomId).remove(userUid);
        }
    }

    /** 방에 있는 사용자 목록 조회 */
    public Set<String> getUsersInRoom(String roomId) {
        return activeUsersByRoom.getOrDefault(roomId, ConcurrentHashMap.newKeySet());
    }
}
