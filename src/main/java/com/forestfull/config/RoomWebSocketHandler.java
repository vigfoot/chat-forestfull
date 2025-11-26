package com.forestfull.config;

import com.forestfull.config.jwt.TikTokTokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RoomWebSocketHandler implements WebSocketHandler {

    private final TikTokTokenValidator validator;
    private final RoomSessionRegistry registry;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery(); // ?token=...&room=...
        String token = extractParam(query, "token");
        String roomId = extractParam(query, "room");

        return validator.validate(token)
                .flatMap(userId -> {
                    registry.addSession(roomId, userId, session);

                    Flux<WebSocketMessage> messages = session.receive()
                            .flatMap(msg -> {
                                String payload = msg.getPayloadAsText();
                                return Flux.fromIterable(registry.getRoomSessions(roomId).entrySet())
                                        .filter(e -> e.getValue().session().isOpen())
                                        .map(e -> e.getValue().session().textMessage("[" + userId + "] " + payload));
                            });

                    return session.send(messages)
                            .doFinally(sig -> registry.removeSession(roomId, userId));
                });
    }

    private String extractParam(String query, String key) {
        if (query == null) return "";
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return "";
    }
}