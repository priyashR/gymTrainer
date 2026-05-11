package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.SearchCriteria;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for vault program persistence operations.
 * Implemented by the JPA adapter in the outbound adapters layer.
 */
public interface VaultProgramRepository {

    Page<VaultItem> findAllByOwner(String ownerUserId, Pageable pageable);

    Optional<VaultProgram> findByIdAndOwner(UUID id, String ownerUserId);

    VaultItem save(VaultProgram program);

    void deleteByIdAndOwner(UUID id, String ownerUserId);

    boolean existsByIdAndOwner(UUID id, String ownerUserId);

    Page<VaultItem> search(SearchCriteria criteria, String ownerUserId, Pageable pageable);
}
