package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Full HTTP round-trip integration tests for the generation endpoint.
 * <p>
 * Uses:
 * - Dev PostgreSQL at localhost:30432 for a real database with Flyway migrations
 * - WireMock to stub the Gemini API
 * - A test RSA key pair so JWTs can be issued and verified within the test
 * <p>
 * Requirements: 1.1, 1.2, 1.3, 6.1, 6.2
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class GenerationIntegrationTest {

    static WireMockServer wireMock;

    static final TestJwtHelper JWT_HELPER = new TestJwtHelper();

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @DynamicPropertySource
    static void overrideGeminiUrl(DynamicPropertyRegistry registry) {
        registry.add("gemini.base-url", () -> "http://localhost:" + wireMock.port());
        registry.add("gemini.api-key", () -> "test-api-key");
        registry.add("gemini.model", () -> "gemini-test-model");
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Primary
        RSAPublicKey rsaPublicKey() {
            return JWT_HELPER.getPublicKey();
        }
    }

    @Autowired
    WebTestClient webTestClient;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    // ---- Test: valid request → 200 with parsed workout ----

    @Test
    void generateWorkout_validRequest_returns200WithParsedWorkout() {
        wireMock.stubFor(post(urlPathMatching("/models/.*:generateContent"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(geminiResponseBody(validWorkoutText()))));

        String token = JWT_HELPER.issueToken(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A strength-focused leg day",
                          "scope": "DAY",
                          "trainingStyles": ["STRENGTH"]
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rawGeminiResponse").isNotEmpty()
                .jsonPath("$.workout").isNotEmpty()
                .jsonPath("$.workout.name").isEqualTo("Leg Day Strength")
                .jsonPath("$.parsingError").doesNotExist();
    }

    // ---- Test: invalid request → 400 with validation errors ----

    @Test
    void generateWorkout_blankDescription_returns400() {
        String token = JWT_HELPER.issueToken(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "   ",
                          "scope": "DAY",
                          "trainingStyles": ["STRENGTH"]
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors").isArray();
    }

    @Test
    void generateWorkout_nullScope_returns400() {
        String token = JWT_HELPER.issueToken(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A leg day",
                          "scope": null,
                          "trainingStyles": ["STRENGTH"]
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors").isArray();
    }

    @Test
    void generateWorkout_emptyTrainingStyles_returns400() {
        String token = JWT_HELPER.issueToken(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A leg day",
                          "scope": "DAY",
                          "trainingStyles": []
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors").isArray();
    }

    @Test
    void generateWorkout_dayScopeWithMultipleStyles_returns400() {
        String token = JWT_HELPER.issueToken(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A leg day",
                          "scope": "DAY",
                          "trainingStyles": ["STRENGTH", "CROSSFIT"]
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors").isArray();
    }

    // ---- Test: Gemini failure → 502 Bad Gateway ----

    @Test
    void generateWorkout_geminiReturns500_returns502() {
        wireMock.stubFor(post(urlPathMatching("/models/.*:generateContent"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error": {"message": "Internal server error"}}
                                """)));

        String token = JWT_HELPER.issueToken(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A leg day",
                          "scope": "DAY",
                          "trainingStyles": ["STRENGTH"]
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.status").isEqualTo(502)
                .jsonPath("$.message").isEqualTo(
                        "AI generation service is currently unavailable. Please try again later.");
    }

    @Test
    void generateWorkout_geminiReturns503_returns502() {
        wireMock.stubFor(post(urlPathMatching("/models/.*:generateContent"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error": "service unavailable"}
                                """)));

        String token = JWT_HELPER.issueToken(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A leg day",
                          "scope": "DAY",
                          "trainingStyles": ["STRENGTH"]
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.status").isEqualTo(502);
    }

    // ---- Test: missing JWT → 401 Unauthorised ----

    @Test
    void generateWorkout_missingJwt_returns401() {
        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A leg day",
                          "scope": "DAY",
                          "trainingStyles": ["STRENGTH"]
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void generateWorkout_expiredJwt_returns401() {
        String expiredToken = JWT_HELPER.issueExpiredToken(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A leg day",
                          "scope": "DAY",
                          "trainingStyles": ["STRENGTH"]
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void generateWorkout_malformedJwt_returns401() {
        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A leg day",
                          "scope": "DAY",
                          "trainingStyles": ["STRENGTH"]
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    // ---- Test: Gemini returns unparseable text → 200 with parsingError ----

    @Test
    void generateWorkout_geminiReturnsUnparseableText_returns200WithParsingError() {
        wireMock.stubFor(post(urlPathMatching("/models/.*:generateContent"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(geminiResponseBody("This is not a valid workout format at all."))));

        String token = JWT_HELPER.issueToken(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/workouts/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "A leg day",
                          "scope": "DAY",
                          "trainingStyles": ["STRENGTH"]
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rawGeminiResponse").isNotEmpty()
                .jsonPath("$.parsingError").isNotEmpty();
    }

    // ---- Helpers ----

    private static String geminiResponseBody(String text) {
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "%s"
                          }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(escaped);
    }

    private static String validWorkoutText() {
        return """
                WORKOUT: Leg Day Strength
                DESCRIPTION: A heavy lower body strength session
                TRAINING_STYLE: STRENGTH
                --- SECTION: Squat Block [TYPE: STRENGTH] ---
                - Back Squat | Sets: 5 | Reps: 5 | Weight: 185 lbs | Rest: 180s
                - Romanian Deadlift | Sets: 3 | Reps: 10 | Weight: 135 lbs | Rest: 120s
                """;
    }
}
