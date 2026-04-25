package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.TokenProvider;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.config.JwtConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * RS256 JWT implementation of the {@link TokenProvider} outbound port.
 * <p>
 * Access tokens are signed JWTs with sub, email, and role claims.
 * Refresh tokens are opaque UUID strings (not JWTs).
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JWSSigner signer;
    private final JWSVerifier verifier;
    private final JwtConfig jwtConfig;

    public JwtTokenProvider(RSAPrivateKey privateKey, RSAPublicKey publicKey, JwtConfig jwtConfig) {
        this.signer = new RSASSASigner(privateKey);
        this.verifier = new RSASSAVerifier(publicKey);
        this.jwtConfig = jwtConfig;
    }

    @Override
    public String generateAccessToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtConfig.getAccessTokenExpiry());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
        return signedJWT.serialize();
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    public UUID extractUserId(String accessToken) {
        try {
            SignedJWT jwt = SignedJWT.parse(accessToken);
            return UUID.fromString(jwt.getJWTClaimsSet().getSubject());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid JWT: cannot extract user ID", e);
        }
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        try {
            SignedJWT jwt = SignedJWT.parse(accessToken);
            if (!jwt.verify(verifier)) {
                return false;
            }
            Date expiration = jwt.getJWTClaimsSet().getExpirationTime();
            return expiration != null && expiration.toInstant().isAfter(Instant.now());
        } catch (ParseException | JOSEException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
