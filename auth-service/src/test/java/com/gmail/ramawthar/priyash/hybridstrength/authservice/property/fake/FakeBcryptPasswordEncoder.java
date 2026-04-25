package com.gmail.ramawthar.priyash.hybridstrength.authservice.property.fake;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.PasswordEncoder;

/**
 * Fake password encoder using real BCrypt for property tests.
 * Uses cost factor 12 to match production configuration.
 */
public class FakeBcryptPasswordEncoder implements PasswordEncoder {

    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder delegate =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
