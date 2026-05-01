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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Centralised exception handler for the Workout Creator Service.
 * <p>
 * Maps domain and framework exceptions to standard error response shapes
 * defined in api-standards.md. Never exposes stack traces, internal class
 * names, or SQL in responses.
 * <p>
 * Authentication errors (401) are handled by the {@code SecurityConfig}
 * authentication entry point, not here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Jakarta Bean Validation failures — 400 Bad Request with per-field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        // Include class-level constraint violations (e.g. @ValidDayScopeTrainingStyles)
        List<FieldError> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(ge -> new FieldError(ge.getObjectName(), ge.getDefaultMessage()))
                .toList();

        List<FieldError> allErrors = new java.util.ArrayList<>(fieldErrors);
        allErrors.addAll(globalErrors);

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ValidationErrorResponse body = new ValidationErrorResponse(
                status.value(),
                "Validation Failed",
                allErrors,
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handles malformed JSON or unreadable request bodies — 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Malformed request body",
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handles Gemini unavailability — 502 Bad Gateway.
     */
    @ExceptionHandler(GeminiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleGeminiUnavailable(
            GeminiUnavailableException ex, HttpServletRequest request) {

        log.error("Gemini unavailable on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        HttpStatus status = HttpStatus.BAD_GATEWAY;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "AI generation service is currently unavailable. Please try again later.",
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Catch-all for unhandled exceptions — 500 Internal Server Error.
     * Logs the full stack trace but returns a generic message to the caller.
     */
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
