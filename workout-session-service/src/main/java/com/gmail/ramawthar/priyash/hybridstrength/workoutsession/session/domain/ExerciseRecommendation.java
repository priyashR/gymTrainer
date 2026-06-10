package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain;

/**
 * Immutable value object representing the prescribed parameters for one exercise.
 * All prescription fields are nullable — a fully-null recommendation means "no prescription available."
 */
public record ExerciseRecommendation(
    int sectionIndex,
    int exerciseIndex,
    String prescribedWeight,    // nullable, max 50 chars, non-blank if present
    String prescribedReps,      // nullable, max 50 chars, non-blank if present
    Integer prescribedSets      // nullable, range [1, 100] if present
) {
    public ExerciseRecommendation {
        if (prescribedWeight != null && prescribedWeight.isBlank()) {
            throw new IllegalArgumentException("prescribedWeight must not be blank");
        }
        if (prescribedWeight != null && prescribedWeight.length() > 50) {
            throw new IllegalArgumentException("prescribedWeight must not exceed 50 characters");
        }
        if (prescribedReps != null && prescribedReps.isBlank()) {
            throw new IllegalArgumentException("prescribedReps must not be blank");
        }
        if (prescribedReps != null && prescribedReps.length() > 50) {
            throw new IllegalArgumentException("prescribedReps must not exceed 50 characters");
        }
        if (prescribedSets != null && (prescribedSets < 1 || prescribedSets > 100)) {
            throw new IllegalArgumentException("prescribedSets must be between 1 and 100");
        }
    }

    /** Returns true if all prescription fields are null. */
    public boolean isEmpty() {
        return prescribedWeight == null && prescribedReps == null && prescribedSets == null;
    }
}
