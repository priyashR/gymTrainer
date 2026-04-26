package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.outbound;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.RefreshToken;

/**
 * JPA entity mapping for the {@code refresh_tokens} table.
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshTokenJpaEntity() {
        // JPA requires a no-arg constructor
    }

    static RefreshTokenJpaEntity fromDomain(RefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.id = token.getId();
        entity.tokenHash = token.getTokenHash();
        entity.userId = token.getUserId();
        entity.expiresAt = token.getExpiresAt();
        entity.createdAt = token.getCreatedAt();
        return entity;
    }

    RefreshToken toDomain() {
        return new RefreshToken(id, tokenHash, userId, expiresAt, createdAt);
    }
}
