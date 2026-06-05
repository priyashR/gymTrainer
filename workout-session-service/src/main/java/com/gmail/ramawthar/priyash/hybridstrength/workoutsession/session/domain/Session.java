package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregate root representing an active workout session.
 * Pure domain object — no framework dependencies.
 */
public class Session {

    private final UUID id;
    private final String userId;
    private final UUID programId;
    private final UUID enrollmentId;
    private final int weekNumber;
    private final int dayNumber;
    private SessionStatus status;
    private int currentSectionIndex;
    private final List<SectionProgress> sectionProgresses;
    private final String workoutSnapshot;
    private final Instant startedAt;
    private Instant pausedAt;
    private Instant completedAt;
    private Instant lastPersistedAt;
    private long totalPausedSeconds;
    private Integer durationSeconds;

    private Session(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.programId = builder.programId;
        this.enrollmentId = builder.enrollmentId;
        this.weekNumber = builder.weekNumber;
        this.dayNumber = builder.dayNumber;
        this.status = builder.status;
        this.currentSectionIndex = builder.currentSectionIndex;
        this.sectionProgresses = new ArrayList<>(builder.sectionProgresses);
        this.workoutSnapshot = builder.workoutSnapshot;
        this.startedAt = builder.startedAt;
        this.pausedAt = builder.pausedAt;
        this.completedAt = builder.completedAt;
        this.lastPersistedAt = builder.lastPersistedAt;
        this.totalPausedSeconds = builder.totalPausedSeconds;
        this.durationSeconds = builder.durationSeconds;
    }

    /**
     * Creates a new session in IN_PROGRESS state from a workout definition.
     */
    public static Session start(UUID id, String userId, UUID programId, UUID enrollmentId,
                                int weekNumber, int dayNumber,
                                List<SectionProgress> sectionProgresses,
                                String workoutSnapshot, Instant now) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
        if (sectionProgresses == null || sectionProgresses.isEmpty()) {
            throw new IllegalArgumentException("sectionProgresses must not be null or empty");
        }
        if (workoutSnapshot == null || workoutSnapshot.isBlank()) {
            throw new IllegalArgumentException("workoutSnapshot must not be null or blank");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        return new Builder()
                .id(id)
                .userId(userId)
                .programId(programId)
                .enrollmentId(enrollmentId)
                .weekNumber(weekNumber)
                .dayNumber(dayNumber)
                .status(SessionStatus.IN_PROGRESS)
                .currentSectionIndex(0)
                .sectionProgresses(sectionProgresses)
                .workoutSnapshot(workoutSnapshot)
                .startedAt(now)
                .lastPersistedAt(now)
                .totalPausedSeconds(0)
                .build();
    }

    /**
     * Marks an exercise as completed. Idempotent — if already completed, this is a no-op.
     *
     * @param sectionIndex  the section containing the exercise
     * @param exerciseIndex the exercise within the section
     * @param now           the completion timestamp
     * @return the effective rest duration in seconds for the completed exercise
     */
    public int completeExercise(int sectionIndex, int exerciseIndex, Instant now) {
        if (status == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot modify a completed session");
        }
        if (sectionIndex < 0 || sectionIndex >= sectionProgresses.size()) {
            throw new IllegalArgumentException(
                    "Invalid sectionIndex: " + sectionIndex + ", valid range: [0, " + (sectionProgresses.size() - 1) + "]");
        }

        SectionProgress section = sectionProgresses.get(sectionIndex);
        List<ExerciseLog> logs = section.getExerciseLogs();

        if (exerciseIndex < 0 || exerciseIndex >= logs.size()) {
            throw new IllegalArgumentException(
                    "Invalid exerciseIndex: " + exerciseIndex + ", valid range: [0, " + (logs.size() - 1) + "]");
        }

        ExerciseLog exercise = logs.get(exerciseIndex);

        // Idempotent: already completed is a no-op but still returns rest duration
        if (exercise.isCompleted()) {
            return exercise.getEffectiveRestSeconds();
        }

        exercise.markCompleted(now);
        section.updateCompletionStatus();
        this.lastPersistedAt = now;

        return exercise.getEffectiveRestSeconds();
    }

    /**
     * Advances the current section index to the target index.
     *
     * @param targetSectionIndex the section to navigate to
     */
    public void advanceSection(int targetSectionIndex) {
        if (status == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot modify a completed session");
        }
        if (targetSectionIndex < 0 || targetSectionIndex >= sectionProgresses.size()) {
            throw new IllegalArgumentException(
                    "Invalid targetSectionIndex: " + targetSectionIndex + ", valid range: [0, " + (sectionProgresses.size() - 1) + "]");
        }
        this.currentSectionIndex = targetSectionIndex;
    }

    /**
     * Pauses the session. Only valid from IN_PROGRESS state.
     *
     * @param now the pause timestamp
     */
    public void pause(Instant now) {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only pause a session that is IN_PROGRESS, current status: " + status);
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.status = SessionStatus.PAUSED;
        this.pausedAt = now;
        this.lastPersistedAt = now;
    }

