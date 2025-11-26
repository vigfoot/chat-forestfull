package com.forestfull.config;

import com.forestfull.chat.ChatMessageDTO;
import com.forestfull.chat.ChatService;
import com.forestfull.config.jwt.TikTokTokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RoomWebSocketHandler implements WebSocketHandler {

    private final TikTokTokenValidator validator;
    private final RoomSessionRegistry registry;
    private final ChatService chatService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        String token = extractParam(query, "token");
        String roomIdStr = extractParam(query, "room");

        if (roomIdStr.isEmpty()) return session.close();
        Long roomId = Long.parseLong(roomIdStr);

        return validator.validate(token)
                .flatMap(memberId -> {
                    registry.addSession(roomIdStr, memberId, session);

                    // receive에서 메시지를 수신
                    Flux<WebSocketMessage> messages = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .flatMap(payload -> {
                                // DB 저장
                                ChatMessageDTO.Message message = new ChatMessageDTO.Message();
                                message.setRoomId(roomId);
                                message.setMemberId(Long.parseLong(memberId));
                                message.setMessage(payload);
                                message.setSentAt(LocalDateTime.now());
                                message.setCreatedBy(memberId);
                                message.setUpdatedBy(memberId);
                                chatService.saveMessage(message);

                                // 브로드캐스트용 Flux<WebSocketMessage> 생성
                                return Flux.fromIterable(registry.getRoomSessions(roomIdStr).entrySet())
                                        .filter(e -> !e.getKey().equals(memberId)) // 자기 자신 제외 가능
                                        .map(e -> e.getValue().getSession().textMessage("[" + memberId + "] " + payload));
                            });

                    // session.send()에는 Flux<WebSocketMessage>만 넣는다
                    return session.send(messages)
                            .doFinally(sig -> registry.removeSession(roomIdStr, memberId));
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