package com.forestfull.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Collections;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final ChatWebSocketHandler handler;
    // 1) WebSocket 경로 매핑
    @Bean
    public HandlerMapping webSocketMapping() {
        return new SimpleUrlHandlerMapping() {{
            setOrder(10);
            setUrlMap(Collections.singletonMap("/ws/chat", handler));
        }};
    }

    // 2) WebFlux WebSocket 요청 처리 어댑터
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}