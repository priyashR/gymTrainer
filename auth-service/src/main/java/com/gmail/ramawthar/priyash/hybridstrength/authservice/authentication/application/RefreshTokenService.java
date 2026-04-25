package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.application;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.RefreshToken;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.TokenPair;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.inbound.RefreshTokenUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.RefreshTokenRepository;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.TokenProvider;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception.InvalidRefreshTokenException;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.config.JwtConfig;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.outbound.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Application service implementing token refresh with rotation.
 * Depends only on outbound ports (interfaces), not adapters.
 */
@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               TokenProvider tokenProvider,
                               UserRepository userRepository,
                               JwtConfig jwtConfig) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public TokenPair refresh(String refreshTokenValue) {
        log.debug("Processing token refresh request");

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> {
                    log.warn("Token refresh rejected — token not found");
                    return new InvalidRefreshTokenException("Invalid refresh token");
                });

        if (storedToken.isExpired()) {
            refreshTokenRepository.deleteByUserId(storedToken.getUserId());
            log.warn("Token refresh rejected — token expired for userId: {}", storedToken.getUserId());
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> {
                    log.warn("Token refresh rejected — user not found for userId: {}", storedToken.getUserId());
                    return new InvalidRefreshTokenException("Invalid refresh token");
                });

        // Generate new access token
        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        // Token rotation: delete old token(s) and issue a new refresh token
        refreshTokenRepository.deleteByUserId(user.getId());

        String rawRefreshToken = tokenProvider.generateRefreshToken();
        String refreshTokenHash = sha256(rawRefreshToken);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtConfig.getRefreshTokenExpiry());

        RefreshToken newRefreshToken = new RefreshToken(
                UUID.randomUUID(),
                refreshTokenHash,
                user.getId(),
                expiresAt,
                now
        );
        refreshTokenRepository.save(newRefreshToken);

        log.info("Token refreshed successfully for userId: {}", user.getId());
        return new TokenPair(newAccessToken, rawRefreshToken);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
