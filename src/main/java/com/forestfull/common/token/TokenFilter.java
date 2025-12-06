package com.forestfull.common.token;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie jwtCookie = Arrays.stream(cookies)
                .filter(c -> "JWT".equals(c.getName()))
                .findFirst()
                .orElse(null);

        if (jwtCookie == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = jwtCookie.getValue();

        try {
            // Access Token 정상 → 인증 처리 후 진행
            DecodedJWT decodedJWT = jwtUtil.verifyToken(accessToken);
            setAuthentication(decodedJWT, request);
            filterChain.doFilter(request, response);
            return;
        } catch (TokenExpiredException ex) {
            // 만료 → Refresh Token으로 처리 시도
            System.out.println("Access Token expired. Trying refresh...");
        } catch (Exception ex) {
            filterChain.doFilter(request, response);
            return;
        }

        // Refresh Token 확인
        Cookie refreshCookie = Arrays.stream(cookies)
                .filter(c -> "REFRESH".equals(c.getName()))
                .findFirst()
                .orElse(null);

        if (refreshCookie == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = refreshCookie.getValue();
        String username = refreshUtil.getUsername(refreshToken);

        if (username == null || !refreshUtil.exists(username, refreshToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Refresh 정상 → 새로운 Access Token 재발급
        List<String> roles = refreshUtil.getRoles(refreshToken);
        String newAccessToken = jwtUtil.generateToken(username, roles);

        // JWT 쿠키 갱신
        Cookie newCookie = new Cookie("JWT", newAccessToken);
        newCookie.setPath("/");
        newCookie.setMaxAge((int) JwtUtil.expireMillis / 1000);
        newCookie.setSecure(true);
        newCookie.setHttpOnly(false); // 👈 프론트에서 접근 가능
        newCookie.setAttribute("SameSite", "None");
        response.addCookie(newCookie);

        setAuthentication(jwtUtil.verifyToken(newAccessToken), request);
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
