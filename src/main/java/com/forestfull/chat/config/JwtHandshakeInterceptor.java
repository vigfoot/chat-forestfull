package com.forestfull.chat.config;

import com.auth0.jwt.interfaces.DecodedJWT;
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

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        Cookie[] cookies = httpRequest.getCookies();

        if (cookies == null) {
            return false;
        }

        Optional<Cookie> jwtCookie = Arrays.stream(cookies)
                .filter(c -> JwtUtil.TOKEN_TYPE.JWT.name().equals(c.getName()))
                .findFirst();

        if (jwtCookie.isEmpty()) {
            return false;
        }

        try {
            DecodedJWT decodedJWT = jwtUtil.verifyToken(jwtCookie.get().getValue());
            attributes.put("username", decodedJWT.getSubject());

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