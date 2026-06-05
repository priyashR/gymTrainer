package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Value object representing a single strength set performed within an exercise.
 * Captures weight, repetitions, and optional RPE (Rate of Perceived Exertion).
 */
public class SetLog {

    private final int setNumber;
    private final BigDecimal weight;
    private final int repetitions;
    private final BigDecimal rpe;
    private final Instant loggedAt;

    public SetLog(int setNumber, BigDecimal weight, int repetitions, BigDecimal rpe, Instant loggedAt) {
        if (setNumber < 1) {
            throw new IllegalArgumentException("setNumber must be 1-based (positive)");
        }
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be greater than zero");
        }
        if (repetitions <= 0) {
            throw new IllegalArgumentException("Repetitions must be at least 1");
        }
        if (rpe != null) {
            if (rpe.compareTo(new BigDecimal("1.0")) < 0 || rpe.compareTo(new BigDecimal("10.0")) > 0) {
                throw new IllegalArgumentException("RPE must be between 1.0 and 10.0 in 0.5 increments");
            }
            // Check 0.5 increment: rpe * 2 must be a whole number
            BigDecimal doubled = rpe.multiply(new BigDecimal("2"));
            if (doubled.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("RPE must be between 1.0 and 10.0 in 0.5 increments");
            }
        }
        if (loggedAt == null) {
            throw new IllegalArgumentException("loggedAt must not be null");
        }
        this.setNumber = setNumber;
        this.weight = weight;
        this.repetitions = repetitions;
        this.rpe = rpe;
        this.loggedAt = loggedAt;
    }

    public int getSetNumber() {
        return setNumber;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public BigDecimal getRpe() {
        return rpe;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }
}
