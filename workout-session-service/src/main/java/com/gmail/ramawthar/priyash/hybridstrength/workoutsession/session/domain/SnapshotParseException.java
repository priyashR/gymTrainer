package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain;

/**
 * Thrown when the workout snapshot JSON cannot be parsed.
 * Signals a data integrity issue — the snapshot stored on the session is malformed.
 */
public class SnapshotParseException extends RuntimeException {

    public SnapshotParseException(String message) {
        super(message);
    }

    public SnapshotParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
