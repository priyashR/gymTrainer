package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A single training session definition containing one or more Sections.
 * <p>
 * Pure Java — no framework imports.
 */
public class Workout {

    private final String name;
    private final String description;
    private final TrainingStyle trainingStyle;
    private final List<Section> sections;

    /**
     * Creates a new Workout.
     *
     * @param name          workout name; must not be null or blank
     * @param description   workout description; must not be null or blank
     * @param trainingStyle training style; must not be null
     * @param sections      sections in this workout; must not be null or empty
     */
    public Workout(String name, String description, TrainingStyle trainingStyle, List<Section> sections) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workout name must not be null or blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Workout description must not be null or blank");
        }
        Objects.requireNonNull(trainingStyle, "Workout trainingStyle must not be null");
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("Workout sections must not be null or empty");
        }
        this.name = name;
        this.description = description;
        this.trainingStyle = trainingStyle;
        this.sections = Collections.unmodifiableList(List.copyOf(sections));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TrainingStyle getTrainingStyle() {
        return trainingStyle;
    }

    public List<Section> getSections() {
        return sections;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Workout workout = (Workout) o;
        return Objects.equals(name, workout.name)
                && Objects.equals(description, workout.description)
                && trainingStyle == workout.trainingStyle
                && Objects.equals(sections, workout.sections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, trainingStyle, sections);
    }

    @Override
    public String toString() {
        return "Workout{name='" + name + "', description='" + description
                + "', trainingStyle=" + trainingStyle + ", sections=" + sections + "}";
    }
}
