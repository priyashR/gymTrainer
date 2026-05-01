package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationScope;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.TrainingStyle;

import java.util.List;

/**
 * API response DTO for a multi-day program.
 */
public record ProgramDto(
        String name,
        String description,
        GenerationScope scope,
        List<TrainingStyle> trainingStyles,
        List<WorkoutDto> workouts
) {}
