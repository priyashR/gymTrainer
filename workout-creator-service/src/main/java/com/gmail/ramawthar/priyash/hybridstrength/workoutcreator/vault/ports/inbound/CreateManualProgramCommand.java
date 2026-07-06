package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignment;

import java.util.List;

/**
 * Command object for creating a manual program.
 * Carries the validated input from the inbound adapter to the application service.
 *
 * @param programName    the user-provided name for the program
 * @param ownerUserId    the authenticated user's ID (resolved from JWT)
 * @param dayAssignments the ordered list of day assignments for the program
 */
public record CreateManualProgramCommand(
        String programName,
        String ownerUserId,
        List<DayAssignment> dayAssignments
) {}
