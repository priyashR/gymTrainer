package com.gmail.ramawthar.priyash.hybridstrength.authservice.property;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.outbound.JwtTokenProvider;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.application.LoginService;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.application.RefreshTokenService;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.TokenPair;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.TokenProvider;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.config.JwtConfig;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.property.fake.FakeBcryptPasswordEncoder;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.property.fake.InMemoryRefreshTokenRepository;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.property.fake.InMemoryUserRepository;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;
import com.nimbusds.jwt.SignedJWT;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.CharRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.web.api.Email;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for authentication (login and token refresh).
 * Uses in-memory fakes (not mocks) for outbound ports — no Spring context needed.
 */
class AuthenticationPropertyTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final FakeBcryptPasswordEncoder passwordEncoder = new FakeBcryptPasswordEncoder();
    private final InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
    private final JwtConfig jwtConfig = new JwtConfig();
    private final TokenProvider tokenProvider;
    private final LoginService loginService;
    private final RefreshTokenService refreshTokenService;

    AuthenticationPropertyTest() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            this.tokenProvider = new JwtTokenProvider(privateKey, publicKey, jwtConfig);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise RSA key pair for tests", e);
        }
        this.loginService = new LoginService(userRepository, passwordEncoder, tokenProvider, refreshTokenRepository, jwtConfig);
        this.refreshTokenService = new RefreshTokenService(refreshTokenRepository, tokenProvider, userRepository, jwtConfig);
    }

    // Feature: auth-service-mvp1, Property 3: Login produces valid JWT
    // **Validates: Requirements 1.3**
    @Property(tries = 100)
    void login_ValidCredentials_ReturnsParseableJwtWithCorrectClaimsAndFutureExpiry(
            @ForAll @Email String email,
            @ForAll @StringLength(min = 8, max = 72) @CharRange(from = '!', to = '~') String password) {

        userRepository.clear();
        refreshTokenRepository.clear();

        // Register a user with hashed password
        UUID userId = UUID.randomUUID();
        String hashedPassword = passwordEncoder.encode(password);
        Instant now = Instant.now();
        User user = new User(userId, email, hashedPassword, "USER", now, now);
        userRepository.save(user);

        // Login with correct credentials
        TokenPair tokenPair = loginService.login(email, password);

        // (a) Access token must be non-null
        assertNotNull(tokenPair.getAccessToken(), "Access token must not be null");

        // (b) Access token must be parseable as a valid JWT with correct claims
        try {
            SignedJWT jwt = SignedJWT.parse(tokenPair.getAccessToken());

            String sub = jwt.getJWTClaimsSet().getSubject();
            assertEquals(userId.toString(), sub,
                    "JWT sub claim must match the user's ID");

            String emailClaim = (String) jwt.getJWTClaimsSet().getClaim("email");
            assertEquals(email, emailClaim,
                    "JWT email claim must match the user's email");

            // (c) Expiry must be in the future
            Date expiration = jwt.getJWTClaimsSet().getExpirationTime();
            assertNotNull(expiration, "JWT must have an expiration time");
            assertTrue(expiration.toInstant().isAfter(Instant.now()),
                    "JWT expiry must be in the future");

        } catch (java.text.ParseException e) {
            fail("Access token must be parseable as a valid JWT, but parsing failed: " + e.getMessage());
        }
    }

    // Feature: auth-service-mvp1, Property 4: Token refresh produces new access token
    // **Validates: Requirements 1.4**
    @Property(tries = 100)
    void refresh_ValidRefreshToken_ReturnsNewJwtWithCorrectSubAndInvalidatesOldToken(
            @ForAll @Email String email,
            @ForAll @StringLength(min = 8, max = 72) @CharRange(from = '!', to = '~') String password) {

        userRepository.clear();
        refreshTokenRepository.clear();

        // Register a user
        UUID userId = UUID.randomUUID();
        String hashedPassword = passwordEncoder.encode(password);
        Instant now = Instant.now();
        User user = new User(userId, email, hashedPassword, "USER", now, now);
        userRepository.save(user);

        // Login to obtain a valid refresh token stored in the repository
        TokenPair loginResult = loginService.login(email, password);
        String originalRefreshToken = loginResult.getRefreshToken();

        // Refresh using the original refresh token
        TokenPair refreshResult = refreshTokenService.refresh(originalRefreshToken);

        // (a) New access token must be parseable as a valid JWT with correct sub claim
        assertNotNull(refreshResult.getAccessToken(), "Refreshed access token must not be null");
        try {
            SignedJWT jwt = SignedJWT.parse(refreshResult.getAccessToken());

            String sub = jwt.getJWTClaimsSet().getSubject();
            assertEquals(userId.toString(), sub,
                    "Refreshed JWT sub claim must match the user's ID");

            Date expiration = jwt.getJWTClaimsSet().getExpirationTime();
            assertNotNull(expiration, "Refreshed JWT must have an expiration time");
            assertTrue(expiration.toInstant().isAfter(Instant.now()),
                    "Refreshed JWT expiry must be in the future");

        } catch (java.text.ParseException e) {
            fail("Refreshed access token must be parseable as a valid JWT: " + e.getMessage());
        }

        // (b) Old refresh token must be invalidated (no longer found in the repository)
        assertTrue(refreshTokenRepository.findByToken(originalRefreshToken).isEmpty(),
                "Old refresh token must be invalidated after refresh");

        // (c) New refresh token must be different from the old one (rotation)
        assertNotEquals(originalRefreshToken, refreshResult.getRefreshToken(),
                "Rotated refresh token must differ from the original");

        // (d) New refresh token must be stored in the repository
        assertTrue(refreshTokenRepository.findByToken(refreshResult.getRefreshToken()).isPresent(),
                "New rotated refresh token must be stored in the repository");
    }
}
