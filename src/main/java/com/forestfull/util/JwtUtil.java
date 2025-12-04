package com.forestfull.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.Getter;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class JwtUtil {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    @Getter
    private final long expireMillis;

    public JwtUtil(String secret, long expireMillis) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.expireMillis = expireMillis;
    }

    // JWT 생성
    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMillis);
        return JWT.create()
                .withSubject(username)
                .withArrayClaim("roles", roles.toArray(new String[0]))
                .withIssuedAt(now)
                .withExpiresAt(exp)
                .sign(algorithm);
    }

    // JWT 검증 후 username과 roles 반환
    public Map<String, Object> verifyAndExtract(String token) throws JWTVerificationException {
        DecodedJWT decoded = verifier.verify(token);
        String username = decoded.getSubject();
        List<String> roles = decoded.getClaim("roles").asList(String.class);
        return Map.of(
                "username", username,
                "roles", roles
        );
    }
}