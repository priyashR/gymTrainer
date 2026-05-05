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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Property-based integration test for upload atomicity.
 *
 * <p>Feature: workout-creator-service-upload
 * <p>Property 4: Upload atomicity
 * <p>Validates: Requirement 6.4
 *
 * <p>For any invalid program JSON submitted to {@code POST /api/v1/uploads/programs},
 * the vault program count before and after the failed request must be equal —
 * no partial state is persisted.
 *
 * <p>Uses {@link JqwikSpringSupport} to share a single Spring application context
 * across all 100 property tries, avoiding the cost of a full context restart per try.
 * The RSA key pair is generated once in a static initialiser; the public key is
 * registered as a {@code @Primary} bean via {@link TestJwtConfig}.
 */
@JqwikSpringSupport
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("integration")
class UploadAtomicityPropertyTest {

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
    // Property 4: Upload atomicity
    // =========================================================================

    /**
     * For any invalid program JSON, the vault count must be unchanged after the failed request.
     *
     * <p>Covers the full range of invalid inputs: schema violations, malformed JSON,
     * wrong {@code duration_weeks}, mismatched {@code program_structure} length,
     * empty {@code equipment_profile}, wrong {@code version}, and empty blocks/movements.
     */
    @Property(tries = 100)
    void upload_anyInvalidJson_vaultCountUnchanged(
            @ForAll("invalidProgramJson") String invalidJson) {

        String userId = UUID.randomUUID().toString();
        long countBefore = programRepository.count();

        HttpEntity<String> request = new HttpEntity<>(invalidJson, authHeaders(userId));
        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl(), request, Map.class);

        // The request must have been rejected (400 or 401 — never 201)
        assertThat(response.getStatusCode())
                .as("Invalid JSON must not produce a 201: body=%s", invalidJson)
                .isNotEqualTo(HttpStatus.CREATED);

