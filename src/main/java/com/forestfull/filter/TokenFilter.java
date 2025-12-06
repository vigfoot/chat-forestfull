package com.forestfull.filter;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.forestfull.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 이미 인증 되어있으면 바로 진행
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie[] cookies = request.getCookies();
        String jwt = null;
        if (cookies != null) {
            Optional<Cookie> opt = Arrays.stream(cookies)
                    .filter(c -> "JWT".equals(c.getName()))
                    .findFirst();
            if (opt.isPresent()) jwt = opt.get().getValue();
        }

        if (jwt != null) {
            try {
                // Access Token 검증
                DecodedJWT decoded = jwtUtil.verifyToken(jwt);
                String username = decoded.getSubject();
                List<String> roles = decoded.getClaim("roles").asList(String.class);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                        );

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                filterChain.doFilter(request, response);
                return;

            } catch (TokenExpiredException tex) {
                // AccessToken 만료 → Refresh 로 재발급 시도
                // (다른 JWTVerificationException은 무시하고 익명으로 진행)
                handleExpiredAccessToken(request, response, filterChain);
                return;
            } catch (Exception ex) {
                // 검증 실패(변조 등) -> 인증 없이 진행(또는 401으로 단호히 막을수도 있음)
                filterChain.doFilter(request, response);
                return;
            }
        }

        // JWT 쿠키 없으면 그냥 진행 (익명)
        filterChain.doFilter(request, response);
    }

    private void handleExpiredAccessToken(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain filterChain) throws IOException, ServletException {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Cookie> optRefresh = Arrays.stream(cookies)
                .filter(c -> "REFRESH".equals(c.getName()))
                .findFirst();

        if (optRefresh.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = optRefresh.get().getValue();

        // 1) refresh token에서 username 추출(검증 포함)
        String username = refreshUtil.getUsername(refreshToken);
        if (username == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2) 저장된 refresh token과 일치하는지 확인
        String stored = refreshUtil.getToken(username);
        if (stored == null || !stored.equals(refreshToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3) DB에서 roles 조회 (UserDetailsService)
        var userDetails = userDetailsService.loadUserByUsername(username);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        // 4) 새 Access Token 생성
        String newAccess = jwtUtil.generateToken(username, roles);

        // 5) Set cookie: JWT (HttpOnly) + JWT_PAYLOAD (non-HttpOnly)
        ResponseCookie jwtCookie = ResponseCookie.from("JWT", newAccess)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(JwtUtil.getExpireMillis() / 1000)
                .sameSite("None")
                .build();

        String[] parts = newAccess.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";

        ResponseCookie payloadCookie = ResponseCookie.from("JWT_PAYLOAD", payload)
                .httpOnly(false)
                .secure(true)
                .path("/")
                .maxAge(JwtUtil.getExpireMillis() / 1000)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", jwtCookie.toString());
        response.addHeader("Set-Cookie", payloadCookie.toString());

        // 6) SecurityContext 설정
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                );
        auth.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 7) 요청 계속 진행
        filterChain.doFilter(request, response);
    }
}
