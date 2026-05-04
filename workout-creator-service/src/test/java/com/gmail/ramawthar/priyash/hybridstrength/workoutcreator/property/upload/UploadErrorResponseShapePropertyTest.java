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
 * Property-based integration test for error response shape conformance.
 *
 * <p>Feature: workout-creator-service-upload
 * <p>Property 6: Error responses conform to platform standard shape
 * <p>Validates: Requirements 9.5, 9.7
 *
 * <p>For any error response from the upload endpoints
 * ({@code POST /api/v1/uploads/programs} and {@code POST /api/v1/uploads/programs/validate}),
 * the response body must:
 * <ul>
 *   <li>Contain {@code status}, {@code error}, and {@code path} fields</li>
 *   <li>Contain either a {@code message} field (single-error shape) or an {@code errors} array
 *       (validation-failure shape)</li>
 *   <li>Contain a {@code timestamp} field</li>
 *   <li>NOT contain stack traces, internal class names, SQL, or internal identifiers</li>
 * </ul>
 *
 * <p>Uses {@link JqwikSpringSupport} to share a single Spring application context
 * across all property tries. The RSA key pair is generated once in a static initialiser;
 * the public key is registered as a {@code @Primary} bean via {@link TestJwtConfig}.
 */
@JqwikSpringSupport
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("integration")
class UploadErrorResponseShapePropertyTest {

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

    // =========================================================================
    // Property 6a: Upload endpoint — error responses conform to platform shape
    // =========================================================================

    /**
     * For any request to the upload endpoint that produces an error (4xx),
     * the response body must conform to the platform standard error shape and
     * must not leak internal implementation details.
     */
    @Property(tries = 100)
    void uploadEndpoint_anyErrorResponse_conformsToPlatformShape(
            @ForAll("errorTriggeringRequests") ErrorRequest errorRequest) {

        ResponseEntity<Map> response = restTemplate.postForEntity(
                uploadUrl(), errorRequest.toHttpEntity(), Map.class);

        assertThat(response.getStatusCode().is4xxClientError())
                .as("Expected a 4xx error but got %s for request type: %s",
                        response.getStatusCode(), errorRequest.description())
                .isTrue();

        assertConformsToStandardShape(response, uploadUrl());
    }

    // =========================================================================
    // Property 6b: Validate endpoint — error responses conform to platform shape
    // =========================================================================

    /**
     * For any request to the validate endpoint that produces an error (4xx),
     * the response body must conform to the platform standard error shape and
     * must not leak internal implementation details.
     */
    @Property(tries = 100)
    void validateEndpoint_anyErrorResponse_conformsToPlatformShape(
            @ForAll("errorTriggeringRequests") ErrorRequest errorRequest) {

        ResponseEntity<Map> response = restTemplate.postForEntity(
                validateUrl(), errorRequest.toHttpEntity(), Map.class);

        assertThat(response.getStatusCode().is4xxClientError())
                .as("Expected a 4xx error but got %s for request type: %s",
                        response.getStatusCode(), errorRequest.description())
                .isTrue();

        assertConformsToStandardShape(response, validateUrl());
    }

    // =========================================================================
    // Core shape assertion
    // =========================================================================

    /**
     * Asserts that a response body conforms to the platform standard error shape:
     * <ul>
     *   <li>{@code status} — integer matching the HTTP status code</li>
     *   <li>{@code error} — non-blank string (e.g. "Bad Request")</li>
     *   <li>{@code message} OR {@code errors} — at least one must be present</li>
     *   <li>{@code path} — non-blank string matching the request path</li>
     *   <li>{@code timestamp} — non-null</li>
     *   <li>No stack trace fields ({@code trace}, {@code stackTrace}, {@code exception})</li>
     *   <li>No internal identifiers in string fields</li>
     * </ul>
     */
    private void assertConformsToStandardShape(ResponseEntity<Map> response, String expectedPath) {
        Map<?, ?> body = response.getBody();
        assertThat(body)
                .as("Error response body must not be null")
                .isNotNull();

        // status field — must be an integer matching the HTTP status
        Object statusField = body.get("status");
        assertThat(statusField)
                .as("Error response must contain a 'status' field")
                .isNotNull();
        assertThat(statusField)
                .as("'status' must be an integer")
                .isInstanceOf(Integer.class);
        assertThat((Integer) statusField)
                .as("'status' must match the HTTP response status code")
                .isEqualTo(response.getStatusCode().value());

        // error field — non-blank string
        Object errorField = body.get("error");
        assertThat(errorField)
                .as("Error response must contain a non-null 'error' field")
                .isNotNull();
        assertThat(errorField.toString().trim())
                .as("'error' must be a non-blank string")
                .isNotEmpty();

        // message OR errors — at least one must be present
        boolean hasMessage = body.containsKey("message") && body.get("message") != null;
        boolean hasErrors = body.containsKey("errors") && body.get("errors") != null;
        assertThat(hasMessage || hasErrors)
                .as("Error response must contain either 'message' or 'errors', but body was: %s", body)
                .isTrue();

        // If errors array is present, each entry must have 'field' and 'message'
        if (hasErrors) {
            Object errorsField = body.get("errors");
            assertThat(errorsField)
                    .as("'errors' must be a List")
                    .isInstanceOf(List.class);
            List<?> errorsList = (List<?>) errorsField;
            for (Object entry : errorsList) {
                assertThat(entry)
                        .as("Each entry in 'errors' must be a Map")
                        .isInstanceOf(Map.class);
                Map<?, ?> errorEntry = (Map<?, ?>) entry;
                assertThat(errorEntry.get("field"))
                        .as("Each error entry must have a 'field' property")
                        .isNotNull();
                assertThat(errorEntry.get("message"))
                        .as("Each error entry must have a 'message' property")
                        .isNotNull();
            }
        }

        // path field — must be present and match the endpoint path
        Object pathField = body.get("path");
        assertThat(pathField)
                .as("Error response must contain a 'path' field")
                .isNotNull();
        assertThat(pathField.toString())
                .as("'path' must contain the request URI")
                .contains("/api/v1/uploads/programs");

        // timestamp field — must be present and non-null
        Object timestampField = body.get("timestamp");
        assertThat(timestampField)
                .as("Error response must contain a 'timestamp' field")
                .isNotNull();

        // No stack trace leakage — these keys must not appear in the response body
        assertThat(body).as("Response must not contain 'trace' (stack trace leak)").doesNotContainKey("trace");
        assertThat(body).as("Response must not contain 'stackTrace'").doesNotContainKey("stackTrace");
        assertThat(body).as("Response must not contain 'exception'").doesNotContainKey("exception");

        // No internal identifiers in string fields — check message and error fields
        assertNoInternalDetails(body.get("message"));
        assertNoInternalDetails(body.get("error"));
    }

