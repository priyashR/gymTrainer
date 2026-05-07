package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto.ErrorResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto.ValidationErrorResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto.ValidationErrorResponse.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Centralised exception handler for the Workout Creator Service.
 * Maps domain exceptions to the platform standard error shapes defined in api-standards.md.
 * Never exposes stack traces, internal class names, or SQL in responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles upload schema validation failures — returns 400 with an {@code errors} array.
     * Each entry carries the failing field path (dot-notation) and a descriptive message.
     */
    @ExceptionHandler(UploadValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleUploadValidation(
            UploadValidationException ex, HttpServletRequest request) {

        List<FieldError> fieldErrors = ex.getErrors().stream()
                .map(e -> new FieldError(e.field(), e.message()))
                .toList();

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ValidationErrorResponse body = new ValidationErrorResponse(
                status.value(),
                "Validation Failed",
                fieldErrors,
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handles empty or unreadable request body — returns 400 per Requirements 2.5 and 6.3.
     * Spring throws this when {@code @RequestBody} cannot read the body (empty body or
     * syntactically invalid JSON that Jackson cannot parse at all).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String message = ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")
                ? "Request body must not be empty"
                : "Uploaded file is not valid JSON";

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handles wrong Content-Type — returns 400 per Requirement 6.2.
     * Spring would normally return 415 Unsupported Media Type, but the spec requires 400.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Content-Type must be application/json",
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handles program access denied — returns 403 Forbidden.
     * Covers both "not found" and "not owned" cases to avoid leaking resource existence.
     */
    @ExceptionHandler(ProgramAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleProgramAccessDenied(
            ProgramAccessDeniedException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                "Forbidden",
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handles service-layer validation failures (e.g. empty search query) — returns 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "An unexpected error occurred",
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }
}
