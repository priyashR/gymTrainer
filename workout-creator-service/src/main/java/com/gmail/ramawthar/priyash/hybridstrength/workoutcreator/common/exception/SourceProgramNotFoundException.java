package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception;

import java.util.UUID;

/**
 * Thrown when a copied_day assignment references a source program that does not
 * exist in the authenticated user's vault. Used during manual program creation
 * to signal that the specified source program cannot be found.
 */
public class SourceProgramNotFoundException extends RuntimeException {

    private final UUID sourceProgramId;

    public SourceProgramNotFoundException(UUID sourceProgramId) {
        super("Source program not found in vault: " + sourceProgramId);
        this.sourceProgramId = sourceProgramId;
    }

    public UUID getSourceProgramId() {
        return sourceProgramId;
    }
}
