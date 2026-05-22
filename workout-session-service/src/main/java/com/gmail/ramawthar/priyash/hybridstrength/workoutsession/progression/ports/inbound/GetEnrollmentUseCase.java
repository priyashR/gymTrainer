package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;

import java.util.Optional;

/**
 * Inbound port for retrieving the user's active program enrollment.
 * Used by the home screen to display the "Next Step" indicator.
 */
public interface GetEnrollmentUseCase {

    /**
     * Returns the user's currently active enrollment, if any.
     *
     * @param userId the authenticated user's ID
     * @return the active enrollment, or empty if the user has no active program
     */
    Optional<ProgramEnrollment> getActiveEnrollment(String userId);
}
