package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the input to the workout/program generation use case.
 * <p>
 * Pure Java — no framework imports.
 */
public class GenerationCommand {

    private final UUID userId;
    private final String description;
    private final GenerationScope scope;
    private final List<TrainingStyle> trainingStyles;

    /**
     * Creates a new GenerationCommand.
     *
     * @param userId         authenticated user's ID; must not be null
     * @param description    natural language description; must not be null or blank
     * @param scope          generation scope; must not be null
     * @param trainingStyles requested training styles; must not be null or empty
     */
    public GenerationCommand(UUID userId, String description, GenerationScope scope,
                             List<TrainingStyle> trainingStyles) {
        Objects.requireNonNull(userId, "GenerationCommand userId must not be null");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("GenerationCommand description must not be null or blank");
        }
        Objects.requireNonNull(scope, "GenerationCommand scope must not be null");
        if (trainingStyles == null || trainingStyles.isEmpty()) {
            throw new IllegalArgumentException("GenerationCommand trainingStyles must not be null or empty");
        }
        this.userId = userId;
        this.description = description;
        this.scope = scope;
        this.trainingStyles = Collections.unmodifiableList(List.copyOf(trainingStyles));
    }

    public UUID getUserId() {
        return userId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenerationCommand that = (GenerationCommand) o;
        return Objects.equals(userId, that.userId)
                && Objects.equals(description, that.description)
                && scope == that.scope
                && Objects.equals(trainingStyles, that.trainingStyles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, description, scope, trainingStyles);
    }

    @Override
    public String toString() {
        return "GenerationCommand{userId=" + userId + ", description='" + description
                + "', scope=" + scope + ", trainingStyles=" + trainingStyles + "}";
    }
}
