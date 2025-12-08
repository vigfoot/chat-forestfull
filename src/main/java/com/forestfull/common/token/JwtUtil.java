package com.forestfull.common.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.forestfull.domain.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    public static final long expireMillis = 24 * 60 * 60 * 1000;
    public static final long refreshExpireMillis = 7L * 24 * 60 * 60 * 1000;  // 7일 유지

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtUtil(@Value("${key}") String secretKey) {
        this.algorithm = Algorithm.HMAC256(secretKey);
        this.verifier = JWT.require(algorithm).build();
    }

    // Access Token 생성
    public String generateToken(String name, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMillis);
        return JWT.create()
                .withSubject(name)
                .withClaim("roles", roles)
                .withIssuedAt(now)
                .withExpiresAt(exp)
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
        ;
        private final RefreshTokenMapper refreshTokenMapper;
        private final UserMapper userMapper;

        public Refresh(@Value("${key}") String secretKey, RefreshTokenMapper refreshTokenMapper, UserMapper userMapper) {
            this.algorithm = Algorithm.HMAC256(secretKey);
            this.verifier = JWT.require(algorithm).build();
            ;
            this.refreshTokenMapper = refreshTokenMapper;
            this.userMapper = userMapper;
        }

        // Refresh Token 생성
        public String generateToken(String name) {
            Date now = new Date();
            Date exp = new Date(now.getTime() + refreshExpireMillis);

            String token = JWT.create()
                    .withSubject(name)
                    .withIssuedAt(now)
                    .withExpiresAt(exp)
                    .sign(algorithm);

            Long memberId = userMapper.findIdByUsername(name);
            if (memberId == null) {
                throw new IllegalStateException("member not found for username: " + name);
            }

            // expiry_date: LocalDateTime 변환
            LocalDateTime expiryDate = LocalDateTime.ofInstant(exp.toInstant(), ZoneId.systemDefault());

            refreshTokenMapper.save(memberId, token, expiryDate);
            return token;
        }

        // 토큰을 저장(이미 다른 로직에서 생성했을 때 사용)
        public void save(String name, String refreshToken) {
            Long memberId = userMapper.findIdByUsername(name);
            if (memberId == null) {
                throw new IllegalStateException("member not found for username: " + name);
            }
            // 토큰 만료시간 파싱
            DecodedJWT decoded = verifier.verify(refreshToken);
            LocalDateTime expiryDate = LocalDateTime.ofInstant(decoded.getExpiresAt().toInstant(), ZoneId.systemDefault());
            refreshTokenMapper.save(memberId, refreshToken, expiryDate);
        }

        // name 기준으로 DB에서 유효한 토큰 조회
        public String getToken(String name) {
            Long memberId = userMapper.findIdByUsername(name);
            if (memberId == null) {
                return null;
            }
            return refreshTokenMapper.findValidTokenByMemberId(memberId);
        }

        // 사용자 로그아웃 등에서 토큰 폐기
        public void deleteTokenByUsername(String name) {
            Long memberId = userMapper.findIdByUsername(name);
            if (memberId == null) return;
            refreshTokenMapper.revokeByMemberId(memberId);
        }

        // 검증: token 자체를 검증하고 name 반환 (null = invalid)
        public String getUsername(String refreshToken) {
            try {
                DecodedJWT jwt = verifier.verify(refreshToken);
                return jwt.getSubject();
            } catch (JWTVerificationException e) {
                return null;
            }
        }

        // Refresh Token 유효성 검증 + DB 저장 여부 확인
        public String validateAndGetUsername(String refreshToken) {
            try {
                // 1️⃣ JWT 자체 검증 (서명, 만료)
                DecodedJWT jwt = verifier.verify(refreshToken);
                String name = jwt.getSubject();

                if (name == null) return null;

                // 2️⃣ DB에 저장된 토큰과 비교 (DB에 없으면 무효)
                Long memberId = userMapper.findIdByUsername(name);
                if (memberId == null) return null;

                String tokenInDb = refreshTokenMapper.findValidTokenByMemberId(memberId);
                if (tokenInDb == null) return null;

                if (!tokenInDb.equals(refreshToken)) {
                    return null; // 다른 Refresh 토큰이면 무효
                }

                return name;
            } catch (Exception e) {
                return null; // JWT 서명/만료 오류 → 무효 토큰
            }
        }

        public DecodedJWT verify(String refreshToken) {
            return verifier.verify(refreshToken);
        }
    }
}
