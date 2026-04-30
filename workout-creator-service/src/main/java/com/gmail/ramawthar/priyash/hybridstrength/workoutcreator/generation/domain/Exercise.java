package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.Objects;

/**
 * A single movement within a Section (e.g. Back Squat, Box Jump),
 * including prescribed sets, reps, weight, and rest.
 * <p>
 * Pure Java — no framework imports.
 */
public class Exercise {

    private final String name;
    private final int sets;
    private final String reps;
    private final String weight;
    private final Integer restSeconds;

    /**
     * Creates a new Exercise.
     *
     * @param name        exercise name (e.g. "Back Squat"); must not be null or blank
     * @param sets        number of sets; must be positive
     * @param reps        rep scheme (e.g. "8-12", "max"); must not be null or blank
     * @param weight      weight prescription (e.g. "135 lbs", "bodyweight"); may be null
     * @param restSeconds rest between sets in seconds; null for timed section types
     */
    public Exercise(String name, int sets, String reps, String weight, Integer restSeconds) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Exercise name must not be null or blank");
        }
        if (sets <= 0) {
            throw new IllegalArgumentException("Exercise sets must be positive, got: " + sets);
        }
        if (reps == null || reps.isBlank()) {
            throw new IllegalArgumentException("Exercise reps must not be null or blank");
        }
        if (restSeconds != null && restSeconds <= 0) {
            throw new IllegalArgumentException("Exercise restSeconds must be positive when present, got: " + restSeconds);
        }
        this.name = name;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.restSeconds = restSeconds;
    }

    public String getName() {
        return name;
    }

    public int getSets() {
        return sets;
    }

    public String getReps() {
        return reps;
    }

    public String getWeight() {
        return weight;
    }

    public Integer getRestSeconds() {
        return restSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exercise exercise = (Exercise) o;
        return sets == exercise.sets
                && Objects.equals(name, exercise.name)
                && Objects.equals(reps, exercise.reps)
                && Objects.equals(weight, exercise.weight)
                && Objects.equals(restSeconds, exercise.restSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sets, reps, weight, restSeconds);
    }

    @Override
    public String toString() {
        return "Exercise{name='" + name + "', sets=" + sets + ", reps='" + reps
                + "', weight='" + weight + "', restSeconds=" + restSeconds + "}";
    }
}
