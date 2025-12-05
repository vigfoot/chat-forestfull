package com.forestfull.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.forestfull.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TokenFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    public TokenFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = Arrays.stream(exchange.getRequest().getCookies().getFirst("JWT") != null ?
                        new String[]{exchange.getRequest().getCookies().getFirst("JWT").getValue()} : new String[]{})
                .findFirst().orElse(null);

        if (token != null) {
            try {
                DecodedJWT jwt = jwtUtil.verifyToken(token);
                String username = jwt.getSubject();
                var authorities = Arrays.stream(jwt.getClaim("roles").asArray(String.class))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(auth))));
            } catch (Exception e) {
                // 유효하지 않은 토큰은 무시하고 체인 진행
            }
        }

        return chain.filter(exchange);
    }
}