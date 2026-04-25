package com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception;

/**
 * Thrown when a refresh token is invalid, expired, or not found.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
