package com.forestfull.common.token;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshJwtUtil;
    private final UserDetailsService userDetailsService; // DB에서 roles 조회용

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String accessToken = getCookieValue(request, "JWT");

        if (accessToken != null) {
            try {
                // Access Token 정상일 경우 바로 인증 세팅
                DecodedJWT decoded = jwtUtil.verifyToken(accessToken);
                setAuthenticationFromDecoded(decoded);
                chain.doFilter(request, response);
                return;

            } catch (JWTVerificationException ex) {
                // 토큰 만료 또는 서명 불일치 등 -> 재발급 로직으로 진행
                log.debug("Access token invalid/expired: {}", ex.getMessage());
            }
        }

        // Access가 없거나 만료된 경우 Refresh 처리 시도
        String refreshToken = getCookieValue(request, "REFRESH");
        if (refreshToken != null) {
            // 1) 토큰 자체 검증 및 username 추출
            String username = refreshJwtUtil.getUsername(refreshToken);
            if (username != null) {
                // 2) DB에 저장된(유효한) 토큰과 일치하는지 확인
                String stored = refreshJwtUtil.getToken(username);
                if (stored != null && stored.equals(refreshToken)) {
                    // 3) DB에서 사용자 정보(roles) 조회
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        List<String> roles = userDetails.getAuthorities().stream()
                                .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")) // Jwt에 roles 필드는 비-ROLE형식으로 저장하고 싶다면 변환
                                .collect(Collectors.toList());

                        // 4) 새 Access Token 발급 (roles 로 전달)
                        String newAccess = jwtUtil.generateToken(username, roles);

                        // 5) 쿠키로 Access + Payload 재설정
                        addAccessCookies(newAccess, response, request.isSecure());

                        // 6) SecurityContext 설정
                        DecodedJWT decodedNew = jwtUtil.verifyToken(newAccess);
                        setAuthenticationFromDecoded(decodedNew);

                        log.info("Access token refreshed for user: {}", username);
                        chain.doFilter(request, response);
                        return;

                    } catch (Exception e) {
                        log.warn("Failed to refresh access token (user load or token create): {}", e.getMessage());
                        // 실패 시 그냥 인증 없이 진행 -> 결국 401을 내거나 인증이 필요한 엔드포인트에서 막힘
                    }
                } else {
                    log.debug("Refresh token mismatch with DB for user: {}", username);
                }
            } else {
                log.debug("Refresh token invalid/expired for token: {}", refreshToken);
            }
        }

        // 아무 것도 안 됐으면 인증 없이 진행
        chain.doFilter(request, response);
    }

    private void setAuthenticationFromDecoded(DecodedJWT decoded) {
        String username = decoded.getSubject();
        List<String> roles = decoded.getClaim("roles").asList(String.class);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void addAccessCookies(String token, HttpServletResponse response, boolean secure) {
        // Access cookie (HttpOnly)
        Cookie accessCookie = new Cookie("JWT", token);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(secure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (JwtUtil.getExpireMillis() / 1000));
        accessCookie.setAttribute("SameSite", "None");
        response.addCookie(accessCookie);

        // Payload cookie (JS에서 읽는 용도)
        String[] parts = token.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";
        Cookie payloadCookie = new Cookie("JWT_PAYLOAD", payload);
        payloadCookie.setHttpOnly(false);
        payloadCookie.setSecure(secure);
        payloadCookie.setPath("/");
        payloadCookie.setMaxAge((int) (JwtUtil.getExpireMillis() / 1000));
        payloadCookie.setAttribute("SameSite", "None");
        response.addCookie(payloadCookie);
    }
}
