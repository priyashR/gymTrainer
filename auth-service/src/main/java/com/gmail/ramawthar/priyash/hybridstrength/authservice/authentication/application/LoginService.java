package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.application;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.RefreshToken;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.TokenPair;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.inbound.LoginUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.PasswordEncoder;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.RefreshTokenRepository;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.TokenProvider;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception.InvalidCredentialsException;
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
 * Application service implementing user login.
 * Depends only on outbound ports (interfaces), not adapters.
 */
@Service
public class LoginService implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;

    public LoginService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        TokenProvider tokenProvider,
                        RefreshTokenRepository refreshTokenRepository,
                        JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public TokenPair login(String email, String rawPassword) {
        log.debug("Processing login request for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login rejected — unknown email: {}", email);
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.warn("Login rejected — wrong password for email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = tokenProvider.generateRefreshToken();

        String refreshTokenHash = sha256(rawRefreshToken);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtConfig.getRefreshTokenExpiry());

        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                refreshTokenHash,
                user.getId(),
                expiresAt,
                now
        );
        refreshTokenRepository.save(refreshToken);

        log.info("User logged in successfully with id: {}", user.getId());
        return new TokenPair(accessToken, rawRefreshToken);
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
