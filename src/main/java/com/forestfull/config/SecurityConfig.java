package com.forestfull.config;

import com.forestfull.domain.CustomUserDetailsService;
import com.forestfull.filter.CustomLoginFilter;
import com.forestfull.filter.JwtAuthenticationFilter;
import com.forestfull.filter.TokenFilter;
import com.forestfull.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    private static final String[] staticResources = {"/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CustomUserDetailsService customUserDetailsService, JwtUtil jwtUtil, JwtUtil.Refresh jwtUtilRefresh) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(staticResources).permitAll()
                        .requestMatchers("/pages/**", "/api/auth/**").permitAll()
                        .requestMatchers("/pages/admin/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/pages/management/**", "/management/**").hasAnyRole("ADMIN", "MANAGER")
                        .anyRequest().authenticated()
                )
                .userDetailsService(customUserDetailsService)
                .addFilterAt(new CustomLoginFilter(jwtUtil, jwtUtilRefresh), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new TokenFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
    JwtUtil.Refresh jwtUtilRefresh() {
        return new JwtUtil.Refresh();
    }
}