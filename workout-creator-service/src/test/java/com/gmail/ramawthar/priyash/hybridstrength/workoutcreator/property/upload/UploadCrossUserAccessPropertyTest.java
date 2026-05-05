package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.upload;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramJpaEntity;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound.ProgramSpringDataRepository;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Property-based integration test for cross-user access denial.
 *
 * <p>Feature: workout-creator-service-upload
 * <p>Property 3: Cross-user access is denied
 * <p>Validates: Requirements 4.3, 4.4
 *
 * <p>For any Program uploaded by user A, user B (a different authenticated user)
 * must not be able to access that program. This is verified at two levels:
 * <ol>
 *   <li><b>Ownership isolation</b> — the persisted program's {@code ownerUserId} equals
 *       user A's JWT subject, never user B's. User B's vault scope contains zero programs
 *       with user A's program ID.</li>
 *   <li><b>Upload endpoint isolation</b> — user B uploading their own valid program
 *       produces a separate record owned by user B; the two programs are independent
 *       and neither user can see the other's record via their own vault scope.</li>
 * </ol>
 *
 * <p>Note: HTTP-level 403 enforcement (Requirements 4.3, 4.4) will be fully exercised
 * once the vault GET/DELETE endpoints ({@code GET /api/v1/programs/{id}},
 * {@code DELETE /api/v1/programs/{id}}) are implemented. At that point, add assertions
 * here that user B receives 403 when requesting user A's program ID directly.
 *
 * <p>Uses {@link JqwikSpringSupport} to share a single Spring application context
 * across all 100 property tries. The RSA key pair is generated once in a static
 * initialiser; the public key is registered as a {@code @Primary} bean via
 * {@link TestJwtConfig}. Each try cleans up both persisted programs after assertion.
 */
@JqwikSpringSupport
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("integration")
class UploadCrossUserAccessPropertyTest {

    // ── Shared RSA key pair ───────────────────────────────────────────────────

    private static final RSAPrivateKey TEST_PRIVATE_KEY;
    private static final RSAPublicKey TEST_PUBLIC_KEY;

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            TEST_PRIVATE_KEY = (RSAPrivateKey) pair.getPrivate();
            TEST_PUBLIC_KEY = (RSAPublicKey) pair.getPublic();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── @TestConfiguration: override the RSAPublicKey bean ───────────────────

    @TestConfiguration
    static class TestJwtConfig {
        @Bean
        @Primary
        RSAPublicKey rsaPublicKey() {
            return TEST_PUBLIC_KEY;
        }
    }

    // ── Spring wiring ─────────────────────────────────────────────────────────

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProgramSpringDataRepository programRepository;

    // =========================================================================
    // Property 3: Cross-user access is denied
    // =========================================================================

