package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception;

import java.util.UUID;

/**
 * Thrown when a program enrollment cannot be found by its ID.
 */
public class EnrollmentNotFoundException extends RuntimeException {

    public EnrollmentNotFoundException(UUID enrollmentId) {
        super("Enrollment not found: " + enrollmentId);
    }
}
