package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and retrieving program enrollments.
 */
public interface EnrollmentRepository {

    /**
     * Persists an enrollment (create or update).
     *
     * @param enrollment the enrollment to save
     * @return the saved enrollment
     */
    ProgramEnrollment save(ProgramEnrollment enrollment);

    /**
     * Finds the user's currently active enrollment (status = ACTIVE).
     *
     * @param userId the user's ID
     * @return the active enrollment, or empty if none exists
     */
    Optional<ProgramEnrollment> findActiveByUserId(String userId);

    /**
     * Finds an enrollment by its unique ID.
     *
     * @param id the enrollment ID
     * @return the enrollment, or empty if not found
     */
    Optional<ProgramEnrollment> findById(UUID id);
}
