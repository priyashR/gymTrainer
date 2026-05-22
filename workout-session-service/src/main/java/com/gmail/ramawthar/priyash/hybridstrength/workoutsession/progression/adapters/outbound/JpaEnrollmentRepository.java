package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.EnrollmentStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.outbound.EnrollmentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the {@link EnrollmentRepository} outbound port.
 * Maps between the domain {@link ProgramEnrollment} and the JPA {@link EnrollmentJpaEntity}.
 */
@Component
public class JpaEnrollmentRepository implements EnrollmentRepository {

    private final SpringDataEnrollmentRepository springDataRepo;

    public JpaEnrollmentRepository(SpringDataEnrollmentRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public ProgramEnrollment save(ProgramEnrollment enrollment) {
        Optional<EnrollmentJpaEntity> existingOpt = springDataRepo.findById(enrollment.getId());

        if (existingOpt.isPresent()) {
            // Update existing entity in-place to avoid orphan removal issues
            EnrollmentJpaEntity existing = existingOpt.get();
            existing.updateFromDomain(enrollment);
            EnrollmentJpaEntity saved = springDataRepo.save(existing);
            return saved.toDomain();
        } else {
            EnrollmentJpaEntity entity = EnrollmentJpaEntity.fromDomain(enrollment);
            EnrollmentJpaEntity saved = springDataRepo.save(entity);
            return saved.toDomain();
        }
    }

    @Override
    public Optional<ProgramEnrollment> findActiveByUserId(String userId) {
        return springDataRepo.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE)
                .map(EnrollmentJpaEntity::toDomain);
    }

    @Override
    public Optional<ProgramEnrollment> findById(UUID id) {
        return springDataRepo.findById(id)
                .map(EnrollmentJpaEntity::toDomain);
    }
}
