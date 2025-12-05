package com.forestfull.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public Mono<Boolean> signup(com.forestfull.domain.User.SignUpRequest request) {
        if (userMapper.findByUsername(request.getUsername()) != null) throw new RuntimeException("User already exists");

        com.forestfull.domain.User user = new com.forestfull.domain.User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(request.getRoles());

        return Mono.just(userMapper.save(user));
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        com.forestfull.domain.User user = userMapper.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("User not found");

        return Mono.just(org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().split(","))
                .build());
    }
}