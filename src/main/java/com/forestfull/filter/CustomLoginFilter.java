package com.forestfull.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forestfull.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    @Value("${spring.config.activate.on-profile}")
    private String onProfile;
    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshJwtUtil;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {

        try {
            Map<String, String> requestBody = new ObjectMapper().readValue(request.getInputStream(), Map.class);
            String username = requestBody.get("username");
            String password = requestBody.get("password");

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

            return this.getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }@Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException {

        String username = authResult.getName();
        List<String> roles = authResult.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtUtil.generateToken(username, roles);
        String refreshToken = refreshJwtUtil.generateToken(username, roles);

        refreshJwtUtil.save(username, refreshToken);

        // Access Token 쿠키 생성 (기존 코드)
        Cookie accessCookie = new Cookie("JWT", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure("prod".equals(onProfile));
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60);
        accessCookie.setAttribute("SameSite", "None");
        response.addCookie(accessCookie);


// ⭐ UI 접근용 Payload 쿠키 생성 추가 (새 코드)
        String[] parts = accessToken.split("\\.");
        String payload = parts[1]; // Base64 Payload

        Cookie payloadCookie = new Cookie("JWT_PAYLOAD", payload);
        payloadCookie.setHttpOnly(false); // JS에서 접근 가능!
        payloadCookie.setSecure("prod".equals(onProfile));
        payloadCookie.setPath("/");
        payloadCookie.setMaxAge(60 * 60);
        payloadCookie.setAttribute("SameSite", "None");
        response.addCookie(payloadCookie);


// Refresh Token 쿠키 생성 (기존 코드)
        Cookie refreshCookie = new Cookie("REFRESH", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure("prod".equals(onProfile));
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(60 * 60 * 24 * 7);
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);

        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Login successful\"}");
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Login failed\"}");
    }
}