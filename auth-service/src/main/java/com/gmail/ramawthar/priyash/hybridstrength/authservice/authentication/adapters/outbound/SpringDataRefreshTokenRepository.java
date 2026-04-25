package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.outbound;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RefreshTokenJpaEntity}.
 * Package-private — only used by {@link JpaRefreshTokenRepository}.
 */
interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    void deleteByUserId(UUID userId);
}