    /**
     * For any valid program JSON uploaded by user A:
     * <ol>
     *   <li>The upload succeeds (201) and the program is owned by user A</li>
     *   <li>User B's vault scope contains no programs owned by user A</li>
     *   <li>User A's program ID does not appear in user B's owned programs</li>
     *   <li>User B uploading their own program produces a separate, independently owned record</li>
     * </ol>
     */
    @Property(tries = 100)
    void upload_programUploadedByUserA_isNotAccessibleByUserB(
            @ForAll("validProgramJson") String validJson) {

        String userA = UUID.randomUUID().toString();
        String userB = UUID.randomUUID().toString();

        // Ensure the two users are distinct (UUID collision is astronomically unlikely but guard it)
        assertThat(userA).isNotEqualTo(userB);

        UUID programAId = null;
        UUID programBId = null;

        try {
            // ── Step 1: User A uploads a program ─────────────────────────────
            HttpEntity<String> requestA = new HttpEntity<>(validJson, authHeaders(userA));
            ResponseEntity<Map> responseA = restTemplate.postForEntity(uploadUrl(), requestA, Map.class);

            assertThat(responseA.getStatusCode())
                    .as("User A's upload must succeed with 201")
                    .isEqualTo(HttpStatus.CREATED);
            assertThat(responseA.getBody()).isNotNull();

            programAId = UUID.fromString((String) responseA.getBody().get("id"));

            // ── Step 2: Verify ownership — program belongs to user A only ────
            Optional<ProgramJpaEntity> persistedA = programRepository.findById(programAId);
            assertThat(persistedA)
                    .as("User A's program must be persisted")
                    .isPresent();

            assertThat(persistedA.get().getOwnerUserId())
                    .as("Program uploaded by user A must be owned by user A (ownerUserId=%s)", userA)
                    .isEqualTo(userA);

            assertThat(persistedA.get().getOwnerUserId())
                    .as("Program uploaded by user A must NOT be owned by user B (userB=%s)", userB)
                    .isNotEqualTo(userB);

            // ── Step 3: User B's vault scope must not contain user A's program ─
            List<ProgramJpaEntity> userBPrograms = programRepository.findAll().stream()
                    .filter(p -> userB.equals(p.getOwnerUserId()))
                    .collect(Collectors.toList());

            final UUID finalProgramAId = programAId;
            assertThat(userBPrograms)
                    .as("User B's vault scope must not contain user A's program (programId=%s)", programAId)
                    .noneMatch(p -> finalProgramAId.equals(p.getId()));

            // ── Step 4: User B uploads their own program — must be independent ─
            HttpEntity<String> requestB = new HttpEntity<>(validJson, authHeaders(userB));
            ResponseEntity<Map> responseB = restTemplate.postForEntity(uploadUrl(), requestB, Map.class);

            assertThat(responseB.getStatusCode())
                    .as("User B's upload must also succeed with 201")
                    .isEqualTo(HttpStatus.CREATED);
            assertThat(responseB.getBody()).isNotNull();

            programBId = UUID.fromString((String) responseB.getBody().get("id"));

            // The two programs must be distinct records
            assertThat(programBId)
                    .as("User A and user B must receive different program IDs")
                    .isNotEqualTo(programAId);

            // User B's program must be owned by user B
            Optional<ProgramJpaEntity> persistedB = programRepository.findById(programBId);
            assertThat(persistedB)
                    .as("User B's program must be persisted")
                    .isPresent();

            assertThat(persistedB.get().getOwnerUserId())
                    .as("Program uploaded by user B must be owned by user B (ownerUserId=%s)", userB)
                    .isEqualTo(userB);

            // ── Step 5: Vault scopes are disjoint ────────────────────────────
            List<ProgramJpaEntity> userAPrograms = programRepository.findAll().stream()
                    .filter(p -> userA.equals(p.getOwnerUserId()))
                    .collect(Collectors.toList());

            final UUID finalProgramBId = programBId;

            // User A cannot see user B's program
            assertThat(userAPrograms)
                    .as("User A's vault scope must not contain user B's program (programId=%s)", programBId)
                    .noneMatch(p -> finalProgramBId.equals(p.getId()));

            // User B cannot see user A's program (re-check after both uploads)
            List<ProgramJpaEntity> userBProgramsAfter = programRepository.findAll().stream()
                    .filter(p -> userB.equals(p.getOwnerUserId()))
                    .collect(Collectors.toList());

            assertThat(userBProgramsAfter)
                    .as("User B's vault scope must not contain user A's program after both uploads")
                    .noneMatch(p -> finalProgramAId.equals(p.getId()));

        } finally {
            // Clean up both programs so each try is independent
            if (programAId != null) {
                try { programRepository.deleteById(programAId); } catch (Exception ignored) {}
            }
            if (programBId != null) {
                try { programRepository.deleteById(programBId); } catch (Exception ignored) {}
            }
        }
    }

    // =========================================================================
    // Arbitraries — valid program JSON generators
    // =========================================================================

