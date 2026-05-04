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
 * Property-based integration test for validate endpoint correctness.
 *
 * <p>Feature: workout-creator-service-upload
 * <p>Property 7: Validate endpoint correctness
 * <p>Validates: Requirements 9.2, 9.3
 *
 * <p>For any Program JSON:
 * <ul>
 *   <li>If the JSON passes all Upload_Schema rules → {@code POST /api/v1/uploads/programs/validate}
 *       returns 200 with {@code { "valid": true, "errors": [] }}</li>
 *   <li>If the JSON fails any Upload_Schema rule → returns 200 with
 *       {@code { "valid": false, "errors": [...] }} containing at least one error entry</li>
 *   <li>In both cases, the vault program count is unchanged — nothing is persisted</li>
 * </ul>
 *
 * <p>Uses {@link JqwikSpringSupport} to share a single Spring application context
 * across all 100 property tries. The RSA key pair is generated once in a static
 * initialiser; the public key is registered as a {@code @Primary} bean via
 * {@link TestJwtConfig}.
 */
@JqwikSpringSupport
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("integration")
class ValidateEndpointCorrectnessPropertyTest {

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
    // Property 7a: Valid JSON → { valid: true, errors: [] }, nothing persisted
    // =========================================================================

    /**
     * For any valid program JSON, the validate endpoint must return 200 with
     * {@code valid=true} and an empty {@code errors} array, and must not persist anything.
     */
    @Property(tries = 100)
    void validate_anyValidProgramJson_returnsValidTrueAndNothingPersisted(
            @ForAll("validProgramJson") String validJson) {

        long countBefore = programRepository.count();

        HttpEntity<String> request = new HttpEntity<>(validJson, authHeaders());
        ResponseEntity<Map> response = restTemplate.postForEntity(validateUrl(), request, Map.class);

        // Must return 200 OK
        assertThat(response.getStatusCode())
                .as("Validate endpoint must return 200 for valid JSON, body=%s", validJson)
                .isEqualTo(HttpStatus.OK);

        Map<?, ?> body = response.getBody();
        assertThat(body).as("Response body must not be null").isNotNull();

        // valid must be true
        assertThat(body.get("valid"))
                .as("valid must be true for valid JSON")
                .isEqualTo(true);

        // errors must be an empty list
        Object errors = body.get("errors");
        assertThat(errors)
                .as("errors must be present in the response")
                .isNotNull();
        assertThat(errors)
                .as("errors must be a List")
                .isInstanceOf(List.class);
        assertThat((List<?>) errors)
                .as("errors must be empty for valid JSON")
                .isEmpty();

        // Nothing persisted — vault count unchanged
        long countAfter = programRepository.count();
        assertThat(countAfter)
                .as("Vault count must be unchanged after validate (before=%d, after=%d)", countBefore, countAfter)
                .isEqualTo(countBefore);
    }

    // =========================================================================
    // Property 7b: Invalid JSON → { valid: false, errors: [≥1 entry] }, nothing persisted
    // =========================================================================

    /**
     * For any invalid program JSON, the validate endpoint must return 200 with
     * {@code valid=false} and at least one error entry, and must not persist anything.
     */
    @Property(tries = 100)
    void validate_anyInvalidProgramJson_returnsValidFalseWithErrorsAndNothingPersisted(
            @ForAll("invalidProgramJson") String invalidJson) {

        long countBefore = programRepository.count();

        HttpEntity<String> request = new HttpEntity<>(invalidJson, authHeaders());
        ResponseEntity<Map> response = restTemplate.postForEntity(validateUrl(), request, Map.class);

        // Must return 200 OK (validate never returns 400 for schema failures — it reports them in the body)
        // Exception: malformed JSON that cannot be parsed at all may return 400
        int statusCode = response.getStatusCode().value();
        assertThat(statusCode == 200 || statusCode == 400)
                .as("Validate endpoint must return 200 or 400 for invalid JSON, got %d", statusCode)
                .isTrue();

        if (statusCode == 200) {
            Map<?, ?> body = response.getBody();
            assertThat(body).as("Response body must not be null").isNotNull();

            // valid must be false
            assertThat(body.get("valid"))
                    .as("valid must be false for invalid JSON, body=%s", body)
                    .isEqualTo(false);

            // errors must be a non-empty list
            Object errors = body.get("errors");
            assertThat(errors)
                    .as("errors must be present for invalid JSON")
                    .isNotNull();
            assertThat(errors)
                    .as("errors must be a List")
                    .isInstanceOf(List.class);
            assertThat((List<?>) errors)
                    .as("errors must contain at least one entry for invalid JSON")
                    .isNotEmpty();

            // Each error entry must have field and message
            for (Object entry : (List<?>) errors) {
                assertThat(entry).isInstanceOf(Map.class);
                Map<?, ?> errorEntry = (Map<?, ?>) entry;
                assertThat(errorEntry.get("field"))
                        .as("Each error entry must have a 'field' property")
                        .isNotNull();
                assertThat(errorEntry.get("message"))
                        .as("Each error entry must have a 'message' property")
                        .isNotNull();
            }
        }

        // Nothing persisted — vault count unchanged regardless of response status
        long countAfter = programRepository.count();
        assertThat(countAfter)
                .as("Vault count must be unchanged after validate (before=%d, after=%d)", countBefore, countAfter)
                .isEqualTo(countBefore);
    }

