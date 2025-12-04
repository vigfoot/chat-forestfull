package com.forestfull.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomUserDetailsService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User.SignUpRequest request) {
        userService.signup(request);
        return ResponseEntity.ok(Map.of("message", "Signup success"));
    }
}