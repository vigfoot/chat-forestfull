package com.forestfull.common.token;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CookieUtil {

    @Value("${spring.config.activate.on-profile}")
    private String onProfile;

    public void addAccessToken(HttpServletResponse response, String token) {
        final ResponseCookie cookie = ResponseCookie.from(JwtUtil.TOKEN_TYPE.JWT.name(), token)
                .httpOnly(true)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("prod".equals(onProfile) ? "None" : "Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addPayload(HttpServletResponse response, String token) {
        final String[] parts = token.split("\\.");
        final String payload = parts.length > 1 ? parts[1] : "";
        ResponseCookie cookie = ResponseCookie.from(JwtUtil.TOKEN_TYPE.JWT_PAYLOAD.name(), payload)
                .httpOnly(false)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshToken(HttpServletResponse response, String token) {
        final ResponseCookie cookie = ResponseCookie.from(JwtUtil.TOKEN_TYPE.REFRESH.name(), token)
                .httpOnly(true)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.refreshExpireMillis / 1000)
                .sameSite("prod".equals(onProfile) ? "None" : "Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void deleteAuthCookies(HttpServletResponse response) {
        Arrays.stream(JwtUtil.TOKEN_TYPE.values())
                .map(Enum::name)
                .forEach(name -> {
                    Cookie c = new Cookie(name, null);
                    c.setHttpOnly(JwtUtil.TOKEN_TYPE.JWT.name().equals(name) || JwtUtil.TOKEN_TYPE.REFRESH.name().equals(name));
                    c.setSecure(true);
                    c.setPath("/");
                    c.setMaxAge(0);
                    c.setAttribute("SameSite", "None");
                    response.addCookie(c);
                });
    }
}