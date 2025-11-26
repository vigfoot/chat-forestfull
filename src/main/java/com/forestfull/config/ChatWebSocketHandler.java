package com.forestfull.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * com.forestfull.config
 *
 * @author vigfoot
 * @version 2025-11-27
 */

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 수신한 각 메시지에 대해 ReactiveSecurityContext에서 인증정보를 가져와서 처리
        Flux<WebSocketMessage> outgoing = session.receive()
                .flatMap(message ->
                        ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> {
                                    Authentication auth = ctx.getAuthentication();
                                    String username = (auth != null) ? auth.getName() : "anonymous";
                                    String payload = message.getPayloadAsText();
                                    return session.textMessage("[" + username + "] " + payload);
                                })
                                // 인증 컨텍스트가 없으면 anonymous 처리
                                .defaultIfEmpty(session.textMessage("[anonymous] " + message.getPayloadAsText()))
                );

        return session.send(outgoing);
    }
}