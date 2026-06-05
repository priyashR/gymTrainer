package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SetLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private List<SetLogDto> setLogs;

    // No-arg constructor for Jackson
    ExerciseLogDto() {
    }

    ExerciseLogDto(int exerciseIndex, String exerciseName, Integer restSeconds,
                   boolean completed, Instant completedAt, List<SetLogDto> setLogs) {
        this.exerciseIndex = exerciseIndex;
        this.exerciseName = exerciseName;
        this.restSeconds = restSeconds;
        this.completed = completed;
        this.completedAt = completedAt;
        this.setLogs = setLogs;
    }

    static ExerciseLogDto fromDomain(ExerciseLog domain) {
        List<SetLogDto> setLogDtos = domain.getSetLogs().stream()
                .map(SetLogDto::fromDomain)
                .toList();
        return new ExerciseLogDto(
                domain.getExerciseIndex(),
                domain.getExerciseName(),
                domain.getRestSeconds(),
                domain.isCompleted(),
                domain.getCompletedAt(),
                setLogDtos
        );
    }

    ExerciseLog toDomain() {
        List<SetLog> domainSetLogs = (setLogs != null)
                ? setLogs.stream().map(SetLogDto::toDomain).toList()
                : new ArrayList<>();
        return new ExerciseLog(exerciseIndex, exerciseName, restSeconds, completed, completedAt, domainSetLogs);
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

    public List<SetLogDto> getSetLogs() {
        return setLogs;
    }

    public void setSetLogs(List<SetLogDto> setLogs) {
        this.setLogs = setLogs;
    }
}
