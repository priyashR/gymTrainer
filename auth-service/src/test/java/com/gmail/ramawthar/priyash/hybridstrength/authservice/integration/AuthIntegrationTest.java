package com.gmail.ramawthar.priyash.hybridstrength.authservice.integration;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.inbound.dto.AccessTokenResponse;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.inbound.dto.LoginRequest;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.dto.ErrorResponse;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.adapters.inbound.dto.RegisterRequest;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.adapters.inbound.dto.RegisterResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Auth Service.
 * Uses external PostgreSQL 16 (Rancher Desktop K8s) with Flyway migrations.
 * Each test gets a clean database via TRUNCATE CASCADE.
 *
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.2, 2.3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    // Non-existent endpoint to test JWT authentication filter behaviour
    private static final String PROTECTED_URL = "/api/v1/protected/test";

    @BeforeEach
    void cleanTables() {
        // Truncate in dependency order; CASCADE handles FK constraints
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens, users CASCADE");
    }

    // --- Helpers ---

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders jsonHeadersWithBearer(String accessToken) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private ResponseEntity<RegisterResponse> register(String email, String password) {
        RegisterRequest request = new RegisterRequest(email, password);
        return restTemplate.postForEntity(REGISTER_URL, new HttpEntity<>(request, jsonHeaders()), RegisterResponse.class);
    }

    private ResponseEntity<AccessTokenResponse> login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        return restTemplate.postForEntity(LOGIN_URL, new HttpEntity<>(request, jsonHeaders()), AccessTokenResponse.class);
    }

    private String extractRefreshTokenCookie(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull().isNotEmpty();
        return cookies.stream()
                .filter(c -> c.startsWith("refreshToken="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("refreshToken cookie not found"));
    }

    private String extractRefreshTokenValue(String cookieHeader) {
        String tokenPart = cookieHeader.split(";")[0];
        return tokenPart.substring("refreshToken=".length());
    }

    // --- Tests ---

    @Test
    @DisplayName("Context loads with Flyway migrations — verifies schema applied on startup (Req 2.2, 2.3)")
    void contextLoads_WithFlywayMigrations_ApplicationStartsSuccessfully() {
        // If we reach this point, the Spring context loaded successfully
        // with Flyway migrations applied against real PostgreSQL.
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('users', 'refresh_tokens')",
                Integer.class);
        assertThat(tableCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Full happy path: register → login → access protected → refresh → access again (Req 1.1, 1.3, 1.4, 1.5)")
    void fullHappyPath_RegisterLoginRefreshAccess_Succeeds() {
        String email = uniqueEmail();
        String password = "SecurePass123!";

        // 1. Register
        ResponseEntity<RegisterResponse> registerResp = register(email, password);
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResp.getBody()).isNotNull();
        assertThat(registerResp.getBody().email()).isEqualTo(email);
        assertThat(registerResp.getBody().id()).isNotNull();

        // 2. Login
        ResponseEntity<AccessTokenResponse> loginResp = login(email, password);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).isNotNull();
        assertThat(loginResp.getBody().accessToken()).isNotBlank();
        assertThat(loginResp.getBody().tokenType()).isEqualTo("Bearer");
        assertThat(loginResp.getBody().expiresIn()).isGreaterThan(0);

        String accessToken = loginResp.getBody().accessToken();
        String refreshCookie = extractRefreshTokenCookie(loginResp);

        // 3. Access protected endpoint with valid token — must NOT be 401
        ResponseEntity<String> protectedResp = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET,
                new HttpEntity<>(jsonHeadersWithBearer(accessToken)),
                String.class);
        assertThat(protectedResp.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);

        // 4. Refresh token
        String refreshTokenValue = extractRefreshTokenValue(refreshCookie);
        HttpHeaders refreshHeaders = jsonHeaders();
        refreshHeaders.add(HttpHeaders.COOKIE, "refreshToken=" + refreshTokenValue);

        ResponseEntity<AccessTokenResponse> refreshResp = restTemplate.exchange(
                REFRESH_URL, HttpMethod.POST,
                new HttpEntity<>(null, refreshHeaders),
                AccessTokenResponse.class);
        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResp.getBody()).isNotNull();
        assertThat(refreshResp.getBody().accessToken()).isNotBlank();

        String newAccessToken = refreshResp.getBody().accessToken();
        // The refresh token must have rotated (new cookie value)
        String newRefreshCookie = extractRefreshTokenCookie(refreshResp);
        String newRefreshTokenValue = extractRefreshTokenValue(newRefreshCookie);
        assertThat(newRefreshTokenValue).isNotEqualTo(refreshTokenValue);

        // 5. Access protected endpoint again with refreshed token
        ResponseEntity<String> protectedResp2 = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET,
                new HttpEntity<>(jsonHeadersWithBearer(newAccessToken)),
                String.class);
        assertThat(protectedResp2.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Register with duplicate email → 409 Conflict (Req 1.2)")
    void register_DuplicateEmail_Returns409Conflict() {
        String email = uniqueEmail();
        String password = "SecurePass123!";

        ResponseEntity<RegisterResponse> first = register(email, password);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ErrorResponse> second = restTemplate.postForEntity(
                REGISTER_URL,
                new HttpEntity<>(new RegisterRequest(email, password), jsonHeaders()),
                ErrorResponse.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().status()).isEqualTo(409);
    }

    @Test
    @DisplayName("Login with wrong password → 401 Unauthorised (Req 1.3)")
    void login_WrongPassword_Returns401Unauthorised() {
        String email = uniqueEmail();
        register(email, "SecurePass123!");

        ResponseEntity<ErrorResponse> resp = restTemplate.postForEntity(
                LOGIN_URL,
                new HttpEntity<>(new LoginRequest(email, "WrongPassword!"), jsonHeaders()),
                ErrorResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Access protected endpoint without token → 401 Unauthorised (Req 1.5)")
    void accessProtected_NoToken_Returns401Unauthorised() {
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                ErrorResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Access protected endpoint with invalid token → 401 Unauthorised (Req 1.5)")
    void accessProtected_InvalidToken_Returns401Unauthorised() {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth("this.is.not.a.valid.jwt");

        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET,
                new HttpEntity<>(headers),
                ErrorResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Old refresh token is invalidated after rotation (Req 1.4)")
    void refresh_OldTokenAfterRotation_Returns401() {
        String email = uniqueEmail();
        register(email, "SecurePass123!");
        ResponseEntity<AccessTokenResponse> loginResp = login(email, "SecurePass123!");
        String oldRefreshToken = extractRefreshTokenValue(extractRefreshTokenCookie(loginResp));

        // First refresh succeeds and rotates the token
        HttpHeaders refreshHeaders = jsonHeaders();
        refreshHeaders.add(HttpHeaders.COOKIE, "refreshToken=" + oldRefreshToken);
        ResponseEntity<AccessTokenResponse> refreshResp = restTemplate.exchange(
                REFRESH_URL, HttpMethod.POST,
                new HttpEntity<>(null, refreshHeaders),
                AccessTokenResponse.class);
        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Second refresh with the OLD token should fail — it was rotated out
        ResponseEntity<ErrorResponse> replayResp = restTemplate.exchange(
                REFRESH_URL, HttpMethod.POST,
                new HttpEntity<>(null, refreshHeaders),
                ErrorResponse.class);
        assertThat(replayResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
