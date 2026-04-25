package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bcrypt-backed implementation of the {@link PasswordEncoder} outbound port.
 * Uses a cost factor of 12 as required by security standards.
 */
@Component
public class BcryptPasswordEncoder implements PasswordEncoder {

    private static final int BCRYPT_STRENGTH = 12;

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
