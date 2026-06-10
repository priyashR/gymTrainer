package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain;

import java.time.Instant;

/**
 * Value object representing a CrossFit-style score for AMRAP, EMOM, or FOR_TIME sections.
 * Captures rounds completed, additional reps in the final partial round, and optional total time.
 */
public class CrossFitScore {

    private final int rounds;
    private final int additionalReps;
    private final Integer totalTimeSeconds;
    private final Instant loggedAt;

    public CrossFitScore(int rounds, int additionalReps, Integer totalTimeSeconds, Instant loggedAt) {
        if (rounds < 0) {
            throw new IllegalArgumentException("Rounds must be non-negative");
        }
        if (additionalReps < 0) {
            throw new IllegalArgumentException("Additional reps must be non-negative");
        }
        if (totalTimeSeconds != null && totalTimeSeconds <= 0) {
            throw new IllegalArgumentException("Total time must be greater than zero for For Time sections");
        }
        if (loggedAt == null) {
            throw new IllegalArgumentException("loggedAt must not be null");
        }
        this.rounds = rounds;
        this.additionalReps = additionalReps;
        this.totalTimeSeconds = totalTimeSeconds;
        this.loggedAt = loggedAt;
    }

    public int getRounds() {
        return rounds;
    }

    public int getAdditionalReps() {
        return additionalReps;
    }

    public Integer getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }
}
