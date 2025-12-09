package com.forestfull.domain;

import com.forestfull.common.token.CookieUtil;
import com.forestfull.common.token.JwtUtil;
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
    private final CookieUtil cookieUtil;
    private final CustomUserDetailsService userService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        try {
            String name = body.get("username");
            String password = body.get("password");

            final Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(name, password));
            final User user = (User) auth.getPrincipal();

            // JWT 발급
            final String accessToken = jwtUtil.generateToken(user.getId(), user.getRoleList());
            final String refreshToken = refreshJwtUtil.generateToken(user.getId(), user.getRoleList());
            refreshJwtUtil.save(user.getId(), refreshToken);

            cookieUtil.addAccessToken(response, accessToken);
            cookieUtil.addPayload(response, accessToken);
            cookieUtil.addRefreshToken(response, refreshToken);

            return ResponseEntity.ok(Map.of("message", "Login successful"));

        } catch (AuthenticationException e) {
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
    public ResponseEntity<?> refreshToken(@CookieValue(value = "REFRESH", required = false) String refreshToken,
                                          HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token");

        Long userId = refreshJwtUtil.getUserId(refreshToken);
        if (userId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");

        if (!Objects.equals(refreshToken, refreshJwtUtil.getToken(userId)))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mismatch");

        // DB에서 권한 조회
        List<String> roles = userService.getRoles(userId);

        // 새 Access Token 생성
        String newAccess = jwtUtil.generateToken(userId, roles);
        cookieUtil.addAccessToken(response, newAccess);

        // JWT_PAYLOAD 쿠키 (JS 접근 가능)
        cookieUtil.addPayload(response, newAccess);

        return ResponseEntity.ok(Map.of("message", "token refreshed"));
    }

    @PostMapping("/signup")
    ResponseEntity<?> signup(@RequestBody User member) {
        return userService.signup(member) ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}