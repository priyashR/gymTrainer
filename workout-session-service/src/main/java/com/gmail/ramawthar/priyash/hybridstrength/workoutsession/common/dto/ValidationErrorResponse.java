package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * Error response shape for validation failures with field-level detail.
 * Matches the structure defined in api-standards.md.
 */
public record ValidationErrorResponse(
        int status,
        String error,
        List<FieldError> errors,
        String path,
        Instant timestamp
) {

    /**
     * A single field-level validation error.
     */
    public record FieldError(
            String field,
            String message
    ) {
    }
}
