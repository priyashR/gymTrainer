package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a manual training program.
 * Contains the program name and an ordered list of day assignments.
 */
public record CreateManualProgramRequest(
        @NotBlank(message = "Program name is required")
        @Size(max = 255, message = "Program name must not exceed 255 characters")
        String programName,

        @NotNull(message = "At least one day assignment is required")
        @Size(min = 1, message = "At least one day assignment is required")
        List<@Valid DayAssignmentRequest> days
) {}
