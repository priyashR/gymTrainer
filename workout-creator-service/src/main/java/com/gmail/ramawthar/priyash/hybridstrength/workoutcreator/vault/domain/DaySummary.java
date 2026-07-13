package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain;

/**
 * Lightweight summary of a single day within a vault program.
 * Used by the browse days endpoint to list available days for copying.
 * Pure domain object — no framework dependencies.
 *
 * @param weekNumber the 1-based week number containing this day
 * @param dayNumber  the 1-based day number within the week
 * @param label      the display label for the day (e.g. "Push Day")
 * @param focusArea  the primary focus area of the day (e.g. "Push", "Pull", "Legs")
 */
public record DaySummary(
        int weekNumber,
        int dayNumber,
        String label,
        String focusArea
) {}
