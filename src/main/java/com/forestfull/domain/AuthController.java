package com.forestfull.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomUserDetailsService userService;

    @PostMapping("/signup")
    Mono<ResponseEntity<?>> signup(@RequestBody User.SignUpRequest request) {
        return userService.signup(request)
                .map(isSuccess -> isSuccess ? ResponseEntity.ok(Map.of("message", "Signup success")) : ResponseEntity.badRequest().build());
    }

    @PostMapping("/logout")
    Mono<ResponseEntity<?>> logout(ServerWebExchange exchange) {
        exchange.getResponse().addCookie(ResponseCookie.from("JWT", "")
                .path("/")
                .maxAge(0)
                .build());
        return Mono.just(ResponseEntity.ok(Map.of("message", "logout success")));
    }
}