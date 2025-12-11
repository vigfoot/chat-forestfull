package com.forestfull.domain;

import com.forestfull.common.file.FileService;
import com.forestfull.common.token.CookieUtil;
import com.forestfull.common.token.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshJwtUtil;
    private final CookieUtil cookieUtil;
    private final FileService fileService;
    private final CustomUserDetailsService customUserService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String name = body.get("username");
        String password = body.get("password");

        try {
            final Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(name, password));
            final User user = (User) auth.getPrincipal();

            // JWT 발급 (기존 로직)
            final String accessToken = jwtUtil.generateToken(user);
            final String refreshToken = refreshJwtUtil.generateToken(user);
            refreshJwtUtil.save(user.getId(), refreshToken);

            cookieUtil.addAccessToken(response, accessToken);
            cookieUtil.addPayload(response, accessToken);
            cookieUtil.addRefreshToken(response, refreshToken);

            return ResponseEntity.ok(Map.of("message", "Login successful"));

        } catch (UsernameNotFoundException e) {
            // Case 1: 아이디 없음 (비회원)
            // 클라이언트에게 404 Not Found 상태 코드를 명시적으로 반환
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));

        } catch (BadCredentialsException e) {
            // Case 2: 아이디는 있으나 비밀번호 오류 (회원)
            // 기존의 401 Unauthorized 상태 코드를 유지
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid password"));

        } catch (AuthenticationException e) {
            // 기타 인증 오류
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login failed"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // JWT 쿠키에서 username 추출
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (JwtUtil.TOKEN_TYPE.JWT.name().equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            Long id = Long.valueOf(jwtUtil.verifyToken(token).getSubject());
            refreshJwtUtil.deleteTokenByUserId(id);
        }
        cookieUtil.deleteAuthCookies(response);

        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "REFRESH", required = false) String refreshToken, HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token");

        Long userId = refreshJwtUtil.getUserId(refreshToken);
        if (userId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");

        if (!Objects.equals(refreshToken, refreshJwtUtil.getToken(userId)))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mismatch");

        final User user = customUserService.loadUserByUserId(userId);

        // 새 Access Token 생성
        final String newAccess = jwtUtil.generateToken(user);
        cookieUtil.addAccessToken(response, newAccess);

        // JWT_PAYLOAD 쿠키 (JS 접근 가능)
        cookieUtil.addPayload(response, newAccess);

        return ResponseEntity.ok(Map.of("message", "token refreshed"));
    }

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

                customUserService.updateProfileImage(savedUser.getId(), profileImageUrl);
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
        return customUserService.isExistedUsername(username)
                ? ResponseEntity.status(HttpStatus.CONFLICT).build()
                : ResponseEntity.ok().build();
    }

    @PostMapping("/check/nickname/{displayName}")
    ResponseEntity<?> checkNickname(@PathVariable String displayName) {
        return customUserService.isExistedNickname(displayName)
                ? ResponseEntity.status(HttpStatus.CONFLICT).build()
                : ResponseEntity.ok().build();
    }
}