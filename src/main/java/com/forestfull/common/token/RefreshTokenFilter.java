package com.forestfull.common.token;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenFilter extends OncePerRequestFilter {

    @Value("${spring.config.activate.on-profile}")
    private String onProfile;

    private final JwtUtil accessTokenUtil;
    private final JwtUtil.Refresh refreshTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 이미 인증 정보가 있으면 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> optionalRefresh =
                    Arrays.stream(cookies)
                            .filter(c -> "REFRESH".equals(c.getName()))
                            .findFirst();

            if (optionalRefresh.isPresent()) {
                try {
                    String refreshToken = optionalRefresh.get().getValue();

                    // refresh token 검증 + DB 확인
                    String username = refreshTokenUtil.validateAndGetUsername(refreshToken);

                    if (username != null) {
                        // 토큰에서 roles 추출
                        DecodedJWT decodedJWT = refreshTokenUtil.verify(refreshToken);
                        List<String> roles = decodedJWT.getClaim("roles").asList(String.class);

                        // 새 Access Token 발급
                        String newAccessToken = accessTokenUtil.generateToken(username, roles);

                        Cookie newJwtCookie = new Cookie("JWT", newAccessToken);
                        newJwtCookie.setHttpOnly(true);
                        newJwtCookie.setSecure("prod".equals(onProfile));
                        newJwtCookie.setPath("/");
                        newJwtCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
                        newJwtCookie.setAttribute("SameSite", "None");
                        response.addCookie(newJwtCookie);

                        // 인증 세션 구성
                        SecurityContextHolder.getContext().setAuthentication(
                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        roles.stream().map(SimpleGrantedAuthority::new).toList()
                                )
                        );
                    }

                } catch (Exception e) {
                    // Refresh Token 오류 → 로그인 필요
                    logger.warn("Invalid or expired refresh token", e);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}