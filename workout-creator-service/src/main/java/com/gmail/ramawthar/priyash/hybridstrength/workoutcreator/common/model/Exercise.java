package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model;

import java.util.Objects;

/**
 * A single movement within a Section/Block.
 * Pure domain object — no framework dependencies.
 */
public final class Exercise {

    private final String name;
    private final ModalityType modalityType; // nullable; required only when parent Day modality is CROSSFIT
    private final int sets;
    private final String reps;
    private final String weight;       // nullable
    private final Integer restSeconds; // nullable
    private final String notes;        // nullable

    public Exercise(String name, ModalityType modalityType, int sets, String reps,
                    String weight, Integer restSeconds, String notes) {
        this.name = name;
        this.modalityType = modalityType;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.restSeconds = restSeconds;
        this.notes = notes;
    }

    public String getName() { return name; }
    public ModalityType getModalityType() { return modalityType; }
    public int getSets() { return sets; }
    public String getReps() { return reps; }
    public String getWeight() { return weight; }
    public Integer getRestSeconds() { return restSeconds; }
    public String getNotes() { return notes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exercise e)) return false;
        return sets == e.sets
                && Objects.equals(name, e.name)
                && modalityType == e.modalityType
                && Objects.equals(reps, e.reps)
                && Objects.equals(weight, e.weight)
                && Objects.equals(restSeconds, e.restSeconds)
                && Objects.equals(notes, e.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, modalityType, sets, reps, weight, restSeconds, notes);
    }
}