    @Provide
    Arbitrary<String> validProgramJson() {
        return Arbitraries.oneOf(
                validOneWeekHypertrophyJson(),
                validFourWeekHypertrophyJson(),
                validOneWeekCrossFitJson()
        );
    }

    private Arbitrary<String> validOneWeekHypertrophyJson() {
        return programNameArb().flatMap(name ->
                goalArb().flatMap(goal ->
                        equipmentArb().map(equipment ->
                                buildProgramJson(name, 1, goal, equipment, "Hypertrophy", 1)
                        )
                )
        );
    }

    private Arbitrary<String> validFourWeekHypertrophyJson() {
        return programNameArb().flatMap(name ->
                goalArb().flatMap(goal ->
                        equipmentArb().map(equipment ->
                                buildProgramJson(name, 4, goal, equipment, "Hypertrophy", 4)
                        )
                )
        );
    }

    private Arbitrary<String> validOneWeekCrossFitJson() {
        return programNameArb().flatMap(name ->
                equipmentArb().map(equipment ->
                        buildProgramJson(name, 1, "GPP", equipment, "CrossFit", 1)
                )
        );
    }

    // =========================================================================
    // JSON builders
    // =========================================================================

    private String buildProgramJson(String name, int durationWeeks, String goal,
                                    List<String> equipment, String modality, int structureSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"program_metadata\":{");
        sb.append("\"program_name\":").append(q(name)).append(",");
        sb.append("\"duration_weeks\":").append(durationWeeks).append(",");
        sb.append("\"goal\":").append(q(goal)).append(",");
        sb.append("\"equipment_profile\":").append(qArray(equipment)).append(",");
        sb.append("\"version\":\"1.0\"");
        sb.append("},\"program_structure\":[");
        for (int w = 1; w <= structureSize; w++) {
            if (w > 1) sb.append(",");
            sb.append("{\"week_number\":").append(w).append(",\"days\":[")
              .append(dayJson(1, modality))
              .append("]}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String dayJson(int dayNumber, String modality) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"day_number\":").append(dayNumber).append(",");
        sb.append("\"day_label\":").append(q("Day " + dayNumber)).append(",");
        sb.append("\"focus_area\":\"Full Body\",");
        sb.append("\"modality\":").append(q(modality)).append(",");
        sb.append("\"warm_up\":[{\"movement\":\"Jog\",\"instruction\":\"5 min easy\"}],");
        sb.append("\"blocks\":[{\"block_type\":\"Tier 1\",\"format\":\"Sets/Reps\",\"movements\":[{");
        sb.append("\"exercise_name\":\"Squat\",");
        if ("CrossFit".equals(modality)) {
            sb.append("\"modality_type\":\"Weightlifting\",");
        }
        sb.append("\"prescribed_sets\":3,");
        sb.append("\"prescribed_reps\":\"5\"");
        sb.append("}]}],");
        sb.append("\"cool_down\":[{\"movement\":\"Stretch\",\"instruction\":\"Hold 30s\"}]");
        sb.append("}");
        return sb.toString();
    }

    // =========================================================================
    // Arbitrary helpers
    // =========================================================================

    private Arbitrary<String> programNameArb() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(40)
                .map(s -> s.trim().isEmpty() ? "Program" : s.trim());
    }

    private Arbitrary<String> goalArb() {
        return Arbitraries.of("Hypertrophy", "Strength", "GPP", "Strength Bias", "Endurance");
    }

    private Arbitrary<List<String>> equipmentArb() {
        return Arbitraries.of("Barbell", "Dumbbell", "Kettlebell", "Bodyweight", "Cable Machine")
                .list().ofMinSize(1).ofMaxSize(3);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String uploadUrl() {
        return "http://localhost:" + port + "/api/v1/uploads/programs";
    }

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
            jwt.sign(new RSASSASigner(TEST_PRIVATE_KEY));
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

    private String q(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String qArray(List<String> values) {
        return values.stream().map(this::q).collect(Collectors.joining(",", "[", "]"));
    }
}
