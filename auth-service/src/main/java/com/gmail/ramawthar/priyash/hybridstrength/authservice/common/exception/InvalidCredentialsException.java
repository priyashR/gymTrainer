package com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception;

/**
 * Thrown when a login attempt fails due to wrong email or password.
 * The message is intentionally generic to avoid revealing which field is incorrect.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
