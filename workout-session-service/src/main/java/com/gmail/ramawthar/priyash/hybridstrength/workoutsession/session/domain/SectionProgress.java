package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks the progress of a single section within a workout session.
 */
public class SectionProgress {

    private final int sectionIndex;
    private final String sectionName;
    private final SectionType sectionType;
    private final List<ExerciseLog> exerciseLogs;
    private boolean completed;

    public SectionProgress(int sectionIndex, String sectionName, SectionType sectionType,
                           List<ExerciseLog> exerciseLogs) {
        if (sectionIndex < 0) {
            throw new IllegalArgumentException("sectionIndex must be non-negative");
        }
        if (sectionName == null || sectionName.isBlank()) {
            throw new IllegalArgumentException("sectionName must not be null or blank");
        }
        if (sectionType == null) {
            throw new IllegalArgumentException("sectionType must not be null");
        }
        if (exerciseLogs == null) {
            throw new IllegalArgumentException("exerciseLogs must not be null");
        }
        this.sectionIndex = sectionIndex;
        this.sectionName = sectionName;
        this.sectionType = sectionType;
        this.exerciseLogs = new ArrayList<>(exerciseLogs);
        this.completed = false;
    }

    public SectionProgress(int sectionIndex, String sectionName, SectionType sectionType,
                           List<ExerciseLog> exerciseLogs, boolean completed) {
        if (sectionIndex < 0) {
            throw new IllegalArgumentException("sectionIndex must be non-negative");
        }
        if (sectionName == null || sectionName.isBlank()) {
            throw new IllegalArgumentException("sectionName must not be null or blank");
        }
        if (sectionType == null) {
            throw new IllegalArgumentException("sectionType must not be null");
        }
        if (exerciseLogs == null) {
            throw new IllegalArgumentException("exerciseLogs must not be null");
        }
        this.sectionIndex = sectionIndex;
        this.sectionName = sectionName;
        this.sectionType = sectionType;
        this.exerciseLogs = new ArrayList<>(exerciseLogs);
        this.completed = completed;
    }

    /**
     * Returns true if all exercises in this section are completed.
     */
    public boolean isAllExercisesCompleted() {
        return exerciseLogs.stream().allMatch(ExerciseLog::isCompleted);
    }

    /**
     * Marks this section as completed if all exercises are done.
     */
    public void updateCompletionStatus() {
        this.completed = isAllExercisesCompleted();
    }

    public int getSectionIndex() {
        return sectionIndex;
    }

    public String getSectionName() {
        return sectionName;
    }

    public SectionType getSectionType() {
        return sectionType;
    }

    public List<ExerciseLog> getExerciseLogs() {
        return Collections.unmodifiableList(exerciseLogs);
    }

    public boolean isCompleted() {
        return completed;
    }
}
