package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto.ExerciseDto;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto.GenerateRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto.GenerateResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto.ProgramDto;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto.SectionDto;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto.WorkoutDto;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.Exercise;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationCommand;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.Program;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.Section;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.Workout;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.ports.inbound.GenerateWorkoutUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Inbound adapter for AI-powered workout/program generation.
 * <p>
 * Validates the request, extracts the authenticated user's ID from the JWT,
 * delegates to the {@link GenerateWorkoutUseCase}, and maps the result to
 * the API response shape.
 */
@RestController
@RequestMapping("/api/v1/workouts")
public class GenerationController {

    private static final Logger log = LoggerFactory.getLogger(GenerationController.class);

    private final GenerateWorkoutUseCase generateWorkoutUseCase;

    public GenerationController(GenerateWorkoutUseCase generateWorkoutUseCase) {
        this.generateWorkoutUseCase = generateWorkoutUseCase;
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerateResponse> generate(
            @Valid @RequestBody GenerateRequest request,
            @AuthenticationPrincipal UUID userId) {

        log.info("Generation request received: scope={}, trainingStyles={}",
                request.scope(), request.trainingStyles());

        GenerationCommand command = new GenerationCommand(
                userId,
                request.description(),
                request.scope(),
                request.trainingStyles()
        );

        GenerationResult result = generateWorkoutUseCase.generate(command);

        GenerateResponse response = toResponse(result);

        log.info("Generation completed: parsingError={}", result.getParsingError() != null);

        return ResponseEntity.ok(response);
    }

    private GenerateResponse toResponse(GenerationResult result) {
        return new GenerateResponse(
                result.getRawGeminiResponse(),
                result.getWorkout() != null ? toWorkoutDto(result.getWorkout()) : null,
                result.getProgram() != null ? toProgramDto(result.getProgram()) : null,
                result.getParsingError()
        );
    }

    private WorkoutDto toWorkoutDto(Workout workout) {
        return new WorkoutDto(
                workout.getName(),
                workout.getDescription(),
                workout.getTrainingStyle(),
                workout.getSections().stream().map(this::toSectionDto).toList()
        );
    }

    private ProgramDto toProgramDto(Program program) {
        return new ProgramDto(
                program.getName(),
                program.getDescription(),
                program.getScope(),
                program.getTrainingStyles(),
                program.getWorkouts().stream().map(this::toWorkoutDto).toList()
        );
    }

    private SectionDto toSectionDto(Section section) {
        return new SectionDto(
                section.getName(),
                section.getType(),
                section.getExercises().stream().map(this::toExerciseDto).toList(),
                section.getTimeCapMinutes(),
                section.getIntervalSeconds(),
                section.getTotalRounds(),
                section.getWorkIntervalSeconds(),
                section.getRestIntervalSeconds()
        );
    }

    private ExerciseDto toExerciseDto(Exercise exercise) {
        return new ExerciseDto(
                exercise.getName(),
                exercise.getSets(),
                exercise.getReps(),
                exercise.getWeight(),
                exercise.getRestSeconds()
        );
    }
}
