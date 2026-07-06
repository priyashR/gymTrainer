package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception;

import java.util.UUID;

/**
 * Thrown when a referenced workout does not exist in the user's vault.
 * Used during manual program creation to signal that a day assignment
 * references a workout ID that cannot be found for the authenticated user.
 */
public class WorkoutNotFoundException extends RuntimeException {

    private final UUID workoutId;

    public WorkoutNotFoundException(UUID workoutId) {
        super("Workout not found in vault: " + workoutId);
        this.workoutId = workoutId;
    }

    public UUID getWorkoutId() {
        return workoutId;
    }
}
