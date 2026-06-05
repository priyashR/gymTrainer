package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.config.RabbitMQConfig;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for Performance Tracking features — set logging, CrossFit score logging,
 * enriched SessionCompleted event, and duration computation.
 *
 * <p>Tests the full HTTP request/response cycle against the running application with
 * real PostgreSQL (via Flyway migrations) and RabbitMQ (local dev instances).
 *
 * <p>Validates: Requirements 1.1, 1.5, 1.7, 2.1, 2.3, 2.7, 4.1, 4.2, 4.3, 4.4, 7.3, 7.6
 */
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@ActiveProfiles("integration")
class PerformanceTrackingIntegrationTest {

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

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        RSAPublicKey rsaPublicKey() {
            return testPublicKey;
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void stubWorkoutCreatorService() {
        wireMockServer.resetAll();
        wireMockServer.stubFor(get(urlPathMatching("/api/v1/vault/programs/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(programWithMixedSections())));
        drainQueue();
    }

    @AfterEach
    void cleanTables() {
        jdbcTemplate.execute("DELETE FROM skip_records");
        jdbcTemplate.execute("DELETE FROM sessions");
        jdbcTemplate.execute("DELETE FROM program_enrollments");
    }

    // =========================================================================
    // 13.1 — Set Logging Endpoint Tests
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/sessions/{id}/sets — Set Logging")
    class SetLoggingTests {

        @Test
        @DisplayName("Happy path — logs a set and returns updated session with setLogs")
        void logSet_HappyPath_ReturnsSetInResponse() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            Map<String, Object> request = Map.of(
                    "sectionIndex", 0,
                    "exerciseIndex", 0,
                    "weight", 100.0,
                    "repetitions", 5,
                    "rpe", 7.5
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/sets",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            // Verify setLogs in response
            List<Map<String, Object>> sections = getSectionProgresses(response);
            List<Map<String, Object>> exercises = getExerciseLogs(sections, 0);
            List<Map<String, Object>> setLogs = (List<Map<String, Object>>) exercises.get(0).get("setLogs");

            assertThat(setLogs).hasSize(1);
            assertThat(((Number) setLogs.get(0).get("setNumber")).intValue()).isEqualTo(1);
            assertThat(new BigDecimal(setLogs.get(0).get("weight").toString())).isEqualByComparingTo(new BigDecimal("100.0"));
            assertThat(((Number) setLogs.get(0).get("repetitions")).intValue()).isEqualTo(5);
            assertThat(new BigDecimal(setLogs.get(0).get("rpe").toString())).isEqualByComparingTo(new BigDecimal("7.5"));
            assertThat(setLogs.get(0).get("loggedAt")).isNotNull();
        }

        @Test
        @DisplayName("Happy path — logs set with null RPE")
        void logSet_NullRpe_Succeeds() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            Map<String, Object> request = Map.of(
                    "sectionIndex", 0,
                    "exerciseIndex", 0,
                    "weight", 80.5,
                    "repetitions", 8
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/sets",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            List<Map<String, Object>> sections = getSectionProgresses(response);
            List<Map<String, Object>> exercises = getExerciseLogs(sections, 0);
            List<Map<String, Object>> setLogs = (List<Map<String, Object>>) exercises.get(0).get("setLogs");

            assertThat(setLogs).hasSize(1);
            assertThat(setLogs.get(0).get("rpe")).isNull();
        }

        @Test
        @DisplayName("Multiple sets — JSONB round-trip preserves all sets in order")
        void logSet_MultipleSets_PreservedInOrder() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Log 3 sets
            for (int i = 1; i <= 3; i++) {
                Map<String, Object> request = Map.of(
                        "sectionIndex", 0,
                        "exerciseIndex", 0,
                        "weight", 100.0 + i * 5,
                        "repetitions", 5
                );
                restTemplate.exchange(
                        sessionsUrl() + "/" + sessionId + "/sets",
                        HttpMethod.POST,
                        new HttpEntity<>(request, authHeaders(userId)),
                        Map.class);
            }

            // GET session to verify round-trip
            ResponseEntity<Map> getResponse = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);

            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            List<Map<String, Object>> sections = getSectionProgresses(getResponse);
            List<Map<String, Object>> exercises = getExerciseLogs(sections, 0);
            List<Map<String, Object>> setLogs = (List<Map<String, Object>>) exercises.get(0).get("setLogs");

            assertThat(setLogs).hasSize(3);
            assertThat(((Number) setLogs.get(0).get("setNumber")).intValue()).isEqualTo(1);
            assertThat(((Number) setLogs.get(1).get("setNumber")).intValue()).isEqualTo(2);
            assertThat(((Number) setLogs.get(2).get("setNumber")).intValue()).isEqualTo(3);
        }

        @Test
        @DisplayName("Validation error — weight zero returns 400")
        void logSet_WeightZero_Returns400() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            Map<String, Object> request = Map.of(
                    "sectionIndex", 0,
                    "exerciseIndex", 0,
                    "weight", 0,
                    "repetitions", 5
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/sets",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation error — repetitions zero returns 400")
        void logSet_RepsZero_Returns400() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            Map<String, Object> request = Map.of(
                    "sectionIndex", 0,
                    "exerciseIndex", 0,
                    "weight", 100.0,
                    "repetitions", 0
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/sets",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation error — RPE out of range returns 400")
        void logSet_RpeOutOfRange_Returns400() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            Map<String, Object> request = Map.of(
                    "sectionIndex", 0,
                    "exerciseIndex", 0,
                    "weight", 100.0,
                    "repetitions", 5,
                    "rpe", 11.0
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/sets",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Wrong section type — logging set on AMRAP section returns 400")
        void logSet_OnAmrapSection_Returns400() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Section index 1 is AMRAP in our test program
            Map<String, Object> request = Map.of(
                    "sectionIndex", 1,
                    "exerciseIndex", 0,
                    "weight", 100.0,
                    "repetitions", 5
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/sets",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Completed session — logging set returns 409")
        void logSet_OnCompletedSession_Returns409() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // End the session first
            restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/end",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);

            Map<String, Object> request = Map.of(
                    "sectionIndex", 0,
                    "exerciseIndex", 0,
                    "weight", 100.0,
                    "repetitions", 5
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/sets",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    // =========================================================================
    // 13.2 — CrossFit Score Endpoint Tests
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/sessions/{id}/scores — CrossFit Score Logging")
    class CrossFitScoreTests {

        @Test
        @DisplayName("Happy path AMRAP — logs score and returns updated session")
        void logScore_Amrap_HappyPath() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Section index 1 is AMRAP
            Map<String, Object> request = Map.of(
                    "sectionIndex", 1,
                    "rounds", 8,
                    "additionalReps", 5
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/scores",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            List<Map<String, Object>> sections = getSectionProgresses(response);
            Map<String, Object> crossFitScore = (Map<String, Object>) sections.get(1).get("crossFitScore");

            assertThat(crossFitScore).isNotNull();
            assertThat(((Number) crossFitScore.get("rounds")).intValue()).isEqualTo(8);
            assertThat(((Number) crossFitScore.get("additionalReps")).intValue()).isEqualTo(5);
            assertThat(crossFitScore.get("totalTimeSeconds")).isNull();
            assertThat(crossFitScore.get("loggedAt")).isNotNull();
        }

        @Test
        @DisplayName("Happy path EMOM — logs score successfully")
        void logScore_Emom_HappyPath() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Section index 2 is EMOM
            Map<String, Object> request = Map.of(
                    "sectionIndex", 2,
                    "rounds", 10,
                    "additionalReps", 0
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/scores",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            List<Map<String, Object>> sections = getSectionProgresses(response);
            Map<String, Object> crossFitScore = (Map<String, Object>) sections.get(2).get("crossFitScore");

            assertThat(crossFitScore).isNotNull();
            assertThat(((Number) crossFitScore.get("rounds")).intValue()).isEqualTo(10);
        }

        @Test
        @DisplayName("Happy path FOR_TIME — logs score with totalTimeSeconds")
        void logScore_ForTime_HappyPath() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Section index 3 is FOR_TIME
            Map<String, Object> request = Map.of(
                    "sectionIndex", 3,
                    "rounds", 5,
                    "additionalReps", 0,
                    "totalTimeSeconds", 720
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/scores",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            List<Map<String, Object>> sections = getSectionProgresses(response);
            Map<String, Object> crossFitScore = (Map<String, Object>) sections.get(3).get("crossFitScore");

            assertThat(crossFitScore).isNotNull();
            assertThat(((Number) crossFitScore.get("rounds")).intValue()).isEqualTo(5);
            assertThat(((Number) crossFitScore.get("totalTimeSeconds")).intValue()).isEqualTo(720);
        }

        @Test
        @DisplayName("Overwrite — second score replaces first")
        void logScore_Overwrite_ReplacesFirst() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Log first score
            Map<String, Object> firstRequest = Map.of(
                    "sectionIndex", 1,
                    "rounds", 5,
                    "additionalReps", 3
            );
            restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/scores",
                    HttpMethod.POST,
                    new HttpEntity<>(firstRequest, authHeaders(userId)),
                    Map.class);

            // Log second score (overwrite)
            Map<String, Object> secondRequest = Map.of(
                    "sectionIndex", 1,
                    "rounds", 9,
                    "additionalReps", 7
            );
            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/scores",
                    HttpMethod.POST,
                    new HttpEntity<>(secondRequest, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Verify via GET that only the latest score is present
            ResponseEntity<Map> getResponse = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);

            List<Map<String, Object>> sections = getSectionProgresses(getResponse);
            Map<String, Object> crossFitScore = (Map<String, Object>) sections.get(1).get("crossFitScore");

            assertThat(crossFitScore).isNotNull();
            assertThat(((Number) crossFitScore.get("rounds")).intValue()).isEqualTo(9);
            assertThat(((Number) crossFitScore.get("additionalReps")).intValue()).isEqualTo(7);
        }

        @Test
        @DisplayName("Validation error — negative rounds returns 400")
        void logScore_NegativeRounds_Returns400() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            Map<String, Object> request = Map.of(
                    "sectionIndex", 1,
                    "rounds", -1,
                    "additionalReps", 0
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/scores",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Wrong section type — logging score on STRENGTH section returns 400")
        void logScore_OnStrengthSection_Returns400() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Section index 0 is STRENGTH
            Map<String, Object> request = Map.of(
                    "sectionIndex", 0,
                    "rounds", 5,
                    "additionalReps", 3
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/scores",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Completed session — logging score returns 409")
        void logScore_OnCompletedSession_Returns409() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // End the session
            restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/end",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);

            Map<String, Object> request = Map.of(
                    "sectionIndex", 1,
                    "rounds", 5,
                    "additionalReps", 3
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/scores",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    // =========================================================================
    // 13.3 — Enriched SessionCompleted Event Tests
    // =========================================================================

    @Nested
    @DisplayName("SessionCompleted Event — Performance Data Enrichment")
    class SessionCompletedEventTests {

        @Test
        @DisplayName("End session with performance data — event includes setLogs and crossFitScore")
        void endSession_WithPerformanceData_EventIncludesAll() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Log a strength set
            Map<String, Object> setRequest = Map.of(
                    "sectionIndex", 0,
                    "exerciseIndex", 0,
                    "weight", 120.0,
                    "repetitions", 5,
                    "rpe", 8.0
            );
            restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/sets",
                    HttpMethod.POST,
                    new HttpEntity<>(setRequest, authHeaders(userId)),
                    Map.class);

            // Log a CrossFit score on AMRAP section
            Map<String, Object> scoreRequest = Map.of(
                    "sectionIndex", 1,
                    "rounds", 7,
                    "additionalReps", 4
            );
            restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/scores",
                    HttpMethod.POST,
                    new HttpEntity<>(scoreRequest, authHeaders(userId)),
                    Map.class);

            // End the session
            ResponseEntity<Map> endResponse = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/end",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);
            assertThat(endResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Receive the event from RabbitMQ
            Message message = rabbitTemplate.receive(RabbitMQConfig.QUEUE_NAME, 10_000);
            assertThat(message).isNotNull();

            String body = new String(message.getBody());

            // Verify set log data in event
            assertThat(body).contains("\"setLogs\"");
            assertThat(body).contains("\"weight\"");
            assertThat(body).contains("\"repetitions\"");
            assertThat(body).contains("\"rpe\"");

            // Verify CrossFit score data in event
            assertThat(body).contains("\"crossFitScore\"");
            assertThat(body).contains("\"rounds\"");
            assertThat(body).contains("\"additionalReps\"");

            // Verify duration is present
            assertThat(body).contains("\"durationSeconds\"");

            // Verify standard event fields
            assertThat(body).contains("\"userId\":\"" + userId + "\"");
            assertThat(body).contains("\"sessionId\":\"" + sessionId + "\"");
            assertThat(body).contains("\"startedAt\"");
            assertThat(body).contains("\"completedAt\"");
        }

        @Test
        @DisplayName("End session without performance data — event includes empty performance fields")
        void endSession_WithoutPerformanceData_EventIncludesEmptyFields() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // End session immediately without logging any data
            ResponseEntity<Map> endResponse = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/end",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);
            assertThat(endResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Receive the event from RabbitMQ
            Message message = rabbitTemplate.receive(RabbitMQConfig.QUEUE_NAME, 10_000);
            assertThat(message).isNotNull();

            String body = new String(message.getBody());

            // Verify sectionProgresses is present (not omitted)
            assertThat(body).contains("\"sectionProgresses\"");

            // Verify setLogs is present as empty array
            assertThat(body).contains("\"setLogs\":[]");

            // Verify crossFitScore is null for sections without scores
            assertThat(body).contains("\"crossFitScore\":null");

            // Verify durationSeconds is present
            assertThat(body).contains("\"durationSeconds\"");
        }
    }

    // =========================================================================
    // 13.4 — Duration Computation End-to-End Tests
    // =========================================================================

    @Nested
    @DisplayName("Duration Computation — End-to-End")
    class DurationComputationTests {

        @Test
        @DisplayName("Start → end (no pauses) — durationSeconds equals elapsed time")
        void endSession_NoPauses_DurationEqualsElapsed() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Small delay to ensure measurable duration
            sleep(1000);

            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/end",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            Integer durationSeconds = (Integer) response.getBody().get("durationSeconds");
            assertThat(durationSeconds).isNotNull();
            assertThat(durationSeconds).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Start → pause → resume → end — durationSeconds excludes paused time")
        void endSession_WithPause_DurationExcludesPausedTime() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            // Small delay before pause
            sleep(500);

            // Pause
            restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/pause",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);

            // Stay paused for 2 seconds
            sleep(2000);

            // Resume
            restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/resume",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);

            // Small delay after resume
            sleep(500);

            // End
            ResponseEntity<Map> response = restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/end",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            Integer durationSeconds = (Integer) response.getBody().get("durationSeconds");
            assertThat(durationSeconds).isNotNull();

            // Total wall time is ~3s (0.5 + 2.0 + 0.5), but active time should be ~1s
            // The duration should be less than the total elapsed time
            // Allow some tolerance for timing imprecision
            assertThat(durationSeconds).isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Duration included in SessionCompleted event")
        void endSession_DurationInEvent() {
            String userId = UUID.randomUUID().toString();
            UUID sessionId = startSessionAndGetId(userId);

            sleep(1000);

            restTemplate.exchange(
                    sessionsUrl() + "/" + sessionId + "/end",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(userId)),
                    Map.class);

            Message message = rabbitTemplate.receive(RabbitMQConfig.QUEUE_NAME, 10_000);
            assertThat(message).isNotNull();

            String body = new String(message.getBody());
            assertThat(body).contains("\"durationSeconds\"");
            // Duration should be a positive number (at least 1 second)
            assertThat(body).doesNotContain("\"durationSeconds\":null");
            assertThat(body).doesNotContain("\"durationSeconds\":0");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void drainQueue() {
        Message msg;
        do {
            msg = rabbitTemplate.receive(RabbitMQConfig.QUEUE_NAME, 100);
        } while (msg != null);
    }

    private String sessionsUrl() {
        return "http://localhost:" + port + "/api/v1/sessions";
    }

    private UUID startSessionAndGetId(String userId) {
        UUID programId = UUID.randomUUID();

        Map<String, Object> request = Map.of(
                "programId", programId.toString(),
                "weekNumber", 1,
                "dayNumber", 1,
                "standalone", true
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                sessionsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSectionProgresses(ResponseEntity<Map> response) {
        return (List<Map<String, Object>>) response.getBody().get("sectionProgresses");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getExerciseLogs(List<Map<String, Object>> sections, int sectionIndex) {
        return (List<Map<String, Object>>) sections.get(sectionIndex).get("exerciseLogs");
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
    // Test data — program JSON with mixed section types (STRENGTH, AMRAP, EMOM, FOR_TIME)
    // =========================================================================

    private String programWithMixedSections() {
        return """
                {
                  "weeks": [
                    {
                      "days": [
                        {
                          "sections": [
                            {
                              "name": "Strength Block",
                              "type": "STRENGTH",
                              "exercises": [
                                {
                                  "name": "Back Squat",
                                  "sets": 5,
                                  "reps": "5",
                                  "restSeconds": 180
                                },
                                {
                                  "name": "Bench Press",
                                  "sets": 5,
                                  "reps": "5",
                                  "restSeconds": 180
                                }
                              ]
                            },
                            {
                              "name": "AMRAP Finisher",
                              "type": "AMRAP",
                              "timeCap": 600,
                              "exercises": [
                                {
                                  "name": "Burpees",
                                  "reps": "10"
                                },
                                {
                                  "name": "Box Jumps",
                                  "reps": "15"
                                }
                              ]
                            },
                            {
                              "name": "EMOM Conditioning",
                              "type": "EMOM",
                              "timeCap": 480,
                              "exercises": [
                                {
                                  "name": "Kettlebell Swings",
                                  "reps": "12"
                                }
                              ]
                            },
                            {
                              "name": "For Time Challenge",
                              "type": "FOR_TIME",
                              "exercises": [
                                {
                                  "name": "Thrusters",
                                  "reps": "21-15-9"
                                },
                                {
                                  "name": "Pull-ups",
                                  "reps": "21-15-9"
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
