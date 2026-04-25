package com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;

/**
 * Inbound port for user registration.
 */
public interface RegisterUserUseCase {

    User register(String email, String rawPassword);
}
