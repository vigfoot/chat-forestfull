package com.forestfull.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forestfull.util.JwtUtil;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class CustomLoginFilter {

    public static AuthenticationWebFilter build(ReactiveAuthenticationManager authManager, JwtUtil jwtUtil, ObjectMapper mapper) {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(authManager);

        filter.setServerAuthenticationConverter(exchange -> exchange.getRequest().getBody()
                .next()
                .flatMap(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        Map<String, String> map = mapper.readValue(bytes, Map.class);
                        String username = map.get("username");
                        String password = map.get("password");
                        return Mono.just(new UsernamePasswordAuthenticationToken(username, password));
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        return Mono.error(new RuntimeException("Invalid request"));
                    }
                }));

        filter.setAuthenticationSuccessHandler((webFilterExchange, authentication) -> {
            Authentication auth = authentication;
            List<String> roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
            String token = jwtUtil.generateToken(auth.getName(), roles);

            ResponseCookie cookie = ResponseCookie.from("JWT", token)
                    .httpOnly(false) // HttpOnly 제거
                    .path("/")
                    .maxAge(jwtUtil.getExpireMillis() / 1000)
                    .build();

            webFilterExchange.getExchange().getResponse().addCookie(cookie);
            webFilterExchange.getExchange().getResponse().getHeaders().add("Content-Type", "application/json");
            byte[] body = "{\"message\":\"success\"}".getBytes();
            return webFilterExchange.getExchange().getResponse().writeWith(Mono.just(webFilterExchange.getExchange().getResponse().bufferFactory().wrap(body)));
        });

        filter.setAuthenticationFailureHandler((webFilterExchange, exception) -> {
            exception.printStackTrace(System.err);

            byte[] body = ("{\"message\":\"failure\"}").getBytes();
            webFilterExchange.getExchange().getResponse().getHeaders().add("Content-Type", "application/json");
            return webFilterExchange.getExchange().getResponse().writeWith(Mono.just(webFilterExchange.getExchange().getResponse().bufferFactory().wrap(body)));
        });

        return filter;
    }
}