package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadValidationError;

import java.util.List;

/**
 * Response body for the validate endpoint (200 OK).
 * {@code errors} is an empty list when {@code valid} is {@code true}.
 */
public record ValidateUploadResponse(
        boolean valid,
        List<UploadValidationError> errors
) {
    public static ValidateUploadResponse ok() {
        return new ValidateUploadResponse(true, List.of());
    }

    public static ValidateUploadResponse invalid(List<UploadValidationError> errors) {
        return new ValidateUploadResponse(false, errors);
    }
}
