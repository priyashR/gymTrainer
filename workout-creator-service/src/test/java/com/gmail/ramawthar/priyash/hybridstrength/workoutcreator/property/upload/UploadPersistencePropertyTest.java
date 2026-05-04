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

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
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
 * Property-based integration test for the upload persistence invariant.
 *
 * <p>Feature: workout-creator-service-upload
 * <p>Property 2: Upload persistence invariant
 * <p>Validates: Requirements 2.1, 2.2, 4.1, 4.2, 4.5
 *
 * <p>For any valid program JSON submitted with a valid JWT:
 * <ul>
 *   <li>The persisted Program's {@code owner} equals the JWT subject claim</li>
 *   <li>The persisted Program's {@code contentSource} is {@code UPLOADED}</li>
 *   <li>The program appears in the user's vault (count increases by exactly 1)</li>
 * </ul>
 *
 * <p>Uses {@link JqwikSpringSupport} to share a single Spring application context
 * across all 100 property tries. The RSA key pair is generated once in a static
 * initialiser; the public key is registered as a {@code @Primary} bean via
 * {@link TestJwtConfig}. Each try cleans up the persisted program after assertion.
 */
@JqwikSpringSupport
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("integration")
class UploadPersistencePropertyTest {

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
    // Property 2: Upload persistence invariant
    // =========================================================================

    /**
     * For any valid program JSON with a valid JWT:
     * <ol>
     *   <li>The response is 201 Created</li>
     *   <li>The persisted Program's owner equals the JWT subject (not any client value)</li>
     *   <li>The persisted Program's contentSource is UPLOADED</li>
     *   <li>The program appears in the vault (user's program count increases by 1)</li>
     * </ol>
     */
    @Property(tries = 100)
    void upload_anyValidProgramJson_persistenceInvariantHolds(
            @ForAll("validProgramJson") String validJson) {

        String userId = UUID.randomUUID().toString();
        long countBefore = countProgramsForUser(userId);

        HttpEntity<String> request = new HttpEntity<>(validJson, authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        try {
            // 1. Must succeed with 201
            assertThat(response.getStatusCode())
                    .as("Valid program JSON must produce 201 Created")
                    .isEqualTo(HttpStatus.CREATED);

            assertThat(response.getBody()).isNotNull();
            String persistedId = (String) response.getBody().get("id");
            assertThat(persistedId)
                    .as("Response must include a persisted program id")
                    .isNotNull();

            UUID programId = UUID.fromString(persistedId);

            // 2. Owner must equal JWT subject — never a client-supplied value
            Optional<ProgramJpaEntity> persisted = programRepository.findById(programId);
            assertThat(persisted)
                    .as("Persisted program must be findable by id")
                    .isPresent();

            assertThat(persisted.get().getOwnerUserId())
                    .as("Owner must equal JWT subject claim (userId=%s)", userId)
                    .isEqualTo(userId);

            // 3. contentSource must be UPLOADED
            assertThat(persisted.get().getContentSource())
                    .as("contentSource must be UPLOADED for any uploaded program")
                    .isEqualTo(ContentSource.UPLOADED);

            // 4. Program appears in vault listing (count increases by exactly 1)
            long countAfter = countProgramsForUser(userId);
            assertThat(countAfter)
                    .as("Vault count must increase by exactly 1 after successful upload (before=%d, after=%d)",
                            countBefore, countAfter)
                    .isEqualTo(countBefore + 1);

        } finally {
            // Clean up: delete the persisted program so each try is independent
            if (response.getBody() != null && response.getBody().get("id") != null) {
                try {
                    programRepository.deleteById(UUID.fromString((String) response.getBody().get("id")));
                } catch (Exception ignored) {
                    // Best-effort cleanup — do not mask assertion failures
                }
            }
        }
    }

    // =========================================================================
    // Arbitraries — valid program JSON generators
    // =========================================================================

    /**
     * Generates valid program JSON strings covering both {@code duration_weeks} values
     * (1 and 4) and both modalities (CrossFit and Hypertrophy), with valid week/day
     * sequences and all required fields present.
     */
    @Provide
    Arbitrary<String> validProgramJson() {
        return Arbitraries.oneOf(
                validOneWeekHypertrophyJson(),
                validFourWeekHypertrophyJson(),
                validOneWeekCrossFitJson(),
                validFourWeekCrossFitJson(),
                validMixedModalityJson()
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

    private Arbitrary<String> validFourWeekCrossFitJson() {
        return programNameArb().flatMap(name ->
                equipmentArb().map(equipment ->
                        buildProgramJson(name, 4, "GPP", equipment, "CrossFit", 4)
                )
        );
    }

    /** Mixed modality: some weeks CrossFit, some Hypertrophy (4-week program). */
    private Arbitrary<String> validMixedModalityJson() {
        return programNameArb().flatMap(name ->
                equipmentArb().map(equipment -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"program_metadata\":{");
                    sb.append("\"program_name\":").append(q(name)).append(",");
                    sb.append("\"duration_weeks\":4,");
                    sb.append("\"goal\":\"Strength Bias\",");
                    sb.append("\"equipment_profile\":").append(qArray(equipment)).append(",");
                    sb.append("\"version\":\"1.0\"");
                    sb.append("},\"program_structure\":[");
                    String[] modalities = {"Hypertrophy", "CrossFit", "Hypertrophy", "CrossFit"};
                    for (int w = 1; w <= 4; w++) {
                        if (w > 1) sb.append(",");
                        sb.append("{\"week_number\":").append(w).append(",\"days\":[")
                          .append(dayJson(1, modalities[w - 1]))
                          .append("]}");
                    }
                    sb.append("]}");
                    return sb.toString();
                })
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

    private long countProgramsForUser(String userId) {
        return programRepository.findAll().stream()
                .filter(p -> userId.equals(p.getOwnerUserId()))
                .count();
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
