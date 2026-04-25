package com.gmail.ramawthar.priyash.hybridstrength.authservice.property;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.property.fake.FakeBcryptPasswordEncoder;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.property.fake.InMemoryUserRepository;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.application.RegisterUserService;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception.DuplicateEmailException;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.CharRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.web.api.Email;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for user registration.
 * Uses in-memory fakes (not mocks) for outbound ports.
 */
class RegistrationPropertyTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final FakeBcryptPasswordEncoder passwordEncoder = new FakeBcryptPasswordEncoder();
    private final RegisterUserService registerUserService = new RegisterUserService(userRepository, passwordEncoder);

    // Feature: auth-service-mvp1, Property 1: Registration round-trip
    @Property(tries = 100)
    void registrationRoundTrip(@ForAll @Email String email,
                              @ForAll @StringLength(min = 8, max = 72) @CharRange(from = '!', to = '~') String password) {
        userRepository.clear();

        User registered = registerUserService.register(email, password);

        Optional<User> found = userRepository.findByEmail(email);

        assertTrue(found.isPresent(), "User should be retrievable by email after registration");
        assertEquals(email, found.get().getEmail(), "Stored email must match registered email");
        assertNotNull(found.get().getPasswordHash(), "Password hash must not be null");
        assertNotEquals(password, found.get().getPasswordHash(), "Password hash must differ from raw password");
        assertTrue(passwordEncoder.matches(password, found.get().getPasswordHash()),
                "Stored hash must verify against the original password");
        assertEquals(registered.getId(), found.get().getId(), "Returned user must match stored user");
    }

    // Feature: auth-service-mvp1, Property 2: Duplicate email rejection
    // **Validates: Requirements 1.2**
    @Property(tries = 100)
    void register_DuplicateEmail_ThrowsDuplicateEmailExceptionAndRepositoryUnchanged(
            @ForAll @Email String email,
            @ForAll @StringLength(min = 8, max = 72) @CharRange(from = '!', to = '~') String password) {
        userRepository.clear();

        // Pre-populate repository with an existing user for this email
        Instant now = Instant.now();
        User existingUser = new User(UUID.randomUUID(), email, "existing-hash", "USER", now, now);
        userRepository.save(existingUser);

        // Second registration with the same email must throw
        assertThrows(DuplicateEmailException.class,
                () -> registerUserService.register(email, password),
                "Registering a duplicate email must throw DuplicateEmailException");

        // Repository must still contain exactly one user with that email
        Optional<User> found = userRepository.findByEmail(email);
        assertTrue(found.isPresent(), "Original user must still exist in repository");
        assertEquals(existingUser.getId(), found.get().getId(),
                "The stored user must be the original, not a replacement");
    }
}