    /**
     * Asserts that a string field does not contain internal implementation details:
     * Java package names, SQL keywords, or raw exception class names.
     *
     * <p>Intentional error labels like "Bad Request" and "Validation Failed" are allowed.
     * Raw exception class names like "NullPointerException" or "PSQLException" are not.
     */
    private void assertNoInternalDetails(Object fieldValue) {
        if (fieldValue == null) return;
        String value = fieldValue.toString();

        // No internal Java package names
        assertThat(value)
                .as("Error field must not contain internal package names: '%s'", value)
                .doesNotContain("com.gmail.ramawthar")
                .doesNotContain("java.lang")
                .doesNotContain("org.springframework");

        // No raw SQL exception identifiers
        assertThat(value)
                .as("Error field must not contain SQL exception identifiers: '%s'", value)
                .doesNotContainIgnoringCase("SQLException")
                .doesNotContainIgnoringCase("ORA-")
                .doesNotContainIgnoringCase("PSQLException");

        // No raw Java exception class names — these end with "Exception" or "Error"
        // but are not the intentional human-readable labels used in error responses.
        // We check for the pattern "<Word>Exception" (e.g. NullPointerException) rather
        // than the word "Exception" alone, since "Bad Request" and "Validation Failed" are fine.
        assertThat(value)
                .as("Error field must not contain raw exception class names: '%s'", value)
                .doesNotContainPattern("[A-Z][a-zA-Z]+Exception")
                .doesNotContainPattern("[A-Z][a-zA-Z]+Error");
    }

    // =========================================================================
    // Arbitraries — requests that should produce error responses
    // =========================================================================

    /**
     * Generates a variety of requests that must produce 4xx error responses,
     * covering all error scenarios defined in Requirements 9.5 and 9.7:
     * <ul>
     *   <li>Missing JWT → 401</li>
     *   <li>Body exceeds 1 MB → 400</li>
     *   <li>Wrong Content-Type → 400</li>
     *   <li>Empty body → 400</li>
     *   <li>Malformed JSON → 400</li>
     *   <li>Schema violations → 400 with errors array</li>
     * </ul>
     */
    @Provide
    Arbitrary<ErrorRequest> errorTriggeringRequests() {
        return Arbitraries.oneOf(
                missingJwtRequest(),
                oversizedBodyRequest(),
                wrongContentTypeRequest(),
                emptyBodyRequest(),
                malformedJsonRequest(),
                invalidDurationWeeksRequest(),
                mismatchedStructureLengthRequest(),
                emptyEquipmentProfileRequest(),
                wrongVersionRequest(),
                emptyBlocksRequest(),
                emptyMovementsRequest(),
                crossFitMissingModalityTypeRequest()
        );
    }

    /** No Authorization header → 401 */
    private Arbitrary<ErrorRequest> missingJwtRequest() {
        return Arbitraries.just(new ErrorRequest(
                "missing-jwt",
                validOneWeekJson(),
                headersWithoutJwt()
        ));
    }

    /** Body > 1 MB → 400 */
    private Arbitrary<ErrorRequest> oversizedBodyRequest() {
        return Arbitraries.just(new ErrorRequest(
                "oversized-body",
                "{\"data\":\"" + "x".repeat(1_100_000) + "\"}",
                authHeaders(UUID.randomUUID().toString())
        ));
    }

