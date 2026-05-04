package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.integration.upload;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramSpringDataRepository;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for the upload feature.
 *
 * <p>Connects to the local dev PostgreSQL instance (application-integration.yml).
 * Flyway migrations run on startup. Each test that creates a program cleans up
 * via explicit DELETE in {@link #cleanUp()} — {@code @Transactional} rollback is
 * not used because {@code RANDOM_PORT} runs the server in a separate thread.
 *
 * <p>JWT generation: a fresh RSA-2048 key pair is generated once per test class.
 * The public key is registered as a {@code @Primary} bean via {@link TestJwtConfig},
 * overriding the default {@link com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.config.JwtConfig}
 * bean. Tokens are signed with the matching private key, so the filter accepts them.
 */
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("integration")
class UploadIntegrationTest {

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

    /**
     * Registers the test public key as the primary {@link RSAPublicKey} bean,
     * replacing the one produced by {@code JwtConfig}. The {@link
     * com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.security.JwtAuthenticationFilter}
     * picks this up and will accept tokens signed with {@code testPrivateKey}.
     */
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
    private final java.util.Set<UUID> createdProgramIds = new java.util.LinkedHashSet<>();

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @AfterEach
    void cleanUp() {
        for (UUID id : createdProgramIds) {
            programRepository.deleteById(id);
        }
        createdProgramIds.clear();
    }

    // =========================================================================
    // Happy path — upload
    // =========================================================================

    @Test
    void uploadProgram_ValidOneWeekProgram_Returns201() {
        String userId = UUID.randomUUID().toString();
        HttpEntity<String> request = new HttpEntity<>(validOneWeekProgramJson(), authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(response.getBody().get("programName")).isEqualTo("Test Program");
        assertThat(response.getBody().get("contentSource")).isEqualTo("UPLOADED");

        // Register for cleanup
        createdProgramIds.add(UUID.fromString((String) response.getBody().get("id")));
    }

    @Test
    void uploadProgram_ValidFourWeekProgram_Returns201() {
        String userId = UUID.randomUUID().toString();
        HttpEntity<String> request = new HttpEntity<>(validFourWeekProgramJson(), authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("durationWeeks")).isEqualTo(4);

        createdProgramIds.add(UUID.fromString((String) response.getBody().get("id")));
    }

    @Test
    void uploadProgram_ValidProgram_AppearsInVaultListing() {
        String userId = UUID.randomUUID().toString();
        long countBefore = countProgramsForUser(userId);

        HttpEntity<String> request = new HttpEntity<>(validOneWeekProgramJson(), authHeaders(userId));
        ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID savedId = UUID.fromString((String) uploadResponse.getBody().get("id"));
        createdProgramIds.add(savedId);

        long countAfter = countProgramsForUser(userId);
        assertThat(countAfter).isEqualTo(countBefore + 1);
        assertThat(programRepository.findById(savedId)).isPresent();
    }

    @Test
    void uploadProgram_ValidProgram_ContentSourceIsUploaded() {
        String userId = UUID.randomUUID().toString();
        HttpEntity<String> request = new HttpEntity<>(validOneWeekProgramJson(), authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID savedId = UUID.fromString((String) response.getBody().get("id"));
        createdProgramIds.add(savedId);

        // Verify at the DB level
        assertThat(programRepository.findById(savedId))
                .isPresent()
                .hasValueSatisfying(entity ->
                        assertThat(entity.getContentSource().name()).isEqualTo("UPLOADED"));
    }

    // =========================================================================
    // Guard failures — 400
    // =========================================================================

    @Test
    void uploadProgram_BodyExceedsOneMb_Returns400BeforeParsing() {
        String userId = UUID.randomUUID().toString();
        // Build a JSON string that exceeds 1,048,576 bytes
        String oversizedBody = "{\"data\":\"" + "x".repeat(1_100_000) + "\"}";

        HttpEntity<String> request = new HttpEntity<>(oversizedBody, authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat((String) response.getBody().get("message"))
                .contains("File size exceeds the maximum allowed limit of 1 MB");
    }

    @Test
    void uploadProgram_WrongContentType_Returns400() {
        String userId = UUID.randomUUID().toString();
        HttpHeaders headers = authHeaders(userId);
        headers.setContentType(MediaType.TEXT_PLAIN);

        HttpEntity<String> request = new HttpEntity<>(validOneWeekProgramJson(), headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat((String) response.getBody().get("message"))
                .contains("Content-Type must be application/json");
    }

    @Test
    void uploadProgram_InvalidJson_Returns400() {
        String userId = UUID.randomUUID().toString();
        HttpEntity<String> request = new HttpEntity<>("this is not json", authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void uploadProgram_SchemaViolationMismatchedWeeks_Returns400WithFieldError() {
        String userId = UUID.randomUUID().toString();
        HttpEntity<String> request = new HttpEntity<>(invalidSchemaJson(), authHeaders(userId));

        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        // The response must contain an errors array
        Object errors = response.getBody().get("errors");
        assertThat(errors).isNotNull();
        assertThat((List<?>) errors).isNotEmpty();
    }

    // =========================================================================
    // Authentication — 401
    // =========================================================================

    @Test
    void uploadProgram_NoJwt_Returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(validOneWeekProgramJson(), headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // =========================================================================
    // Validate endpoint
    // =========================================================================

    @Test
    void validateProgram_ValidJson_Returns200WithValidTrue() {
        String userId = UUID.randomUUID().toString();
        long countBefore = programRepository.count();

        HttpEntity<String> request = new HttpEntity<>(validOneWeekProgramJson(), authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.postForEntity(validateUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("valid")).isEqualTo(true);
        assertThat((List<?>) response.getBody().get("errors")).isEmpty();

        // Nothing persisted
        assertThat(programRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void validateProgram_InvalidJson_Returns200WithValidFalseAndErrors() {
        String userId = UUID.randomUUID().toString();
        long countBefore = programRepository.count();

        HttpEntity<String> request = new HttpEntity<>(invalidSchemaJson(), authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.postForEntity(validateUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("valid")).isEqualTo(false);
        assertThat((List<?>) response.getBody().get("errors")).isNotEmpty();

        // Nothing persisted
        assertThat(programRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void validateProgram_NoJwt_Returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(validOneWeekProgramJson(), headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(validateUrl(), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String uploadUrl() {
        return "http://localhost:" + port + "/api/v1/uploads/programs";
    }

    private String validateUrl() {
        return "http://localhost:" + port + "/api/v1/uploads/programs/validate";
    }

    private long countProgramsForUser(String userId) {
        return programRepository.findAll().stream()
                .filter(p -> userId.equals(p.getOwnerUserId()))
                .count();
    }

    /**
     * Generates a valid RS256 JWT signed with the test private key.
     * The {@code sub} claim carries the userId; the token is valid for 15 minutes.
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

    /** Returns HttpHeaders with {@code Content-Type: application/json} and a valid Bearer token. */
    private HttpHeaders authHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateJwt(userId));
        return headers;
    }

    // =========================================================================
    // Test data
    // =========================================================================

    private String validOneWeekProgramJson() {
        return """
                {
                  "program_metadata": {
                    "program_name": "Test Program",
                    "duration_weeks": 1,
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
                    }
                  ]
                }
                """;
    }

    private String validFourWeekProgramJson() {
        String weekTemplate = """
                {
                  "week_number": %d,
                  "days": [
                    {
                      "day_number": 1,
                      "day_label": "Monday",
                      "focus_area": "Full Body",
                      "modality": "Hypertrophy",
                      "warm_up": [{"movement": "Jog", "instruction": "5 minutes easy"}],
                      "blocks": [
                        {
                          "block_type": "Strength",
                          "format": "Sets/Reps",
                          "movements": [
                            {
                              "exercise_name": "Squat",
                              "prescribed_sets": 3,
                              "prescribed_reps": "5"
                            }
                          ]
                        }
                      ],
                      "cool_down": [{"movement": "Hip flexor stretch", "instruction": "Hold 30s"}]
                    }
                  ]
                }
                """;
        return """
                {
                  "program_metadata": {
                    "program_name": "Four Week Block",
                    "duration_weeks": 4,
                    "goal": "Strength",
                    "equipment_profile": ["Barbell"],
                    "version": "1.0"
                  },
                  "program_structure": [
                    %s, %s, %s, %s
                  ]
                }
                """.formatted(
                weekTemplate.formatted(1),
                weekTemplate.formatted(2),
                weekTemplate.formatted(3),
                weekTemplate.formatted(4)
        );
    }

    /**
     * JSON with {@code duration_weeks: 1} but two weeks in {@code program_structure}.
     * This triggers the "number of weeks does not match duration_weeks" schema error.
     */
    private String invalidSchemaJson() {
        return """
                {
                  "program_metadata": {
                    "program_name": "Mismatched Program",
                    "duration_weeks": 1,
                    "goal": "GPP",
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
                          "focus_area": "Full Body",
                          "modality": "Hypertrophy",
                          "warm_up": [],
                          "blocks": [
                            {
                              "block_type": "Strength",
                              "format": "Sets/Reps",
                              "movements": [
                                {"exercise_name": "Squat", "prescribed_sets": 3, "prescribed_reps": "5"}
                              ]
                            }
                          ],
                          "cool_down": []
                        }
                      ]
                    },
                    {
                      "week_number": 2,
                      "days": [
                        {
                          "day_number": 1,
                          "day_label": "Monday",
                          "focus_area": "Full Body",
                          "modality": "Hypertrophy",
                          "warm_up": [],
                          "blocks": [
                            {
                              "block_type": "Strength",
                              "format": "Sets/Reps",
                              "movements": [
                                {"exercise_name": "Deadlift", "prescribed_sets": 3, "prescribed_reps": "5"}
                              ]
                            }
                          ],
                          "cool_down": []
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
