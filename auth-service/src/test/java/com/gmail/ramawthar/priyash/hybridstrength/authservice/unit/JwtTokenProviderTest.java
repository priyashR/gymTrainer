package com.gmail.ramawthar.priyash.hybridstrength.authservice.unit;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.outbound.JwtTokenProvider;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.config.JwtConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private JwtConfig jwtConfig;

    // A separate key pair for "wrong key" tests
    private RSAPrivateKey wrongPrivateKey;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "test@example.com";
    private static final String ROLE = "USER";

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        KeyPair wrongKeyPair = generator.generateKeyPair();
        wrongPrivateKey = (RSAPrivateKey) wrongKeyPair.getPrivate();

        jwtConfig = new JwtConfig();
        jwtConfig.setAccessTokenExpiry(Duration.ofMinutes(15));

        tokenProvider = new JwtTokenProvider(privateKey, publicKey, jwtConfig);
    }

    // --- generateAccessToken ---

    @Test
    @DisplayName("generateAccessToken — valid inputs — returns parseable JWT with correct claims")
    void generateAccessToken_ValidInputs_ReturnsParsableJwtWithCorrectClaims() throws Exception {
        String token = tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);

        SignedJWT jwt = SignedJWT.parse(token);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();

        assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(claims.getStringClaim("email")).isEqualTo(EMAIL);
        assertThat(claims.getStringClaim("role")).isEqualTo(ROLE);
    }

    @Test
    @DisplayName("generateAccessToken — valid inputs — token uses RS256 algorithm")
    void generateAccessToken_ValidInputs_UsesRS256() throws Exception {
        String token = tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);

        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
    }

    @Test
    @DisplayName("generateAccessToken — valid inputs — expiry is in the future within expected window")
    void generateAccessToken_ValidInputs_ExpiryIsInFutureWithinExpectedWindow() throws Exception {
        Instant before = Instant.now();
        String token = tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);
        Instant after = Instant.now();

        SignedJWT jwt = SignedJWT.parse(token);
        Instant expiry = jwt.getJWTClaimsSet().getExpirationTime().toInstant();

        // Expiry should be ~15 minutes from now, with a small tolerance
        assertThat(expiry).isAfter(before.plus(Duration.ofMinutes(14)));
        assertThat(expiry).isBefore(after.plus(Duration.ofMinutes(16)));
    }

    // --- generateRefreshToken ---

    @Test
    @DisplayName("generateRefreshToken — called — returns non-null UUID string")
    void generateRefreshToken_Called_ReturnsNonNullUuidString() {
        String refreshToken = tokenProvider.generateRefreshToken();

        assertThat(refreshToken).isNotNull().isNotBlank();
        // Should be a valid UUID
        assertThat(UUID.fromString(refreshToken)).isNotNull();
    }

    @Test
    @DisplayName("generateRefreshToken — called twice — returns distinct values")
    void generateRefreshToken_CalledTwice_ReturnsDistinctValues() {
        String first = tokenProvider.generateRefreshToken();
        String second = tokenProvider.generateRefreshToken();

        assertThat(first).isNotEqualTo(second);
    }

    // --- extractUserId ---

    @Test
    @DisplayName("extractUserId — valid token — returns correct user ID")
    void extractUserId_ValidToken_ReturnsCorrectUserId() {
        String token = tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);

        UUID extracted = tokenProvider.extractUserId(token);

        assertThat(extracted).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("extractUserId — garbage string — throws IllegalArgumentException")
    void extractUserId_GarbageString_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> tokenProvider.extractUserId("not-a-jwt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- validateAccessToken ---

    @Test
    @DisplayName("validateAccessToken — valid non-expired token — returns true")
    void validateAccessToken_ValidNonExpiredToken_ReturnsTrue() {
        String token = tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE);

        assertThat(tokenProvider.validateAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateAccessToken — expired token — returns false")
    void validateAccessToken_ExpiredToken_ReturnsFalse() throws Exception {
        // Build a token that expired 1 minute ago
        Instant past = Instant.now().minus(Duration.ofMinutes(1));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(USER_ID.toString())
                .claim("email", EMAIL)
                .claim("role", ROLE)
                .issueTime(Date.from(past.minus(Duration.ofMinutes(15))))
                .expirationTime(Date.from(past))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(privateKey));

        assertThat(tokenProvider.validateAccessToken(jwt.serialize())).isFalse();
    }

    @Test
    @DisplayName("validateAccessToken — token signed with wrong key — returns false")
    void validateAccessToken_WrongKey_ReturnsFalse() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(USER_ID.toString())
                .expirationTime(Date.from(Instant.now().plus(Duration.ofMinutes(15))))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(wrongPrivateKey));

        assertThat(tokenProvider.validateAccessToken(jwt.serialize())).isFalse();
    }

    @Test
    @DisplayName("validateAccessToken — random string — returns false")
    void validateAccessToken_RandomString_ReturnsFalse() {
        assertThat(tokenProvider.validateAccessToken("totally-not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("validateAccessToken — empty string — returns false")
    void validateAccessToken_EmptyString_ReturnsFalse() {
        assertThat(tokenProvider.validateAccessToken("")).isFalse();
    }

    @Test
    @DisplayName("validateAccessToken — null expiration claim — returns false")
    void validateAccessToken_NullExpiration_ReturnsFalse() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(USER_ID.toString())
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(privateKey));

        assertThat(tokenProvider.validateAccessToken(jwt.serialize())).isFalse();
    }
}
