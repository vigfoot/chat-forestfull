package com.forestfull.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.forestfull.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TokenFilter implements Filter {

    private final JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final Cookie[] cookies = ((HttpServletRequest) request).getCookies();

        if (!ObjectUtils.isEmpty(cookies)) {
            final Optional<Cookie> optionalJWT = Arrays.stream((cookies))
                    .filter(cookie -> cookie.getName().equals("JWT"))
                    .findFirst();

            if (optionalJWT.isEmpty()) {
                chain.doFilter(request, response);
                return;
            }

            DecodedJWT jwt = jwtUtil.verifyToken(optionalJWT.get().getValue());
            String username = jwt.getSubject();
            List<SimpleGrantedAuthority> authorities = Arrays.stream(jwt.getClaim("roles").asArray(String.class))
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            SecurityContextHolder.setContext(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(username, null, authorities)));
        }
        chain.doFilter(request, response);
    }
}