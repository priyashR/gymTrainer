package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto;

/**
 * API response DTO for the generation endpoint.
 * <p>
 * Always contains the raw Gemini response. On success, contains either a
 * {@code workout} (DAY scope) or {@code program} (WEEK/FOUR_WEEK scope)
 * with a null {@code parsingError}. On parse failure, domain objects are
 * null and {@code parsingError} contains a human-readable message.
 */
public record GenerateResponse(
        String rawGeminiResponse,
        WorkoutDto workout,
        ProgramDto program,
        String parsingError
) {}
