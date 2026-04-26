package com.gmail.ramawthar.priyash.hybridstrength.authservice.property;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.outbound.JwtTokenProvider;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.config.JwtConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Property-based tests for JWT validation.
 * Verifies that any string that is not a validly-signed, non-expired JWT
 * is rejected by {@link JwtTokenProvider#validateAccessToken(String)}.
 */
class JwtValidationPropertyTest {

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final RSAPrivateKey wrongPrivateKey;
    private final JwtTokenProvider tokenProvider;

    JwtValidationPropertyTest() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        KeyPair wrongKeyPair = generator.generateKeyPair();
        wrongPrivateKey = (RSAPrivateKey) wrongKeyPair.getPrivate();

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAccessTokenExpiry(Duration.ofMinutes(15));
        tokenProvider = new JwtTokenProvider(privateKey, publicKey, jwtConfig);
    }

    // Feature: auth-service-mvp1, Property 5: Invalid tokens are rejected
    // For any arbitrary string, validateAccessToken returns false (random strings are not valid JWTs).
    // **Validates: Requirements 1.5**
    @Property(tries = 100)
    void validateAccessToken_RandomString_ReturnsFalse(
            @ForAll @StringLength(min = 0, max = 500) String randomString) {
        assertFalse(tokenProvider.validateAccessToken(randomString),
                "Random string must not be accepted as a valid token");
    }

    // Feature: auth-service-mvp1, Property 5: Invalid tokens are rejected
    // For any user, an expired token signed with the correct key must be rejected.
    // **Validates: Requirements 1.5**
    @Property(tries = 100)
    void validateAccessToken_ExpiredToken_ReturnsFalse(
            @ForAll @StringLength(min = 1, max = 50) String email) throws Exception {
        UUID userId = UUID.randomUUID();
        Instant expired = Instant.now().minus(Duration.ofMinutes(5));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", "USER")
                .issueTime(Date.from(expired.minus(Duration.ofMinutes(15))))
                .expirationTime(Date.from(expired))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(privateKey));

        assertFalse(tokenProvider.validateAccessToken(jwt.serialize()),
                "Expired token must be rejected even if signature is valid");
    }

    // Feature: auth-service-mvp1, Property 5: Invalid tokens are rejected
    // For any user, a token signed with a different RSA key must be rejected.
    // **Validates: Requirements 1.5**
    @Property(tries = 100)
    void validateAccessToken_WrongKeyToken_ReturnsFalse(
            @ForAll @StringLength(min = 1, max = 50) String email) throws Exception {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", "USER")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(15))))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(wrongPrivateKey));

        assertFalse(tokenProvider.validateAccessToken(jwt.serialize()),
                "Token signed with a different key must be rejected");
    }
}
