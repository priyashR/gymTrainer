package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.upload;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Upload_Schema validation.
 *
 * Feature: workout-creator-service-upload
 * Property 1: Schema validation accepts all valid programs and rejects all invalid ones
 * Validates: Requirements 1.2, 2.6
 *
 * Each property targets a single constraint from Requirement 1.2.
 */
class UploadSchemaValidationPropertyTest {

    private final UploadParser parser = new UploadParser();

    // ── Property 1a: valid programs always succeed ────────────────────────────

    /**
     * For any generated valid 1-week program, the parser must return Success.
     * Validates Requirement 1.2 (positive case, duration_weeks = 1).
     */
    @Property(tries = 100)
    void parse_validOneWeekProgram_alwaysReturnsSuccess(
            @ForAll("validOneWeekProgramJson") String json) {
        assertThat(parser.parse(json))
                .as("Valid 1-week program must parse successfully: %s", json)
                .isInstanceOf(ParseResult.Success.class);
    }

    /**
     * For any generated valid 4-week program, the parser must return Success.
     * Validates Requirement 1.2 (positive case, duration_weeks = 4).
     */
    @Property(tries = 100)
    void parse_validFourWeekProgram_alwaysReturnsSuccess(
            @ForAll("validFourWeekProgramJson") String json) {
        assertThat(parser.parse(json))
                .as("Valid 4-week program must parse successfully: %s", json)
                .isInstanceOf(ParseResult.Success.class);
    }

    // ── Property 1b: invalid duration_weeks always fails ─────────────────────

