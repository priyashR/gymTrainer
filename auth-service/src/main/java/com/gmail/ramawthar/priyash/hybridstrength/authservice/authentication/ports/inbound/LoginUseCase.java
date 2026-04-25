package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.TokenPair;

/**
 * Inbound port for user login.
 */
public interface LoginUseCase {

    TokenPair login(String email, String rawPassword);
}
