package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model;

import java.util.List;
import java.util.Objects;

/**
 * A named training block within a Day (e.g., "Tier 1: Compound", "Metcon", "Finisher").
 * Corresponds to the {@code blocks} array in the Upload_Schema.
 * Pure domain object — no framework dependencies.
 */
public final class Section {

    private final String name;           // block_type in Upload_Schema
    private final SectionType sectionType; // mapped from format string
    private final String format;         // raw format string (e.g. "Sets/Reps", "AMRAP")
    private final Integer timeCap;       // nullable; time_cap_minutes in Upload_Schema
    private final List<Exercise> exercises;

    public Section(String name, SectionType sectionType, String format,
                   Integer timeCap, List<Exercise> exercises) {
        this.name = name;
        this.sectionType = sectionType;
        this.format = format;
        this.timeCap = timeCap;
        this.exercises = List.copyOf(exercises);
    }

    public String getName() { return name; }
    public SectionType getSectionType() { return sectionType; }
    public String getFormat() { return format; }
    public Integer getTimeCap() { return timeCap; }
    public List<Exercise> getExercises() { return exercises; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Section s)) return false;
        return Objects.equals(name, s.name)
                && sectionType == s.sectionType
                && Objects.equals(format, s.format)
                && Objects.equals(timeCap, s.timeCap)
                && Objects.equals(exercises, s.exercises);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sectionType, format, timeCap, exercises);
    }
}
