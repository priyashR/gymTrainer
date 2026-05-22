package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;

import java.util.UUID;

/**
 * Inbound port for skipping the current program day.
 * Advances the day pointer and records a skip event for history tracking.
 */
public interface SkipDayUseCase {

    /**
     * Skips the current day in the enrollment, advancing the pointer
     * and recording a SkipRecord with the skipped day's identifier and timestamp.
     *
     * @param enrollmentId the enrollment to skip a day on
     * @param userId       the authenticated user's ID (for ownership check)
     * @return the updated enrollment
     */
    ProgramEnrollment skipDay(UUID enrollmentId, String userId);
}
