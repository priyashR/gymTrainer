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
 * Integration tests verifying RabbitMQ event publishing on session completion.
 *
 * <p>Connects to the real RabbitMQ instance running in the dev Kubernetes namespace
 * (NodePort 30672). Verifies that the SessionCompleted event arrives on the
 * {@code progress-tracker.session-completed} queue after a session is ended.
 *
 * <p>Validates: Requirements 5.3
 */
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@ActiveProfiles("integration")
class RabbitMQIntegrationTest {

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
                        .withBody(validProgramJson())));

        // Drain any leftover messages from previous test runs
        drainQueue();
    }

    @AfterEach
    void cleanTables() {
        jdbcTemplate.execute("DELETE FROM skip_records");
        jdbcTemplate.execute("DELETE FROM sessions");
        jdbcTemplate.execute("DELETE FROM program_enrollments");
    }

    // =========================================================================
    // RabbitMQ Event Publishing Tests
    // =========================================================================

    @Test
    @DisplayName("End session — publishes SessionCompleted event to RabbitMQ queue")
    void endSession_PublishesSessionCompletedEvent() {
        String userId = UUID.randomUUID().toString();
        UUID sessionId = startSessionAndGetId(userId);

        // End the session — this triggers event publishing
        ResponseEntity<Map> response = restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/end",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Receive the message from the queue (with 10s timeout)
        Message message = rabbitTemplate.receive(RabbitMQConfig.QUEUE_NAME, 10_000);

        assertThat(message).isNotNull();
        assertThat(message.getBody()).isNotNull();

        // Parse the JSON payload
        String body = new String(message.getBody());
        assertThat(body).contains("\"userId\":\"" + userId + "\"");
        assertThat(body).contains("\"sessionId\":\"" + sessionId + "\"");
        assertThat(body).contains("\"standalone\":true");
        assertThat(body).contains("\"weekNumber\":1");
        assertThat(body).contains("\"dayNumber\":1");
        assertThat(body).contains("\"eventId\"");
        assertThat(body).contains("\"occurredAt\"");
        assertThat(body).contains("\"startedAt\"");
        assertThat(body).contains("\"completedAt\"");
        assertThat(body).contains("\"sectionProgresses\"");
    }

    @Test
    @DisplayName("End session — event payload contains section progress with exercise data")
    void endSession_EventContainsSectionProgressData() {
        String userId = UUID.randomUUID().toString();
        UUID sessionId = startSessionAndGetId(userId);

        // Complete an exercise before ending
        Map<String, Object> completeReq = Map.of("sectionIndex", 0, "exerciseIndex", 0);
        restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/exercises",
                HttpMethod.PATCH,
                new HttpEntity<>(completeReq, authHeaders(userId)),
                Map.class);

        // End the session
        ResponseEntity<Map> response = restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/end",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Receive the message from the queue
        Message message = rabbitTemplate.receive(RabbitMQConfig.QUEUE_NAME, 10_000);

        assertThat(message).isNotNull();
        String body = new String(message.getBody());

        // Verify section progress data is present
        assertThat(body).contains("Tier 1: Compound");
        assertThat(body).contains("Tier 2: Accessory");
        assertThat(body).contains("Back Squat");
        assertThat(body).contains("STRENGTH");
    }

    @Test
    @DisplayName("End session — message arrives with correct routing key on the bound queue")
    void endSession_MessageArrivesOnBoundQueue() {
        String userId = UUID.randomUUID().toString();
        UUID sessionId = startSessionAndGetId(userId);

        // End the session
        restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/end",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        // The fact that we can receive from the queue proves the routing key binding works
        // (exchange: session.events, routing key: session.completed → queue: progress-tracker.session-completed)
        Message message = rabbitTemplate.receive(RabbitMQConfig.QUEUE_NAME, 10_000);
        assertThat(message).isNotNull();

        // Verify message properties
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void drainQueue() {
        // Remove any stale messages from previous test runs
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
                                },
                                {
                                  "name": "Bench Press",
                                  "sets": 4,
                                  "reps": "5",
                                  "restSeconds": 120
                                }
                              ]
                            },
                            {
                              "name": "Tier 2: Accessory",
                              "type": "STRENGTH",
                              "exercises": [
                                {
                                  "name": "Dumbbell Row",
                                  "sets": 3,
                                  "reps": "10",
                                  "restSeconds": 60
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
