package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

/**
 * Enumeration of supported section kinds within a workout.
 * Each type has specific timing field requirements enforced by {@link Section}.
 */
public enum SectionType {
    STRENGTH,
    AMRAP,
    EMOM,
    TABATA,
    FOR_TIME,
    ACCESSORY
}
