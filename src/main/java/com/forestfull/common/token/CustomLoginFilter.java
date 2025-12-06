package com.forestfull.common.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    @Value("${spring.config.activate.on-profile}")
    private String onProfile;
    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshJwtUtil;
    private final ObjectMapper objectMapper;

    public CustomLoginFilter(AuthenticationConfiguration authConfig, JwtUtil jwtUtil, JwtUtil.Refresh refreshJwtUtil, ObjectMapper objectMapper) throws Exception {
        super(authConfig.getAuthenticationManager());
        this.jwtUtil = jwtUtil;
        this.refreshJwtUtil = refreshJwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            Map<String, String> body = objectMapper.readValue(request.getInputStream(), Map.class);
            String username = body.get("username");
            String password = body.get("password");
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
            return this.getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {
        String username = authResult.getName();
        List<String> roles = authResult.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        // Access / Refresh Token 발급
        String accessToken = jwtUtil.generateToken(username, roles);
        String refreshToken = refreshJwtUtil.generateToken(username);

        refreshJwtUtil.save(username, refreshToken); // DB 저장

        // JWT 쿠키 (HttpOnly)
        Cookie accessCookie = new Cookie("JWT", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure("prod".equals(onProfile));
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
        accessCookie.setAttribute("SameSite", "None");
        response.addCookie(accessCookie);

        // JWT_PAYLOAD 쿠키 (JS 접근 가능)
        String[] parts = accessToken.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";
        Cookie payloadCookie = new Cookie("JWT_PAYLOAD", payload);
        payloadCookie.setHttpOnly(false);
        payloadCookie.setSecure("prod".equals(onProfile));
        payloadCookie.setPath("/");
        payloadCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
        payloadCookie.setAttribute("SameSite", "None");
        response.addCookie(payloadCookie);

        // Refresh Token 쿠키 (HttpOnly)
        Cookie refreshCookie = new Cookie("REFRESH", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure("prod".equals(onProfile));
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (JwtUtil.refreshExpireMillis / 1000));
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);

        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Login successful\"}");
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Login failed\"}");
    }
}
