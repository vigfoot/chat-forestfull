package com.forestfull.config.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * com.forestfull.config.jwt
 *
 * @author vigfoot
 * @version 2025-11-26
 */
@RequiredArgsConstructor
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtVerifier verifier;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        if (token == null) return Mono.empty();

        boolean ok = verifier.validateToken(token);
        if (!ok) return Mono.empty();

        String sub = verifier.getSubject(token);
        // 권한은 간단하게 ROLE_USER 할당 — 실제 매핑 필요하면 확장
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                sub, token, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return Mono.just(auth);
    }
}
