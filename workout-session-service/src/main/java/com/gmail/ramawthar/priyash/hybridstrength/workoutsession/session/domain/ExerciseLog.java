package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks the completion state and performance data of a single exercise within a section.
 * Includes the rest duration (in seconds) from the exercise definition,
 * used to drive the Rest Timer after exercise completion.
 */
public class ExerciseLog {

    /** Default rest duration in seconds when no explicit value is defined. */
    public static final int DEFAULT_REST_SECONDS = 60;

    private final int exerciseIndex;
    private final String exerciseName;
    private final Integer restSeconds;
    private boolean completed;
    private Instant completedAt;
    private final List<SetLog> setLogs;

    public ExerciseLog(int exerciseIndex, String exerciseName) {
        this(exerciseIndex, exerciseName, null);
    }

    public ExerciseLog(int exerciseIndex, String exerciseName, Integer restSeconds) {
        if (exerciseIndex < 0) {
            throw new IllegalArgumentException("exerciseIndex must be non-negative");
        }
        if (exerciseName == null || exerciseName.isBlank()) {
            throw new IllegalArgumentException("exerciseName must not be null or blank");
        }
        this.exerciseIndex = exerciseIndex;
        this.exerciseName = exerciseName;
        this.restSeconds = restSeconds;
        this.completed = false;
        this.completedAt = null;
        this.setLogs = new ArrayList<>();
    }

    public ExerciseLog(int exerciseIndex, String exerciseName, Integer restSeconds,
                       boolean completed, Instant completedAt) {
        this(exerciseIndex, exerciseName, restSeconds, completed, completedAt, new ArrayList<>());
    }

    /**
     * Constructor for reconstitution from persistence, including set logs.
     */
    public ExerciseLog(int exerciseIndex, String exerciseName, Integer restSeconds,
                       boolean completed, Instant completedAt, List<SetLog> setLogs) {
        if (exerciseIndex < 0) {
            throw new IllegalArgumentException("exerciseIndex must be non-negative");
        }
        if (exerciseName == null || exerciseName.isBlank()) {
            throw new IllegalArgumentException("exerciseName must not be null or blank");
        }
        this.exerciseIndex = exerciseIndex;
        this.exerciseName = exerciseName;
        this.restSeconds = restSeconds;
        this.completed = completed;
        this.completedAt = completedAt;
        this.setLogs = setLogs != null ? new ArrayList<>(setLogs) : new ArrayList<>();
    }

    public void markCompleted(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
        this.completed = true;
        this.completedAt = timestamp;
    }

    /**
     * Appends a set log to this exercise's set log list.
     */
    public void addSetLog(SetLog setLog) {
        if (setLog == null) {
            throw new IllegalArgumentException("setLog must not be null");
        }
        this.setLogs.add(setLog);
    }

    /**
     * Returns an unmodifiable view of the set logs in chronological order.
     */
    public List<SetLog> getSetLogs() {
        return Collections.unmodifiableList(setLogs);
    }

    public int getExerciseIndex() {
        return exerciseIndex;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    /**
     * Returns the configured rest duration in seconds from the exercise definition,
     * or null if not specified.
     */
    public Integer getRestSeconds() {
        return restSeconds;
    }

    /**
     * Returns the effective rest duration in seconds.
     * Uses the exercise definition's restSeconds if available, otherwise returns the default (60s).
     */
    public int getEffectiveRestSeconds() {
        return restSeconds != null ? restSeconds : DEFAULT_REST_SECONDS;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
