package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root representing a user's enrollment in a multi-week program.
 * Tracks the current day pointer and skip history.
 * Pure domain object — no framework dependencies.
 */
public class ProgramEnrollment {

    private final UUID id;
    private final String userId;
    private final UUID programId;
    private final String programName;
    private int currentWeek;
    private int currentDay;
    private final int totalWeeks;
    private final int totalDaysPerWeek;
    private EnrollmentStatus status;
    private final Instant enrolledAt;
    private Instant completedAt;
    private final List<SkipRecord> skips;

    private ProgramEnrollment(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.programId = builder.programId;
        this.programName = builder.programName;
        this.currentWeek = builder.currentWeek;
        this.currentDay = builder.currentDay;
        this.totalWeeks = builder.totalWeeks;
        this.totalDaysPerWeek = builder.totalDaysPerWeek;
        this.status = builder.status;
        this.enrolledAt = builder.enrolledAt;
        this.completedAt = builder.completedAt;
        this.skips = new ArrayList<>(builder.skips);
    }

    /**
     * Creates a new enrollment at week 1, day 1 with ACTIVE status.
     */
    public static ProgramEnrollment create(UUID id, String userId, UUID programId, String programName,
                                           int totalWeeks, int totalDaysPerWeek, Instant now) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
        if (programId == null) {
            throw new IllegalArgumentException("programId must not be null");
        }
        if (programName == null || programName.isBlank()) {
            throw new IllegalArgumentException("programName must not be null or blank");
        }
        if (totalWeeks < 1) {
            throw new IllegalArgumentException("totalWeeks must be at least 1");
        }
        if (totalDaysPerWeek < 1) {
            throw new IllegalArgumentException("totalDaysPerWeek must be at least 1");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        return new Builder()
                .id(id)
                .userId(userId)
                .programId(programId)
                .programName(programName)
                .currentWeek(1)
                .currentDay(1)
                .totalWeeks(totalWeeks)
                .totalDaysPerWeek(totalDaysPerWeek)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(now)
                .skips(new ArrayList<>())
                .build();
    }

    /**
     * Advances the day pointer to the next scheduled workout.
     * If past the last day of the week, increments week and resets day to 1.
     * If past the last week, marks the enrollment as COMPLETED.
     */
    public void advanceDay() {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Can only advance day on an ACTIVE enrollment, current status: " + status);
        }

        if (currentDay < totalDaysPerWeek) {
            currentDay++;
        } else if (currentWeek < totalWeeks) {
            currentWeek++;
            currentDay = 1;
        } else {
            // Past the last day of the last week — program is complete
            this.status = EnrollmentStatus.COMPLETED;
            this.completedAt = Instant.now();
        }
    }

    /**
     * Skips the current day: advances the pointer and records a SkipRecord.
     *
     * @param now the timestamp of the skip
     */
    public void skipDay(Instant now) {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Can only skip day on an ACTIVE enrollment, current status: " + status);
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        SkipRecord record = new SkipRecord(currentWeek, currentDay, now);
        skips.add(record);

        advanceDay();
    }

    /**
     * Marks this enrollment as REPLACED (when a user starts a new program).
     */
    public void markReplaced() {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Can only replace an ACTIVE enrollment, current status: " + status);
        }
        this.status = EnrollmentStatus.REPLACED;
    }

    /**
     * Marks this enrollment as COMPLETED with a completion timestamp.
     *
     * @param now the completion timestamp
     */
    public void markCompleted(Instant now) {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Can only complete an ACTIVE enrollment, current status: " + status);
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.status = EnrollmentStatus.COMPLETED;
        this.completedAt = now;
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

    public String getProgramName() {
        return programName;
    }

    public int getCurrentWeek() {
        return currentWeek;
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public int getTotalWeeks() {
        return totalWeeks;
    }

    public int getTotalDaysPerWeek() {
        return totalDaysPerWeek;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<SkipRecord> getSkips() {
        return Collections.unmodifiableList(skips);
    }

    // --- Builder for reconstitution from persistence ---

    public static class Builder {
        private UUID id;
        private String userId;
        private UUID programId;
        private String programName;
        private int currentWeek;
        private int currentDay;
        private int totalWeeks;
        private int totalDaysPerWeek;
        private EnrollmentStatus status;
        private Instant enrolledAt;
        private Instant completedAt;
        private List<SkipRecord> skips = new ArrayList<>();

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

        public Builder programName(String programName) {
            this.programName = programName;
            return this;
        }

        public Builder currentWeek(int currentWeek) {
            this.currentWeek = currentWeek;
            return this;
        }

        public Builder currentDay(int currentDay) {
            this.currentDay = currentDay;
            return this;
        }

        public Builder totalWeeks(int totalWeeks) {
            this.totalWeeks = totalWeeks;
            return this;
        }

        public Builder totalDaysPerWeek(int totalDaysPerWeek) {
            this.totalDaysPerWeek = totalDaysPerWeek;
            return this;
        }

        public Builder status(EnrollmentStatus status) {
            this.status = status;
            return this;
        }

        public Builder enrolledAt(Instant enrolledAt) {
            this.enrolledAt = enrolledAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder skips(List<SkipRecord> skips) {
            this.skips = skips;
            return this;
        }

        public ProgramEnrollment build() {
            return new ProgramEnrollment(this);
        }
    }
}
