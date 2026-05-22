package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the {@link SessionRepository} outbound port.
 * Maps between the domain {@link Session} and the JPA {@link SessionJpaEntity}.
 */
@Component
public class JpaSessionRepository implements SessionRepository {

    private static final List<SessionStatus> ACTIVE_STATUSES = List.of(
            SessionStatus.IN_PROGRESS,
            SessionStatus.PAUSED
    );

    private final SpringDataSessionRepository springDataRepo;

    public JpaSessionRepository(SpringDataSessionRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Session save(Session session) {
        SessionJpaEntity entity = SessionJpaEntity.fromDomain(session);
        SessionJpaEntity saved = springDataRepo.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Session> findById(UUID id) {
        return springDataRepo.findById(id)
                .map(SessionJpaEntity::toDomain);
    }

    @Override
    public Optional<Session> findActiveByUserId(String userId) {
        return springDataRepo.findFirstByUserIdAndStatusIn(userId, ACTIVE_STATUSES)
                .map(SessionJpaEntity::toDomain);
    }
}
