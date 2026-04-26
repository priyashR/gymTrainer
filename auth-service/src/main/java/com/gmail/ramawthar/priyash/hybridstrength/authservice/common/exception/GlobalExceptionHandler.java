package com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.dto.ErrorResponse;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.dto.ValidationErrorResponse;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.dto.ValidationErrorResponse.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Centralised exception handler for the Auth Service.
 * Maps domain exceptions to standard error response shapes defined in api-standards.md.
 * Never exposes stack traces, internal class names, or SQL in responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
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
