package com.forestfull.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;
import java.util.List;

public class JwtUtil {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expireMillis;

    public JwtUtil(String secret, long expireMillis) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.expireMillis = expireMillis;
    }

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

    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }

    public String getUsernameFromToken(DecodedJWT jwt) {
        return jwt.getSubject();
    }
}