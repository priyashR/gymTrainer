package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

/**
 * Thrown when the Workout Creator Service is unreachable or returns an error.
 * Maps to HTTP 502 Bad Gateway in the global exception handler.
 */
public class WorkoutFetchException extends RuntimeException {

    public WorkoutFetchException(String message) {
        super(message);
    }

    public WorkoutFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
