package com.forestfull.member;

import com.forestfull.common.file.FileService;
import com.forestfull.domain.CustomUserDetailsService;
import com.forestfull.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {

    private final FileService fileService;
    private final MemberService memberService;
    private final CustomUserDetailsService customUserService;

    // 🚩 MODIFIED: 파일 저장 후처리 로직을 포함한 signup 메서드
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @ModelAttribute User.SignupRequest request) {
        User user = User.builder()
                .name(request.getName())
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
}