    /** Content-Type: text/plain → 400 */
    private Arbitrary<ErrorRequest> wrongContentTypeRequest() {
        return Arbitraries.just(new ErrorRequest(
                "wrong-content-type",
                validOneWeekJson(),
                headersWithContentType(MediaType.TEXT_PLAIN, UUID.randomUUID().toString())
        ));
    }

    /** Empty body → 400 */
    private Arbitrary<ErrorRequest> emptyBodyRequest() {
        return Arbitraries.just(new ErrorRequest(
                "empty-body",
                "",
                authHeaders(UUID.randomUUID().toString())
        ));
    }

    /** Syntactically invalid JSON → 400 */
    private Arbitrary<ErrorRequest> malformedJsonRequest() {
        return Arbitraries.of(
                "not json at all",
                "{\"program_metadata\": {",
                "null",
                "[]"
        ).map(body -> new ErrorRequest(
                "malformed-json",
                body,
                authHeaders(UUID.randomUUID().toString())
        ));
    }

    /** duration_weeks not in {1, 4} → 400 with errors array */
    private Arbitrary<ErrorRequest> invalidDurationWeeksRequest() {
        return Arbitraries.integers().between(2, 3).map(weeks -> new ErrorRequest(
                "invalid-duration-weeks",
                buildProgramJson(weeks, "Test", "Hypertrophy", List.of("Barbell"), "Hypertrophy", weeks),
                authHeaders(UUID.randomUUID().toString())
        ));
    }

    /** program_structure length != duration_weeks → 400 with errors array */
    private Arbitrary<ErrorRequest> mismatchedStructureLengthRequest() {
        return Arbitraries.of(1, 4).flatMap(dw ->
                Arbitraries.oneOf(
                        Arbitraries.integers().between(0, dw - 1),
                        Arbitraries.integers().between(dw + 1, dw + 3)
                ).map(sz -> new ErrorRequest(
                        "mismatched-structure-length",
                        buildProgramJson(dw, "Test", "Hypertrophy", List.of("Barbell"), "Hypertrophy", sz),
                        authHeaders(UUID.randomUUID().toString())
                ))
        );
    }

    /** equipment_profile is empty → 400 with errors array */
    private Arbitrary<ErrorRequest> emptyEquipmentProfileRequest() {
        return Arbitraries.just(new ErrorRequest(
                "empty-equipment-profile",
                buildProgramJsonWithEquipment(List.of()),
                authHeaders(UUID.randomUUID().toString())
        ));
    }

    /** version != "1.0" → 400 with errors array */
    private Arbitrary<ErrorRequest> wrongVersionRequest() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8)
                .filter(v -> !"1.0".equals(v))
                .map(v -> new ErrorRequest(
                        "wrong-version",
                        buildProgramJsonWithVersion(v),
                        authHeaders(UUID.randomUUID().toString())
                ));
    }

    /** A day has an empty blocks array → 400 with errors array */
    private Arbitrary<ErrorRequest> emptyBlocksRequest() {
        return Arbitraries.just(new ErrorRequest(
                "empty-blocks",
                buildProgramJsonWithEmptyBlocks(),
                authHeaders(UUID.randomUUID().toString())
        ));
    }

    /** A block has an empty movements array → 400 with errors array */
    private Arbitrary<ErrorRequest> emptyMovementsRequest() {
        return Arbitraries.just(new ErrorRequest(
                "empty-movements",
                buildProgramJsonWithEmptyMovements(),
                authHeaders(UUID.randomUUID().toString())
        ));
    }

    /** CrossFit day with movement missing modality_type → 400 with errors array */
    private Arbitrary<ErrorRequest> crossFitMissingModalityTypeRequest() {
        return Arbitraries.just(new ErrorRequest(
                "crossfit-missing-modality-type",
                buildCrossFitProgramWithoutModalityType(),
                authHeaders(UUID.randomUUID().toString())
        ));
    }

    // =========================================================================
    // JSON builders (mirrors UploadAtomicityPropertyTest)
    // =========================================================================

    private String validOneWeekJson() {
        return buildProgramJson(1, "Test Program", "Hypertrophy", List.of("Barbell"), "Hypertrophy", 1);
    }

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
    // Header helpers
    // =========================================================================

    private HttpHeaders authHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateJwt(userId));
        return headers;
    }

    private HttpHeaders headersWithoutJwt() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders headersWithContentType(MediaType contentType, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setBearerAuth(generateJwt(userId));
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

    private String uploadUrl() {
        return "http://localhost:" + port + "/api/v1/uploads/programs";
    }

    private String validateUrl() {
        return "http://localhost:" + port + "/api/v1/uploads/programs/validate";
    }

    // =========================================================================
    // Value object for parameterised error requests
    // =========================================================================

    /**
     * Encapsulates a request that is expected to produce an error response.
     * The {@code description} field is used in assertion messages for diagnostics.
     */
    record ErrorRequest(String description, String body, HttpHeaders headers) {
        HttpEntity<String> toHttpEntity() {
            return new HttpEntity<>(body, headers);
        }
    }
}
