package com.forestfull.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.Getter;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class JwtUtil {

    @Getter
    private static final long expireMillis = 24 * 60 * 60 * 1000;
    private static final long refreshExpireMillis = 7L * 24 * 60 * 60 * 1000;  // 7일 유지
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    // Access Token 발급기
    public JwtUtil(String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
    }

    // Access Token 생성
    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMillis);
        return JWT.create()
                .withSubject(username)
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
    public static class Refresh {

        private final Algorithm algorithm;
        private final JWTVerifier verifier;

        // 임시 저장소(DB 대체) : username -> refreshToken
        private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

        public Refresh(String secret) {
            this.algorithm = Algorithm.HMAC256(secret);
            this.verifier = JWT.require(algorithm).build();
        }

        // Refresh Token 생성
        public String generateToken(String username, List<String> roles) {
            Date now = new Date();
            Date exp = new Date(now.getTime() + refreshExpireMillis);

            String token = JWT.create()
                    .withSubject(username)
                    .withIssuedAt(now)
                    .withExpiresAt(exp)
                    .sign(algorithm);

            store.put(username, token);
            return token;
        }

        // 저장(DB로 변경 가능)
        public void save(String username, String refreshToken) {
            store.put(username, refreshToken);
        }

        // 조회
        public String getToken(String username) {
            return store.get(username);
        }

        // 삭제
        public void deleteToken(String username) {
            store.remove(username);
        }

        // Refresh Token 검증
        public DecodedJWT verify(String token) throws JWTVerificationException {
            return verifier.verify(token);
        }
        // 🔥 Refresh Token 검증 후 username 반환
        public String getUsername(String refreshToken) {
            try {
                DecodedJWT jwt = verifier.verify(refreshToken);
                return jwt.getSubject(); // username 추출
            } catch (JWTVerificationException e) {
                return null; // 토큰 위조 or 만료된 경우
            }
        }
    }
}
