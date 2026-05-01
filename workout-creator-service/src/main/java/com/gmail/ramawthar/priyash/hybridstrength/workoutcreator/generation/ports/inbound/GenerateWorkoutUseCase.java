package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationCommand;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationResult;

/**
 * Inbound port for AI-powered workout/program generation.
 * <p>
 * Accepts a {@link GenerationCommand} containing the user's description, scope,
 * and training styles, and returns a {@link GenerationResult} with the raw Gemini
 * response and either a parsed domain object or a parsing error message.
 */
public interface GenerateWorkoutUseCase {

    /**
     * Generates a workout or program based on the given command.
     *
     * @param command the generation parameters; must not be null
     * @return the generation result containing raw text and parsed domain object (or error)
     */
    GenerationResult generate(GenerationCommand command);
}
