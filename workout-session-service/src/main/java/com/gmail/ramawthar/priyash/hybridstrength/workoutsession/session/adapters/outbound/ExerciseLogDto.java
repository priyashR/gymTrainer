package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;

import java.time.Instant;

/**
 * DTO for JSON serialization of exercise log data within the section_progresses JSONB column.
 * Package-private — used only by the persistence layer.
 */
class ExerciseLogDto {

    private int exerciseIndex;
    private String exerciseName;
    private Integer restSeconds;
    private boolean completed;
    private Instant completedAt;

    // No-arg constructor for Jackson
    ExerciseLogDto() {
    }

    ExerciseLogDto(int exerciseIndex, String exerciseName, Integer restSeconds,
                   boolean completed, Instant completedAt) {
        this.exerciseIndex = exerciseIndex;
        this.exerciseName = exerciseName;
        this.restSeconds = restSeconds;
        this.completed = completed;
        this.completedAt = completedAt;
    }

    static ExerciseLogDto fromDomain(ExerciseLog domain) {
        return new ExerciseLogDto(
                domain.getExerciseIndex(),
                domain.getExerciseName(),
                domain.getRestSeconds(),
                domain.isCompleted(),
                domain.getCompletedAt()
        );
    }

    ExerciseLog toDomain() {
        return new ExerciseLog(exerciseIndex, exerciseName, restSeconds, completed, completedAt);
    }

    // --- Getters and setters for Jackson ---

    public int getExerciseIndex() {
        return exerciseIndex;
    }

    public void setExerciseIndex(int exerciseIndex) {
        this.exerciseIndex = exerciseIndex;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public Integer getRestSeconds() {
        return restSeconds;
    }

    public void setRestSeconds(Integer restSeconds) {
        this.restSeconds = restSeconds;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
