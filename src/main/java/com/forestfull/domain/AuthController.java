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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshJwtUtil;
    private final CustomUserDetailsService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.config.activate.on-profile}")
    private String onProfile;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");

        var user = (org.springframework.security.core.userdetails.User) userService.loadUserByUsername(username);

        if (!passwordEncoder.matches(password, user.getPassword()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtUtil.generateToken(username, roles);
        String refreshToken = refreshJwtUtil.generateToken(username);

        // JWT HttpOnly
        ResponseCookie jwtCookie = ResponseCookie.from("JWT", accessToken)
                .httpOnly(true)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("None")
                .build();

        // JWT_PAYLOAD (JS 접근용)
        String[] parts = accessToken.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";
        ResponseCookie payloadCookie = ResponseCookie.from("JWT_PAYLOAD", payload)
                .httpOnly(false)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("None")
                .build();

        // REFRESH 쿠키
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", refreshToken)
                .httpOnly(true)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.refreshExpireMillis / 1000)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, payloadCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(Map.of("roles", roles));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // JWT 쿠키에서 username 추출
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            try {
                String username = jwtUtil.verifyToken(token).getSubject();
                refreshJwtUtil.deleteTokenByUsername(username);
            } catch (Exception e) {
            }
        }

        // 쿠키 삭제
        List<String> cookiesToDelete = List.of("JWT", "JWT_PAYLOAD", "REFRESH");
        for (String name : cookiesToDelete) {
            Cookie c = new Cookie(name, null);
            c.setHttpOnly("JWT".equals(name) || "REFRESH".equals(name));
            c.setSecure("prod".equals(onProfile));
            c.setPath("/");
            c.setMaxAge(0);
            c.setAttribute("SameSite", "None");
            response.addCookie(c);
        }

        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "REFRESH", required = false) String refreshToken,
                                          HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token");

        String username = refreshJwtUtil.getUsername(refreshToken);
        if (username == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");

        if (!Objects.equals(refreshToken, refreshJwtUtil.getToken(username)))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mismatch");

        // DB에서 권한 조회
        UserDetails userDetails = userService.loadUserByUsername(username);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList();

        // 새 Access Token 생성
        String newAccess = jwtUtil.generateToken(username, roles);

        // JWT 쿠키 (HttpOnly)
        ResponseCookie accessCookie = ResponseCookie.from("JWT", newAccess)
                .httpOnly(true)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("None")
                .build();

        // JWT_PAYLOAD 쿠키 (JS 접근 가능)
        String[] parts = newAccess.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";
        ResponseCookie payloadCookie = ResponseCookie.from("JWT_PAYLOAD", payload)
                .httpOnly(false)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("None")
                .build();

        // 쿠키 헤더에 추가
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, payloadCookie.toString());

        return ResponseEntity.ok(Map.of("message", "token refreshed"));
    }
}