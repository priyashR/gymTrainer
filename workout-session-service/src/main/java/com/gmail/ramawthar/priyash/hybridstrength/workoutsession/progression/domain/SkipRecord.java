package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain;

import java.time.Instant;

/**
 * Records a skipped day within a program enrollment.
 */
public class SkipRecord {

    private final int weekNumber;
    private final int dayNumber;
    private final Instant skippedAt;

    public SkipRecord(int weekNumber, int dayNumber, Instant skippedAt) {
        if (weekNumber < 1) {
            throw new IllegalArgumentException("weekNumber must be at least 1");
        }
        if (dayNumber < 1) {
            throw new IllegalArgumentException("dayNumber must be at least 1");
        }
        if (skippedAt == null) {
            throw new IllegalArgumentException("skippedAt must not be null");
        }
        this.weekNumber = weekNumber;
        this.dayNumber = dayNumber;
        this.skippedAt = skippedAt;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public Instant getSkippedAt() {
        return skippedAt;
    }
}
