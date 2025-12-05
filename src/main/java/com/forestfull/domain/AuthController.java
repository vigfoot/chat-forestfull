package com.forestfull.domain;

import com.forestfull.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomUserDetailsService userService;
    private final JwtUtil jwtUtil;
    private final JwtUtil.Refresh refreshJwtUtil;

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

    @PostMapping("/api/auth/logout")
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
}