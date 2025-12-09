package com.forestfull.chat.config;

import com.forestfull.common.token.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return false;

        final HttpServletRequest httpRequest = servletRequest.getServletRequest();
        final Cookie[] cookies = httpRequest.getCookies();

        if (cookies == null) return false;

        final Optional<Cookie> jwtCookie = Arrays.stream(cookies)
                .filter(c -> JwtUtil.TOKEN_TYPE.JWT.name().equals(c.getName()))
                .findFirst();

        if (jwtCookie.isEmpty()) return false;

        try {
            attributes.put(JwtUtil.TOKEN_TYPE.JWT.name(), jwtCookie.get().getValue());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception == null) {
            log.info("[WebSocket] Handshake completed successfully.");
        } else {
            log.error("[WebSocket] Handshake failed: {}", exception.getMessage());
        }
    }
}