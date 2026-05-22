package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception;

/**
 * Thrown when a user attempts to access a resource they do not own.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("Access denied");
    }
}
