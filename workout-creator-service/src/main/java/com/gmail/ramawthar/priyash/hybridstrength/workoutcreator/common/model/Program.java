package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model;

import java.util.List;
import java.util.Objects;

/**
 * Core domain object representing a training program.
 * Shared across generation, vault, and upload features.
 * Pure domain object — no framework dependencies.
 */
public final class Program {

    private final String name;
    private final int durationWeeks;
    private final String goal;
    private final List<String> equipmentProfile;
    private final List<Week> weeks;

    public Program(String name, int durationWeeks, String goal,
                   List<String> equipmentProfile, List<Week> weeks) {
        this.name = name;
        this.durationWeeks = durationWeeks;
        this.goal = goal;
        this.equipmentProfile = List.copyOf(equipmentProfile);
        this.weeks = List.copyOf(weeks);
    }

    public String getName() { return name; }
    public int getDurationWeeks() { return durationWeeks; }
    public String getGoal() { return goal; }
    public List<String> getEquipmentProfile() { return equipmentProfile; }
    public List<Week> getWeeks() { return weeks; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Program p)) return false;
        return durationWeeks == p.durationWeeks
                && Objects.equals(name, p.name)
                && Objects.equals(goal, p.goal)
                && Objects.equals(equipmentProfile, p.equipmentProfile)
                && Objects.equals(weeks, p.weeks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, durationWeeks, goal, equipmentProfile, weeks);
    }
}
