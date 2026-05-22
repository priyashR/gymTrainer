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
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for WebSocket session updates via STOMP.
 *
 * <p>Connects a STOMP client, subscribes to the session topic, and verifies that
 * session completion messages are pushed when sessions are ended.
 *
 * <p>Note: The endSession test reliably receives messages because the RabbitMQ retry
 * loop (no local RabbitMQ) introduces a delay between the REST response and the
 * WebSocket notification, ensuring the subscription is fully registered.
 *
 * <p>Validates: Requirements 4.13
 */
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.rabbitmq.listener.simple.auto-startup=false"
        }
)
@ActiveProfiles("integration")
class WebSocketIntegrationTest {

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
    // WebSocket Tests
    // =========================================================================

    @Test
    @DisplayName("STOMP client can connect and subscribe to session topic")
    void stompClient_CanConnectAndSubscribe() throws Exception {
        String userId = UUID.randomUUID().toString();

        WebSocketStompClient stompClient = createStompClient();

        String wsUrl = "ws://localhost:" + port + "/ws/sessions";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("token", generateJwt(userId));

        StompSession stompSession = stompClient.connectAsync(wsUrl,
                        (WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        assertThat(stompSession.isConnected()).isTrue();

        // Subscribe to a topic — should not throw
        StompSession.Subscription subscription = stompSession.subscribe(
                "/topic/sessions/" + UUID.randomUUID(), new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        // no-op
                    }
                });

        assertThat(subscription).isNotNull();

        stompSession.disconnect();
        stompClient.stop();
    }

    @Test
    @DisplayName("End session — pushes SESSION_COMPLETED via WebSocket")
    void endSession_PushesSessionCompleted() throws Exception {
        String userId = UUID.randomUUID().toString();
        UUID sessionId = startSessionAndGetId(userId);

        // Connect STOMP client with JWT in STOMP headers
        WebSocketStompClient stompClient = createStompClient();
        BlockingQueue<Map> messages = new LinkedBlockingQueue<>();

        String wsUrl = "ws://localhost:" + port + "/ws/sessions";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("token", generateJwt(userId));

        StompSession stompSession = stompClient.connectAsync(wsUrl,
                        (WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        // Subscribe to session topic
        stompSession.subscribe("/topic/sessions/" + sessionId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((Map) payload);
            }
        });

        // Give subscription time to register on the broker
        Thread.sleep(1000);

        // End the session via REST — this triggers RabbitMQ retry loop (no local RabbitMQ),
        // which delays the SESSION_COMPLETED WebSocket notification by ~15 seconds,
        // ensuring the subscription is fully registered when the message arrives
        ResponseEntity<Map> response = restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/end",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Wait for WebSocket messages — we expect SESSION_COMPLETED
        // The service pushes SESSION_COMPLETED after the RabbitMQ event publish completes
        Map completedMessage = null;
        for (int i = 0; i < 5; i++) {
            Map msg = messages.poll(20, TimeUnit.SECONDS);
            if (msg == null) break;
            if ("SESSION_COMPLETED".equals(msg.get("type"))) {
                completedMessage = msg;
                break;
            }
        }

        assertThat(completedMessage).isNotNull();
        assertThat(completedMessage.get("type")).isEqualTo("SESSION_COMPLETED");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) completedMessage.get("payload");
        assertThat(payload).isNotNull();
        assertThat(payload.get("sessionId")).isEqualTo(sessionId.toString());
        assertThat(payload.get("completedAt")).isNotNull();

