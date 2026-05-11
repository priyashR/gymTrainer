package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;

import java.util.UUID;

/**
 * Inbound port for updating a program's content via full JSON replacement.
 */
public interface UpdateProgramUseCase {

    VaultItem updateProgram(UUID programId, String rawJson, String ownerUserId);
}
