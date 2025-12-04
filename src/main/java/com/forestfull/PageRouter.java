package com.forestfull;

import com.forestfull.domain.CustomUserDetailsService;
import com.forestfull.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PageRouter {

    private final CustomUserDetailsService customUserDetailsService;

    @GetMapping("/admin/emoji")
    String emojiPage(){
        return "admin-emoji";
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<?> signup(@RequestBody User.SignUpRequest request) {
        customUserDetailsService.signup(request);
        return ResponseEntity.ok(Map.of("message", "Signup success"));
    }

}