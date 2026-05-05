package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property.upload;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadFormatter;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Size;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for the parse–format–parse round-trip.
 *
 * Feature: workout-creator-service-upload
 * Property 5: Parse–format–parse round-trip
 * Validates: Requirements 5.3, 5.4
 *
 * Asserts: parse(format(parse(json))) ≡ parse(json)
 * i.e. the second parse produces a Program domain object equivalent to the first.
 */
class UploadRoundTripPropertyTest {

    private final UploadParser parser = new UploadParser();
    private final UploadFormatter formatter = new UploadFormatter();

    // ── Property 5: round-trip for 1-week programs ───────────────────────────

    /**
     * For any valid 1-week program JSON, parse → format → parse produces an equivalent Program.
     * Validates Requirement 5.3 and 5.4 (duration_weeks = 1 case).
     */
    @Property(tries = 100)
    void parseFormatParse_validOneWeekProgram_producesEquivalentDomainObject(
            @ForAll("validOneWeekProgramJson") String json) {

        ParseResult first = parser.parse(json);
        assertThat(first)
                .as("Generated JSON must be valid: %s", json)
                .isInstanceOf(ParseResult.Success.class);

        Program p1 = ((ParseResult.Success) first).program();
        String formatted = formatter.format(p1);

        ParseResult second = parser.parse(formatted);
        assertThat(second)
                .as("Re-formatted JSON must still be valid")
                .isInstanceOf(ParseResult.Success.class);

        Program p2 = ((ParseResult.Success) second).program();
        assertThat(p2)
                .as("parse(format(parse(json))) must equal parse(json)")
                .isEqualTo(p1);
    }

    // ── Property 5: round-trip for 4-week programs ───────────────────────────

    /**
     * For any valid 4-week program JSON, parse → format → parse produces an equivalent Program.
     * Validates Requirement 5.3 and 5.4 (duration_weeks = 4 case).
     */
    @Property(tries = 100)
    void parseFormatParse_validFourWeekProgram_producesEquivalentDomainObject(
            @ForAll("validFourWeekProgramJson") String json) {

        ParseResult first = parser.parse(json);
        assertThat(first)
                .as("Generated JSON must be valid: %s", json)
                .isInstanceOf(ParseResult.Success.class);

        Program p1 = ((ParseResult.Success) first).program();
        String formatted = formatter.format(p1);

        ParseResult second = parser.parse(formatted);
        assertThat(second)
                .as("Re-formatted JSON must still be valid")
                .isInstanceOf(ParseResult.Success.class);

        Program p2 = ((ParseResult.Success) second).program();
        assertThat(p2)
                .as("parse(format(parse(json))) must equal parse(json)")
                .isEqualTo(p1);
    }

    // ── Property 5: round-trip for mixed-modality programs ───────────────────

    /**
     * For any valid program with mixed CrossFit and Hypertrophy days,
     * parse → format → parse produces an equivalent Program.
     * Validates Requirement 5.4 (both modalities case).
     */
    @Property(tries = 100)
    void parseFormatParse_mixedModalityProgram_producesEquivalentDomainObject(
            @ForAll("validMixedModalityProgramJson") String json) {

        ParseResult first = parser.parse(json);
        assertThat(first)
                .as("Generated mixed-modality JSON must be valid: %s", json)
                .isInstanceOf(ParseResult.Success.class);

        Program p1 = ((ParseResult.Success) first).program();
        String formatted = formatter.format(p1);

        ParseResult second = parser.parse(formatted);
        assertThat(second)
                .as("Re-formatted mixed-modality JSON must still be valid")
                .isInstanceOf(ParseResult.Success.class);

        Program p2 = ((ParseResult.Success) second).program();
        assertThat(p2)
                .as("parse(format(parse(json))) must equal parse(json) for mixed-modality programs")
                .isEqualTo(p1);
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> validOneWeekProgramJson() {
        return validProgramJson(Arbitraries.just(1));
    }

    @Provide
    Arbitrary<String> validFourWeekProgramJson() {
        return validProgramJson(Arbitraries.just(4));
    }

    @Provide
    Arbitrary<String> validMixedModalityProgramJson() {
        // Always 1 week, but the day will have both CrossFit and Hypertrophy days
        return validProgramJsonWithMixedModalities();
    }

    // ── JSON builders ─────────────────────────────────────────────────────────

    private Arbitrary<String> validProgramJson(Arbitrary<Integer> durationWeeksArb) {
        return Combinators.combine(
                durationWeeksArb,
                nonBlankAlphanumeric(),   // program_name
                nonBlankAlphanumeric(),   // goal
                equipmentProfileArb(),
                modalityArb()             // single modality for all days
        ).as((durationWeeks, name, goal, equipment, modality) ->
                buildProgramJson(durationWeeks, name, goal, equipment, modality)
        );
    }

    private Arbitrary<String> validProgramJsonWithMixedModalities() {
        return Combinators.combine(
                nonBlankAlphanumeric(),
                nonBlankAlphanumeric(),
                equipmentProfileArb()
        ).as((name, goal, equipment) ->
                buildMixedModalityProgramJson(name, goal, equipment)
        );
    }

    /**
     * Builds a complete valid program JSON string with the given parameters.
     * All weeks use sequential week_numbers [1..durationWeeks].
     * Each week has one day with day_number=1.
     */
    private String buildProgramJson(int durationWeeks, String name, String goal,
                                    List<String> equipment, String modality) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"program_metadata\":{");
        sb.append("\"program_name\":").append(jsonString(name)).append(",");
        sb.append("\"duration_weeks\":").append(durationWeeks).append(",");
        sb.append("\"goal\":").append(jsonString(goal)).append(",");
        sb.append("\"equipment_profile\":").append(jsonStringArray(equipment)).append(",");
        sb.append("\"version\":\"1.0\"");
        sb.append("},");
        sb.append("\"program_structure\":[");

