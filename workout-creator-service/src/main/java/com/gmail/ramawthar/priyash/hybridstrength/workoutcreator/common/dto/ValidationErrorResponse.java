package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * Error response shape for validation failures with per-field error details.
 * Matches the structure defined in api-standards.md.
 */
public record ValidationErrorResponse(
        int status,
        String error,
        List<FieldError> errors,
        String path,
        Instant timestamp
) {
    public record FieldError(String field, String message) {}
}
