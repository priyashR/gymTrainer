package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto;

import java.time.Instant;

/**
 * Standard error response shape for all non-validation error responses.
 * Matches the structure defined in api-standards.md.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp
) {}