        long countAfter = programRepository.count();
        assertThat(countAfter)
                .as("Vault count must be unchanged after a failed upload (before=%d, after=%d, body=%s)",
                        countBefore, countAfter, invalidJson)
                .isEqualTo(countBefore);
    }

    // =========================================================================
    // Arbitraries — invalid program JSON generators
    // =========================================================================

    /**
     * Generates a variety of invalid program JSON strings covering all schema constraints
     * from Requirement 1.2 that the parser must reject.
     */
    @Provide
    Arbitrary<String> invalidProgramJson() {
        return Arbitraries.oneOf(
                invalidDurationWeeksJson(),
                mismatchedStructureLengthJson(),
                emptyEquipmentProfileJson(),
                wrongVersionJson(),
                emptyBlocksJson(),
                emptyMovementsJson(),
                crossFitMissingModalityTypeJson(),
                outOfRangeDayNumberJson(),
                malformedJson()
        );
    }

    /** duration_weeks is 2 or 3 — not in {1, 4}. */
    private Arbitrary<String> invalidDurationWeeksJson() {
        return Arbitraries.integers().between(2, 3).map(weeks ->
                buildProgramJson(weeks, "Test", "Hypertrophy", List.of("Barbell"), "Hypertrophy", weeks)
        );
    }

    /** program_structure has more or fewer weeks than duration_weeks declares. */
    private Arbitrary<String> mismatchedStructureLengthJson() {
        return Arbitraries.of(1, 4).flatMap(dw ->
                Arbitraries.oneOf(
                        // too few
                        Arbitraries.integers().between(0, dw - 1),
                        // too many
                        Arbitraries.integers().between(dw + 1, dw + 3)
                ).map(sz -> buildProgramJson(dw, "Test", "Hypertrophy", List.of("Barbell"), "Hypertrophy", sz))
        );
    }

    /** equipment_profile is an empty array. */
    private Arbitrary<String> emptyEquipmentProfileJson() {
        return Arbitraries.just(
                buildProgramJsonWithEquipment(List.of())
        );
    }

    /** version is not "1.0". */
    private Arbitrary<String> wrongVersionJson() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8)
                .filter(v -> !"1.0".equals(v))
                .map(this::buildProgramJsonWithVersion);
    }

    /** A day has an empty blocks array. */
    private Arbitrary<String> emptyBlocksJson() {
        return Arbitraries.just(buildProgramJsonWithEmptyBlocks());
    }

    /** A block has an empty movements array. */
    private Arbitrary<String> emptyMovementsJson() {
        return Arbitraries.just(buildProgramJsonWithEmptyMovements());
    }

    /** A CrossFit day has a movement without modality_type. */
    private Arbitrary<String> crossFitMissingModalityTypeJson() {
        return Arbitraries.just(buildCrossFitProgramWithoutModalityType());
    }

    /** day_number is 0 or ≥ 8. */
    private Arbitrary<String> outOfRangeDayNumberJson() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(-10, 0),
                Arbitraries.integers().between(8, 20)
        ).map(this::buildProgramJsonWithDayNumber);
    }

    /** Syntactically invalid JSON — not parseable at all. */
    private Arbitrary<String> malformedJson() {
        return Arbitraries.of(
                "not json at all",
                "{\"program_metadata\": {",
                "null",
                "[]",
                ""
        );
    }

    // =========================================================================
    // JSON builders
    // =========================================================================

    private String buildProgramJson(int durationWeeks, String name, String goal,
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

    private String buildProgramJsonWithEquipment(List<String> equipment) {
        return "{\"program_metadata\":{" +
                "\"program_name\":\"Test\"," +
                "\"duration_weeks\":1," +
                "\"goal\":\"Hypertrophy\"," +
                "\"equipment_profile\":" + qArray(equipment) + "," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[" +
                dayJson(1, "Hypertrophy") +
                "]}]}";
    }

    private String buildProgramJsonWithVersion(String version) {
        return "{\"program_metadata\":{" +
                "\"program_name\":\"Test\"," +
                "\"duration_weeks\":1," +
                "\"goal\":\"Hypertrophy\"," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":" + q(version) +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[" +
                dayJson(1, "Hypertrophy") +
                "]}]}";
    }

    private String buildProgramJsonWithEmptyBlocks() {
        return "{\"program_metadata\":{" +
                "\"program_name\":\"Test\"," +
                "\"duration_weeks\":1," +
                "\"goal\":\"Hypertrophy\"," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[{" +
                "\"day_number\":1,\"day_label\":\"Monday\",\"focus_area\":\"Push\"," +
                "\"modality\":\"Hypertrophy\",\"blocks\":[]" +
                "}]}]}";
    }

    private String buildProgramJsonWithEmptyMovements() {
        return "{\"program_metadata\":{" +
                "\"program_name\":\"Test\"," +
                "\"duration_weeks\":1," +
                "\"goal\":\"Hypertrophy\"," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[{" +
                "\"day_number\":1,\"day_label\":\"Monday\",\"focus_area\":\"Push\"," +
                "\"modality\":\"Hypertrophy\"," +
                "\"blocks\":[{\"block_type\":\"Tier 1\",\"format\":\"Sets/Reps\",\"movements\":[]}]" +
                "}]}]}";
    }

    private String buildCrossFitProgramWithoutModalityType() {
        return "{\"program_metadata\":{" +
                "\"program_name\":\"Test\"," +
                "\"duration_weeks\":1," +
                "\"goal\":\"GPP\"," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[{" +
                "\"day_number\":1,\"day_label\":\"Monday\",\"focus_area\":\"Engine\"," +
                "\"modality\":\"CrossFit\"," +
                "\"blocks\":[{\"block_type\":\"Metcon\",\"format\":\"AMRAP\",\"movements\":[{" +
                "\"exercise_name\":\"Burpees\"," +
                "\"prescribed_sets\":1," +
                "\"prescribed_reps\":\"AMRAP\"" +
                // modality_type intentionally omitted
                "}]}]" +
                "}]}]}";
    }

    private String buildProgramJsonWithDayNumber(int dayNumber) {
        return "{\"program_metadata\":{" +
                "\"program_name\":\"Test\"," +
                "\"duration_weeks\":1," +
                "\"goal\":\"Hypertrophy\"," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[" +
                dayJson(dayNumber, "Hypertrophy") +
                "]}]}";
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

    /** Wraps a string in JSON double-quotes, escaping backslash and double-quote. */
    private String q(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Serialises a list of strings as a JSON array. */
    private String qArray(List<String> values) {
        return values.stream().map(this::q).collect(Collectors.joining(",", "[", "]"));
    }
}
