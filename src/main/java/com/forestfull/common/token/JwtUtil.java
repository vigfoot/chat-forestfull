package com.forestfull.common.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.forestfull.domain.User;
import com.forestfull.domain.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
public class JwtUtil {
    public static final long expireMillis = 24 * 60 * 60 * 1000;
    public static final long refreshExpireMillis = 7L * 24 * 60 * 60 * 1000;  // 7일 유지

    public enum TOKEN_TYPE {
        JWT, JWT_PAYLOAD, REFRESH
    }

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtUtil(@Value("${key}") String secretKey) {
        this.algorithm = Algorithm.HMAC256(secretKey);
        this.verifier = JWT.require(algorithm).build();
    }

    public Optional<DecodedJWT> getJwtToken(HttpServletRequest request) {
        return Arrays.stream(request.getCookies())
                .filter(cookie -> TOKEN_TYPE.JWT.name().equals(cookie.getName()))
                .map(cookie -> verifier.verify(cookie.getValue()))
                .findFirst();
    }

    // JwtUtil.java 예시
    public String generateToken(User user) {
        return JWT.create()
                .withSubject(String.valueOf(user.getId()))
                .withClaim("roles", user.getRoles())
                .withClaim("username", user.getUsername())
                .withClaim("email", user.getEmail())
                .withClaim("displayName", user.getDisplayName())
                .withClaim("profileImage", user.getProfileImage())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expireMillis))
                .sign(algorithm);
    }

    // Access Token 검증
    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }

    /**
     * Refresh Token 관리 (Static Inner Class)
     * - 역할 분리 : Access Token과 따로 만료시간 다르게
     * - 서버 저장소(메모리) 기반: 이후 DB 연결로 확장 가능
     */
    @Component
    public static class Refresh {
        private final Algorithm algorithm;
        private final JWTVerifier verifier;
        private final UserMapper userMapper;
        private final RefreshTokenMapper refreshTokenMapper;

        public Refresh(@Value("${key}") String secretKey, UserMapper userMapper, RefreshTokenMapper refreshTokenMapper) {
            this.algorithm = Algorithm.HMAC256(secretKey);
            this.verifier = JWT.require(algorithm).build();
            this.userMapper = userMapper;
            this.refreshTokenMapper = refreshTokenMapper;
        }

        // Refresh Token 생성
        public String generateToken(User user) {
            Date expiresAt = new Date(System.currentTimeMillis() + refreshExpireMillis);
            String token = JWT.create()
                    .withSubject(String.valueOf(user.getId()))
                    .withClaim("roles", user.getRoles())
                    .withClaim("username", user.getUsername())
                    .withClaim("email", user.getEmail())
                    .withClaim("displayName", user.getDisplayName())
                    .withClaim("profileImage", user.getProfileImage())
                    .withIssuedAt(new Date())
                    .withExpiresAt(expiresAt)
                    .sign(algorithm);

            Long memberId = userMapper.findUserIdById(user.getId());
            if (memberId == null) {
                throw new IllegalStateException("member not found for username: " + user.getId());
            }

            LocalDateTime expiryDate = LocalDateTime.ofInstant(expiresAt.toInstant(), ZoneId.systemDefault());
            refreshTokenMapper.save(memberId, token, expiryDate);

            return token;
        }

        // 토큰을 저장(이미 다른 로직에서 생성했을 때 사용)
        public void save(Long id, String refreshToken) {
            Long memberId = userMapper.findUserIdById(id);
            if (memberId == null) {
                throw new IllegalStateException("member not found for id: " + id);
            }
            // 토큰 만료시간 파싱
            DecodedJWT decoded = verifier.verify(refreshToken);
            LocalDateTime expiryDate = LocalDateTime.ofInstant(decoded.getExpiresAt().toInstant(), ZoneId.systemDefault());
            refreshTokenMapper.save(memberId, refreshToken, expiryDate);
        }

        // name 기준으로 DB에서 유효한 토큰 조회
        public String getToken(Long id) {
            Long memberId = userMapper.findUserIdById(id);
            if (memberId == null) {
                return null;
            }
            return refreshTokenMapper.findValidTokenByMemberId(memberId);
        }

        // 사용자 로그아웃 등에서 토큰 폐기
        public void deleteTokenByUserId(Long id) {
            Long memberId = userMapper.findUserIdById(id);
            if (memberId == null) return;
            refreshTokenMapper.revokeByMemberId(memberId);
        }

        // 검증: token 자체를 검증하고 name 반환 (null = invalid)
        public Long getUserId(String refreshToken) {
            try {
                DecodedJWT jwt = verifier.verify(refreshToken);
                return Long.valueOf(jwt.getSubject());
            } catch (JWTVerificationException e) {
                return null;
            }
        }

        // Refresh Token 유효성 검증 + DB 저장 여부 확인
        public User validateAndGetUser(String refreshToken) {
            try {
                // 1️⃣ JWT 자체 검증 (서명, 만료)
                DecodedJWT jwt = verifier.verify(refreshToken);

                Long id = Long.valueOf(jwt.getSubject());

                if (jwt.getSubject() == null) throw new RuntimeException();

                // 2️⃣ DB에 저장된 토큰과 비교 (DB에 없으면 무효)
                User user = userMapper.findByUserId(id);
                if (Objects.isNull(user)) throw new RuntimeException();

                String tokenInDB = refreshTokenMapper.findValidTokenByMemberId(user.getId());
                if (tokenInDB == null) throw new RuntimeException();

                if (!tokenInDB.equals(refreshToken)) {
                    return null; // 다른 Refresh 토큰이면 무효
                }

                return user;
            } catch (Exception e) {
                return null; // JWT 서명/만료 오류 → 무효 토큰
            }
        }

        public DecodedJWT verify(String refreshToken) {
            return verifier.verify(refreshToken);
        }
    }
}