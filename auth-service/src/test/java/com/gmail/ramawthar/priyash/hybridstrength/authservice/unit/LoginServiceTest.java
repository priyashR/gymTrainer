package com.gmail.ramawthar.priyash.hybridstrength.authservice.unit;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.application.LoginService;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.RefreshToken;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.TokenPair;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.PasswordEncoder;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.RefreshTokenRepository;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.TokenProvider;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception.InvalidCredentialsException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TokenProvider tokenProvider;
    private RefreshTokenRepository refreshTokenRepository;
    private JwtConfig jwtConfig;
    private LoginService loginService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "securePass1";
    private static final String PASSWORD_HASH = "$2a$12$hashedValue";
    private static final String ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test";
    private static final String RAW_REFRESH_TOKEN = "random-refresh-token-value";

    private User existingUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenProvider = mock(TokenProvider.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        jwtConfig = mock(JwtConfig.class);

        when(jwtConfig.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(7));

        loginService = new LoginService(userRepository, passwordEncoder, tokenProvider, refreshTokenRepository, jwtConfig);

        Instant now = Instant.now();
        existingUser = new User(USER_ID, EMAIL, PASSWORD_HASH, "USER", now, now);
    }

    @Test
    @DisplayName("login — valid credentials — returns TokenPair with access and refresh tokens")
    void login_ValidCredentials_ReturnsTokenPair() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tokenProvider.generateAccessToken(USER_ID, EMAIL, "USER")).thenReturn(ACCESS_TOKEN);
        when(tokenProvider.generateRefreshToken()).thenReturn(RAW_REFRESH_TOKEN);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenPair result = loginService.login(EMAIL, RAW_PASSWORD);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.getRefreshToken()).isEqualTo(RAW_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("login — valid credentials — persists hashed refresh token via RefreshTokenRepository")
    void login_ValidCredentials_PersistsHashedRefreshToken() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tokenProvider.generateAccessToken(USER_ID, EMAIL, "USER")).thenReturn(ACCESS_TOKEN);
        when(tokenProvider.generateRefreshToken()).thenReturn(RAW_REFRESH_TOKEN);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        loginService.login(EMAIL, RAW_PASSWORD);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getTokenHash()).isNotNull();
        assertThat(saved.getTokenHash()).isNotEqualTo(RAW_REFRESH_TOKEN);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("login — valid credentials — delegates token generation to TokenProvider")
    void login_ValidCredentials_DelegatesTokenGeneration() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tokenProvider.generateAccessToken(USER_ID, EMAIL, "USER")).thenReturn(ACCESS_TOKEN);
        when(tokenProvider.generateRefreshToken()).thenReturn(RAW_REFRESH_TOKEN);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        loginService.login(EMAIL, RAW_PASSWORD);

        verify(tokenProvider).generateAccessToken(eq(USER_ID), eq(EMAIL), eq("USER"));
        verify(tokenProvider).generateRefreshToken();
    }

    @Test
    @DisplayName("login — wrong password — throws InvalidCredentialsException")
    void login_WrongPassword_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(EMAIL, "wrongPassword"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login — wrong password — does not generate tokens or persist refresh token")
    void login_WrongPassword_DoesNotGenerateTokensOrPersist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", PASSWORD_HASH)).thenReturn(false);

        try {
            loginService.login(EMAIL, "wrongPassword");
        } catch (InvalidCredentialsException ignored) {
        }

        verify(tokenProvider, never()).generateAccessToken(any(), any(), any());
        verify(tokenProvider, never()).generateRefreshToken();
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("login — unknown email — throws InvalidCredentialsException")
    void login_UnknownEmail_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login("unknown@example.com", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login — unknown email — does not check password or generate tokens")
    void login_UnknownEmail_DoesNotCheckPasswordOrGenerateTokens() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        try {
            loginService.login("unknown@example.com", RAW_PASSWORD);
        } catch (InvalidCredentialsException ignored) {
        }

        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenProvider, never()).generateAccessToken(any(), any(), any());
        verify(tokenProvider, never()).generateRefreshToken();
        verify(refreshTokenRepository, never()).save(any());
    }
}
