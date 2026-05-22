package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;

import java.util.UUID;

/**
 * Inbound port for advancing the program day pointer.
 * Called after a session is completed to move to the next scheduled workout.
 */
public interface AdvanceDayUseCase {

    /**
     * Advances the enrollment's day pointer to the next scheduled workout.
     * Handles week rollover and program completion.
     *
     * @param enrollmentId the enrollment to advance
     * @param userId       the authenticated user's ID (for ownership check)
     * @return the updated enrollment
     */
    ProgramEnrollment advanceDay(UUID enrollmentId, String userId);
}
