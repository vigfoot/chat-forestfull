package com.forestfull.common.token;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.forestfull.config.SecurityConfig;
import com.forestfull.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshTokenUtil;
    private final CookieUtil cookieUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityConfig.isPublicResources(request.getRequestURI()) || SecurityConfig.isAllowPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 🚨 수정: 쿠키 없으면 401 반환
            return;
        }

        boolean isTokenValidated = false; // 🚨 추가: Access Token 검증 성공 여부 플래그

        try {
            // 1. Access Token 검증 시도 및 인증 컨텍스트 설정
            Optional<Cookie> optionalCookie = Arrays.stream(cookies)
                    .filter(c -> JwtUtil.TOKEN_TYPE.JWT.name().equals(c.getName()))
                    .findFirst();

            if (optionalCookie.isEmpty())
                throw new JWTVerificationException("JWT cookie not found");

            optionalCookie
                    .map(cookie -> {
                        final DecodedJWT decodedJWT = jwtUtil.verifyToken(cookie.getValue());
                        final Long userId = Long.valueOf(decodedJWT.getSubject());

                        User user = User.builder()
                                .id(userId)
                                .name(decodedJWT.getClaim("username").asString())
                                .displayName(decodedJWT.getClaim("displayName").asString())
                                .profileImage(decodedJWT.getClaim("profileImage").asString())
                                .roles(decodedJWT.getClaim("roles").asString())
                                .build();

                        final UsernamePasswordAuthenticationToken auth
                                = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        return true;
                    });
            isTokenValidated = true;

        } catch (JWTVerificationException e) {
            // 2. Access Token 만료/위조 시, Refresh Token 확인 및 갱신 시도
            final Optional<Cookie> optionalCookie = Arrays.stream(cookies)
                    .filter(c -> JwtUtil.TOKEN_TYPE.REFRESH.name().equals(c.getName()))
                    .findFirst();

            if (optionalCookie.isPresent())
                isTokenValidated = handleRefreshToken(optionalCookie.get(), request, response);

            // 갱신 성공 시 isTokenValidated는 true가 됨
        }

        // 3. 필터 체인 진행 결정
        if (isTokenValidated) {
            filterChain.doFilter(request, response);
        } else {
            // Access Token도 없고, Refresh Token도 없거나 갱신에 실패한 경우
            // 🚨 수정: 401 응답 코드를 명시적으로 설정하고 체인 진행을 막음
            if (response.getStatus() == HttpServletResponse.SC_OK) { // 아직 상태가 설정되지 않았다면
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
            }
        }
    }

    // 🚨 반환 타입을 boolean으로 변경하여 갱신 성공 여부를 알림
    private boolean handleRefreshToken(Cookie refreshCookie,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        final String oldRefreshToken = refreshCookie.getValue();
        User user = refreshTokenUtil.validateAndGetUser(oldRefreshToken);

        // Refresh Token 검증 실패 (만료, 위조, DB 불일치 등)
        if (Objects.isNull(user)) {
            cookieUtil.deleteAuthCookies(response);
            return false; // 갱신 실패
        }

        // 🚨 1. [제거됨] 기존 토큰 삭제 로직은 generateToken 내부에서 처리됨을 가정

        List<String> roles = null;
        try {
            roles = Arrays.stream(refreshTokenUtil.verify(oldRefreshToken).getClaim("roles").as(String.class).split(",")).toList();
        } catch (Exception ignored) {
            return false;
        }

        if (ObjectUtils.isEmpty(roles)) return false;

        // 2. 새 Access Token과 Refresh Token 생성 및 DB 저장 (generateToken 내부에서 처리)
        final String newAccessToken = jwtUtil.generateToken(user);
        final String newRefreshToken = refreshTokenUtil.generateToken(user);

        // 3. 새 쿠키 발급 (클라이언트에게 전송)
        cookieUtil.addAccessToken(response, newAccessToken);
        cookieUtil.addPayload(response, newAccessToken);
        cookieUtil.addRefreshToken(response, newRefreshToken);

        // 4. Security Context 재설정
        try {
            // 새 토큰으로 인증 정보 재설정
            final DecodedJWT decodedJWT = jwtUtil.verifyToken(newAccessToken);
            final Long userId = Long.valueOf(decodedJWT.getSubject());

            user = User.builder()
                    .id(userId)
                    .name(decodedJWT.getClaim("username").asString())
                    .displayName(decodedJWT.getClaim("displayName").asString())
                    .profileImage(decodedJWT.getClaim("profileImage").asString())
                    .roles(decodedJWT.getClaim("roles").asString())
                    .build();

            final UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, roles.stream().map(SimpleGrantedAuthority::new).toList());

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            return true; // 갱신 성공
        } catch (Exception e) {
            // 갱신 후 설정 실패 (매우 드묾)
            SecurityContextHolder.clearContext();
            cookieUtil.deleteAuthCookies(response);
            return false; // 갱신 실패
        }
    }
}