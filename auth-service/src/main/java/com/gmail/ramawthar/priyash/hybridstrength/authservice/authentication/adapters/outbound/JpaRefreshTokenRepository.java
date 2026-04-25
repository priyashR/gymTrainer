package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.RefreshToken;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.outbound.RefreshTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the {@link RefreshTokenRepository} outbound port.
 * <p>
 * The port's {@code findByToken} accepts the raw opaque token value.
 * This adapter hashes it with SHA-256 before querying, since only hashes are stored.
 */
@Component
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository springDataRepo;

    public JpaRefreshTokenRepository(SpringDataRefreshTokenRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity saved = springDataRepo.save(RefreshTokenJpaEntity.fromDomain(token));
        return saved.toDomain();
    }

    @Override
    public Optional<RefreshToken> findByToken(String tokenValue) {
        String hash = sha256(tokenValue);
        return springDataRepo.findByTokenHash(hash).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        springDataRepo.deleteByUserId(userId);
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
