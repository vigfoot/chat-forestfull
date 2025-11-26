package com.forestfull.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.*;

@Component
public class RoomSessionRegistry {

    private final Map<String, Map<String, SessionWrapper>> rooms = new HashMap<>();

    public void addSession(String roomId, String userId, WebSocketSession session) {
        rooms.computeIfAbsent(roomId, k -> new HashMap<>())
                .put(userId, new SessionWrapper(session));
    }

    public void removeSession(String roomId, String userId) {
        Map<String, SessionWrapper> room = rooms.get(roomId);
        if (room != null) {
            room.remove(userId);
            if (room.isEmpty()) rooms.remove(roomId);
        }
    }

    public Map<String, SessionWrapper> getRoomSessions(String roomId) {
        return rooms.getOrDefault(roomId, Collections.emptyMap());
    }

    @AllArgsConstructor
    @Getter
    public static class SessionWrapper {
        private final WebSocketSession session;
    }
}
