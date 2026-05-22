package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link EnrollmentJpaEntity}.
 * Package-private — only used by {@link JpaEnrollmentRepository}.
 */
interface SpringDataEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, UUID> {

    Optional<EnrollmentJpaEntity> findByUserIdAndStatus(String userId, EnrollmentStatus status);
}