        stompSession.disconnect();
        stompClient.stop();
    }

    @Test
    @DisplayName("Complete exercise — pushes SESSION_STATE_UPDATE via WebSocket")
    void completeExercise_PushesSessionStateUpdate() throws Exception {
        String userId = UUID.randomUUID().toString();
        UUID sessionId = startSessionAndGetId(userId);

        // Connect STOMP client with JWT in STOMP headers
        WebSocketStompClient stompClient = createStompClient();
        BlockingQueue<Map> messages = new LinkedBlockingQueue<>();

        String wsUrl = "ws://localhost:" + port + "/ws/sessions";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("token", generateJwt(userId));

        StompSession stompSession = stompClient.connectAsync(wsUrl,
                        (WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        // Subscribe to session topic
        stompSession.subscribe("/topic/sessions/" + sessionId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((Map) payload);
            }
        });

        // Give subscription time to register on the broker
        Thread.sleep(1000);

        // Complete an exercise via REST — this triggers SESSION_STATE_UPDATE push
        Map<String, Object> completeRequest = Map.of(
                "sectionIndex", 0,
                "exerciseIndex", 0
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/exercises",
                HttpMethod.PATCH,
                new HttpEntity<>(completeRequest, authHeaders(userId)),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Wait for WebSocket message — we expect SESSION_STATE_UPDATE
        Map stateUpdateMessage = messages.poll(10, TimeUnit.SECONDS);

        assertThat(stateUpdateMessage).isNotNull();
        assertThat(stateUpdateMessage.get("type")).isEqualTo("SESSION_STATE_UPDATE");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) stateUpdateMessage.get("payload");
        assertThat(payload).isNotNull();
        assertThat(payload.get("id")).isEqualTo(sessionId.toString());
        assertThat(payload.get("status")).isEqualTo("IN_PROGRESS");

        // Verify the section progresses reflect the completed exercise
        @SuppressWarnings("unchecked")
        var sectionProgresses = (java.util.List<Map<String, Object>>) payload.get("sectionProgresses");
        assertThat(sectionProgresses).isNotEmpty();

        @SuppressWarnings("unchecked")
        var firstSection = sectionProgresses.get(0);
        @SuppressWarnings("unchecked")
        var exerciseLogs = (java.util.List<Map<String, Object>>) firstSection.get("exerciseLogs");
        assertThat(exerciseLogs).isNotEmpty();

        // First exercise should be marked as completed
        var firstExercise = exerciseLogs.get(0);
        assertThat(firstExercise.get("completed")).isEqualTo(true);
        assertThat(firstExercise.get("completedAt")).isNotNull();

        stompSession.disconnect();
        stompClient.stop();
    }

    @Test
    @DisplayName("Complete exercise then end session — receives both message types in order")
    void completeExerciseThenEnd_ReceivesBothMessageTypes() throws Exception {
        String userId = UUID.randomUUID().toString();
        UUID sessionId = startSessionAndGetId(userId);

        // Connect STOMP client
        WebSocketStompClient stompClient = createStompClient();
        BlockingQueue<Map> messages = new LinkedBlockingQueue<>();

        String wsUrl = "ws://localhost:" + port + "/ws/sessions";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("token", generateJwt(userId));

        StompSession stompSession = stompClient.connectAsync(wsUrl,
                        (WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        // Subscribe to session topic
        stompSession.subscribe("/topic/sessions/" + sessionId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((Map) payload);
            }
        });

        // Give subscription time to register
        Thread.sleep(1000);

        // Complete an exercise
        Map<String, Object> completeRequest = Map.of(
                "sectionIndex", 0,
                "exerciseIndex", 0
        );
        restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/exercises",
                HttpMethod.PATCH,
                new HttpEntity<>(completeRequest, authHeaders(userId)),
                Map.class);

        // Wait for SESSION_STATE_UPDATE
        Map stateUpdate = messages.poll(10, TimeUnit.SECONDS);
        assertThat(stateUpdate).isNotNull();
        assertThat(stateUpdate.get("type")).isEqualTo("SESSION_STATE_UPDATE");

        // End the session
        restTemplate.exchange(
                sessionsUrl() + "/" + sessionId + "/end",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        // Wait for SESSION_COMPLETED — may take longer due to RabbitMQ retry
        Map completedMessage = null;
        for (int i = 0; i < 5; i++) {
            Map msg = messages.poll(20, TimeUnit.SECONDS);
            if (msg == null) break;
            if ("SESSION_COMPLETED".equals(msg.get("type"))) {
                completedMessage = msg;
                break;
            }
        }

        assertThat(completedMessage).isNotNull();
        assertThat(completedMessage.get("type")).isEqualTo("SESSION_COMPLETED");

        @SuppressWarnings("unchecked")
        Map<String, Object> completedPayload = (Map<String, Object>) completedMessage.get("payload");
        assertThat(completedPayload.get("sessionId")).isEqualTo(sessionId.toString());
        assertThat(completedPayload.get("completedAt")).isNotNull();

        stompSession.disconnect();
        stompClient.stop();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        return stompClient;
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
