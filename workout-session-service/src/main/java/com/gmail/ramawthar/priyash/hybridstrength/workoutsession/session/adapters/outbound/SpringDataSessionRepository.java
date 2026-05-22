package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SessionJpaEntity}.
 * Package-private — only used by {@link JpaSessionRepository}.
 */
interface SpringDataSessionRepository extends JpaRepository<SessionJpaEntity, UUID> {

    /**
     * Finds the first session for a user with status IN_PROGRESS or PAUSED.
     */
    Optional<SessionJpaEntity> findFirstByUserIdAndStatusIn(String userId, Iterable<SessionStatus> statuses);
}
