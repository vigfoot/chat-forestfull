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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
    private final AuthenticationManager authenticationManager;

    @Value("${spring.config.activate.on-profile}")
    private String onProfile;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        try {
            String name = body.get("username");
            String password = body.get("password");

            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(name, password));

            List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

            // JWT 발급
            String accessToken = jwtUtil.generateToken(name, roles);
            String refreshToken = refreshJwtUtil.generateToken(name);
            refreshJwtUtil.save(name, refreshToken);

            // 쿠키 세팅
            Cookie accessCookie = new Cookie(JwtUtil.TOKEN_TYPE.JWT.name(), accessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure("prod".equals(onProfile));
            accessCookie.setPath("/");
            accessCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
            accessCookie.setAttribute("SameSite", "prod".equals(onProfile) ? "None" : "Lax");
            response.addCookie(accessCookie);

            // JWT_PAYLOAD 쿠키 (JS 접근 가능)
            String[] parts = accessToken.split("\\.");
            String payload = parts.length > 1 ? parts[1] : "";
            Cookie payloadCookie = new Cookie(JwtUtil.TOKEN_TYPE.JWT_PAYLOAD.name(), payload);
            payloadCookie.setHttpOnly(false);
            payloadCookie.setSecure("prod".equals(onProfile));
            payloadCookie.setPath("/");
            payloadCookie.setMaxAge((int) (JwtUtil.expireMillis / 1000));
            payloadCookie.setAttribute("SameSite", "Lax");
            response.addCookie(payloadCookie);

            Cookie refreshCookie = new Cookie(JwtUtil.TOKEN_TYPE.REFRESH.name(), refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure("prod".equals(onProfile));
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge((int) (JwtUtil.refreshExpireMillis / 1000));
            refreshCookie.setAttribute("SameSite", "prod".equals(onProfile) ? "None" : "Lax");
            response.addCookie(refreshCookie);

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
            try {
                String name = jwtUtil.verifyToken(token).getSubject();
                refreshJwtUtil.deleteTokenByUsername(name);
            } catch (Exception e) {
            }
        }

        // 쿠키 삭제
        Arrays.stream(JwtUtil.TOKEN_TYPE.values())
                .map(Enum::name)
                .forEach(name -> {
                    Cookie c = new Cookie(name, null);
                    c.setHttpOnly(JwtUtil.TOKEN_TYPE.JWT.name().equals(name) || JwtUtil.TOKEN_TYPE.REFRESH.name().equals(name));
                    c.setSecure("prod".equals(onProfile));
                    c.setPath("/");
                    c.setMaxAge(0);
                    c.setAttribute("SameSite", name.equals(JwtUtil.TOKEN_TYPE.JWT_PAYLOAD.name()) ? "Lax" : "None");
                    response.addCookie(c);
                });

        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "REFRESH", required = false) String refreshToken,
                                          HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token");

        String name = refreshJwtUtil.getUsername(refreshToken);
        if (name == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");

        if (!Objects.equals(refreshToken, refreshJwtUtil.getToken(name)))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mismatch");

        // DB에서 권한 조회
        UserDetails userDetails = userService.loadUserByUsername(name);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList();

        // 새 Access Token 생성
        String newAccess = jwtUtil.generateToken(name, roles);

        // JWT 쿠키 (HttpOnly)
        ResponseCookie accessCookie = ResponseCookie.from(JwtUtil.TOKEN_TYPE.JWT.name(), newAccess)
                .httpOnly(true)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("prod".equals(onProfile) ? "None" : "Lax")
                .build();

        // JWT_PAYLOAD 쿠키 (JS 접근 가능)
        String[] parts = newAccess.split("\\.");
        String payload = parts.length > 1 ? parts[1] : "";
        ResponseCookie payloadCookie = ResponseCookie.from(JwtUtil.TOKEN_TYPE.JWT_PAYLOAD.name(), payload)
                .httpOnly(false)
                .secure("prod".equals(onProfile))
                .path("/")
                .maxAge(JwtUtil.expireMillis / 1000)
                .sameSite("Lax")
                .build();

        // 쿠키 헤더에 추가
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, payloadCookie.toString());

        return ResponseEntity.ok(Map.of("message", "token refreshed"));
    }

    @PostMapping("/signup")
    ResponseEntity<?> signup(@RequestBody MemberDTO.Member member) {
        return userService.signup(member) ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}