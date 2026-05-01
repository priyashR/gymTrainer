package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.application;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.ContentSanitiser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationCommand;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationScope;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.ParsingException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.Program;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.PromptBuilder;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.Workout;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.WorkoutParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.ports.inbound.GenerateWorkoutUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.ports.outbound.GeminiClient;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Application service that orchestrates AI-powered workout/program generation.
 * <p>
 * Flow: build prompt → call Gemini → sanitise response → parse into domain object.
 * On parse failure, returns the raw Gemini text with a human-readable error message
 * rather than throwing an exception (graceful degradation).
 */
@Service
public class GenerationService implements GenerateWorkoutUseCase {

    private final GeminiClient geminiClient;

    public GenerationService(GeminiClient geminiClient) {
        this.geminiClient = Objects.requireNonNull(geminiClient, "GeminiClient must not be null");
    }

    @Override
    public GenerationResult generate(GenerationCommand command) {
        Objects.requireNonNull(command, "GenerationCommand must not be null");

        String prompt = PromptBuilder.buildPrompt(
                command.getDescription(),
                command.getScope(),
                command.getTrainingStyles()
        );

        String rawResponse = geminiClient.generate(prompt);
        String sanitised = ContentSanitiser.sanitise(rawResponse);

        try {
            if (command.getScope() == GenerationScope.DAY) {
                Workout workout = WorkoutParser.parseWorkout(sanitised, command.getScope());
                return GenerationResult.successWithWorkout(rawResponse, workout);
            } else {
                Program program = WorkoutParser.parseProgram(sanitised, command.getScope());
                return GenerationResult.successWithProgram(rawResponse, program);
            }
        } catch (ParsingException e) {
            return GenerationResult.failure(rawResponse, e.getMessage());
        }
    }
}
