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
    private CrossFitScore crossFitScore;
    private int roundCount;

    public SectionProgress(int sectionIndex, String sectionName, SectionType sectionType,
                           List<ExerciseLog> exerciseLogs) {
        this(sectionIndex, sectionName, sectionType, exerciseLogs, false, null, 0);
    }

    public SectionProgress(int sectionIndex, String sectionName, SectionType sectionType,
                           List<ExerciseLog> exerciseLogs, boolean completed) {
        this(sectionIndex, sectionName, sectionType, exerciseLogs, completed, null, 0);
    }

    /**
     * Constructor for reconstitution from persistence, including CrossFit score and round counter.
     */
    public SectionProgress(int sectionIndex, String sectionName, SectionType sectionType,
                           List<ExerciseLog> exerciseLogs, boolean completed,
                           CrossFitScore crossFitScore, int roundCount) {
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
        this.crossFitScore = crossFitScore;
        this.roundCount = roundCount;
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

    /**
     * Sets or overwrites the CrossFit score for this section.
     * Only one score is allowed per section per session.
     */
    public void setCrossFitScore(CrossFitScore crossFitScore) {
        this.crossFitScore = crossFitScore;
    }

    /**
     * Returns the CrossFit score for this section, or null if not yet logged.
     */
    public CrossFitScore getCrossFitScore() {
        return crossFitScore;
    }

    /**
     * Sets the AMRAP round counter state.
     */
    public void setRoundCount(int roundCount) {
        this.roundCount = roundCount;
    }

    /**
     * Returns the current AMRAP round counter state.
     */
    public int getRoundCount() {
        return roundCount;
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
