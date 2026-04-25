package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.TokenPair;

/**
 * Inbound port for refreshing an access token using a refresh token.
 * Returns a {@link TokenPair} containing the new access token and the rotated refresh token.
 */
public interface RefreshTokenUseCase {

    TokenPair refresh(String refreshTokenValue);
}
