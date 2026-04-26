package com.gmail.ramawthar.priyash.hybridstrength.authservice.property.fake;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.RefreshToken;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory fake for property tests — no Spring context needed.
 * Stores refresh tokens keyed by their token hash.
 */
public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<String, RefreshToken> store = new HashMap<>();

    @Override
    public RefreshToken save(RefreshToken token) {
        store.put(token.getTokenHash(), token);
        return token;
    }

    @Override
    public Optional<RefreshToken> findByToken(String tokenValue) {
        String hash = sha256(tokenValue);
        return Optional.ofNullable(store.get(hash));
    }

    @Override
    public void deleteByUserId(UUID userId) {
        store.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
    }

    public void clear() {
        store.clear();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
