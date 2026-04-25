package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound;

/**
 * Outbound port for password encoding and verification.
 */
public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
