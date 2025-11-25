package com.forestfull.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {

    @Bean
    WebSocketHandler webSocketHandler() {
        return session -> session.send(
                session.receive()
                        .map(msg -> session.textMessage("🔁 Echo: " + msg.getPayloadAsText()))
        );
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
