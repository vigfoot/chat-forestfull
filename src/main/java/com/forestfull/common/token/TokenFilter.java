package com.forestfull.common.token;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {

    @Value("${spring.config.activate.on-profile}")
    private String onProfile;

    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Cookie[] cookies = request.getCookies();
        if (cookies != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            extractJWT(cookies).ifPresentOrElse(jwtCookie -> {
                authenticateAccessToken(jwtCookie, request);

            }, () -> {
                extractRefreshToken(cookies).ifPresent(refreshCookie -> {
                    handleRefreshToken(refreshCookie, request, response);
                });
            });
        }

        filterChain.doFilter(request, response);
    }

    /** ---------------------- JWT 인증 ---------------------- */
    private Optional<Cookie> extractJWT(Cookie[] cookies) {
        return Arrays.stream(cookies)
                .filter(c -> JwtUtil.TOKEN_TYPE.JWT.name().equals(c.getName()))
                .findFirst();
    }

    private void authenticateAccessToken(Cookie jwtCookie, HttpServletRequest request) {
        try {
            DecodedJWT decoded = jwtUtil.verifyToken(jwtCookie.getValue());
            setAuthentication(decoded, request);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }
    }

    /** ------------------- Refresh 인증 + 재발급 ------------------- */
    private Optional<Cookie> extractRefreshToken(Cookie[] cookies) {
        return Arrays.stream(cookies)
                .filter(c -> JwtUtil.TOKEN_TYPE.REFRESH.name().equals(c.getName()))
                .findFirst();
    }

    private void handleRefreshToken(Cookie refreshCookie,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {

        String oldRefreshToken = refreshCookie.getValue();
        String username = refreshTokenUtil.validateAndGetUsername(oldRefreshToken);
        if (username == null) return;

        refreshTokenUtil.deleteTokenByUsername(username);

        DecodedJWT oldDecoded = null;
        List<String> roles = null;
        try {
            oldDecoded = refreshTokenUtil.verify(oldRefreshToken);
            roles = oldDecoded.getClaim("roles").asList(String.class);
        } catch (Exception ignored) {}

        if (roles == null) roles = List.of("ROLE_USER");

        String newRefreshToken = refreshTokenUtil.generateToken(username, roles);
        addRefreshCookie(response, newRefreshToken);

        String newAccessToken = jwtUtil.generateToken(username, roles);
        addAccessAndPayloadCookies(response, newAccessToken);

        authenticateAccessToken(createCookie(JwtUtil.TOKEN_TYPE.JWT.name(), newAccessToken), request);
    }

    /** ------------------- Cookie Helper ------------------- */
    private void addRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = createCookie(JwtUtil.TOKEN_TYPE.REFRESH.name(), token);
        cookie.setMaxAge((int) (JwtUtil.refreshExpireMillis / 1000));
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", isProd() ? "None" : "Lax");
        response.addCookie(cookie);
    }

    private void addAccessAndPayloadCookies(HttpServletResponse response, String accessToken) {
        Cookie accessCookie = createCookie(JwtUtil.TOKEN_TYPE.JWT.name(), accessToken);
        accessCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
        accessCookie.setHttpOnly(true);
        accessCookie.setAttribute("SameSite", isProd() ? "None" : "Lax");
        response.addCookie(accessCookie);

        String[] parts = accessToken.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";
        Cookie payloadCookie = createCookie(JwtUtil.TOKEN_TYPE.JWT_PAYLOAD.name(), payload);
        payloadCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
        payloadCookie.setHttpOnly(false);
        payloadCookie.setAttribute("SameSite", "Lax");
        response.addCookie(payloadCookie);
    }

    private Cookie createCookie(String name, String value) {
        Cookie c = new Cookie(name, value);
        c.setPath("/");
        c.setSecure(isProd());
        return c;
    }

    private boolean isProd() {
        return "prod".equals(onProfile);
    }

    /** ------------------- SecurityContext 설정 ------------------- */
    private void setAuthentication(DecodedJWT jwt, HttpServletRequest request) {
        String username = jwt.getSubject();
        List<String> roles = jwt.getClaim("roles").asList(String.class);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        username, null,
                        roles.stream().map(SimpleGrantedAuthority::new).toList()
                );

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}