package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadValidationError;

import java.util.List;

/**
 * Thrown by the upload application service when the Upload_Parser returns a
 * {@code ParseResult.Failure}. Carries the structured list of field-level errors
 * so the {@code GlobalExceptionHandler} can map them to the platform error shape.
 */
public class UploadValidationException extends RuntimeException {

    private final List<UploadValidationError> errors;

    public UploadValidationException(List<UploadValidationError> errors) {
        super("Upload validation failed with " + errors.size() + " error(s)");
        this.errors = List.copyOf(errors);
    }

    public List<UploadValidationError> getErrors() {
        return errors;
    }
}
