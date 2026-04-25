package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.RefreshToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for refresh token persistence operations.
 */
public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByToken(String tokenValue);

    void deleteByUserId(UUID userId);
}
