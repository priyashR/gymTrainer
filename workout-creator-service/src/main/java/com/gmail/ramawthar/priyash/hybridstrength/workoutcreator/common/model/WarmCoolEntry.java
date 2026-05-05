package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model;

/**
 * A single warm-up or cool-down entry within a Day.
 * Pure domain object — no framework dependencies.
 */
public record WarmCoolEntry(String movement, String instruction) {}
