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
    private final MemberService memberService; // ID/Nickname 중복 확인 및 Profile Image 업데이트 담당 (가정)
    private final CustomUserDetailsService customUserService; // 회원 DB 저장 로직 담당 (가정)
    private final EmailVerificationService emailVerificationService; // 이메일 인증 상태 관리 담당

    /**
     * 최종 회원가입 및 프로필 이미지 저장 로직을 처리합니다.
     * 인증 성공 상태와 트랜잭션 관리가 필요합니다.
     *
     * URI: POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @ModelAttribute MemberDTO request) {

        // 1. 이메일 인증 상태 검사: 인증 코드 확인을 통과했는지, 그리고 상태가 만료되지 않았는지 확인
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
            // 2. 사용자 등록 (DB 트랜잭션 시작 지점 - CustomUserDetailsService 내부에서 처리 가정)
            savedUser = customUserService.signup(user);

            if (savedUser == null || savedUser.getId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sign up failed during database registration."));
            }

            // 3. 프로필 이미지 처리
            if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
                Long profileFileId = fileService.saveProfileImage(request.getProfileImage(), savedUser.getId());

                if (profileFileId == null) {
                    // 파일 저장 실패 시, DB에 저장된 user를 롤백(삭제)하는 로직이 customUserService/MemberService에 있어야 함.
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "User registered, but failed to upload profile image."));
                }

                String profileImageUrl = "/file/" + profileFileId;
                memberService.updateProfileImage(savedUser.getId(), profileImageUrl);
                savedUser.setProfileImage(profileImageUrl);
            }

            // 4. 인증 완료 상태 제거 (재가입 방지, 일회성)
            emailVerificationService.removeVerificationStatus(request.getEmail());

            // 5. 최종 성공 응답 반환
            return ResponseEntity.ok(Map.of("message", "Sign up successful"));

        } catch (Exception e) {
            // 예외 발생 시 트랜잭션 롤백 처리 필요
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred during sign up."));
        }
    }

    /**
     * 사용자 ID(Username) 중복을 확인합니다.
     *
     * URI: POST /api/auth/check/id/{username}
     */
    @PostMapping("/check/id/{username}")
    ResponseEntity<?> checkUsername(@PathVariable String username) {
        return memberService.isExistedUsername(username)
                ? ResponseEntity.status(HttpStatus.CONFLICT).build() // 중복
                : ResponseEntity.ok().build(); // 사용 가능
    }

    /**
     * 닉네임(DisplayName) 중복을 확인합니다.
     *
     * URI: POST /api/auth/check/nickname/{displayName}
     */
    @PostMapping("/check/nickname/{displayName}")
    ResponseEntity<?> checkNickname(@PathVariable String displayName) {
        return memberService.isExistedNickname(displayName)
                ? ResponseEntity.status(HttpStatus.CONFLICT).build() // 중복
                : ResponseEntity.ok().build(); // 사용 가능
    }

    /**
     * 이메일 중복 확인 후, 인증 코드를 발송합니다.
     *
     * URI: POST /api/auth/verify/send
     */
    @PostMapping("/verify/send/email")
    public ResponseEntity<Void> sendVerificationCode(@RequestBody VerificationEmail verificationEmail) {
        final String email = verificationEmail.getEmail();
        if (!StringUtils.hasText(email)) return ResponseEntity.badRequest().build();

        // 1. 이메일 중복 검사 (요청하신 '이메일 중복 불허' 정책)
        if (memberService.isEmailRegistered(email))
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        try {
            // 2. 인증 코드 발송 및 코드 저장
            emailVerificationService.sendVerificationCode(email);
            return ResponseEntity.ok().build();
        } catch (MessagingException e) {
            // 메일 서버 연결/설정 오류 등
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자 입력 인증 코드를 검증합니다. 성공 시, 해당 이메일을 '인증 완료 상태'로 만듭니다.
     *
     * URI: POST /api/auth/verify/check
     */
    @PostMapping("/verify/check/email")
    public ResponseEntity<Void> checkVerificationCode(@RequestBody VerificationEmail verificationEmail) {
        final String email = verificationEmail.getEmail();
        final String code = verificationEmail.getCode();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(code))
            return ResponseEntity.badRequest().build();

        // 코드 검증 (성공 시 EmailVerificationService 내부에서 Verified Store에 상태 저장)
        boolean verified = emailVerificationService.verifyCode(email, code);

        return verified
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 코드 불일치, 만료 등
    }
}