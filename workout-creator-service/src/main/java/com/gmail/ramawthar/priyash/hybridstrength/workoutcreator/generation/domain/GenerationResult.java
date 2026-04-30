package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.Objects;

/**
 * Value object representing the output of the workout/program generation use case.
 * <p>
 * Always contains the raw Gemini response text. On successful parsing, contains
 * either a {@link Workout} (for DAY scope) or a {@link Program} (for WEEK/FOUR_WEEK scope)
 * with a null parsing error. On parse failure, contains null domain objects and a
 * human-readable parsing error message.
 * <p>
 * Pure Java — no framework imports.
 */
public class GenerationResult {

    private final String rawGeminiResponse;
    private final Workout workout;
    private final Program program;
    private final String parsingError;

    private GenerationResult(String rawGeminiResponse, Workout workout, Program program, String parsingError) {
        if (rawGeminiResponse == null || rawGeminiResponse.isBlank()) {
            throw new IllegalArgumentException("GenerationResult rawGeminiResponse must not be null or blank");
        }
        this.rawGeminiResponse = rawGeminiResponse;
        this.workout = workout;
        this.program = program;
        this.parsingError = parsingError;
    }

    /**
     * Creates a successful result containing a parsed Workout (DAY scope).
     */
    public static GenerationResult successWithWorkout(String rawGeminiResponse, Workout workout) {
        Objects.requireNonNull(workout, "Workout must not be null for a successful workout result");
        return new GenerationResult(rawGeminiResponse, workout, null, null);
    }

    /**
     * Creates a successful result containing a parsed Program (WEEK/FOUR_WEEK scope).
     */
    public static GenerationResult successWithProgram(String rawGeminiResponse, Program program) {
        Objects.requireNonNull(program, "Program must not be null for a successful program result");
        return new GenerationResult(rawGeminiResponse, null, program, null);
    }

    /**
     * Creates a failure result containing the raw response and a parsing error message.
     */
    public static GenerationResult failure(String rawGeminiResponse, String parsingError) {
        if (parsingError == null || parsingError.isBlank()) {
            throw new IllegalArgumentException("parsingError must not be null or blank for a failure result");
        }
        return new GenerationResult(rawGeminiResponse, null, null, parsingError);
    }

    public String getRawGeminiResponse() {
        return rawGeminiResponse;
    }

    public Workout getWorkout() {
        return workout;
    }

    public Program getProgram() {
        return program;
    }

    public String getParsingError() {
        return parsingError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenerationResult that = (GenerationResult) o;
        return Objects.equals(rawGeminiResponse, that.rawGeminiResponse)
                && Objects.equals(workout, that.workout)
                && Objects.equals(program, that.program)
                && Objects.equals(parsingError, that.parsingError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawGeminiResponse, workout, program, parsingError);
    }

    @Override
    public String toString() {
        return "GenerationResult{rawGeminiResponse='" + rawGeminiResponse + "'"
                + ", workout=" + workout
                + ", program=" + program
                + ", parsingError='" + parsingError + "'}";
    }
}