    /**
     * For any duration_weeks value of 2 or 3, the parser must return Failure.
     * Validates Requirement 1.2 constraint: duration_weeks must be 1 or 4.
     * Also validates Requirement 2.8.
     */
    @Property(tries = 100)
    void parse_durationWeeksTwoOrThree_alwaysReturnsFailure(
            @ForAll @IntRange(min = 2, max = 3) int invalidWeeks) {
        String json = buildProgramJson(invalidWeeks, "Test", "Hypertrophy",
                List.of("Barbell"), "Hypertrophy", invalidWeeks);
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("duration_weeks=%d must be rejected", invalidWeeks)
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.field())
                .contains("program_metadata.duration_weeks");
    }

    /**
     * For any duration_weeks value outside {1, 4} (e.g. 0, 5–100), the parser must return Failure.
     * Validates Requirement 1.2 constraint: duration_weeks must be 1 or 4.
     */
    @Property(tries = 100)
    void parse_durationWeeksOutsideValidSet_alwaysReturnsFailure(
            @ForAll("invalidDurationWeeks") int invalidWeeks) {
        String json = buildProgramJson(invalidWeeks, "Test", "Hypertrophy",
                List.of("Barbell"), "Hypertrophy", 1);
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("duration_weeks=%d must be rejected", invalidWeeks)
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.field())
                .contains("program_metadata.duration_weeks");
    }

    // ── Property 1c: wrong version always fails ───────────────────────────────

    /**
     * For any version string other than "1.0", the parser must return Failure.
     * Validates Requirement 1.2 constraint: version must be "1.0".
     */
    @Property(tries = 100)
    void parse_versionNotOnePointZero_alwaysReturnsFailure(
            @ForAll("invalidVersionString") String badVersion) {
        String json = buildProgramJsonWithVersion(badVersion);
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("version='%s' must be rejected", badVersion)
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.field())
                .contains("program_metadata.version");
    }

    // ── Property 1d: empty equipment_profile always fails ────────────────────

    /**
     * A program with an empty equipment_profile array must always be rejected.
     * Validates Requirement 1.2 constraint: equipment_profile must have at least one entry.
     */
    @Property(tries = 100)
    void parse_emptyEquipmentProfile_alwaysReturnsFailure(
            @ForAll("validProgramNameAndGoal") String[] nameAndGoal) {
        String json = buildProgramJsonWithEquipment(nameAndGoal[0], nameAndGoal[1], List.of());
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("Empty equipment_profile must be rejected")
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.field())
                .contains("program_metadata.equipment_profile");
    }

    // ── Property 1e: program_structure length mismatch always fails ───────────

    /**
     * When program_structure contains fewer weeks than duration_weeks, the parser must fail.
     * Validates Requirement 1.2 constraint: program_structure length must equal duration_weeks.
     */
    @Property(tries = 100)
    void parse_programStructureTooShort_alwaysReturnsFailure(
            @ForAll("durationAndShortStructure") int[] params) {
        int durationWeeks = params[0];
        int structureSize = params[1]; // always < durationWeeks
        String json = buildProgramJson(durationWeeks, "Test", "Hypertrophy",
                List.of("Barbell"), "Hypertrophy", structureSize);
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("program_structure size=%d with duration_weeks=%d must be rejected",
                        structureSize, durationWeeks)
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.field())
                .contains("program_structure");
    }

    /**
     * When program_structure contains more weeks than duration_weeks, the parser must fail.
     * Validates Requirement 1.2 constraint: program_structure length must equal duration_weeks.
     */
    @Property(tries = 100)
    void parse_programStructureTooLong_alwaysReturnsFailure(
            @ForAll("durationAndLongStructure") int[] params) {
        int durationWeeks = params[0];
        int structureSize = params[1]; // always > durationWeeks
        String json = buildProgramJson(durationWeeks, "Test", "Hypertrophy",
                List.of("Barbell"), "Hypertrophy", structureSize);
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("program_structure size=%d with duration_weeks=%d must be rejected",
                        structureSize, durationWeeks)
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.field())
                .contains("program_structure");
    }

    // ── Property 1f: out-of-range day_number always fails ────────────────────

    /**
     * A day_number of 0 or 8+ must always be rejected.
     * Validates Requirement 1.2 constraint: day_number must be within [1, 7].
     */
    @Property(tries = 100)
    void parse_dayNumberOutOfRange_alwaysReturnsFailure(
            @ForAll("outOfRangeDayNumber") int badDayNumber) {
        String json = buildProgramJsonWithDayNumber(badDayNumber);
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("day_number=%d must be rejected", badDayNumber)
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.message())
                .anyMatch(m -> m.contains("day_number must be within [1, 7]"));
    }

    // ── Property 1g: empty blocks array always fails ──────────────────────────

    /**
     * A day with an empty blocks array must always be rejected.
     * Validates Requirement 1.2 constraint: blocks must be non-empty.
     */
    @Property(tries = 100)
    void parse_emptyBlocksArray_alwaysReturnsFailure(
            @ForAll("validProgramNameAndGoal") String[] nameAndGoal) {
        String json = buildProgramJsonWithEmptyBlocks(nameAndGoal[0], nameAndGoal[1]);
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("Empty blocks array must be rejected")
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.message())
                .anyMatch(m -> m.contains("blocks must be a non-empty array"));
    }

    // ── Property 1h: empty movements array always fails ──────────────────────

    /**
     * A block with an empty movements array must always be rejected.
     * Validates Requirement 1.2 constraint: movements must be non-empty.
     */
    @Property(tries = 100)
    void parse_emptyMovementsArray_alwaysReturnsFailure(
            @ForAll("validProgramNameAndGoal") String[] nameAndGoal) {
        String json = buildProgramJsonWithEmptyMovements(nameAndGoal[0], nameAndGoal[1]);
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("Empty movements array must be rejected")
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.message())
                .anyMatch(m -> m.contains("movements must be a non-empty array"));
    }

    // ── Property 1i: missing modality_type on CrossFit day always fails ───────

    /**
     * A CrossFit day with any movement missing modality_type must always be rejected.
     * Validates Requirement 1.2 constraint: modality_type required when day modality is CrossFit.
     */
    @Property(tries = 100)
    void parse_crossFitDayWithoutModalityType_alwaysReturnsFailure(
            @ForAll("validProgramNameAndGoal") String[] nameAndGoal) {
        String json = buildCrossFitProgramWithoutModalityType(nameAndGoal[0], nameAndGoal[1]);
        ParseResult result = parser.parse(json);
        assertThat(result)
                .as("CrossFit day without modality_type must be rejected")
                .isInstanceOf(ParseResult.Failure.class);
        assertThat(((ParseResult.Failure) result).errors())
                .extracting(e -> e.message())
                .anyMatch(m -> m.contains("modality_type is required when day modality is CrossFit"));
    }

    // ── Arbitraries ───────────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> validOneWeekProgramJson() {
        return validProgramJsonArb(Arbitraries.just(1));
    }

    @Provide
    Arbitrary<String> validFourWeekProgramJson() {
        return validProgramJsonArb(Arbitraries.just(4));
    }

    @Provide
    Arbitrary<Integer> invalidDurationWeeks() {
        // Values outside {1, 4}: negatives, 0, 5–100
        return Arbitraries.oneOf(
                Arbitraries.integers().between(Integer.MIN_VALUE, 0),
                Arbitraries.integers().between(5, 100)
        );
    }

    @Provide
    Arbitrary<String> invalidVersionString() {
        // Any non-blank string that is not "1.0"
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10)
                .filter(s -> !s.isBlank() && !"1.0".equals(s));
    }

    @Provide
    Arbitrary<String[]> validProgramNameAndGoal() {
        Arbitrary<String> name = nonBlankAlphanumeric();
        Arbitrary<String> goal = nonBlankAlphanumeric();
        return Combinators.combine(name, goal).as((n, g) -> new String[]{n, g});
    }

    @Provide
    Arbitrary<int[]> durationAndShortStructure() {
        // duration_weeks in {1, 4}, structureSize in [0, durationWeeks - 1]
        return Arbitraries.of(1, 4).flatMap(dw ->
                Arbitraries.integers().between(0, dw - 1)
                        .map(sz -> new int[]{dw, sz})
        );
    }

    @Provide
    Arbitrary<int[]> durationAndLongStructure() {
        // duration_weeks in {1, 4}, structureSize in [durationWeeks + 1, durationWeeks + 5]
        return Arbitraries.of(1, 4).flatMap(dw ->
                Arbitraries.integers().between(dw + 1, dw + 5)
                        .map(sz -> new int[]{dw, sz})
        );
    }

    @Provide
    Arbitrary<Integer> outOfRangeDayNumber() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(Integer.MIN_VALUE, 0),
                Arbitraries.integers().between(8, 100)
        );
    }

    // ── JSON builders ─────────────────────────────────────────────────────────

    private Arbitrary<String> validProgramJsonArb(Arbitrary<Integer> durationWeeksArb) {
        return Combinators.combine(
                durationWeeksArb,
                nonBlankAlphanumeric(),
                nonBlankAlphanumeric(),
                equipmentProfileArb(),
                Arbitraries.of("CrossFit", "Hypertrophy")
        ).as((dw, name, goal, equipment, modality) ->
                buildProgramJson(dw, name, goal, equipment, modality, dw)
        );
    }

    /**
     * Builds a program JSON with {@code durationWeeks} declared in metadata
     * but only {@code structureSize} weeks in program_structure.
     * Week numbers are sequential starting from 1.
     */
    private String buildProgramJson(int durationWeeks, String name, String goal,
                                    List<String> equipment, String modality, int structureSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"program_metadata\":{");
        sb.append("\"program_name\":").append(jsonString(name)).append(",");
        sb.append("\"duration_weeks\":").append(durationWeeks).append(",");
        sb.append("\"goal\":").append(jsonString(goal)).append(",");
        sb.append("\"equipment_profile\":").append(jsonStringArray(equipment)).append(",");
        sb.append("\"version\":\"1.0\"");
        sb.append("},\"program_structure\":[");
        for (int w = 1; w <= structureSize; w++) {
            if (w > 1) sb.append(",");
            sb.append("{\"week_number\":").append(w).append(",\"days\":[");
            sb.append(buildDayJson(1, modality));
            sb.append("]}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String buildProgramJsonWithVersion(String version) {
        return "{\"program_metadata\":{" +
                "\"program_name\":\"Test\"," +
                "\"duration_weeks\":1," +
                "\"goal\":\"Hypertrophy\"," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":" + jsonString(version) +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[" +
                buildDayJson(1, "Hypertrophy") +
                "]}]}";
    }

    private String buildProgramJsonWithEquipment(String name, String goal, List<String> equipment) {
        return "{\"program_metadata\":{" +
                "\"program_name\":" + jsonString(name) + "," +
                "\"duration_weeks\":1," +
                "\"goal\":" + jsonString(goal) + "," +
                "\"equipment_profile\":" + jsonStringArray(equipment) + "," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[" +
                buildDayJson(1, "Hypertrophy") +
                "]}]}";
    }

    private String buildProgramJsonWithDayNumber(int dayNumber) {
        return "{\"program_metadata\":{" +
                "\"program_name\":\"Test\"," +
                "\"duration_weeks\":1," +
                "\"goal\":\"Hypertrophy\"," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[" +
                buildDayJson(dayNumber, "Hypertrophy") +
                "]}]}";
    }

    private String buildProgramJsonWithEmptyBlocks(String name, String goal) {
        return "{\"program_metadata\":{" +
                "\"program_name\":" + jsonString(name) + "," +
                "\"duration_weeks\":1," +
                "\"goal\":" + jsonString(goal) + "," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[{" +
                "\"day_number\":1," +
                "\"day_label\":\"Monday\"," +
                "\"focus_area\":\"Push\"," +
                "\"modality\":\"Hypertrophy\"," +
                "\"blocks\":[]" +
                "}]}]}";
    }

    private String buildProgramJsonWithEmptyMovements(String name, String goal) {
        return "{\"program_metadata\":{" +
                "\"program_name\":" + jsonString(name) + "," +
                "\"duration_weeks\":1," +
                "\"goal\":" + jsonString(goal) + "," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[{" +
                "\"day_number\":1," +
                "\"day_label\":\"Monday\"," +
                "\"focus_area\":\"Push\"," +
                "\"modality\":\"Hypertrophy\"," +
                "\"blocks\":[{\"block_type\":\"Tier 1\",\"format\":\"Sets/Reps\",\"movements\":[]}]" +
                "}]}]}";
    }

    private String buildCrossFitProgramWithoutModalityType(String name, String goal) {
        return "{\"program_metadata\":{" +
                "\"program_name\":" + jsonString(name) + "," +
                "\"duration_weeks\":1," +
                "\"goal\":" + jsonString(goal) + "," +
                "\"equipment_profile\":[\"Barbell\"]," +
                "\"version\":\"1.0\"" +
                "},\"program_structure\":[{\"week_number\":1,\"days\":[{" +
                "\"day_number\":1," +
                "\"day_label\":\"Monday\"," +
                "\"focus_area\":\"Engine\"," +
                "\"modality\":\"CrossFit\"," +
                "\"blocks\":[{\"block_type\":\"Metcon\",\"format\":\"AMRAP\",\"movements\":[{" +
                "\"exercise_name\":\"Burpees\"," +
                "\"prescribed_sets\":1," +
                "\"prescribed_reps\":\"AMRAP\"" +
                // modality_type intentionally omitted
                "}]}]" +
                "}]}]}";
    }

    private String buildDayJson(int dayNumber, String modality) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"day_number\":").append(dayNumber).append(",");
        sb.append("\"day_label\":").append(jsonString("Day " + dayNumber)).append(",");
        sb.append("\"focus_area\":\"Full Body\",");
        sb.append("\"modality\":").append(jsonString(modality)).append(",");
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

    // ── Arbitrary helpers ─────────────────────────────────────────────────────

    private Arbitrary<String> nonBlankAlphanumeric() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.isBlank());
    }

    private Arbitrary<List<String>> equipmentProfileArb() {
        return nonBlankAlphanumeric().list().ofMinSize(1).ofMaxSize(4);
    }

    // ── JSON serialisation helpers ────────────────────────────────────────────

    private String jsonString(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    private String jsonStringArray(List<String> values) {
        return values.stream()
                .map(this::jsonString)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
