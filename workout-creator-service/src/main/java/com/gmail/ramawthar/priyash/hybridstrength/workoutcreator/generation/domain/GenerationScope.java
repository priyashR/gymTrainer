package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

/**
 * The temporal scope of a generation request.
 * <ul>
 *   <li>{@code DAY} — single Workout</li>
 *   <li>{@code WEEK} — 7-day Program</li>
 *   <li>{@code FOUR_WEEK} — 28-day Program</li>
 * </ul>
 */
public enum GenerationScope {
    DAY,
    WEEK,
    FOUR_WEEK
}
