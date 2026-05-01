package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto;

/**
 * API response DTO for an exercise within a section.
 */
public record ExerciseDto(
        String name,
        int sets,
        String reps,
        String weight,
        Integer restSeconds
) {}
