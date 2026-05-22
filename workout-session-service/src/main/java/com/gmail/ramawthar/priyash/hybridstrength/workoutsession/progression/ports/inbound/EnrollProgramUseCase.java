package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;

import java.util.UUID;

/**
 * Inbound port for enrolling a user in a program.
 * If the user already has an active enrollment, it is marked as REPLACED
 * and the new enrollment starts at week 1, day 1.
 */
public interface EnrollProgramUseCase {

    /**
     * Enrolls a user in a program. Replaces any existing active enrollment.
     *
     * @param userId          the authenticated user's ID
     * @param programId       the program to enroll in
     * @param programName     the display name of the program
     * @param totalWeeks      the total number of weeks in the program
     * @param totalDaysPerWeek the maximum number of training days per week
     * @return the newly created enrollment
     */
    ProgramEnrollment enrollProgram(String userId, UUID programId, String programName,
                                    int totalWeeks, int totalDaysPerWeek);
}
