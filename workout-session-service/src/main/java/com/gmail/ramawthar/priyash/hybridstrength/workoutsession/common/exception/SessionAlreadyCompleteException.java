package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception;

import java.util.UUID;

/**
 * Thrown when an operation is attempted on a session that is already completed.
 */
public class SessionAlreadyCompleteException extends RuntimeException {

    public SessionAlreadyCompleteException(UUID sessionId) {
        super("Session is already completed: " + sessionId);
    }
}