        for (int w = 1; w <= durationWeeks; w++) {
            if (w > 1) sb.append(",");
            sb.append("{");
            sb.append("\"week_number\":").append(w).append(",");
            sb.append("\"days\":[");
            sb.append(buildDayJson(1, modality));
            sb.append("]");
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * Builds a 1-week program with two days: one CrossFit and one Hypertrophy.
     */
    private String buildMixedModalityProgramJson(String name, String goal, List<String> equipment) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"program_metadata\":{");
        sb.append("\"program_name\":").append(jsonString(name)).append(",");
        sb.append("\"duration_weeks\":1,");
        sb.append("\"goal\":").append(jsonString(goal)).append(",");
        sb.append("\"equipment_profile\":").append(jsonStringArray(equipment)).append(",");
        sb.append("\"version\":\"1.0\"");
        sb.append("},");
        sb.append("\"program_structure\":[{");
        sb.append("\"week_number\":1,");
        sb.append("\"days\":[");
        sb.append(buildDayJson(1, "CrossFit"));
        sb.append(",");
        sb.append(buildDayJson(2, "Hypertrophy"));
        sb.append("]}]}");
        return sb.toString();
    }

    /**
     * Builds a single day JSON object with the given day_number and modality.
     * CrossFit days include modality_type on all movements; Hypertrophy days omit it.
     */
    private String buildDayJson(int dayNumber, String modality) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"day_number\":").append(dayNumber).append(",");
        sb.append("\"day_label\":").append(jsonString("Day " + dayNumber)).append(",");
        sb.append("\"focus_area\":").append(jsonString("Full Body")).append(",");
        sb.append("\"modality\":").append(jsonString(modality)).append(",");
        sb.append("\"warm_up\":[{\"movement\":\"Jog\",\"instruction\":\"5 minutes easy\"}],");
        sb.append("\"blocks\":[");
        sb.append(buildBlockJson(modality));
        sb.append("],");
        sb.append("\"cool_down\":[{\"movement\":\"Stretch\",\"instruction\":\"Hold 30 seconds\"}]");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Builds a single block JSON object with one movement.
     * CrossFit blocks include modality_type; Hypertrophy blocks omit it.
     */
    private String buildBlockJson(String modality) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"block_type\":\"Tier 1: Compound\",");
        sb.append("\"format\":\"Sets/Reps\",");
        sb.append("\"movements\":[");
        sb.append("{");
        sb.append("\"exercise_name\":\"Back Squat\",");
        if ("CrossFit".equals(modality)) {
            sb.append("\"modality_type\":\"Weightlifting\",");
        }
        sb.append("\"prescribed_sets\":3,");
        sb.append("\"prescribed_reps\":\"5\"");
        sb.append("}");
        sb.append("]}");
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
        return nonBlankAlphanumeric()
                .list()
                .ofMinSize(1)
                .ofMaxSize(4);
    }

    private Arbitrary<String> modalityArb() {
        return Arbitraries.of("CrossFit", "Hypertrophy");
    }

    // ── JSON serialisation helpers ────────────────────────────────────────────

    /** Wraps a string value in JSON double-quotes, escaping backslash and double-quote. */
    private String jsonString(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    /** Serialises a list of strings as a JSON array. */
    private String jsonStringArray(List<String> values) {
        return values.stream()
                .map(this::jsonString)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
