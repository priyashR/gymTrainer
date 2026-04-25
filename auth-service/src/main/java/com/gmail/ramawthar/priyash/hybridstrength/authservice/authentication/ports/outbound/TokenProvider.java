package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound;

import java.util.UUID;

/**
 * Outbound port for JWT access token and refresh token operations.
 */
public interface TokenProvider {

    String generateAccessToken(UUID userId, String email, String role);

    String generateRefreshToken();

    UUID extractUserId(String accessToken);

    boolean validateAccessToken(String accessToken);
}
