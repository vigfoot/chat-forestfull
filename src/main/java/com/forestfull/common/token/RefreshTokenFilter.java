package com.forestfull.common.token;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class RefreshTokenFilter extends OncePerRequestFilter {

    @Value("${spring.config.activate.on-profile}")
    private String onProfile;
    private final JwtUtil accessTokenUtil;
    private final JwtUtil.Refresh refreshTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> optionalRefresh =
                    Arrays.stream(cookies)
                            .filter(c -> c.getName().equals("REFRESH"))
                            .findFirst();

            if (optionalRefresh.isPresent()) {
                try {
                    String refreshToken = optionalRefresh.get().getValue();

                    // 저장소에서 refresh token 일치 여부 확인
                    String username = refreshTokenUtil.getUsername(refreshToken);
                    String savedRt = refreshTokenUtil.getToken(username);

                    if (savedRt != null && savedRt.equals(refreshToken)) {

                        DecodedJWT decodedJWT = refreshTokenUtil.verify(refreshToken);
                        String subject = decodedJWT.getSubject();
                        List<String> roles = decodedJWT.getClaim("roles").asList(String.class);

                        // 새 Access Token 발급
                        String newAccessToken =
                                accessTokenUtil.generateToken(subject, roles);

                        Cookie newJwtCookie = new Cookie("JWT", newAccessToken);
                        newJwtCookie.setHttpOnly(true);
                        newJwtCookie.setSecure("prod".equals(onProfile));
                        newJwtCookie.setPath("/");
                        newJwtCookie.setMaxAge((int) (JwtUtil.getExpireMillis() / 1000));
                        newJwtCookie.setAttribute("SameSite", "None");
                        response.addCookie(newJwtCookie);

                        // 인증 세션 구성
                        SecurityContextHolder.getContext().setAuthentication(
                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                        subject,
                                        null,
                                        roles.stream().map(SimpleGrantedAuthority::new).toList()
                                )
                        );
                    }

                } catch (Exception ignore) {
                    // Refresh Token 만료 → 로그인 필요
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}