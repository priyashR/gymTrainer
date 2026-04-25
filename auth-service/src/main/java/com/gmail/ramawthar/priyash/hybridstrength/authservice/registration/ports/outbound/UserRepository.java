package com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for user persistence operations.
 */
public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    User save(User user);

    boolean existsByEmail(String email);
}
