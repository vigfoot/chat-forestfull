package com.forestfull.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forestfull.domain.CustomUserDetailsService;
import com.forestfull.filter.CustomLoginFilter;
import com.forestfull.filter.TokenFilter;
import com.forestfull.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private static final String[] staticResources = {"/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"};

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveAuthenticationManager manager, JwtUtil jwtUtil) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(staticResources).permitAll()
                        .pathMatchers("/pages/**", "/api/auth/**").permitAll()
                        .pathMatchers("/pages/admin/**", "/admin/**").hasRole("ADMIN")
                        .pathMatchers("/pages/management/**", "/management/**").hasAnyRole("ADMIN", "MANAGER")
                        .anyExchange().authenticated()
                )
                .addFilterAt(CustomLoginFilter.build(manager, jwtUtil, objectMapper), SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(new TokenFilter(jwtUtil), SecurityWebFiltersOrder.AUTHENTICATION)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())  // ⬅ Stateless 핵심
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtUtil jwtUtil() {
        String secret = System.getenv().getOrDefault("key", String.valueOf(System.nanoTime()));
        long expireMillis = 24L * 60 * 60 * 1000;
        return new JwtUtil(secret, expireMillis);
    }

    @Bean
    ReactiveAuthenticationManager reactiveAuthenticationManager(PasswordEncoder passwordEncoder, CustomUserDetailsService reactiveUserDetailsService) {
        UserDetailsRepositoryReactiveAuthenticationManager manager = new UserDetailsRepositoryReactiveAuthenticationManager(reactiveUserDetailsService);
        manager.setPasswordEncoder(passwordEncoder);
        return manager;
    }
}