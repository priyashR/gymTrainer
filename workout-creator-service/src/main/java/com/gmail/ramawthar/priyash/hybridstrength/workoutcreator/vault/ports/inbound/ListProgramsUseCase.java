package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port for listing all programs in a user's vault.
 */
public interface ListProgramsUseCase {

    Page<VaultItem> listPrograms(String ownerUserId, Pageable pageable);
}
