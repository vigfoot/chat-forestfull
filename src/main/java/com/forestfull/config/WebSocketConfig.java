package com.forestfull.config;

import com.forestfull.config.common.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final JwtProvider jwtProvider;

    @Bean
    WebSocketHandler webSocketHandler() {
        return session -> {
            Function<String, String> extractTokenFunction = uri -> uri.contains("token=") ? uri.substring(uri.indexOf("token=") + 6) : null;

            // 토큰 추출
            String uri = session.getHandshakeInfo().getUri().toString();
            String token = extractTokenFunction.apply(uri);

            if (token == null || !jwtProvider.validateToken(token)) {
                return session.close();
            }

            String userId = jwtProvider.getUserId(token);

            return session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(msg -> System.out.println("[" + userId + "] " + msg))
                    .flatMap(msg -> session.send(Mono.just(session.textMessage(userId + ": " + msg))))
                    .then();
        };
    }

    @Bean
    SimpleUrlHandlerMapping handlerMapping(WebSocketHandler webSocketHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/chat", webSocketHandler);

        return new SimpleUrlHandlerMapping(map, 1);
    }

    @Bean
    WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
