package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain;

import java.util.UUID;

/**
 * A single day assignment within a manual program.
 * Maps a day number to either a vault workout (by ID), an external activity (by type name),
 * or a copied day (by JSON snapshot with provenance).
 * Pure domain object — no framework dependencies.
 *
 * @param dayNumber        the 1-based day number in the program
 * @param type             whether this day is a WORKOUT, ACTIVITY, or COPIED_DAY
 * @param workoutId        non-null when type is WORKOUT; the UUID of the referenced vault workout
 * @param activityType     non-null when type is ACTIVITY; the name of the external activity (e.g. "Soccer")
 * @param snapshotData     non-null when type is COPIED_DAY; serialized JSON of the copied day structure
 * @param sourceProgramId  non-null when type is COPIED_DAY; UUID of the source program
 * @param sourceWeekNumber non-null when type is COPIED_DAY; week number in the source program
 * @param sourceDayNumber  non-null when type is COPIED_DAY; day number within the source week
 */
public record DayAssignment(
        int dayNumber,
        DayAssignmentType type,
        UUID workoutId,
        String activityType,
        String snapshotData,
        UUID sourceProgramId,
        Integer sourceWeekNumber,
        Integer sourceDayNumber
) {
    /** Backwards-compatible constructor for ACTIVITY/WORKOUT types. */
    public DayAssignment(int dayNumber, DayAssignmentType type, UUID workoutId, String activityType) {
        this(dayNumber, type, workoutId, activityType, null, null, null, null);
    }
}
