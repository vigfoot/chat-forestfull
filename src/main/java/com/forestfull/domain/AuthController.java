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
        if (refreshToken == null) return ResponseEntity.status(401).body("No refresh token");

        String username = refreshJwtUtil.getUsername(refreshToken);
        if (username == null) return ResponseEntity.status(401).body("Invalid refresh token");

        if (!refreshToken.equals(refreshJwtUtil.getToken(username)))
            return ResponseEntity.status(401).body("Token mismatch");

        UserDetails userDetails = userService.loadUserByUsername(username);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList();

        String newAccessToken = jwtUtil.generateToken(username, roles);

        // 쿠키 재설정
        ResponseCookie accessCookie = ResponseCookie.from("JWT", newAccessToken)
                .httpOnly(true)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("None")
                .build();

        String[] parts = newAccessToken.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";
        ResponseCookie payloadCookie = ResponseCookie.from("JWT_PAYLOAD", payload)
                .httpOnly(false)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, payloadCookie.toString())
                .body("{\"message\":\"token refreshed\"}");
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

        // 1️⃣ JWT 쿠키에서 Access Token 가져오기
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 2️⃣ Access Token으로 username 추출 후 DB에서 Refresh Token 폐기
        if (token != null) {
            try {
                String username = jwtUtil.verifyToken(token).getSubject();
                refreshJwtUtil.deleteTokenByUsername(username); // DB revoke
            } catch (Exception e) {
                // JWT 파싱 실패 시 무시
            }
        }

        // 3️⃣ JWT / JWT_PAYLOAD / REFRESH 쿠키 삭제
        Cookie deleteAccess = new Cookie("JWT", null);
        deleteAccess.setHttpOnly(true);
        deleteAccess.setPath("/");
        deleteAccess.setMaxAge(0);
        deleteAccess.setAttribute("SameSite", "None");
        deleteAccess.setSecure("prod".equals(onProfile));
        response.addCookie(deleteAccess);

        Cookie deletePayload = new Cookie("JWT_PAYLOAD", null);
        deletePayload.setHttpOnly(false);
        deletePayload.setPath("/");
        deletePayload.setMaxAge(0);
        deletePayload.setAttribute("SameSite", "None");
        deletePayload.setSecure("prod".equals(onProfile));
        response.addCookie(deletePayload);

        Cookie deleteRefresh = new Cookie("REFRESH", null);
        deleteRefresh.setHttpOnly(true);
        deleteRefresh.setPath("/");
        deleteRefresh.setMaxAge(0);
        deleteRefresh.setAttribute("SameSite", "None");
        deleteRefresh.setSecure("prod".equals(onProfile));
        response.addCookie(deleteRefresh);

        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }
}