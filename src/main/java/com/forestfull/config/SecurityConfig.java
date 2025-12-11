package com.forestfull.config;

import com.forestfull.common.token.TokenFilter;
import com.forestfull.domain.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenFilter tokenFilter;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private static final String[] PUBLIC_RESOURCES = {"/", "/favicon.ico", "/css/**", "/js/**", "/images/**", "/webjars/**", "/document/**"};
    private static final String[] ALLOW_PATHS = {"/api/auth/login", "/pages/signup", "/api/auth/signup", "/api/auth/check/*/*", "/api/auth/verify/*"};

    public static boolean isPublicResources(String path) {
        return Arrays.stream(PUBLIC_RESOURCES).anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }

    public static boolean isLoginPath(String path) {
        return Arrays.stream(ALLOW_PATHS).anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(PUBLIC_RESOURCES).permitAll()
                        .requestMatchers("/api/**", "/pages/**").permitAll()
                        .requestMatchers("/pages/admin/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/pages/management/**", "/management/**").hasAnyRole("ADMIN", "MANAGER")
                        .anyRequest().authenticated()
                )
                .userDetailsService(customUserDetailsService)
                .addFilterAt(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }
}