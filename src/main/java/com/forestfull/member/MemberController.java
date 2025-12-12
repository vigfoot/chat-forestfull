package com.forestfull.member;

import com.forestfull.common.file.FileService;
import com.forestfull.common.smtp.EmailVerificationService;
import com.forestfull.common.smtp.VerificationEmail;
import com.forestfull.domain.CustomUserDetailsService;
import com.forestfull.domain.User;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {

    private final FileService fileService;
    private final MemberService memberService;
    private final CustomUserDetailsService customUserService;
    private final EmailVerificationService emailVerificationService;

    // 🚩 MODIFIED: 파일 저장 후처리 로직을 포함한 signup 메서드
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @ModelAttribute MemberDTO request) {
        User user = User.builder()
                .name(request.getUsername())
                .password(request.getPassword())
                .displayName(request.getDisplayName())
                .build();

        User savedUser = null;
        try {
            savedUser = customUserService.signup(user);

            if (savedUser == null || savedUser.getId() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Sign up failed during database registration."));


            if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
                Long profileFileId = fileService.saveProfileImage(request.getProfileImage(), savedUser.getId());

                if (profileFileId == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "User registered, but failed to upload profile image."));
                }

                String profileImageUrl = "/file/" + profileFileId;

                memberService.updateProfileImage(savedUser.getId(), profileImageUrl);
                savedUser.setProfileImage(profileImageUrl);
            }

            // 5. 최종 성공 응답 반환
            return ResponseEntity.ok(Map.of("message", "Sign up successful"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred during sign up."));
        }
    }

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

        // 1. 🚩 ACTIVATED: 이메일 중복 검사 (요청하신 '이메일 중복 불허' 정책)
        if (memberService.isEmailRegistered(email))
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        try {
            // 2. 인증 코드 발송
            emailVerificationService.sendVerificationCode(email);
            return ResponseEntity.ok().build();
        } catch (MessagingException e) {
            // 메일 서버 오류 또는 설정 오류
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/verify/check/email")
    public ResponseEntity<Void> checkVerificationCode(@RequestBody VerificationEmail verificationEmail) {
        final String email = verificationEmail.getEmail();
        final String code = verificationEmail.getCode();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(code))
            return ResponseEntity.badRequest().build();

        return emailVerificationService.verifyCode(email, code)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}