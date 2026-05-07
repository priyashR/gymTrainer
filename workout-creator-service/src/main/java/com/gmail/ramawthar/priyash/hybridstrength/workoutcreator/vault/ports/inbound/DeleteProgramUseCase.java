package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound;

import java.util.UUID;

/**
 * Inbound port for deleting a program from the vault.
 */
public interface DeleteProgramUseCase {

    void deleteProgram(UUID programId, String ownerUserId);
}
