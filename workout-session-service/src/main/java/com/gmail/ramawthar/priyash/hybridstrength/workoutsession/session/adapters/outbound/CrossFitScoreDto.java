package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.CrossFitScore;

import java.time.Instant;

/**
 * DTO for JSON serialization of CrossFit score data within the section_progresses JSONB column.
 * Package-private — used only by the persistence layer.
 */
class CrossFitScoreDto {

    private int rounds;
    private int additionalReps;
    private Integer totalTimeSeconds;
    private Instant loggedAt;

    // No-arg constructor for Jackson
    CrossFitScoreDto() {
    }

    CrossFitScoreDto(int rounds, int additionalReps, Integer totalTimeSeconds, Instant loggedAt) {
        this.rounds = rounds;
        this.additionalReps = additionalReps;
        this.totalTimeSeconds = totalTimeSeconds;
        this.loggedAt = loggedAt;
    }

    static CrossFitScoreDto fromDomain(CrossFitScore domain) {
        if (domain == null) {
            return null;
        }
        return new CrossFitScoreDto(
                domain.getRounds(),
                domain.getAdditionalReps(),
                domain.getTotalTimeSeconds(),
                domain.getLoggedAt()
        );
    }

    CrossFitScore toDomain() {
        return new CrossFitScore(rounds, additionalReps, totalTimeSeconds, loggedAt);
    }

    // --- Getters and setters for Jackson ---

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public int getAdditionalReps() {
        return additionalReps;
    }

    public void setAdditionalReps(int additionalReps) {
        this.additionalReps = additionalReps;
    }

    public Integer getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(Integer totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Instant loggedAt) {
        this.loggedAt = loggedAt;
    }
}
