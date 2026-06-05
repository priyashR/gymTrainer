package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SetLog;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for JSON serialization of set log data within the section_progresses JSONB column.
 * Package-private — used only by the persistence layer.
 */
class SetLogDto {

    private int setNumber;
    private BigDecimal weight;
    private int repetitions;
    private BigDecimal rpe;
    private Instant loggedAt;

    // No-arg constructor for Jackson
    SetLogDto() {
    }

    SetLogDto(int setNumber, BigDecimal weight, int repetitions, BigDecimal rpe, Instant loggedAt) {
        this.setNumber = setNumber;
        this.weight = weight;
        this.repetitions = repetitions;
        this.rpe = rpe;
        this.loggedAt = loggedAt;
    }

    static SetLogDto fromDomain(SetLog domain) {
        return new SetLogDto(
                domain.getSetNumber(),
                domain.getWeight(),
                domain.getRepetitions(),
                domain.getRpe(),
                domain.getLoggedAt()
        );
    }

    SetLog toDomain() {
        return new SetLog(setNumber, weight, repetitions, rpe, loggedAt);
    }

    // --- Getters and setters for Jackson ---

    public int getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public BigDecimal getRpe() {
        return rpe;
    }

    public void setRpe(BigDecimal rpe) {
        this.rpe = rpe;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Instant loggedAt) {
        this.loggedAt = loggedAt;
    }
}
