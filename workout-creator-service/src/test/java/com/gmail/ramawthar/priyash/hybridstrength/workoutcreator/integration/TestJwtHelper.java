package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.integration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;

/**
 * Test utility that generates a 2048-bit RSA key pair and issues signed JWTs
 * for use in integration tests.
 * <p>
 * The public key is exposed so it can be registered as the {@code RSAPublicKey}
 * bean in the test Spring context, replacing the randomly-generated production key.
 */
public final class TestJwtHelper {

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public TestJwtHelper() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key pair generation failed in test helper", e);
        }
    }

    /**
     * Returns the RSA public key — register this as the {@code RSAPublicKey} bean
     * in the test Spring context so the {@code JwtAuthenticationFilter} can verify
     * tokens issued by this helper.
     */
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Issues a signed RS256 JWT with the given subject (userId) and a 1-hour expiry.
     *
     * @param userId the UUID to use as the JWT subject claim
     * @return a compact serialised JWT string suitable for use in an Authorization header
     */
    public String issueToken(UUID userId) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000L)) // 1 hour
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.RS256),
                    claims
            );
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign test JWT", e);
        }
    }

    /**
     * Issues a signed RS256 JWT that is already expired (1 hour in the past).
     * Useful for testing 401 responses on expired tokens.
     */
    public String issueExpiredToken(UUID userId) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .issueTime(new Date(System.currentTimeMillis() - 7_200_000L))
                    .expirationTime(new Date(System.currentTimeMillis() - 3_600_000L))
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.RS256),
                    claims
            );
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign expired test JWT", e);
        }
    }
}
