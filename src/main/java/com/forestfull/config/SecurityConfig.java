package com.forestfull.config;

import com.forestfull.domain.CustomUserDetailsService;
import com.forestfull.filter.CustomLoginFilter;
import com.forestfull.filter.TokenFilter;
import com.forestfull.filter.RefreshTokenFilter;
import com.forestfull.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    private static final String[] staticResources = {"/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CustomUserDetailsService customUserDetailsService, JwtUtil jwtUtil, JwtUtil.Refresh refreshJwtUtil) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowCredentials(true);
                    config.addAllowedOriginPattern("*");
                    config.addAllowedHeader("*");
                    config.addAllowedMethod("*");
                    return config;
                }))
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
                .addFilterBefore(new RefreshTokenFilter(jwtUtil, refreshJwtUtil), TokenFilter.class)
                .addFilterBefore(new TokenFilter(jwtUtil, refreshJwtUtil, customUserDetailsService), UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(new CustomLoginFilter(jwtUtil, refreshJwtUtil), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Value("${key}")
    private String secretKey;

    @Bean
    JwtUtil jwtUtil() {
        return new JwtUtil(secretKey);
    }

    @Bean
    JwtUtil.Refresh jwtUtilRefresh() {
        return new JwtUtil.Refresh(secretKey);
    }
}