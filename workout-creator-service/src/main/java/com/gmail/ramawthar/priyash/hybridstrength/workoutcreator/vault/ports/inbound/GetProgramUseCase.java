package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;

import java.util.UUID;

/**
 * Inbound port for retrieving the full details of a single program.
 */
public interface GetProgramUseCase {

    VaultProgram getProgram(UUID programId, String ownerUserId);
}