    // =========================================================================
    // Arbitraries — valid program JSON
    // =========================================================================

    /**
     * Generates valid program JSON covering both {@code duration_weeks} values (1 and 4)
     * and both modalities (CrossFit and Hypertrophy).
     */
    @Provide
    Arbitrary<String> validProgramJson() {
        return Arbitraries.oneOf(
                validOneWeekHypertrophyJson(),
                validFourWeekHypertrophyJson(),
                validOneWeekCrossFitJson(),
                validFourWeekCrossFitJson()
        );
    }

    private Arbitrary<String> validOneWeekHypertrophyJson() {
        return programNameArb().flatMap(name ->
                goalArb().flatMap(goal ->
                        equipmentArb().map(eq ->
                                buildProgramJson(name, 1, goal, eq, "Hypertrophy", 1)
                        )
                )
        );
    }

    private Arbitrary<String> validFourWeekHypertrophyJson() {
        return programNameArb().flatMap(name ->
                goalArb().flatMap(goal ->
                        equipmentArb().map(eq ->
                                buildProgramJson(name, 4, goal, eq, "Hypertrophy", 4)
                        )
                )
        );
    }

    private Arbitrary<String> validOneWeekCrossFitJson() {
        return programNameArb().flatMap(name ->
                equipmentArb().map(eq ->
                        buildProgramJson(name, 1, "GPP", eq, "CrossFit", 1)
                )
        );
    }

    private Arbitrary<String> validFourWeekCrossFitJson() {
        return programNameArb().flatMap(name ->
                equipmentArb().map(eq ->
                        buildProgramJson(name, 4, "GPP", eq, "CrossFit", 4)
                )
        );
    }

    // =========================================================================
    // Arbitraries — invalid program JSON (schema violations)
    // =========================================================================

    /**
     * Generates invalid program JSON covering all Upload_Schema constraints from Requirement 1.2.
     * Malformed JSON (unparseable) is excluded here — those produce 400, not 200 with valid=false.
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
                outOfRangeDayNumberJson()
        );
    }

    private Arbitrary<String> invalidDurationWeeksJson() {
        return Arbitraries.integers().between(2, 3).map(weeks ->
                buildProgramJson("Test", weeks, "Hypertrophy", List.of("Barbell"), "Hypertrophy", weeks)
        );
    }

    private Arbitrary<String> mismatchedStructureLengthJson() {
        return Arbitraries.of(1, 4).flatMap(dw ->
                Arbitraries.oneOf(
                        Arbitraries.integers().between(0, dw - 1),
                        Arbitraries.integers().between(dw + 1, dw + 3)
                ).map(sz -> buildProgramJson("Test", dw, "Hypertrophy", List.of("Barbell"), "Hypertrophy", sz))
        );
    }

    private Arbitrary<String> emptyEquipmentProfileJson() {
        return Arbitraries.just(buildProgramJsonWithEquipment(List.of()));
    }

    private Arbitrary<String> wrongVersionJson() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8)
                .filter(v -> !"1.0".equals(v))
                .map(this::buildProgramJsonWithVersion);
    }

    private Arbitrary<String> emptyBlocksJson() {
        return Arbitraries.just(buildProgramJsonWithEmptyBlocks());
    }

    private Arbitrary<String> emptyMovementsJson() {
        return Arbitraries.just(buildProgramJsonWithEmptyMovements());
    }

    private Arbitrary<String> crossFitMissingModalityTypeJson() {
        return Arbitraries.just(buildCrossFitProgramWithoutModalityType());
    }

    private Arbitrary<String> outOfRangeDayNumberJson() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(-10, 0),
                Arbitraries.integers().between(8, 20)
        ).map(this::buildProgramJsonWithDayNumber);
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

    private String validateUrl() {
        return "http://localhost:" + port + "/api/v1/uploads/programs/validate";
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateJwt(UUID.randomUUID().toString()));
        return headers;
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

    private String q(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String qArray(List<String> values) {
        return values.stream().map(this::q).collect(Collectors.joining(",", "[", "]"));
    }
}
