package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A named block within a Workout representing a distinct training phase
 * (e.g. a strength block, an AMRAP, a Tabata interval).
 * <p>
 * Timing fields are populated based on {@link SectionType} and enforced at construction:
 * <ul>
 *   <li>AMRAP / FOR_TIME — {@code timeCapMinutes} non-null and positive; exercise restSeconds null</li>
 *   <li>EMOM — {@code intervalSeconds} and {@code totalRounds} non-null and positive; exercise restSeconds null</li>
 *   <li>TABATA — {@code workIntervalSeconds}, {@code restIntervalSeconds}, and {@code totalRounds} non-null and positive; exercise restSeconds null</li>
 *   <li>STRENGTH / ACCESSORY — exercise restSeconds non-null and positive; all timed fields null</li>
 * </ul>
 * Pure Java — no framework imports.
 */
public class Section {

    private final String name;
    private final SectionType type;
    private final List<Exercise> exercises;
    private final Integer timeCapMinutes;
    private final Integer intervalSeconds;
    private final Integer totalRounds;
    private final Integer workIntervalSeconds;
    private final Integer restIntervalSeconds;

    /**
     * Creates a new Section with timing invariant enforcement.
     *
     * @param name                 section name; must not be null or blank
     * @param type                 section type; must not be null
     * @param exercises            exercises in this section; must not be null or empty
     * @param timeCapMinutes       time cap in minutes (AMRAP, FOR_TIME)
     * @param intervalSeconds      interval duration in seconds (EMOM)
     * @param totalRounds          total rounds (EMOM, TABATA)
     * @param workIntervalSeconds  work interval in seconds (TABATA)
     * @param restIntervalSeconds  rest interval in seconds (TABATA)
     * @throws IllegalArgumentException if timing invariants are violated
     */
    public Section(String name, SectionType type, List<Exercise> exercises,
                   Integer timeCapMinutes, Integer intervalSeconds, Integer totalRounds,
                   Integer workIntervalSeconds, Integer restIntervalSeconds) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Section name must not be null or blank");
        }
        Objects.requireNonNull(type, "Section type must not be null");
        if (exercises == null || exercises.isEmpty()) {
            throw new IllegalArgumentException("Section exercises must not be null or empty");
        }

        this.name = name;
        this.type = type;
        this.exercises = Collections.unmodifiableList(List.copyOf(exercises));
        this.timeCapMinutes = timeCapMinutes;
        this.intervalSeconds = intervalSeconds;
        this.totalRounds = totalRounds;
        this.workIntervalSeconds = workIntervalSeconds;
        this.restIntervalSeconds = restIntervalSeconds;

        validateTimingInvariants();
    }

    private void validateTimingInvariants() {
        switch (type) {
            case AMRAP, FOR_TIME -> validateTimedSection();
            case EMOM -> validateEmomSection();
            case TABATA -> validateTabataSection();
            case STRENGTH, ACCESSORY -> validateRestBasedSection();
        }
    }

    private void validateTimedSection() {
        if (timeCapMinutes == null || timeCapMinutes <= 0) {
            throw new IllegalArgumentException(
                    type + " section requires a positive timeCapMinutes, got: " + timeCapMinutes);
        }
        requireNullTimedFields("intervalSeconds", intervalSeconds);
        requireNullTimedFields("totalRounds", totalRounds);
        requireNullTimedFields("workIntervalSeconds", workIntervalSeconds);
        requireNullTimedFields("restIntervalSeconds", restIntervalSeconds);
        requireExerciseRestSecondsNull();
    }

    private void validateEmomSection() {
        if (intervalSeconds == null || intervalSeconds <= 0) {
            throw new IllegalArgumentException(
                    "EMOM section requires a positive intervalSeconds, got: " + intervalSeconds);
        }
        if (totalRounds == null || totalRounds <= 0) {
            throw new IllegalArgumentException(
                    "EMOM section requires a positive totalRounds, got: " + totalRounds);
        }
        requireNullTimedFields("timeCapMinutes", timeCapMinutes);
        requireNullTimedFields("workIntervalSeconds", workIntervalSeconds);
        requireNullTimedFields("restIntervalSeconds", restIntervalSeconds);
        requireExerciseRestSecondsNull();
    }

    private void validateTabataSection() {
        if (workIntervalSeconds == null || workIntervalSeconds <= 0) {
            throw new IllegalArgumentException(
                    "TABATA section requires a positive workIntervalSeconds, got: " + workIntervalSeconds);
        }
        if (restIntervalSeconds == null || restIntervalSeconds <= 0) {
            throw new IllegalArgumentException(
                    "TABATA section requires a positive restIntervalSeconds, got: " + restIntervalSeconds);
        }
        if (totalRounds == null || totalRounds <= 0) {
            throw new IllegalArgumentException(
                    "TABATA section requires a positive totalRounds, got: " + totalRounds);
        }
        requireNullTimedFields("timeCapMinutes", timeCapMinutes);
        requireNullTimedFields("intervalSeconds", intervalSeconds);
        requireExerciseRestSecondsNull();
    }

    private void validateRestBasedSection() {
        requireNullTimedFields("timeCapMinutes", timeCapMinutes);
        requireNullTimedFields("intervalSeconds", intervalSeconds);
        requireNullTimedFields("totalRounds", totalRounds);
        requireNullTimedFields("workIntervalSeconds", workIntervalSeconds);
        requireNullTimedFields("restIntervalSeconds", restIntervalSeconds);
        for (Exercise exercise : exercises) {
            if (exercise.getRestSeconds() == null || exercise.getRestSeconds() <= 0) {
                throw new IllegalArgumentException(
                        type + " section requires each exercise to have a positive restSeconds, "
                                + "but exercise '" + exercise.getName() + "' has: " + exercise.getRestSeconds());
            }
        }
    }

    private void requireNullTimedFields(String fieldName, Integer value) {
        if (value != null) {
            throw new IllegalArgumentException(
                    type + " section must not have " + fieldName + " set, got: " + value);
        }
    }

    private void requireExerciseRestSecondsNull() {
        for (Exercise exercise : exercises) {
            if (exercise.getRestSeconds() != null) {
                throw new IllegalArgumentException(
                        type + " section must not have per-exercise restSeconds, "
                                + "but exercise '" + exercise.getName() + "' has: " + exercise.getRestSeconds());
            }
        }
    }

    public String getName() {
        return name;
    }

    public SectionType getType() {
        return type;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public Integer getTimeCapMinutes() {
        return timeCapMinutes;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public Integer getTotalRounds() {
        return totalRounds;
    }

    public Integer getWorkIntervalSeconds() {
        return workIntervalSeconds;
    }

    public Integer getRestIntervalSeconds() {
        return restIntervalSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Section section = (Section) o;
        return Objects.equals(name, section.name)
                && type == section.type
                && Objects.equals(exercises, section.exercises)
                && Objects.equals(timeCapMinutes, section.timeCapMinutes)
                && Objects.equals(intervalSeconds, section.intervalSeconds)
                && Objects.equals(totalRounds, section.totalRounds)
                && Objects.equals(workIntervalSeconds, section.workIntervalSeconds)
                && Objects.equals(restIntervalSeconds, section.restIntervalSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, exercises, timeCapMinutes, intervalSeconds,
                totalRounds, workIntervalSeconds, restIntervalSeconds);
    }

    @Override
    public String toString() {
        return "Section{name='" + name + "', type=" + type + ", exercises=" + exercises
                + ", timeCapMinutes=" + timeCapMinutes + ", intervalSeconds=" + intervalSeconds
                + ", totalRounds=" + totalRounds + ", workIntervalSeconds=" + workIntervalSeconds
                + ", restIntervalSeconds=" + restIntervalSeconds + "}";
    }
}
