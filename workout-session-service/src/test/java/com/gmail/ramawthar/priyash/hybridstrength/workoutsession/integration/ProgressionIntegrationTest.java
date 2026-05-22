package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for the Workout Session Service — progression lifecycle.
 *
 * <p>Tests the full HTTP request/response cycle: enroll → advance day (via session
 * completion) → skip day → complete program. Uses H2 with Flyway migrations and
 * WireMock to stub the Workout Creator Service for session start.
 *
 * <p>Validates: Requirements 2.1, 2.3, 2.7, 2.8
 */
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.rabbitmq.listener.simple.auto-startup=false"
        }
)
@ActiveProfiles("integration")
class ProgressionIntegrationTest {

    // ── Shared RSA key pair (generated once for the whole test class) ─────────

    private static RSAPrivateKey testPrivateKey;
    private static RSAPublicKey testPublicKey;
    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        testPrivateKey = (RSAPrivateKey) pair.getPrivate();
        testPublicKey = (RSAPublicKey) pair.getPublic();

        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("workout-creator-service.base-url",
                () -> "http://localhost:" + wireMockServer.port());
    }

    // ── @TestConfiguration: override the RSAPublicKey bean ───────────────────

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        RSAPublicKey rsaPublicKey() {
            return testPublicKey;
        }
    }

    // ── Spring wiring ─────────────────────────────────────────────────────────

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void stubWorkoutCreatorService() {
        wireMockServer.resetAll();
        wireMockServer.stubFor(get(urlPathMatching("/api/v1/vault/programs/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(validProgramJson())));
    }

    @AfterEach
    void cleanTables() {
        jdbcTemplate.execute("DELETE FROM skip_records");
        jdbcTemplate.execute("DELETE FROM sessions");
        jdbcTemplate.execute("DELETE FROM program_enrollments");
    }

    // =========================================================================
    // Enrollment Tests
    // =========================================================================

    @Test
    @DisplayName("Enroll in program — returns 201 with ACTIVE enrollment at week 1, day 1")
    void enrollProgram_ReturnsCreatedEnrollment() {
        String userId = UUID.randomUUID().toString();

        Map<String, Object> request = Map.of(
                "programId", UUID.randomUUID().toString(),
                "programName", "Hypertrophy 12-Week",
                "totalWeeks", 12,
                "totalDaysPerWeek", 5
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("ACTIVE");
        assertThat(response.getBody().get("currentWeek")).isEqualTo(1);
        assertThat(response.getBody().get("currentDay")).isEqualTo(1);
        assertThat(response.getBody().get("totalWeeks")).isEqualTo(12);
        assertThat(response.getBody().get("programName")).isEqualTo("Hypertrophy 12-Week");
        assertThat(response.getBody().get("enrolledAt")).isNotNull();
        assertThat(response.getBody().get("nextDay")).isNotNull();
    }

    @Test
    @DisplayName("Get active enrollment — returns current enrollment")
    void getActiveEnrollment_ReturnsEnrollment() {
        String userId = UUID.randomUUID().toString();
        enrollUser(userId, "Test Program", 4, 3);

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("ACTIVE");
        assertThat(response.getBody().get("programName")).isEqualTo("Test Program");
    }

    @Test
    @DisplayName("Get active enrollment — returns 204 when no enrollment exists")
    void getActiveEnrollment_NoEnrollment_Returns204() {
        String userId = UUID.randomUUID().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Skip day — advances pointer and records skip")
    void skipDay_AdvancesPointerAndRecordsSkip() {
        String userId = UUID.randomUUID().toString();
        UUID enrollmentId = enrollUser(userId, "Test Program", 4, 3);

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl() + "/" + enrollmentId + "/skip",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Should advance from day 1 to day 2
        assertThat(response.getBody().get("currentWeek")).isEqualTo(1);
        assertThat(response.getBody().get("currentDay")).isEqualTo(2);

        // Verify skip record was persisted
        Integer skipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM skip_records WHERE enrollment_id = ?",
                Integer.class, enrollmentId);
        assertThat(skipCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Enroll replaces existing active enrollment")
    void enrollProgram_ReplacesExistingEnrollment() {
        String userId = UUID.randomUUID().toString();

        // First enrollment
        UUID firstEnrollmentId = enrollUser(userId, "Program A", 8, 4);

        // Second enrollment — should replace the first
        Map<String, Object> request = Map.of(
                "programId", UUID.randomUUID().toString(),
                "programName", "Program B",
                "totalWeeks", 6,
                "totalDaysPerWeek", 3
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("programName")).isEqualTo("Program B");
        assertThat(response.getBody().get("currentWeek")).isEqualTo(1);
        assertThat(response.getBody().get("currentDay")).isEqualTo(1);

        // Verify the first enrollment was marked as REPLACED
        String firstStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM program_enrollments WHERE id = ?",
                String.class, firstEnrollmentId);
        assertThat(firstStatus).isEqualTo("REPLACED");
    }

    @Test
    @DisplayName("Skip day at end of week — advances to next week day 1")
    void skipDay_EndOfWeek_AdvancesToNextWeek() {
        String userId = UUID.randomUUID().toString();
        // Create enrollment with 2 days per week, then manually set to day 2
        UUID enrollmentId = enrollUser(userId, "Test Program", 4, 2);

        // Skip day 1 → moves to day 2
        ResponseEntity<Map> skip1 = restTemplate.exchange(
                enrollmentsUrl() + "/" + enrollmentId + "/skip",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        assertThat(skip1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(skip1.getBody().get("currentWeek")).isEqualTo(1);
        assertThat(skip1.getBody().get("currentDay")).isEqualTo(2);
    }

    // =========================================================================
    // Full Lifecycle Tests
    // =========================================================================

    @Test
    @DisplayName("Full lifecycle — enroll → advance day via session end → verify pointer advances")
    void fullLifecycle_EnrollAndAdvanceViaSessionEnd() {
        String userId = UUID.randomUUID().toString();
        UUID programId = UUID.randomUUID();

        // 1. Enroll in a program (3 weeks, 2 days per week)
        UUID enrollmentId = enrollUserWithProgramId(userId, programId, "Strength 3x2", 3, 2);

        // Verify starting position
        ResponseEntity<Map> enrollmentResp = restTemplate.exchange(
                enrollmentsUrl() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        assertThat(enrollmentResp.getBody().get("currentWeek")).isEqualTo(1);
        assertThat(enrollmentResp.getBody().get("currentDay")).isEqualTo(1);

        // 2. Start a non-standalone session for this enrollment
        UUID sessionId = startProgramSession(userId, programId, enrollmentId, 1, 1);

        // 3. End the session — this should trigger advanceDay on the enrollment
        ResponseEntity<Map> endResp = restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/end",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        assertThat(endResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(endResp.getBody().get("status")).isEqualTo("COMPLETED");

        // 4. Verify enrollment advanced from (1,1) to (1,2)
        ResponseEntity<Map> afterAdvance = restTemplate.exchange(
                enrollmentsUrl() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        assertThat(afterAdvance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterAdvance.getBody().get("currentWeek")).isEqualTo(1);
        assertThat(afterAdvance.getBody().get("currentDay")).isEqualTo(2);
        assertThat(afterAdvance.getBody().get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Full lifecycle — enroll → advance → skip → advance to program completion")
    void fullLifecycle_EnrollAdvanceSkipUntilComplete() {
        String userId = UUID.randomUUID().toString();
        UUID programId = UUID.randomUUID();

        // Enroll in a small program: 2 weeks, 2 days per week (4 total days)
        UUID enrollmentId = enrollUserWithProgramId(userId, programId, "Short Program", 2, 2);

        // Day 1 of Week 1: complete via session end → advances to (1,2)
        UUID session1 = startProgramSession(userId, programId, enrollmentId, 1, 1);
        endSession(userId, session1);
        verifyEnrollmentPosition(userId, 1, 2, "ACTIVE");

        // Day 2 of Week 1: skip → advances to (2,1)
        ResponseEntity<Map> skipResp = restTemplate.exchange(
                enrollmentsUrl() + "/" + enrollmentId + "/skip",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        assertThat(skipResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(skipResp.getBody().get("currentWeek")).isEqualTo(2);
        assertThat(skipResp.getBody().get("currentDay")).isEqualTo(1);

        // Day 1 of Week 2: complete via session end → advances to (2,2)
        UUID session2 = startProgramSession(userId, programId, enrollmentId, 2, 1);
        endSession(userId, session2);
        verifyEnrollmentPosition(userId, 2, 2, "ACTIVE");

        // Day 2 of Week 2 (final day): complete via session end → marks COMPLETED
        UUID session3 = startProgramSession(userId, programId, enrollmentId, 2, 2);
        endSession(userId, session3);

        // Verify program is now COMPLETED
        ResponseEntity<Map> finalResp = restTemplate.exchange(
                enrollmentsUrl() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        // No active enrollment should exist — it's completed
        assertThat(finalResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify via DB that the enrollment is COMPLETED
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM program_enrollments WHERE id = ?",
                String.class, enrollmentId);
        assertThat(status).isEqualTo("COMPLETED");

        // Verify skip record was persisted for the skipped day
        Integer skipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM skip_records WHERE enrollment_id = ?",
                Integer.class, enrollmentId);
        assertThat(skipCount).isEqualTo(1);

        // Verify the skip was for week 1, day 2
        Map<String, Object> skipRecord = jdbcTemplate.queryForMap(
                "SELECT week_number, day_number FROM skip_records WHERE enrollment_id = ?",
                enrollmentId);
        assertThat(skipRecord.get("week_number")).isEqualTo(1);
        assertThat(skipRecord.get("day_number")).isEqualTo(2);
    }

    @Test
    @DisplayName("Advance day via session end — week rollover from last day of week to next week")
    void advanceViaSessionEnd_WeekRollover() {
        String userId = UUID.randomUUID().toString();
        UUID programId = UUID.randomUUID();

        // Enroll: 3 weeks, 2 days per week
        UUID enrollmentId = enrollUserWithProgramId(userId, programId, "Rollover Test", 3, 2);

        // Complete day 1 → (1,2)
        UUID session1 = startProgramSession(userId, programId, enrollmentId, 1, 1);
        endSession(userId, session1);
        verifyEnrollmentPosition(userId, 1, 2, "ACTIVE");

        // Complete day 2 (last day of week 1) → should roll over to (2,1)
        UUID session2 = startProgramSession(userId, programId, enrollmentId, 1, 2);
        endSession(userId, session2);
        verifyEnrollmentPosition(userId, 2, 1, "ACTIVE");
    }

    @Test
    @DisplayName("Skip day at final position — marks program COMPLETED")
    void skipDay_FinalPosition_MarksProgramCompleted() {
        String userId = UUID.randomUUID().toString();

        // Enroll in a 1-week, 1-day program (only one day total)
        UUID enrollmentId = enrollUser(userId, "Single Day Program", 1, 1);

        // Skip the only day — should mark COMPLETED
        ResponseEntity<Map> skipResp = restTemplate.exchange(
                enrollmentsUrl() + "/" + enrollmentId + "/skip",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        assertThat(skipResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(skipResp.getBody().get("status")).isEqualTo("COMPLETED");
        assertThat(skipResp.getBody().get("nextDay")).isNull();

        // Verify no active enrollment
        ResponseEntity<Map> activeResp = restTemplate.exchange(
                enrollmentsUrl() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        assertThat(activeResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Standalone session does NOT advance enrollment pointer")
    void standaloneSession_DoesNotAdvanceEnrollment() {
        String userId = UUID.randomUUID().toString();
        UUID programId = UUID.randomUUID();

        // Enroll in a program
        enrollUserWithProgramId(userId, programId, "Main Program", 4, 3);

        // Start a standalone session (not tied to enrollment)
        Map<String, Object> sessionRequest = Map.of(
                "programId", programId.toString(),
                "weekNumber", 1,
                "dayNumber", 1,
                "standalone", true
        );

        ResponseEntity<Map> startResp = restTemplate.exchange(
                sessionsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(sessionRequest, authHeaders(userId)),
                Map.class);
        assertThat(startResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID sessionId = UUID.fromString((String) startResp.getBody().get("id"));

        // End the standalone session
        endSession(userId, sessionId);

        // Verify enrollment is still at (1,1) — not advanced
        verifyEnrollmentPosition(userId, 1, 1, "ACTIVE");
    }

    // =========================================================================
    // Error Scenarios
    // =========================================================================

    @Test
    @DisplayName("Skip day — non-existent enrollment returns 404")
    void skipDay_NotFound_Returns404() {
        String userId = UUID.randomUUID().toString();
        UUID fakeId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl() + "/" + fakeId + "/skip",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Skip day — wrong user returns 403")
    void skipDay_WrongUser_Returns403() {
        String owner = UUID.randomUUID().toString();
        String otherUser = UUID.randomUUID().toString();
        UUID enrollmentId = enrollUser(owner, "Test Program", 4, 3);

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl() + "/" + enrollmentId + "/skip",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(otherUser)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Enroll — missing required fields returns 400")
    void enrollProgram_MissingFields_Returns400() {
        String userId = UUID.randomUUID().toString();

        // Missing programName
        Map<String, Object> request = Map.of(
                "programId", UUID.randomUUID().toString(),
                "totalWeeks", 4,
                "totalDaysPerWeek", 3
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("No JWT — returns 401")
    void noJwt_Returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String enrollmentsUrl() {
        return "http://localhost:" + port + "/api/v1/enrollments";
    }

    private String sessionsUrl() {
        return "http://localhost:" + port + "/api/v1/sessions";
    }

    private UUID enrollUser(String userId, String programName, int totalWeeks, int totalDaysPerWeek) {
        return enrollUserWithProgramId(userId, UUID.randomUUID(), programName, totalWeeks, totalDaysPerWeek);
    }

    private UUID enrollUserWithProgramId(String userId, UUID programId, String programName,
                                         int totalWeeks, int totalDaysPerWeek) {
        Map<String, Object> request = Map.of(
                "programId", programId.toString(),
                "programName", programName,
                "totalWeeks", totalWeeks,
                "totalDaysPerWeek", totalDaysPerWeek
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private UUID startProgramSession(String userId, UUID programId, UUID enrollmentId,
                                     int weekNumber, int dayNumber) {
        Map<String, Object> request = Map.of(
                "programId", programId.toString(),
                "weekNumber", weekNumber,
                "dayNumber", dayNumber,
                "standalone", false
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                sessionsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private void endSession(String userId, UUID sessionId) {
        ResponseEntity<Map> response = restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/end",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("COMPLETED");
    }

    private void verifyEnrollmentPosition(String userId, int expectedWeek, int expectedDay,
                                          String expectedStatus) {
        ResponseEntity<Map> response = restTemplate.exchange(
                enrollmentsUrl() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        if ("COMPLETED".equals(expectedStatus)) {
            // Completed enrollments won't appear in /active
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        } else {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("currentWeek")).isEqualTo(expectedWeek);
            assertThat(response.getBody().get("currentDay")).isEqualTo(expectedDay);
            assertThat(response.getBody().get("status")).isEqualTo(expectedStatus);
        }
    }

    private String generateJwt(String userId) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId)
                    .claim("role", "USER")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(900)))
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(testPrivateKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate test JWT", e);
        }
    }

    private HttpHeaders authHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateJwt(userId));
        return headers;
    }

    // =========================================================================
    // Test data — program JSON matching the structure SessionService expects
    // =========================================================================

    private String validProgramJson() {
        return """
                {
                  "weeks": [
                    {
                      "days": [
                        {
                          "sections": [
                            {
                              "name": "Tier 1: Compound",
                              "type": "STRENGTH",
                              "exercises": [
                                {
                                  "name": "Back Squat",
                                  "sets": 4,
                                  "reps": "5",
                                  "restSeconds": 120
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "sections": [
                            {
                              "name": "Tier 1: Compound",
                              "type": "STRENGTH",
                              "exercises": [
                                {
                                  "name": "Bench Press",
                                  "sets": 4,
                                  "reps": "5",
                                  "restSeconds": 120
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "days": [
                        {
                          "sections": [
                            {
                              "name": "Tier 1: Compound",
                              "type": "STRENGTH",
                              "exercises": [
                                {
                                  "name": "Deadlift",
                                  "sets": 3,
                                  "reps": "5",
                                  "restSeconds": 180
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "sections": [
                            {
                              "name": "Tier 1: Compound",
                              "type": "STRENGTH",
                              "exercises": [
                                {
                                  "name": "Overhead Press",
                                  "sets": 4,
                                  "reps": "5",
                                  "restSeconds": 120
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
