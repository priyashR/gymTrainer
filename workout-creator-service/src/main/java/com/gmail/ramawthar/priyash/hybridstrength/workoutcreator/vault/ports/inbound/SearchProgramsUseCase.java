package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.SearchCriteria;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port for searching and filtering programs in the vault.
 */
public interface SearchProgramsUseCase {

    Page<VaultItem> searchPrograms(SearchCriteria criteria, String ownerUserId, Pageable pageable);
}
