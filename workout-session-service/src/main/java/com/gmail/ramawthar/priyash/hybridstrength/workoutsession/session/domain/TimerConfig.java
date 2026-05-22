package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain;

/**
 * Timer configuration for a workout section, determined by section type.
 * <ul>
 *   <li>AMRAP: uses durationSeconds (countdown)</li>
 *   <li>STRENGTH: uses stopwatch (no fixed duration)</li>
 *   <li>TABATA: uses workSeconds, restSeconds, rounds (interval)</li>
 *   <li>EMOM: uses durationSeconds, rounds (interval)</li>
 * </ul>
 */
public class TimerConfig {

    private final SectionType sectionType;
    private final Integer durationSeconds;
    private final Integer workSeconds;
    private final Integer restSeconds;
    private final Integer rounds;

    public TimerConfig(SectionType sectionType, Integer durationSeconds, Integer workSeconds,
                       Integer restSeconds, Integer rounds) {
        if (sectionType == null) {
            throw new IllegalArgumentException("sectionType must not be null");
        }
        this.sectionType = sectionType;
        this.durationSeconds = durationSeconds;
        this.workSeconds = workSeconds;
        this.restSeconds = restSeconds;
        this.rounds = rounds;
    }

    public SectionType getSectionType() {
        return sectionType;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Integer getWorkSeconds() {
        return workSeconds;
    }

    public Integer getRestSeconds() {
        return restSeconds;
    }

    public Integer getRounds() {
        return rounds;
    }
}
