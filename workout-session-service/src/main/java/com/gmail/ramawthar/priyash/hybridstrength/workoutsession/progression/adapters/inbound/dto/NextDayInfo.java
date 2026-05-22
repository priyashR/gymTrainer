package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.inbound.dto;

/**
 * Information about the next scheduled workout day within a program enrollment.
 * Included in the enrollment response for the home screen "Next Step" indicator.
 */
public record NextDayInfo(
        String programName,
        int weekNumber,
        int dayNumber,
        String dayLabel
) {
}
