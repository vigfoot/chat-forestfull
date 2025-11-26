package com.forestfull.config.jwt;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;

@RequiredArgsConstructor
@Component
public class JwtVerifier {

    private final JwtProperties props;

    public boolean validate(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            String alg = jwt.getAlgorithm();

            if ("HS256".equalsIgnoreCase(alg)) {
                Algorithm hs = Algorithm.HMAC256(props.getSecret());
                hs.verify(jwt);
                return true;
            }

            if ("RS256".equalsIgnoreCase(alg)) {
                JwkProvider provider = new UrlJwkProvider(new URL(props.getJwksUri()));
                RSAPublicKey publicKey = (RSAPublicKey) provider.get(jwt.getKeyId()).getPublicKey();
                Algorithm rsa = Algorithm.RSA256(publicKey, null);
                rsa.verify(jwt);
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return JWT.decode(token).getSubject();
    }
}
