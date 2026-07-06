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
 * Integration tests for the copy-day-to-manual-program feature.
 *
 * <p>Tests the full request/response cycle for:
 * - Creating a manual program with copied_day assignments
 * - Browsing available days via GET /{id}/days
 * - Verifying snapshot independence after source deletion
 *
 * <p>Connects to the local dev PostgreSQL instance (application-integration.yml).
 *
 * <p>Requirements validated: 1.1, 1.4, 2.1, 2.4, 5.1, 5.2, 6.1, 6.3
 */
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("integration")
class CopyDayIntegrationTest {

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
    // POST /api/v1/vault/programs with copied_day → 201, verify snapshot_data
    // =========================================================================

    @Test
    void createManualProgram_WithCopiedDay_Returns201_AndSnapshotPersisted() {
        String userId = UUID.randomUUID().toString();

        // First, upload a source program that we'll copy from
        UUID sourceProgramId = uploadProgram(userId, validFourWeekProgramJson());

        // Create a manual program with a copied_day assignment referencing the source
        String requestBody = """
                {
                  "programName": "My Copy Day Program",
                  "days": [
                    {
                      "dayNumber": 1,
                      "type": "copied_day",
                      "sourceProgramId": "%s",
                      "sourceWeekNumber": 1,
                      "sourceDayNumber": 1
                    },
                    {
                      "dayNumber": 2,
                      "type": "activity",
                      "activityType": "Running"
                    }
                  ]
                }
                """.formatted(sourceProgramId);

        HttpEntity<String> request = new HttpEntity<>(requestBody, authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                programsUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();

        String createdId = (String) response.getBody().get("id");
        UUID manualProgramId = UUID.fromString(createdId);
        createdProgramIds.add(manualProgramId);

        // Verify the program detail includes the snapshot data
        ResponseEntity<Map> detailResponse = restTemplate.exchange(
                programsUrl() + "/" + manualProgramId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody()).isNotNull();

        List<Map<String, Object>> dayAssignments =
                (List<Map<String, Object>>) detailResponse.getBody().get("dayAssignments");
        assertThat(dayAssignments).hasSize(2);

        // Find the copied_day assignment
        Map<String, Object> copiedDay = dayAssignments.stream()
                .filter(da -> "copied_day".equals(da.get("type")))
                .findFirst()
                .orElseThrow();

        assertThat(copiedDay.get("dayNumber")).isEqualTo(1);
        assertThat(copiedDay.get("snapshotData")).isNotNull();
        assertThat(copiedDay.get("sourceProgramId")).isEqualTo(sourceProgramId.toString());
        assertThat(copiedDay.get("sourceWeekNumber")).isEqualTo(1);
        assertThat(copiedDay.get("sourceDayNumber")).isEqualTo(1);

        // Verify the snapshot contains expected structure from the source day
        Map<String, Object> snapshot = (Map<String, Object>) copiedDay.get("snapshotData");
        assertThat(snapshot.get("label")).isEqualTo("Monday");
        assertThat(snapshot.get("focusArea")).isEqualTo("Push");
    }

    // =========================================================================
    // POST /api/v1/vault/programs with copied_day referencing non-existent source → 400
    // =========================================================================

    @Test
    void createManualProgram_WithCopiedDay_NonExistentSource_Returns400() {
        String userId = UUID.randomUUID().toString();
        UUID fakeSourceId = UUID.randomUUID();

        String requestBody = """
                {
                  "programName": "Bad Copy Program",
                  "days": [
                    {
                      "dayNumber": 1,
                      "type": "copied_day",
                      "sourceProgramId": "%s",
                      "sourceWeekNumber": 1,
                      "sourceDayNumber": 1
                    }
                  ]
                }
                """.formatted(fakeSourceId);

        HttpEntity<String> request = new HttpEntity<>(requestBody, authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                programsUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat((String) response.getBody().get("message"))
                .contains("Source program not found");
    }

    // =========================================================================
    // GET /api/v1/vault/programs/{id}/days → 200 with correct structure
    // =========================================================================

    @Test
    void getProgramDays_ValidProgram_Returns200WithCorrectStructure() {
        String userId = UUID.randomUUID().toString();
        UUID programId = uploadProgram(userId, validFourWeekProgramJson());

        ResponseEntity<Map> response = restTemplate.exchange(
                programsUrl() + "/" + programId + "/days",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<Map<String, Object>> weeks =
                (List<Map<String, Object>>) response.getBody().get("weeks");
        assertThat(weeks).hasSize(4);

        // Verify week 1
        Map<String, Object> week1 = weeks.stream()
                .filter(w -> Integer.valueOf(1).equals(w.get("weekNumber")))
                .findFirst()
                .orElseThrow();
        List<Map<String, Object>> week1Days = (List<Map<String, Object>>) week1.get("days");
        assertThat(week1Days).hasSize(1);
        assertThat(week1Days.get(0).get("dayNumber")).isEqualTo(1);
        assertThat(week1Days.get(0).get("label")).isEqualTo("Monday");
        assertThat(week1Days.get(0).get("focusArea")).isEqualTo("Push");

        // Verify week 2
        Map<String, Object> week2 = weeks.stream()
                .filter(w -> Integer.valueOf(2).equals(w.get("weekNumber")))
                .findFirst()
                .orElseThrow();
        List<Map<String, Object>> week2Days = (List<Map<String, Object>>) week2.get("days");
        assertThat(week2Days).hasSize(1);
        assertThat(week2Days.get(0).get("dayNumber")).isEqualTo(1);
        assertThat(week2Days.get(0).get("label")).isEqualTo("Wednesday");
        assertThat(week2Days.get(0).get("focusArea")).isEqualTo("Pull");
    }

    // =========================================================================
    // GET /api/v1/vault/programs/{id}/days for non-owned program → 403
    // =========================================================================

    @Test
    void getProgramDays_NonOwnedProgram_Returns403() {
        String owner = UUID.randomUUID().toString();
        String otherUser = UUID.randomUUID().toString();
        UUID programId = uploadProgram(owner, validFourWeekProgramJson());

        ResponseEntity<Map> response = restTemplate.exchange(
                programsUrl() + "/" + programId + "/days",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(otherUser)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // GET /api/v1/vault/programs/{id} for manual program with copied_day → 200
    // =========================================================================

    @Test
    void getProgram_ManualProgramWithCopiedDay_Returns200WithFullSnapshot() {
        String userId = UUID.randomUUID().toString();

        // Upload a source program
        UUID sourceProgramId = uploadProgram(userId, validFourWeekProgramJson());

        // Create a manual program with a copied_day from week 2, day 1
        String requestBody = """
                {
                  "programName": "My Snapshot Program",
                  "days": [
                    {
                      "dayNumber": 1,
                      "type": "copied_day",
                      "sourceProgramId": "%s",
                      "sourceWeekNumber": 2,
                      "sourceDayNumber": 1
                    }
                  ]
                }
                """.formatted(sourceProgramId);

        HttpEntity<String> createRequest = new HttpEntity<>(requestBody, authHeaders(userId));
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                programsUrl(), createRequest, Map.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String manualId = (String) createResponse.getBody().get("id");
        createdProgramIds.add(UUID.fromString(manualId));

        // GET the manual program and verify full snapshot in response
        ResponseEntity<Map> detailResponse = restTemplate.exchange(
                programsUrl() + "/" + manualId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody()).isNotNull();
        assertThat(detailResponse.getBody().get("contentSource")).isEqualTo("MANUAL");

        List<Map<String, Object>> dayAssignments =
                (List<Map<String, Object>>) detailResponse.getBody().get("dayAssignments");
        assertThat(dayAssignments).hasSize(1);

        Map<String, Object> copiedDay = dayAssignments.get(0);
        assertThat(copiedDay.get("type")).isEqualTo("copied_day");
        assertThat(copiedDay.get("sourceProgramId")).isEqualTo(sourceProgramId.toString());
        assertThat(copiedDay.get("sourceWeekNumber")).isEqualTo(2);
        assertThat(copiedDay.get("sourceDayNumber")).isEqualTo(1);

        // Verify snapshot contains the full day structure from week 2, day 1
        Map<String, Object> snapshot = (Map<String, Object>) copiedDay.get("snapshotData");
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.get("label")).isEqualTo("Wednesday");
        assertThat(snapshot.get("focusArea")).isEqualTo("Pull");
        assertThat(snapshot).containsKey("warmUp");
        assertThat(snapshot).containsKey("sections");
        assertThat(snapshot).containsKey("coolDown");
    }

    // =========================================================================
    // Delete source → GET target manual program still returns 200 with snapshot
    // =========================================================================

    @Test
    void deleteSourceProgram_ManualProgramSnapshotRemainsIntact() {
        String userId = UUID.randomUUID().toString();

        // Upload source program
        UUID sourceProgramId = uploadProgram(userId, validFourWeekProgramJson());

        // Create manual program with copied day from the source
        String requestBody = """
                {
                  "programName": "Independent Snapshot Program",
                  "days": [
                    {
                      "dayNumber": 1,
                      "type": "copied_day",
                      "sourceProgramId": "%s",
                      "sourceWeekNumber": 1,
                      "sourceDayNumber": 1
                    }
                  ]
                }
                """.formatted(sourceProgramId);

        HttpEntity<String> createRequest = new HttpEntity<>(requestBody, authHeaders(userId));
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                programsUrl(), createRequest, Map.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String manualId = (String) createResponse.getBody().get("id");
        createdProgramIds.add(UUID.fromString(manualId));

        // Delete the source program
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                programsUrl() + "/" + sourceProgramId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(userId)),
                Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // Remove from cleanup since it's already deleted
        createdProgramIds.remove(sourceProgramId);

        // GET the manual program — should still return 200 with snapshot intact
        ResponseEntity<Map> detailResponse = restTemplate.exchange(
                programsUrl() + "/" + manualId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody()).isNotNull();

        List<Map<String, Object>> dayAssignments =
                (List<Map<String, Object>>) detailResponse.getBody().get("dayAssignments");
        assertThat(dayAssignments).hasSize(1);

        Map<String, Object> copiedDay = dayAssignments.get(0);
        assertThat(copiedDay.get("type")).isEqualTo("copied_day");

        // Snapshot should still be fully intact
        Map<String, Object> snapshot = (Map<String, Object>) copiedDay.get("snapshotData");
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.get("label")).isEqualTo("Monday");
        assertThat(snapshot.get("focusArea")).isEqualTo("Push");

        // Provenance metadata should still reference the deleted source
        assertThat(copiedDay.get("sourceProgramId")).isEqualTo(sourceProgramId.toString());
        assertThat(copiedDay.get("sourceWeekNumber")).isEqualTo(1);
        assertThat(copiedDay.get("sourceDayNumber")).isEqualTo(1);
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
     * Uploads a program via the upload endpoint and returns the created program's UUID.
     * Registers the ID for cleanup.
     */
    private UUID uploadProgram(String userId, String json) {
        HttpEntity<String> request = new HttpEntity<>(json, authHeaders(userId));
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
     * Returns a valid upload program JSON with 4 weeks (parser requires duration_weeks of 1 or 4).
     * Week 1: Day 1 — Monday / Push / Hypertrophy
     * Week 2: Day 1 — Wednesday / Pull / Strength
     * Week 3: Day 1 — Friday / Legs / Hypertrophy
     * Week 4: Day 1 — Saturday / Core / Strength
     */
    private String validFourWeekProgramJson() {
        return """
                {
                  "program_metadata": {
                    "program_name": "Source Program",
                    "duration_weeks": 4,
                    "goal": "Hypertrophy",
                    "equipment_profile": ["Barbell", "Dumbbell"],
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
                          "modality": "Hypertrophy",
                          "warm_up": [{"movement": "Arm circles", "instruction": "10 reps each direction"}],
                          "blocks": [
                            {
                              "block_type": "Tier 1: Compound",
                              "format": "Sets/Reps",
                              "movements": [
                                {
                                  "exercise_name": "Bench Press",
                                  "prescribed_sets": 4,
                                  "prescribed_reps": "8-10"
                                }
                              ]
                            }
                          ],
                          "cool_down": [{"movement": "Chest stretch", "instruction": "Hold 30s each side"}]
                        }
                      ]
                    },
                    {
                      "week_number": 2,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Wednesday",
                          "focus_area": "Pull",
                          "modality": "Strength",
                          "warm_up": [{"movement": "Band pull-aparts", "instruction": "15 reps"}],
                          "blocks": [
                            {
                              "block_type": "Tier 1: Compound",
                              "format": "Sets/Reps",
                              "movements": [
                                {
                                  "exercise_name": "Barbell Row",
                                  "prescribed_sets": 5,
                                  "prescribed_reps": "5"
                                }
                              ]
                            }
                          ],
                          "cool_down": [{"movement": "Lat stretch", "instruction": "Hold 30s each side"}]
                        }
                      ]
                    },
                    {
                      "week_number": 3,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Friday",
                          "focus_area": "Legs",
                          "modality": "Hypertrophy",
                          "warm_up": [{"movement": "Leg swings", "instruction": "10 each leg"}],
                          "blocks": [
                            {
                              "block_type": "Tier 1: Compound",
                              "format": "Sets/Reps",
                              "movements": [
                                {
                                  "exercise_name": "Squat",
                                  "prescribed_sets": 4,
                                  "prescribed_reps": "6-8"
                                }
                              ]
                            }
                          ],
                          "cool_down": [{"movement": "Quad stretch", "instruction": "Hold 30s each side"}]
                        }
                      ]
                    },
                    {
                      "week_number": 4,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Saturday",
                          "focus_area": "Core",
                          "modality": "Strength",
                          "warm_up": [{"movement": "Cat-cow", "instruction": "10 reps"}],
                          "blocks": [
                            {
                              "block_type": "Tier 2: Accessory",
                              "format": "Sets/Reps",
                              "movements": [
                                {
                                  "exercise_name": "Plank",
                                  "prescribed_sets": 3,
                                  "prescribed_reps": "60s"
                                }
                              ]
                            }
                          ],
                          "cool_down": [{"movement": "Child pose", "instruction": "Hold 60s"}]
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
