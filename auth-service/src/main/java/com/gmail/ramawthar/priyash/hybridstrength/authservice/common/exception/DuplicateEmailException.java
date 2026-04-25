package com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception;

/**
 * Thrown when a registration attempt uses an email that is already registered.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
