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
        // 이미 SecurityContext에 인증 정보가 있으면 바로 진행
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final Cookie[] cookies = request.getCookies();

        // JWT 확인
        if (cookies == null) {
            filterChain.doFilter(request, response);
            return;
        }

        final Optional<Cookie> optionalJWT = Arrays.stream(cookies)
                .filter(c -> "JWT".equals(c.getName()))
                .findFirst();

        if (optionalJWT.isPresent()) {
            final DecodedJWT decodedJWT = jwtUtil.verifyToken(optionalJWT.get().getValue());
            setAuthentication(decodedJWT, request);

        } else {
            Arrays.stream(cookies)
                    .filter(c -> "REFRESH".equals(c.getName()))
                    .findFirst().ifPresent(cookie -> {
                        String oldRefreshToken = cookie.getValue();
                        String name = refreshTokenUtil.validateAndGetUsername(oldRefreshToken);

                        if (name == null) return;

                        // 기존 Refresh 무효화
                        refreshTokenUtil.deleteTokenByUsername(name);

                        // 새 Refresh 토큰 생성 + DB 저장
                        String newRefreshToken = refreshTokenUtil.generateToken(name);
                        Cookie refreshCookie = new Cookie("REFRESH", newRefreshToken);
                        refreshCookie.setHttpOnly(true);
                        refreshCookie.setSecure("prod".equals(onProfile));
                        refreshCookie.setPath("/");
                        refreshCookie.setMaxAge((int) (JwtUtil.refreshExpireMillis / 1000));
                        refreshCookie.setAttribute("SameSite", "prod".equals(onProfile) ? "None" : "Lax");
                        response.addCookie(refreshCookie);

                        // 기존 로직: Access Token 새로 발급
                        DecodedJWT decodedJWT = refreshTokenUtil.verify(newRefreshToken);
                        List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
                        String newAccessToken = jwtUtil.generateToken(name, roles);

                        Cookie newJwtCookie = new Cookie("JWT", newAccessToken);
                        newJwtCookie.setHttpOnly(true);
                        newJwtCookie.setSecure("prod".equals(onProfile));
                        newJwtCookie.setPath("/");
                        newJwtCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
                        newJwtCookie.setAttribute("SameSite", "prod".equals(onProfile) ? "None" : "Lax");
                        response.addCookie(newJwtCookie);

                        // 페이로드 쿠키
                        String[] parts = newAccessToken.split("\\.");
                        String payload = parts.length > 1 ? parts[1] : "";
                        Cookie payloadCookie = new Cookie("JWT_PAYLOAD", payload);
                        payloadCookie.setHttpOnly(false);
                        payloadCookie.setSecure("prod".equals(onProfile));
                        payloadCookie.setPath("/");
                        payloadCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
                        payloadCookie.setAttribute("SameSite", "Lax");
                        response.addCookie(payloadCookie);

                        setAuthentication(jwtUtil.verifyToken(newAccessToken), request);
                    });
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(DecodedJWT jwt, HttpServletRequest request) {
        final String name = jwt.getSubject();
        final List<String> roles = jwt.getClaim("roles").asList(String.class);

        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(name, null, roles.stream().map(SimpleGrantedAuthority::new).toList());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}