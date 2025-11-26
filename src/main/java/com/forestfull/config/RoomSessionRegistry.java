package com.forestfull.config;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * com.forestfull.config
 *
 * @author vigfoot
 * @version 2025-11-27
 */
@Component
public class RoomSessionRegistry {

    public record SessionInfo(WebSocketSession session, Instant lastActive) {}

    private final Map<String, Map<String, SessionInfo>> rooms = new ConcurrentHashMap<>();

    public void addSession(String roomId, String userId, WebSocketSession session) {
        rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(userId, new SessionInfo(session, Instant.now()));
    }

    public void removeSession(String roomId, String userId) {
        Map<String, SessionInfo> room = rooms.get(roomId);
        if (room != null) room.remove(userId);
    }

    public Map<String, SessionInfo> getRoomSessions(String roomId) {
        return rooms.getOrDefault(roomId, Map.of());
    }

    public void cleanupExpiredSessions() {
        Instant now = Instant.now();
        rooms.values().forEach(room -> room.entrySet().removeIf(e ->
                now.minusSeconds(300).isAfter(e.getValue().lastActive())
        ));
    }
}