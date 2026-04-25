package com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.outbound.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the {@link UserRepository} outbound port.
 */
@Component
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository springDataRepo;

    public JpaUserRepository(SpringDataUserRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataRepo.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataRepo.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = springDataRepo.save(UserJpaEntity.fromDomain(user));
        return saved.toDomain();
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepo.existsByEmail(email);
    }
}