    /**
     * Ends the session, marking it as COMPLETED. Valid from IN_PROGRESS or PAUSED.
     *
     * @param now the completion timestamp
     */
    public void end(Instant now) {
        if (status == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Session is already completed");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.status = SessionStatus.COMPLETED;
        this.completedAt = now;
        this.lastPersistedAt = now;
    }

    /**
     * Computes the active duration of the session excluding paused time.
     * Should be called after end() to set the durationSeconds field.
     *
     * @param endTime the time the session ended
     */
    public void computeDuration(Instant endTime) {
        if (startedAt == null || endTime == null) {
            throw new IllegalArgumentException("startedAt and endTime must not be null");
        }
        long totalSeconds = Duration.between(startedAt, endTime).getSeconds();
        this.durationSeconds = (int) Math.max(0, totalSeconds - totalPausedSeconds);
    }

    /**
     * Returns true if any ExerciseLog has non-empty setLogs
     * OR any SectionProgress has a non-null crossFitScore.
     */
    public boolean hasPerformanceData() {
        for (SectionProgress section : sectionProgresses) {
            if (section.getCrossFitScore() != null) {
                return true;
            }
            for (ExerciseLog exerciseLog : section.getExerciseLogs()) {
                if (!exerciseLog.getSetLogs().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Computes the "next up" indicator:
     * <ol>
     *   <li>First uncompleted exercise in the current section (returns exercise name)</li>
     *   <li>Next section name if current section is fully complete and more sections exist</li>
     *   <li>Empty if all sections are complete</li>
     * </ol>
     */
    public Optional<String> computeNextUp() {
        SectionProgress currentSection = sectionProgresses.get(currentSectionIndex);

        // Find first uncompleted exercise in current section
        for (ExerciseLog log : currentSection.getExerciseLogs()) {
            if (!log.isCompleted()) {
                return Optional.of(log.getExerciseName());
            }
        }

        // Current section is fully complete — look for next section
        for (int i = currentSectionIndex + 1; i < sectionProgresses.size(); i++) {
            SectionProgress nextSection = sectionProgresses.get(i);
            if (!nextSection.isAllExercisesCompleted()) {
                return Optional.of(nextSection.getSectionName());
            }
        }

        // All sections complete
        return Optional.empty();
    }

    /**
     * Returns true if every exercise in every section has been completed.
     */
    public boolean isAllComplete() {
        return sectionProgresses.stream().allMatch(SectionProgress::isAllExercisesCompleted);
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public UUID getProgramId() {
        return programId;
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public int getCurrentSectionIndex() {
        return currentSectionIndex;
    }

    public List<SectionProgress> getSectionProgresses() {
        return Collections.unmodifiableList(sectionProgresses);
    }

    public String getWorkoutSnapshot() {
        return workoutSnapshot;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getLastPersistedAt() {
        return lastPersistedAt;
    }

    public long getTotalPausedSeconds() {
        return totalPausedSeconds;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    // --- Builder for reconstitution from persistence ---

    public static class Builder {
        private UUID id;
        private String userId;
        private UUID programId;
        private UUID enrollmentId;
        private int weekNumber;
        private int dayNumber;
        private SessionStatus status;
        private int currentSectionIndex;
        private List<SectionProgress> sectionProgresses = new ArrayList<>();
        private String workoutSnapshot;
        private Instant startedAt;
        private Instant pausedAt;
        private Instant completedAt;
        private Instant lastPersistedAt;
        private long totalPausedSeconds;
        private Integer durationSeconds;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder programId(UUID programId) {
            this.programId = programId;
            return this;
        }

        public Builder enrollmentId(UUID enrollmentId) {
            this.enrollmentId = enrollmentId;
            return this;
        }

        public Builder weekNumber(int weekNumber) {
            this.weekNumber = weekNumber;
            return this;
        }

        public Builder dayNumber(int dayNumber) {
            this.dayNumber = dayNumber;
            return this;
        }

        public Builder status(SessionStatus status) {
            this.status = status;
            return this;
        }

        public Builder currentSectionIndex(int currentSectionIndex) {
            this.currentSectionIndex = currentSectionIndex;
            return this;
        }

        public Builder sectionProgresses(List<SectionProgress> sectionProgresses) {
            this.sectionProgresses = sectionProgresses;
            return this;
        }

        public Builder workoutSnapshot(String workoutSnapshot) {
            this.workoutSnapshot = workoutSnapshot;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder pausedAt(Instant pausedAt) {
            this.pausedAt = pausedAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder lastPersistedAt(Instant lastPersistedAt) {
            this.lastPersistedAt = lastPersistedAt;
            return this;
        }

        public Builder totalPausedSeconds(long totalPausedSeconds) {
            this.totalPausedSeconds = totalPausedSeconds;
            return this;
        }

        public Builder durationSeconds(Integer durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Session build() {
            return new Session(this);
        }
    }
}
