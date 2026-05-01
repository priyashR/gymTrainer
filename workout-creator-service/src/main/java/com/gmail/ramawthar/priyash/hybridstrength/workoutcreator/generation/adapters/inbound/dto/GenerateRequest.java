package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.validation.ValidDayScopeTrainingStyles;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationScope;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.TrainingStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * API request DTO for the generation endpoint.
 * <p>
 * Validated with Jakarta Bean Validation. The custom {@link ValidDayScopeTrainingStyles}
 * constraint enforces that DAY scope requires exactly one training style.
 */
@ValidDayScopeTrainingStyles
public record GenerateRequest(
        @NotBlank(message = "must not be blank")
        String description,

        @NotNull(message = "must not be null")
        GenerationScope scope,

        @NotEmpty(message = "must not be empty")
        List<TrainingStyle> trainingStyles
) {}
