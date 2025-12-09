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
        if (SecurityConfig.isPublicResources(request.getRequestURI()) || SecurityConfig.isLoginPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        final Cookie[] cookies = request.getCookies();
        if (cookies == null) throw new RuntimeException("Cookies Not found");

        try {
            Arrays.stream(cookies)
                    .filter(c -> JwtUtil.TOKEN_TYPE.JWT.name().equals(c.getName()))
                    .findFirst()
                    .ifPresent(cookie -> {
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
                    });

        } catch (JWTVerificationException e) {
            Arrays.stream(cookies)
                    .filter(c -> JwtUtil.TOKEN_TYPE.REFRESH.name().equals(c.getName()))
                    .findFirst()
                    .ifPresent(cookie -> handleRefreshToken(cookie, request, response));
        }

        filterChain.doFilter(request, response);
    }

    private void handleRefreshToken(Cookie refreshCookie,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        final String oldRefreshToken = refreshCookie.getValue();
        User user = refreshTokenUtil.validateAndGetUser(oldRefreshToken);
        if (Objects.isNull(user)) return;

        refreshTokenUtil.deleteTokenByUserId(user.getId());

        List<String> roles = null;
        try {
            roles = refreshTokenUtil.verify(oldRefreshToken).getClaim("roles").asList(String.class);
        } catch (Exception ignored) {
        }

        if (ObjectUtils.isEmpty(roles)) return;

        final String newAccessToken = jwtUtil.generateToken(user);
        final String newRefreshToken = refreshTokenUtil.generateToken(user);

        cookieUtil.addAccessToken(response, newAccessToken);
        cookieUtil.addPayload(response, newAccessToken);
        cookieUtil.addRefreshToken(response, newRefreshToken);

        try {
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
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }
    }
}