package com.gmail.ramawthar.priyash.hybridstrength.authservice.property;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.PasswordEncoder;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.property.fake.FakeBcryptPasswordEncoder;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.CharRange;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for password encoding.
 * Uses in-memory fakes (not mocks) for outbound ports.
 */
class PasswordEncodingPropertyTest {

    private final PasswordEncoder passwordEncoder = new FakeBcryptPasswordEncoder();

    // Feature: auth-service-mvp1, Property 6: Password hashing round-trip with cost factor invariant
    // **Validates: Requirements 1.6**
    @Property(tries = 100)
    void passwordHashingRoundTrip_EncodesAndMatches_WithCostFactorAtLeast12(
            @ForAll @StringLength(min = 8, max = 72) @CharRange(from = '!', to = '~') String rawPassword) {

        String hash = passwordEncoder.encode(rawPassword);

        // (a) matches(rawPassword, hash) returns true
        assertTrue(passwordEncoder.matches(rawPassword, hash),
                "Encoded hash must verify against the original raw password");

        // (b) hash is not equal to the raw password
        assertNotEquals(rawPassword, hash,
                "Hash must differ from the raw password");

        // (c) bcrypt prefix indicates cost factor >= 12
        assertTrue(hash.matches("^\\$2[ab]\\$1[2-9]\\$.*"),
                "Bcrypt hash must have a cost factor of at least 12, got: " + hash);
    }
}
