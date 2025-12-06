package com.forestfull.domain;

import com.forestfull.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    @Value("${spring.config.activate.on-profile}")
    private String onProfile;
    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshJwtUtil;
    private final CustomUserDetailsService userService;

    @PostMapping("/signup")
    ResponseEntity<?> signup(@RequestBody User.SignUpRequest request) {
        return userService.signup(request) ? ResponseEntity.ok(Map.of("message", "Signup success")) : ResponseEntity.badRequest().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 1. 사용자 조회
        var user = userService.loadUserByUsername(username); // UserDetails 반환
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        // 2. 비밀번호 검증
        if (!userService.getPasswordEncoder().matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        // 3. JWT 발급
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        String accessToken = jwtUtil.generateToken(username, roles);
        String refreshToken = jwtUtil.generateToken(username, roles);
        refreshJwtUtil.save(username, refreshToken);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "roles", roles
        ));
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {

        // 1. Authorization 헤더 또는 JWT 쿠키 확인
        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // 쿠키에서 가져오기
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("JWT".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // 2. username 추출 후 Refresh Token 삭제
        if (token != null) {
            try {
                String username = jwtUtil.verifyToken(token).getSubject();
                refreshJwtUtil.deleteToken(username);
            } catch (Exception e) {
                // JWT 파싱 실패 시 무시
            }
        }

        // 3. JWT 쿠키 삭제
        Cookie deleteCookie = new Cookie("JWT", null);
        deleteCookie.setHttpOnly(true);
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        response.addCookie(deleteCookie);

        return "{\"message\":\"로그아웃 되었습니다.\"}";
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "REFRESH") String refreshToken) {
        if (!StringUtils.hasText(refreshToken))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token");

        String username = refreshJwtUtil.getUsername(refreshToken);

        if (!StringUtils.hasText(username))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");

        if (!Objects.equals(refreshToken, refreshJwtUtil.getToken(username)))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mismatch");

        // DB에서 사용자 권한(roles) 조회
        UserDetails userDetails = userService.loadUserByUsername(username);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")) // Jwt는 roles 그대로 저장하도록
                .collect(Collectors.toList());

        // 새 Access Token 생성
        String newAccess = jwtUtil.generateToken(username, roles);

        // 쿠키 생성: JWT (HttpOnly) + JWT_PAYLOAD (JS 사용 가능)
        ResponseCookie accessCookie = ResponseCookie.from("JWT", newAccess)
                .httpOnly(true)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.getExpireMillis() / 1000)
                .sameSite("None")
                .build();

        // payload (base64 payload part) — JS에서 디코딩 가능하도록 non-HttpOnly
        String[] parts = newAccess.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";
        ResponseCookie payloadCookie = ResponseCookie.from("JWT_PAYLOAD", payload)
                .httpOnly(false)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.getExpireMillis() / 1000)
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, payloadCookie.toString())
                .body("{\"message\":\"token refreshed\"}");
    }
}