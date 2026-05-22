package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception;

import java.util.UUID;

/**
 * Thrown when a session cannot be found by its ID.
 */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("Session not found: " + sessionId);
    }
}
