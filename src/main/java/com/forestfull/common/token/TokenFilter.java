package com.forestfull.common.token;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie jwtCookie = Arrays.stream(cookies)
                .filter(c -> "JWT".equals(c.getName()))
                .findFirst()
                .orElse(null);

        if (jwtCookie != null) {
            String accessToken = jwtCookie.getValue();
            try {
                DecodedJWT decodedJWT = jwtUtil.verifyToken(accessToken);
                setAuthentication(decodedJWT, request);
                filterChain.doFilter(request, response);
                return;
            } catch (JWTVerificationException ex) {
                // Access Token 만료 또는 무효 → Refresh Token 처리
            }
        }

        // Refresh Token 확인
        Cookie refreshCookie = Arrays.stream(cookies)
                .filter(c -> "REFRESH".equals(c.getName()))
                .findFirst()
                .orElse(null);

        if (refreshCookie != null) {
            String refreshToken = refreshCookie.getValue();
            // validateAndGetUsername 안에서 DB 저장 여부까지 확인
            String username = refreshUtil.validateAndGetUsername(refreshToken);

            if (username != null) {
                // DecodedJWT로 roles 추출
                DecodedJWT decodedJWT = refreshUtil.verify(refreshToken);
                List<String> roles = decodedJWT.getClaim("roles").asList(String.class);

                // 새로운 Access Token 발급
                String newAccessToken = jwtUtil.generateToken(username, roles);

                Cookie newCookie = new Cookie("JWT", newAccessToken);
                newCookie.setPath("/");
                newCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
                newCookie.setSecure(true);
                newCookie.setHttpOnly(false); // 프론트 접근 가능
                newCookie.setAttribute("SameSite", "None");
                response.addCookie(newCookie);

                setAuthentication(jwtUtil.verifyToken(newAccessToken), request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(DecodedJWT jwt, HttpServletRequest request) {
        String username = jwt.getSubject();
        List<String> roles = jwt.getClaim("roles").asList(String.class);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        roles.stream().map(SimpleGrantedAuthority::new).toList()
                );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}