package com.forestfull.config;

import com.forestfull.common.token.CustomLoginFilter;
import com.forestfull.common.token.JwtAuthenticationFilter;
import com.forestfull.common.token.RefreshTokenFilter;
import com.forestfull.common.token.TokenFilter;
import com.forestfull.domain.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    private final CustomUserDetailsService customUserDetailsService;
    private final TokenFilter tokenFilter;
    private final RefreshTokenFilter refreshTokenFilter;
    private final CustomLoginFilter customLoginFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private static final String[] staticResources = {"/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
                .addFilterBefore(refreshTokenFilter, TokenFilter.class)
                .addFilterBefore(tokenFilter, CustomLoginFilter.class)
                .addFilterBefore(customLoginFilter, JwtAuthenticationFilter.class)
                .addFilterAt(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}