package com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.application;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.PasswordEncoder;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.common.exception.DuplicateEmailException;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.inbound.RegisterUserUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.outbound.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service implementing user registration.
 * Depends only on outbound ports (interfaces), not adapters.
 */
@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String email, String rawPassword) {
        log.debug("Processing registration request for email: {}", email);

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected — duplicate email: {}", email);
            throw new DuplicateEmailException("Email already registered: " + email);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);

        Instant now = Instant.now();
        User user = new User(UUID.randomUUID(), email, encodedPassword, "USER", now, now);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        return savedUser;
    }
}
