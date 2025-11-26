package com.forestfull.config.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;

/**
 * com.forestfull.config.common
 *
 * @author vigfoot
 * @version 2025-11-25
 */
@Component
public class JwtProvider {

    private final String SECRET = "your-secret-key";
    private final Algorithm algorithm = Algorithm.HMAC256(SECRET);
    private final JWTVerifier verifier = JWT.require(algorithm).build();

    public boolean validateToken(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        DecodedJWT decodedJWT = verifier.verify(token);
        return decodedJWT.getSubject(); // sub 필드 기반 유저 식별
    }
}
