package com.gmail.ramawthar.priyash.hybridstrength.authservice.unit;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.PasswordEncoder;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception.DuplicateEmailException;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.application.RegisterUserService;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RegisterUserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RegisterUserService registerUserService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        registerUserService = new RegisterUserService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("register — valid credentials — returns persisted user with correct email and encoded password")
    void register_ValidCredentials_ReturnsSavedUserWithEncodedPassword() {
        String email = "user@example.com";
        String rawPassword = "securePass1";
        String encodedPassword = "$2a$12$hashedValue";

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = registerUserService.register(email, rawPassword);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getPasswordHash()).isEqualTo(encodedPassword);
        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("register — valid credentials — delegates password encoding to PasswordEncoder")
    void register_ValidCredentials_DelegatesPasswordEncoding() {
        String email = "user@example.com";
        String rawPassword = "securePass1";
        String encodedPassword = "$2a$12$hashedValue";

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registerUserService.register(email, rawPassword);

        verify(passwordEncoder).encode(eq(rawPassword));
    }

    @Test
    @DisplayName("register — valid credentials — persists user via UserRepository")
    void register_ValidCredentials_PersistsUserViaRepository() {
        String email = "user@example.com";
        String rawPassword = "securePass1";
        String encodedPassword = "$2a$12$hashedValue";

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registerUserService.register(email, rawPassword);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User persisted = captor.getValue();
        assertThat(persisted.getEmail()).isEqualTo(email);
        assertThat(persisted.getPasswordHash()).isEqualTo(encodedPassword);
    }

    @Test
    @DisplayName("register — duplicate email — throws DuplicateEmailException")
    void register_DuplicateEmail_ThrowsDuplicateEmailException() {
        String email = "existing@example.com";

        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> registerUserService.register(email, "password123"))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining(email);
    }

    @Test
    @DisplayName("register — duplicate email — does not encode password or persist user")
    void register_DuplicateEmail_DoesNotEncodeOrPersist() {
        String email = "existing@example.com";

        when(userRepository.existsByEmail(email)).thenReturn(true);

        try {
            registerUserService.register(email, "password123");
        } catch (DuplicateEmailException ignored) {
            // expected
        }

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }
}
