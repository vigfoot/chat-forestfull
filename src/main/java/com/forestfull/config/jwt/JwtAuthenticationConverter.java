package com.forestfull.config.jwt;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * com.forestfull.config.jwt
 *
 * @author vigfoot
 * @version 2025-11-26
 */
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (auth != null && auth.startsWith("Bearer "))
            return Mono.just(new UsernamePasswordAuthenticationToken(auth.substring(7), auth.substring(7)));

        // WebSocket 지원: Query Param ?token=xxx
        String query = exchange.getRequest().getURI().getQuery();
        if (query != null && query.startsWith("token=")) {
            String token = query.substring(6);
            return Mono.just(new UsernamePasswordAuthenticationToken(token, token));
        }

        return Mono.empty();
    }
}