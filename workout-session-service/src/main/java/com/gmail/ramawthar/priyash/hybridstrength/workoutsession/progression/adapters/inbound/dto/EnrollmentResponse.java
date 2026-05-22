package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.EnrollmentStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;

import java.time.Instant;
import java.util.UUID;

/**
 * Response representation of a program enrollment.
 */
public record EnrollmentResponse(
        UUID id,
        UUID programId,
        String programName,
        int currentWeek,
        int currentDay,
        int totalWeeks,
        String status,
        Instant enrolledAt,
        NextDayInfo nextDay
) {

    /**
     * Maps a domain ProgramEnrollment to its API response representation.
     */
    public static EnrollmentResponse from(ProgramEnrollment enrollment) {
        NextDayInfo nextDay = null;

        // Only include next day info if the enrollment is still active
        if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
            String dayLabel = "Week " + enrollment.getCurrentWeek() + ", Day " + enrollment.getCurrentDay();
            nextDay = new NextDayInfo(
                    enrollment.getProgramName(),
                    enrollment.getCurrentWeek(),
                    enrollment.getCurrentDay(),
                    dayLabel
            );
        }

        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getProgramId(),
                enrollment.getProgramName(),
                enrollment.getCurrentWeek(),
                enrollment.getCurrentDay(),
                enrollment.getTotalWeeks(),
                enrollment.getStatus().name(),
                enrollment.getEnrolledAt(),
                nextDay
        );
    }
}
