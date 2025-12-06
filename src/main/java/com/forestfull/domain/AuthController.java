package com.forestfull.domain;

import com.forestfull.common.token.JwtUtil;
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
        String refreshToken = refreshJwtUtil.generateToken(username);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "roles", roles
        ));
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "REFRESH", required = false) String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No refresh token"));
        }

        // 1️⃣ Refresh Token 검증 및 username 확인
        String username = refreshJwtUtil.getUsername(refreshToken);
        if (!StringUtils.hasText(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
        }

        // 2️⃣ DB에 저장된 토큰과 비교
        String savedToken = refreshJwtUtil.getToken(username);
        if (!refreshToken.equals(savedToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token mismatch"));
        }

        // 3️⃣ 사용자 권한 조회 (roles)
        UserDetails userDetails = userService.loadUserByUsername(username);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")) // Jwt roles는 그대로 저장
                .toList();

        // 4️⃣ 새 Access Token 생성
        String newAccessToken = jwtUtil.generateToken(username, roles);

        // 5️⃣ JWT 쿠키 생성
        ResponseCookie accessCookie = ResponseCookie.from("JWT", newAccessToken)
                .httpOnly(true)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.getExpireMillis() / 1000)
                .sameSite("None")
                .build();

        // 6️⃣ JWT_PAYLOAD 쿠키 생성 (JS에서 읽도록)
        String[] parts = newAccessToken.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";
        ResponseCookie payloadCookie = ResponseCookie.from("JWT_PAYLOAD", payload)
                .httpOnly(false)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.getExpireMillis() / 1000)
                .sameSite("None")
                .build();

        // 7️⃣ Response
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, payloadCookie.toString())
                .body(Map.of("message", "Access token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

        // 1️⃣ JWT 확인 (Authorization 헤더 또는 쿠키)
        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        // 2️⃣ Refresh Token DB에서 폐기
        if (token != null) {
            try {
                String username = jwtUtil.verifyToken(token).getSubject();
                refreshJwtUtil.deleteTokenByUsername(username);
            } catch (Exception e) {
                // 토큰이 유효하지 않으면 무시
            }
        }

        // 3️⃣ JWT 쿠키 삭제
        Cookie deleteJwt = new Cookie("JWT", null);
        deleteJwt.setHttpOnly(true);
        deleteJwt.setSecure("prod".equals(onProfile));
        deleteJwt.setPath("/");
        deleteJwt.setMaxAge(0);
        deleteJwt.setAttribute("SameSite", "None");
        response.addCookie(deleteJwt);

        // 4️⃣ Payload 쿠키 삭제
        Cookie deletePayload = new Cookie("JWT_PAYLOAD", null);
        deletePayload.setHttpOnly(false);
        deletePayload.setSecure("prod".equals(onProfile));
        deletePayload.setPath("/");
        deletePayload.setMaxAge(0);
        deletePayload.setAttribute("SameSite", "None");
        response.addCookie(deletePayload);

        // 5️⃣ Refresh Token 쿠키 삭제
        Cookie deleteRefresh = new Cookie("REFRESH", null);
        deleteRefresh.setHttpOnly(true);
        deleteRefresh.setSecure("prod".equals(onProfile));
        deleteRefresh.setPath("/");
        deleteRefresh.setMaxAge(0);
        deleteRefresh.setAttribute("SameSite", "None");
        response.addCookie(deleteRefresh);

        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }
}