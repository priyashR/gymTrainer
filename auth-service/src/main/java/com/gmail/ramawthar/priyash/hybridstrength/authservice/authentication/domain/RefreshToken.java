package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a stored refresh token.
 * Pure Java — no framework imports.
 */
public class RefreshToken {

    private UUID id;
    private String tokenHash;
    private UUID userId;
    private Instant expiresAt;
    private Instant createdAt;

    public RefreshToken(UUID id, String tokenHash, UUID userId, Instant expiresAt, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RefreshToken{id=" + id + ", userId=" + userId + ", expiresAt=" + expiresAt + "}";
    }
}
