package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception;

/**
 * Thrown when the Gemini AI service is unreachable, times out, or the
 * circuit breaker is in the open state.
 * <p>
 * Mapped to 502 Bad Gateway by the {@code GlobalExceptionHandler}.
 */
public class GeminiUnavailableException extends RuntimeException {

    public GeminiUnavailableException(String message) {
        super(message);
    }

    public GeminiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
