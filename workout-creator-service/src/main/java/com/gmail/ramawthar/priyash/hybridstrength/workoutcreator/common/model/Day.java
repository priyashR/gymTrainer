package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model;

import java.util.List;
import java.util.Objects;

/**
 * A single training day within a Week.
 * Pure domain object — no framework dependencies.
 */
public final class Day {

    private final int dayNumber;
    private final String label;
    private final String focusArea;
    private final Modality modality;
    private final List<WarmCoolEntry> warmUp;
    private final List<Section> sections;
    private final List<WarmCoolEntry> coolDown;
    private final String methodologySource; // nullable

    public Day(int dayNumber, String label, String focusArea, Modality modality,
               List<WarmCoolEntry> warmUp, List<Section> sections,
               List<WarmCoolEntry> coolDown, String methodologySource) {
        this.dayNumber = dayNumber;
        this.label = label;
        this.focusArea = focusArea;
        this.modality = modality;
        this.warmUp = List.copyOf(warmUp);
        this.sections = List.copyOf(sections);
        this.coolDown = List.copyOf(coolDown);
        this.methodologySource = methodologySource;
    }

    public int getDayNumber() { return dayNumber; }
    public String getLabel() { return label; }
    public String getFocusArea() { return focusArea; }
    public Modality getModality() { return modality; }
    public List<WarmCoolEntry> getWarmUp() { return warmUp; }
    public List<Section> getSections() { return sections; }
    public List<WarmCoolEntry> getCoolDown() { return coolDown; }
    public String getMethodologySource() { return methodologySource; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Day d)) return false;
        return dayNumber == d.dayNumber
                && Objects.equals(label, d.label)
                && Objects.equals(focusArea, d.focusArea)
                && modality == d.modality
                && Objects.equals(warmUp, d.warmUp)
                && Objects.equals(sections, d.sections)
                && Objects.equals(coolDown, d.coolDown)
                && Objects.equals(methodologySource, d.methodologySource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayNumber, label, focusArea, modality, warmUp, sections, coolDown, methodologySource);
    }
}
