package com.forestfull.config.jwt;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtVerifier verifier;

    public JwtAuthenticationManager(JwtVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();
        if (!verifier.validate(token)) return Mono.empty();

        return Mono.just(
                new UsernamePasswordAuthenticationToken(
                        verifier.getSubject(token),
                        token,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }
}