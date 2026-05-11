package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;

import java.util.UUID;

/**
 * Inbound port for copying/duplicating a program in the vault.
 */
public interface CopyProgramUseCase {

    VaultItem copyProgram(UUID programId, String ownerUserId);
}
