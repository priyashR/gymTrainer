package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.integration.vault;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramSpringDataRepository;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for the manual program creation endpoint: POST /api/v1/vault/programs.
 *
 * <p>Connects to the local dev PostgreSQL instance (application-integration.yml).
 * Tests the full request/response cycle including validation, persistence, and retrieval.
 *
 * <p>Requirements validated: 1.1, 1.2, 2.1, 3.2, 4.1, 6.2, 6.3
 */
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("integration")
class ManualProgramCreationIntegrationTest {

    // ── Shared RSA key pair (generated once for the whole test class) ─────────

    private static RSAPrivateKey testPrivateKey;
    private static RSAPublicKey testPublicKey;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        testPrivateKey = (RSAPrivateKey) pair.getPrivate();
        testPublicKey = (RSAPublicKey) pair.getPublic();
    }

    // ── @TestConfiguration: override the RSAPublicKey bean ───────────────────

    @TestConfiguration
    static class TestJwtConfig {
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
    private ProgramSpringDataRepository programRepository;

    // Track IDs created during each test so @AfterEach can delete them
    private final Set<UUID> createdProgramIds = new LinkedHashSet<>();

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @AfterEach
    void cleanUp() {
        for (UUID id : createdProgramIds) {
            programRepository.deleteById(id);
        }
        createdProgramIds.clear();
    }

    // =========================================================================
    // Happy path — Valid body returns 201 with UUID
    // =========================================================================

    @Test
    void createManualProgram_ValidBody_Returns201WithUuid() {
        String userId = UUID.randomUUID().toString();

        String requestBody = activityOnlyManualProgramJson();
        HttpEntity<String> request = new HttpEntity<>(requestBody, authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                programsUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();

        // Verify the ID is a valid UUID
        String createdId = (String) response.getBody().get("id");
        UUID programId = UUID.fromString(createdId);
        assertThat(programId).isNotNull();
        createdProgramIds.add(programId);
    }

    // =========================================================================
    // 401 — Missing JWT
    // =========================================================================

    @Test
    void createManualProgram_MissingJwt_Returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = activityOnlyManualProgramJson();
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                programsUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // =========================================================================
    // 400 — Malformed JSON
    // =========================================================================

    @Test
    void createManualProgram_MalformedJson_Returns400() {
        String userId = UUID.randomUUID().toString();

        HttpEntity<String> request = new HttpEntity<>(
                "this is not valid json", authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                programsUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 400 — Blank program name
    // =========================================================================

    @Test
    void createManualProgram_BlankProgramName_Returns400() {
        String userId = UUID.randomUUID().toString();

        String requestBody = """
                {
                  "programName": "   ",
                  "days": [
                    {
                      "dayNumber": 1,
                      "type": "activity",
                      "activityType": "Soccer"
                    }
                  ]
                }
                """;

        HttpEntity<String> request = new HttpEntity<>(requestBody, authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                programsUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 400 — Unrecognised day assignment type
    // =========================================================================

    @Test
    void createManualProgram_UnrecognisedDayType_Returns400() {
        String userId = UUID.randomUUID().toString();

        String requestBody = """
                {
                  "programName": "My Program",
                  "days": [
                    {
                      "dayNumber": 1,
                      "type": "workout",
                      "workoutId": null
                    }
                  ]
                }
                """;

        HttpEntity<String> request = new HttpEntity<>(requestBody, authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                programsUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat((String) response.getBody().get("message"))
                .contains("Valid types are: activity, copied_day");
    }

    // =========================================================================
    // 400 — Unrecognised type with a workout ID still gets rejected
    // =========================================================================

    @Test
    void createManualProgram_WorkoutTypeNoLongerSupported_Returns400() {
        String userId = UUID.randomUUID().toString();
        UUID fakeWorkoutId = UUID.randomUUID();

        String requestBody = """
                {
                  "programName": "My Program",
                  "days": [
                    {
                      "dayNumber": 1,
                      "type": "workout",
                      "workoutId": "%s"
                    }
                  ]
                }
                """.formatted(fakeWorkoutId);

        HttpEntity<String> request = new HttpEntity<>(requestBody, authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                programsUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat((String) response.getBody().get("message"))
                .contains("Valid types are: activity, copied_day");
    }

    // =========================================================================
    // Created program appears in GET list
    // =========================================================================

    @Test
    void createManualProgram_ThenAppearsInGetList() {
        String userId = UUID.randomUUID().toString();

        // Create a manual program with activity-only days (no workout reference needed)
        String requestBody = activityOnlyManualProgramJson();
        HttpEntity<String> request = new HttpEntity<>(requestBody, authHeaders(userId));

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                programsUrl(), request, Map.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String createdId = (String) createResponse.getBody().get("id");
        createdProgramIds.add(UUID.fromString(createdId));

        // Now list programs for this user and verify the new one appears
        ResponseEntity<Map> listResponse = restTemplate.exchange(
                programsUrl() + "?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();

        List<Map<String, Object>> content =
                (List<Map<String, Object>>) listResponse.getBody().get("content");
        assertThat(content).isNotEmpty();

        boolean found = content.stream()
                .anyMatch(item -> createdId.equals(item.get("id")));
        assertThat(found).isTrue();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String programsUrl() {
        return "http://localhost:" + port + "/api/v1/vault/programs";
    }

    private String uploadUrl() {
        return "http://localhost:" + port + "/api/v1/uploads/programs";
    }

    /**
     * Uploads a workout via the upload endpoint and returns the created program's UUID.
     * This is used as a referenced workout for workout-type day assignments.
     */
    private UUID uploadWorkout(String userId) {
        HttpEntity<String> request = new HttpEntity<>(validUploadProgramJson(), authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        UUID id = UUID.fromString((String) response.getBody().get("id"));
        createdProgramIds.add(id);
        return id;
    }

    /**
     * Generates a valid RS256 JWT signed with the test private key.
     */
    private String generateJwt(String userId) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId)
                    .claim("email", userId + "@test.example")
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

    /** Returns HttpHeaders with Content-Type: application/json and a valid Bearer token. */
    private HttpHeaders authHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateJwt(userId));
        return headers;
    }

    // =========================================================================
    // Test data
    // =========================================================================

    /**
     * Returns a valid manual program creation JSON body that references a given workoutId.
     */
    private String validManualProgramJson(String workoutId) {
        return """
                {
                  "programName": "My Integration Test Program",
                  "days": [
                    {
                      "dayNumber": 1,
                      "type": "workout",
                      "workoutId": "%s"
                    },
                    {
                      "dayNumber": 2,
                      "type": "activity",
                      "activityType": "Swimming"
                    },
                    {
                      "dayNumber": 3,
                      "type": "activity",
                      "activityType": "Soccer"
                    }
                  ]
                }
                """.formatted(workoutId);
    }

    /**
     * Returns a valid manual program creation JSON body with activity-only days.
     * No workout references needed, so no pre-existing workout is required.
     */
    private String activityOnlyManualProgramJson() {
        return """
                {
                  "programName": "Activity Only Program",
                  "days": [
                    {
                      "dayNumber": 1,
                      "type": "activity",
                      "activityType": "Running"
                    },
                    {
                      "dayNumber": 2,
                      "type": "activity",
                      "activityType": "Yoga"
                    }
                  ]
                }
                """;
    }

    /**
     * Returns a valid upload program JSON (used to create a workout that can be referenced).
     */
    private String validUploadProgramJson() {
        return """
                {
                  "program_metadata": {
                    "program_name": "Referenced Workout",
                    "duration_weeks": 1,
                    "goal": "Strength",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    {
                      "week_number": 1,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Push",
                          "modality": "Strength",
                          "warm_up": [{"movement": "Arm circles", "instruction": "10 reps each direction"}],
                          "blocks": [
                            {
                              "block_type": "Tier 1: Compound",
                              "format": "Sets/Reps",
                              "movements": [
                                {
                                  "exercise_name": "Bench Press",
                                  "prescribed_sets": 5,
                                  "prescribed_reps": "5"
                                }
                              ]
                            }
                          ],
                          "cool_down": [{"movement": "Chest stretch", "instruction": "Hold 30s"}]
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
