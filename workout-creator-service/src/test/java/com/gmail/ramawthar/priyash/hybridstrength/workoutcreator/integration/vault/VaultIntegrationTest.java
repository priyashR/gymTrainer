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
 * Integration tests for the Vault feature endpoints.
 *
 * <p>Connects to the local dev PostgreSQL instance (application-integration.yml).
 * Tests the full request/response cycle for list, get, update, delete, copy, and search.
 */
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("integration")
class VaultIntegrationTest {

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
    // Happy path — List programs
    // =========================================================================

    @Test
    void listPrograms_ReturnsUploadedPrograms() {
        String userId = UUID.randomUUID().toString();
        UUID programId = uploadProgram(userId, validOneWeekProgramJson());

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("page")).isEqualTo(0);
        assertThat(response.getBody().get("size")).isEqualTo(20);
        assertThat((List<?>) response.getBody().get("content")).isNotEmpty();
    }

    // =========================================================================
    // Happy path — Get program detail
    // =========================================================================

    @Test
    void getProgram_ReturnsFullDetail() {
        String userId = UUID.randomUUID().toString();
        UUID programId = uploadProgram(userId, validOneWeekProgramJson());

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + programId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isEqualTo(programId.toString());
        assertThat(response.getBody().get("name")).isEqualTo("Test Program");
        assertThat(response.getBody().get("goal")).isEqualTo("Hypertrophy");
        assertThat(response.getBody().get("durationWeeks")).isEqualTo(1);
        assertThat((List<?>) response.getBody().get("weeks")).hasSize(1);
        assertThat(response.getBody().get("contentSource")).isEqualTo("UPLOADED");
    }

    // =========================================================================
    // Happy path — Update program
    // =========================================================================

    @Test
    void updateProgram_ReplacesContent_Returns200() {
        String userId = UUID.randomUUID().toString();
        UUID programId = uploadProgram(userId, validOneWeekProgramJson());

        // Update with a different program name and goal
        String updatedJson = validOneWeekProgramJson()
                .replace("\"Test Program\"", "\"Updated Program\"")
                .replace("\"Hypertrophy\"", "\"Strength\"");

        HttpEntity<String> request = new HttpEntity<>(updatedJson, authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + programId,
                HttpMethod.PUT,
                request,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isEqualTo(programId.toString());
        assertThat(response.getBody().get("name")).isEqualTo("Updated Program");
        assertThat(response.getBody().get("contentSource")).isEqualTo("UPLOADED"); // preserved
    }

    @Test
    void updateProgram_PreservesImmutableFields() {
        String userId = UUID.randomUUID().toString();
        UUID programId = uploadProgram(userId, validOneWeekProgramJson());

        String updatedJson = validOneWeekProgramJson()
                .replace("\"Test Program\"", "\"New Name\"");

        HttpEntity<String> request = new HttpEntity<>(updatedJson, authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + programId,
                HttpMethod.PUT,
                request,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // id and contentSource must be preserved
        assertThat(response.getBody().get("id")).isEqualTo(programId.toString());
        assertThat(response.getBody().get("contentSource")).isEqualTo("UPLOADED");
    }

    // =========================================================================
    // Happy path — Delete program
    // =========================================================================

    @Test
    void deleteProgram_Returns204_AndProgramIsGone() {
        String userId = UUID.randomUUID().toString();
        UUID programId = uploadProgram(userId, validOneWeekProgramJson());

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                vaultUrl() + "/" + programId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(userId)),
                Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Remove from cleanup since it's already deleted
        createdProgramIds.remove(programId);

        // Subsequent GET should return 403
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                vaultUrl() + "/" + programId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // Happy path — Copy program
    // =========================================================================

    @Test
    void copyProgram_Returns201_WithNewIdAndCopySuffix() {
        String userId = UUID.randomUUID().toString();
        UUID programId = uploadProgram(userId, validOneWeekProgramJson());

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + programId + "/copy",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        String copyId = (String) response.getBody().get("id");
        assertThat(copyId).isNotEqualTo(programId.toString()); // new ID
        assertThat(response.getBody().get("name")).isEqualTo("Test Program (Copy)");
        assertThat(response.getBody().get("contentSource")).isEqualTo("MANUAL");

        // Track the copy for cleanup
        createdProgramIds.add(UUID.fromString(copyId));
    }

    // =========================================================================
    // Happy path — Search
    // =========================================================================

    @Test
    void searchPrograms_ByKeyword_ReturnsMatchingResults() {
        String userId = UUID.randomUUID().toString();
        uploadProgram(userId, validOneWeekProgramJson()); // name = "Test Program"

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/search?q=Test",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotEmpty();
    }

    @Test
    void searchPrograms_WithFocusAreaFilter_ReturnsFilteredResults() {
        String userId = UUID.randomUUID().toString();
        uploadProgram(userId, validOneWeekProgramJson()); // focusArea = "Push"

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/search?q=Test&focusArea=Push",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotEmpty();
    }

    @Test
    void searchPrograms_WithModalityFilter_ReturnsFilteredResults() {
        String userId = UUID.randomUUID().toString();
        uploadProgram(userId, validOneWeekProgramJson()); // modality = "Hypertrophy"

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/search?q=Test&modality=Hypertrophy",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotEmpty();
    }

    @Test
    void searchPrograms_OnlyReturnsOwnedPrograms() {
        String userId1 = UUID.randomUUID().toString();
        String userId2 = UUID.randomUUID().toString();
        uploadProgram(userId1, validOneWeekProgramJson());

        // User2 searches — should not see user1's programs
        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/search?q=Test",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId2)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isEmpty();
    }

    // =========================================================================
    // 403 — Non-owner access and non-existent programs
    // =========================================================================

    @Test
    void getProgram_NonOwner_Returns403() {
        String owner = UUID.randomUUID().toString();
        String otherUser = UUID.randomUUID().toString();
        UUID programId = uploadProgram(owner, validOneWeekProgramJson());

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + programId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(otherUser)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getProgram_NonExistent_Returns403() {
        String userId = UUID.randomUUID().toString();
        UUID fakeId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + fakeId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateProgram_NonOwner_Returns403() {
        String owner = UUID.randomUUID().toString();
        String otherUser = UUID.randomUUID().toString();
        UUID programId = uploadProgram(owner, validOneWeekProgramJson());

        HttpEntity<String> request = new HttpEntity<>(validOneWeekProgramJson(), authHeaders(otherUser));
        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + programId,
                HttpMethod.PUT,
                request,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteProgram_NonOwner_Returns403() {
        String owner = UUID.randomUUID().toString();
        String otherUser = UUID.randomUUID().toString();
        UUID programId = uploadProgram(owner, validOneWeekProgramJson());

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + programId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(otherUser)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteProgram_NonExistent_Returns403() {
        String userId = UUID.randomUUID().toString();
        UUID fakeId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + fakeId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void copyProgram_NonOwner_Returns403() {
        String owner = UUID.randomUUID().toString();
        String otherUser = UUID.randomUUID().toString();
        UUID programId = uploadProgram(owner, validOneWeekProgramJson());

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + programId + "/copy",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(otherUser)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // 400 — Invalid JSON on update and empty search query
    // =========================================================================

    @Test
    void updateProgram_InvalidJson_Returns400() {
        String userId = UUID.randomUUID().toString();
        UUID programId = uploadProgram(userId, validOneWeekProgramJson());

        HttpEntity<String> request = new HttpEntity<>("this is not valid json", authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + programId,
                HttpMethod.PUT,
                request,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void searchPrograms_EmptyQuery_Returns400() {
        String userId = UUID.randomUUID().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/search?q=",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat((String) response.getBody().get("message")).contains("Search query must not be empty");
    }

    @Test
    void listPrograms_PageSizeExceedsMax_Returns400() {
        String userId = UUID.randomUUID().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "?page=0&size=101",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat((String) response.getBody().get("message")).contains("Page size must not exceed 100");
    }

    // =========================================================================
    // 401 — No JWT
    // =========================================================================

    @Test
    void listPrograms_NoJwt_Returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getProgram_NoJwt_Returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/" + UUID.randomUUID(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // =========================================================================
    // Pagination — multiple pages
    // =========================================================================

    @Test
    void listPrograms_Pagination_ReturnsCorrectMetadata() {
        String userId = UUID.randomUUID().toString();

        // Upload 3 programs
        for (int i = 0; i < 3; i++) {
            uploadProgram(userId, validOneWeekProgramJson());
        }

        // Request page 0 with size 2
        ResponseEntity<Map> page0 = restTemplate.exchange(
                vaultUrl() + "?page=0&size=2",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(page0.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page0.getBody()).isNotNull();
        assertThat((List<?>) page0.getBody().get("content")).hasSize(2);
        assertThat(page0.getBody().get("page")).isEqualTo(0);
        assertThat(page0.getBody().get("size")).isEqualTo(2);
        assertThat((Integer) page0.getBody().get("totalElements")).isGreaterThanOrEqualTo(3);
        assertThat((Integer) page0.getBody().get("totalPages")).isGreaterThanOrEqualTo(2);

        // Request page 1 with size 2
        ResponseEntity<Map> page1 = restTemplate.exchange(
                vaultUrl() + "?page=1&size=2",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(page1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page1.getBody()).isNotNull();
        assertThat((List<?>) page1.getBody().get("content")).isNotEmpty();
        assertThat(page1.getBody().get("page")).isEqualTo(1);
    }

    // =========================================================================
    // Upload → Vault integration
    // =========================================================================

    @Test
    void uploadThenRetrieveViaVault_FullIntegration() {
        String userId = UUID.randomUUID().toString();

        // Upload via upload endpoint
        HttpEntity<String> uploadRequest = new HttpEntity<>(validOneWeekProgramJson(), authHeaders(userId));
        ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(uploadUrl(), uploadRequest, Map.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String uploadedId = (String) uploadResponse.getBody().get("id");
        createdProgramIds.add(UUID.fromString(uploadedId));

        // Retrieve via vault GET endpoint
        ResponseEntity<Map> vaultResponse = restTemplate.exchange(
                vaultUrl() + "/" + uploadedId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(vaultResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(vaultResponse.getBody()).isNotNull();
        assertThat(vaultResponse.getBody().get("id")).isEqualTo(uploadedId);
        assertThat(vaultResponse.getBody().get("name")).isEqualTo("Test Program");
        assertThat(vaultResponse.getBody().get("contentSource")).isEqualTo("UPLOADED");
        assertThat((List<?>) vaultResponse.getBody().get("weeks")).hasSize(1);

        // Also verify it appears in the listing
        ResponseEntity<Map> listResponse = restTemplate.exchange(
                vaultUrl(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) listResponse.getBody().get("content");
        assertThat(content).isNotEmpty();
    }

    // =========================================================================
    // Search with filters against real database
    // =========================================================================

    @Test
    void searchPrograms_NonMatchingFocusArea_ReturnsEmpty() {
        String userId = UUID.randomUUID().toString();
        uploadProgram(userId, validOneWeekProgramJson()); // focusArea = "Push"

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/search?q=Test&focusArea=Pull",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isEmpty();
    }

    @Test
    void searchPrograms_NonMatchingModality_ReturnsEmpty() {
        String userId = UUID.randomUUID().toString();
        uploadProgram(userId, validOneWeekProgramJson()); // modality = "Hypertrophy"

        ResponseEntity<Map> response = restTemplate.exchange(
                vaultUrl() + "/search?q=Test&modality=Strength",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userId)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isEmpty();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String vaultUrl() {
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
}
