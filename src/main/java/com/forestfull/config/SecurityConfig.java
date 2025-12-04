package com.forestfull.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forestfull.domain.CustomUserDetailsService;
import com.forestfull.filter.CustomLoginFilter;
import com.forestfull.filter.TokenFilter;
import com.forestfull.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * com.forestfull.config
 *
 * @author vigfoot
 * @version 2025-12-05
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] staticResources = {"/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"};
    private final ObjectMapper objectMapper;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUtil jwtUtil, AuthenticationManager manager) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(staticResources).permitAll()
                        .requestMatchers(new String[]{"/admin/**", "/pages/admin/**"}).hasRole("ADMIN")
                        .requestMatchers(new String[]{"/management/**", "/pages/management/**"}).hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(new String[]{"/pages/**", "/file/**","/api/auth/**"}).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new TokenFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(new CustomLoginFilter(manager, jwtUtil, objectMapper), UsernamePasswordAuthenticationFilter.class)
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
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}