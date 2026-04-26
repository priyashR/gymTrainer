package com.gmail.ramawthar.priyash.hybridstrength.authservice.unit;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.application.RefreshTokenService;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.RefreshToken;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.TokenPair;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.RefreshTokenRepository;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.TokenProvider;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception.InvalidRefreshTokenException;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.config.JwtConfig;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private TokenProvider tokenProvider;
    private UserRepository userRepository;
    private JwtConfig jwtConfig;
    private RefreshTokenService refreshTokenService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@example.com";
    private static final String ROLE = "USER";
    private static final String RAW_REFRESH_TOKEN = "existing-raw-refresh-token";
    private static final String NEW_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.new-access";
    private static final String NEW_RAW_REFRESH_TOKEN = "new-raw-refresh-token";
    private static final String STORED_TOKEN_HASH = "stored-hash-value";

    private User existingUser;
    private RefreshToken validStoredToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        tokenProvider = mock(TokenProvider.class);
        userRepository = mock(UserRepository.class);
        jwtConfig = mock(JwtConfig.class);

        when(jwtConfig.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(7));

        refreshTokenService = new RefreshTokenService(refreshTokenRepository, tokenProvider, userRepository, jwtConfig);

        Instant now = Instant.now();
        existingUser = new User(USER_ID, EMAIL, "$2a$12$hashedValue", ROLE, now, now);

        // Valid (non-expired) stored token — expires 1 hour from now
        validStoredToken = new RefreshToken(
                UUID.randomUUID(),
                STORED_TOKEN_HASH,
                USER_ID,
                Instant.now().plus(Duration.ofHours(1)),
                now
        );
    }

    // --- Scenario 1: Successful refresh returns TokenPair ---

    @Test
    @DisplayName("refresh — valid token, non-expired, user exists — returns TokenPair with new access and refresh tokens")
    void Refresh_ValidTokenAndUserExists_ReturnsTokenPair() {
        when(refreshTokenRepository.findByToken(RAW_REFRESH_TOKEN)).thenReturn(Optional.of(validStoredToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE)).thenReturn(NEW_ACCESS_TOKEN);
        when(tokenProvider.generateRefreshToken()).thenReturn(NEW_RAW_REFRESH_TOKEN);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenPair result = refreshTokenService.refresh(RAW_REFRESH_TOKEN);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(result.getRefreshToken()).isEqualTo(NEW_RAW_REFRESH_TOKEN);
    }

    // --- Scenario 2: Successful refresh verifies token rotation ---

    @Test
    @DisplayName("refresh — valid token — deletes old tokens and saves new token with hash ≠ raw value")
    void Refresh_ValidToken_RotatesTokenAndSavesHash() {
        when(refreshTokenRepository.findByToken(RAW_REFRESH_TOKEN)).thenReturn(Optional.of(validStoredToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE)).thenReturn(NEW_ACCESS_TOKEN);
        when(tokenProvider.generateRefreshToken()).thenReturn(NEW_RAW_REFRESH_TOKEN);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.refresh(RAW_REFRESH_TOKEN);

        verify(refreshTokenRepository).deleteByUserId(USER_ID);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getTokenHash()).isNotNull();
        assertThat(saved.getTokenHash()).isNotEqualTo(NEW_RAW_REFRESH_TOKEN);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    // --- Scenario 3: Successful refresh delegates access token generation with correct claims ---

    @Test
    @DisplayName("refresh — valid token — delegates access token generation to TokenProvider with correct user claims")
    void Refresh_ValidToken_DelegatesAccessTokenGenerationWithCorrectClaims() {
        when(refreshTokenRepository.findByToken(RAW_REFRESH_TOKEN)).thenReturn(Optional.of(validStoredToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLE)).thenReturn(NEW_ACCESS_TOKEN);
        when(tokenProvider.generateRefreshToken()).thenReturn(NEW_RAW_REFRESH_TOKEN);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.refresh(RAW_REFRESH_TOKEN);

        verify(tokenProvider).generateAccessToken(eq(USER_ID), eq(EMAIL), eq(ROLE));
        verify(tokenProvider).generateRefreshToken();
    }

    // --- Scenario 4: Invalid token (not found) throws exception ---

    @Test
    @DisplayName("refresh — token not found — throws InvalidRefreshTokenException")
    void Refresh_TokenNotFound_ThrowsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refresh("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    // --- Scenario 5: Invalid token (not found) does not generate tokens or delete anything ---

    @Test
    @DisplayName("refresh — token not found — does not generate tokens or delete anything")
    void Refresh_TokenNotFound_DoesNotGenerateTokensOrDelete() {
        when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        try {
            refreshTokenService.refresh("unknown-token");
        } catch (InvalidRefreshTokenException ignored) {
        }

        verify(tokenProvider, never()).generateAccessToken(any(), any(), any());
        verify(tokenProvider, never()).generateRefreshToken();
        verify(refreshTokenRepository, never()).deleteByUserId(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    // --- Scenario 6: Expired token throws exception ---

    @Test
    @DisplayName("refresh — expired token — throws InvalidRefreshTokenException")
    void Refresh_ExpiredToken_ThrowsInvalidRefreshTokenException() {
        RefreshToken expiredToken = new RefreshToken(
                UUID.randomUUID(),
                STORED_TOKEN_HASH,
                USER_ID,
                Instant.now().minus(Duration.ofHours(1)), // expired 1 hour ago
                Instant.now().minus(Duration.ofDays(8))
        );
        when(refreshTokenRepository.findByToken(RAW_REFRESH_TOKEN)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.refresh(RAW_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");
    }

    // --- Scenario 7: Expired token deletes all user tokens (cleanup) ---

    @Test
    @DisplayName("refresh — expired token — deletes all user tokens for cleanup")
    void Refresh_ExpiredToken_DeletesAllUserTokens() {
        RefreshToken expiredToken = new RefreshToken(
                UUID.randomUUID(),
                STORED_TOKEN_HASH,
                USER_ID,
                Instant.now().minus(Duration.ofHours(1)),
                Instant.now().minus(Duration.ofDays(8))
        );
        when(refreshTokenRepository.findByToken(RAW_REFRESH_TOKEN)).thenReturn(Optional.of(expiredToken));

        try {
            refreshTokenService.refresh(RAW_REFRESH_TOKEN);
        } catch (InvalidRefreshTokenException ignored) {
        }

        verify(refreshTokenRepository).deleteByUserId(USER_ID);
        verify(tokenProvider, never()).generateAccessToken(any(), any(), any());
    }

    // --- Scenario 8: User not found throws exception ---

    @Test
    @DisplayName("refresh — token exists but user was deleted — throws InvalidRefreshTokenException")
    void Refresh_UserNotFound_ThrowsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken(RAW_REFRESH_TOKEN)).thenReturn(Optional.of(validStoredToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refresh(RAW_REFRESH_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
