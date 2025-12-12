package com.forestfull.member;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.forestfull.common.file.FileService;
import com.forestfull.common.smtp.EmailVerificationService;
import com.forestfull.common.smtp.VerificationEmail;
import com.forestfull.common.token.CookieUtil;
import com.forestfull.common.token.JwtUtil;
import com.forestfull.domain.CustomUserDetailsService;
import com.forestfull.domain.User;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {

    private final FileService fileService;
    private final MemberService memberService;
    private final CustomUserDetailsService customUserService;
    private final EmailVerificationService emailVerificationService;
    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh jwtRefreshUtil;
    private final CookieUtil cookieUtil;

    // ---------------------------------------------------------------------------------
    // [ Private Utility: JWT에서 ID 추출 ]
    // ---------------------------------------------------------------------------------

    /**
     * HttpServletRequest에서 JWT를 파싱하여 사용자 ID를 추출합니다.
     * ID 추출 실패 시 null을 반환합니다.
     */
    private Long extractUserIdFromRequest(HttpServletRequest request) {
        final Optional<DecodedJWT> decodedJWTOptional = jwtUtil.getJwtToken(request);
        if (decodedJWTOptional.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(decodedJWTOptional.get().getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }


    // ---------------------------------------------------------------------------------
    // [ 인증 및 회원가입 관련 API ] (변경 없음)
    // ---------------------------------------------------------------------------------

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @ModelAttribute MemberDTO request) {
        if (!emailVerificationService.isVerifiedForSignup(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email verification is required or has expired."));
        }

        User user = User.builder()
                .name(request.getUsername())
                .password(request.getPassword())
                .roles("ROLE_USER")
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .build();

        User savedUser = null;
        try {
            savedUser = customUserService.signup(user);

            if (savedUser == null || savedUser.getId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sign up failed during database registration."));
            }

            if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
                Long profileFileId = fileService.saveProfileImage(request.getProfileImage(), savedUser.getId());

                if (profileFileId == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "User registered, but failed to upload profile image."));
                }

                String profileImageUrl = "/file/" + profileFileId;
                memberService.updateProfileImage(savedUser.getId(), profileImageUrl);
            }

            emailVerificationService.removeVerificationStatus(request.getEmail());

            return ResponseEntity.ok(Map.of("message", "Sign up successful"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred during sign up."));
        }
    }

    // (중복 확인 및 이메일 인증 API는 생략)
    @PostMapping("/check/id/{username}")
    ResponseEntity<?> checkUsername(@PathVariable String username) {
        return memberService.isExistedUsername(username)
                ? ResponseEntity.status(HttpStatus.CONFLICT).build()
                : ResponseEntity.ok().build();
    }

    @PostMapping("/check/nickname/{displayName}")
    ResponseEntity<?> checkNickname(@PathVariable String displayName) {
        return memberService.isExistedNickname(displayName)
                ? ResponseEntity.status(HttpStatus.CONFLICT).build()
                : ResponseEntity.ok().build();
    }

    @PostMapping("/verify/send/email")
    public ResponseEntity<Void> sendVerificationCode(@RequestBody VerificationEmail verificationEmail) {
        final String email = verificationEmail.getEmail();
        if (!StringUtils.hasText(email)) return ResponseEntity.badRequest().build();
        if (memberService.isEmailRegistered(email)) return ResponseEntity.status(HttpStatus.CONFLICT).build();

        try {
            emailVerificationService.sendVerificationCode(email);
            return ResponseEntity.ok().build();
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/verify/check/email")
    public ResponseEntity<Void> checkVerificationCode(@RequestBody VerificationEmail verificationEmail) {
        final String email = verificationEmail.getEmail();
        final String code = verificationEmail.getCode();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(code))
            return ResponseEntity.badRequest().build();

        boolean verified = emailVerificationService.verifyCode(email, code);

        return verified
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }


    // ---------------------------------------------------------------------------------
    // [ 로그인 사용자 관리 API ] - 마이페이지 기능 통합
    // ---------------------------------------------------------------------------------

    /**
     * 1. Profile Information Update (Image, Nickname, Email)
     * URI: POST /api/auth/users/profile (Multipart/form-data)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/users/profile")
    public ResponseEntity<?> updateProfile(@Valid @ModelAttribute MemberDTO request, HttpServletRequest httpRequest, HttpServletResponse response) { // 🚩 HttpServletRequest 추가
        final Long userId = extractUserIdFromRequest(httpRequest); // 🚩 ID 추출
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 인증 실패 시 401 반환

        // 1. 닉네임 유효성 검증
        if (!memberService.isNicknameAvailableForUpdate(userId, request.getDisplayName())) { // 🚩 userId 사용
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Nickname already taken by another user."));
        }

        // 2. 이메일 유효성 및 인증 검증
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (memberService.isNewEmail(userId, request.getEmail())) { // 🚩 userId 사용
                if (!emailVerificationService.isVerified(request.getEmail())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "The new email address must be verified first."));
                }
                if (memberService.isEmailRegisteredByOtherUser(userId, request.getEmail())) { // 🚩 userId 사용
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "This email is already used by another user."));
                }
            }
        }

        try {
            memberService.updateProfile(
                    userId, // 🚩 userId 사용
                    request.getDisplayName(),
                    request.getEmail(),
                    request.getProfileImage()
            );

            final User user = customUserService.loadUserByUserId(userId);
            final String accessToken = jwtUtil.generateToken(user);
            final String refreshToken = jwtRefreshUtil.generateToken(user);
            jwtRefreshUtil.save(user.getId(), refreshToken);

            cookieUtil.addAccessToken(response, accessToken);
            cookieUtil.addPayload(response, accessToken);
            cookieUtil.addRefreshToken(response, refreshToken);

            return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process profile update."));
        }
    }

    /**
     * 2. Change Password
     * URI: PUT /api/auth/users/password
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/users/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody MemberDTO request, HttpServletRequest httpRequest, HttpServletResponse response) { // 🚩 HttpServletRequest 추가
        final Long userId = extractUserIdFromRequest(httpRequest); // 🚩 ID 추출
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 인증 실패 시 401 반환

        if (request.getPassword() == null || request.getNewPassword() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Current and new passwords are required."));

        try {
            memberService.changePassword(userId, request.getPassword(), request.getNewPassword()); // 🚩 userId 사용
            jwtRefreshUtil.deleteTokenByUserId(userId); // 🚩 userId 사용
            cookieUtil.deleteAuthCookies(response);

            return ResponseEntity.ok(Map.of("message", "Password changed successfully. Please re-login."));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Current password is incorrect."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to change password."));
        }
    }

    /**
     * 3. Delete Account (회원 탈퇴)
     * URI: DELETE /api/auth/users
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/users")
    public ResponseEntity<?> deleteAccount(HttpServletRequest request, HttpServletResponse response) {
        final Long userId = extractUserIdFromRequest(request); // 🚩 ID 추출 유틸리티 사용
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 인증 실패 시 401 반환

        try {
            // 1. Refresh Token DB에서 삭제
            jwtRefreshUtil.deleteTokenByUserId(userId);

            // 2. 사용자 DB에서 삭제 (파일 시스템상의 프로필 이미지 파일도 삭제해야 함)
            memberService.deleteUser(userId);

            // 3. 인증 관련 쿠키 삭제
            cookieUtil.deleteAuthCookies(response);

            return ResponseEntity.ok(Map.of("message", "Account successfully deleted."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete account."));
        }
    }
}