package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.TrainingStyle;

import java.util.List;

/**
 * API response DTO for a single workout.
 */
public record WorkoutDto(
        String name,
        String description,
        TrainingStyle trainingStyle,
        List<SectionDto> sections
) {}
