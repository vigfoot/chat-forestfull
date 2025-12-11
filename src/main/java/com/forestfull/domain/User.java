package com.forestfull.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.attribute.UserPrincipal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails, UserPrincipal {
    private Long id;
    private String name;
    private String password;
    private String roles = "ROLE_USER";
    private String displayName;
    private String profileImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // --- UserDetails / UserPrincipal 구현 메서드 생략 ---
    public List<String> getRoleList() {
        return Arrays.stream(roles.split(",")).collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.stream(roles.split(","))
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return getName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
    // ----------------------------------------------------


    // 🚩 ADDED: Inner Static Class로 SignupRequest 구현
    @Data
    public static class SignupRequest {
        // ID (Username)
        @NotBlank(message = "ID is required.")
        @Size(min = 4, max = 50, message = "ID must be between 4 and 20 characters.")
        // 🚩 MODIFIED: 영어 대소문자와 숫자만 허용하는 정규식 추가
        @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "ID must contain only English letters and numbers.")
        private String name;

        // Password
        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters.")
        private String password;

        // Nickname
        @NotBlank(message = "Nickname is required.")
        @Size(min = 2, max = 30, message = "Nickname must be between 2 and 30 characters.")
        private String displayName;

        // Profile Image File (Optional)
        private MultipartFile profileImage;
    }
}