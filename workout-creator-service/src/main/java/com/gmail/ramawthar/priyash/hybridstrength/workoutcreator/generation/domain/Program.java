package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A structured collection of Workouts spanning one or more weeks.
 * <p>
 * Pure Java — no framework imports.
 */
public class Program {

    private final String name;
    private final String description;
    private final GenerationScope scope;
    private final List<TrainingStyle> trainingStyles;
    private final List<Workout> workouts;

    /**
     * Creates a new Program.
     *
     * @param name           program name; must not be null or blank
     * @param description    program description; must not be null or blank
     * @param scope          generation scope (WEEK or FOUR_WEEK); must not be null
     * @param trainingStyles training styles used; must not be null or empty
     * @param workouts       workouts in this program, ordered by day; must not be null or empty
     */
    public Program(String name, String description, GenerationScope scope,
                   List<TrainingStyle> trainingStyles, List<Workout> workouts) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Program name must not be null or blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Program description must not be null or blank");
        }
        Objects.requireNonNull(scope, "Program scope must not be null");
        if (trainingStyles == null || trainingStyles.isEmpty()) {
            throw new IllegalArgumentException("Program trainingStyles must not be null or empty");
        }
        if (workouts == null || workouts.isEmpty()) {
            throw new IllegalArgumentException("Program workouts must not be null or empty");
        }
        this.name = name;
        this.description = description;
        this.scope = scope;
        this.trainingStyles = Collections.unmodifiableList(List.copyOf(trainingStyles));
        this.workouts = Collections.unmodifiableList(List.copyOf(workouts));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public GenerationScope getScope() {
        return scope;
    }

    public List<TrainingStyle> getTrainingStyles() {
        return trainingStyles;
    }

    public List<Workout> getWorkouts() {
        return workouts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Program program = (Program) o;
        return Objects.equals(name, program.name)
                && Objects.equals(description, program.description)
                && scope == program.scope
                && Objects.equals(trainingStyles, program.trainingStyles)
                && Objects.equals(workouts, program.workouts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, scope, trainingStyles, workouts);
    }

    @Override
    public String toString() {
        return "Program{name='" + name + "', description='" + description
                + "', scope=" + scope + ", trainingStyles=" + trainingStyles
                + ", workouts=" + workouts + "}";
    }
}
