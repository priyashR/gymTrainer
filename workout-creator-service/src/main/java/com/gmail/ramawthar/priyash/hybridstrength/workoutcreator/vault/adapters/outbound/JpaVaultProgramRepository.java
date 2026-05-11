package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.SearchCriteria;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-based implementation of the {@link VaultProgramRepository} outbound port.
 * Delegates to {@link ProgramSpringDataRepository} for persistence and uses
 * {@link ProgramEntityMapper} for entity↔domain mapping.
 *
 * <p>Handles full replacement on update by clearing existing weeks and rebuilding
 * the entity tree from the updated domain object.
 */
@Repository
public class JpaVaultProgramRepository implements VaultProgramRepository {

    private final ProgramSpringDataRepository programRepo;

    public JpaVaultProgramRepository(ProgramSpringDataRepository programRepo) {
        this.programRepo = programRepo;
    }

    @Override
    public Page<VaultItem> findAllByOwner(String ownerUserId, Pageable pageable) {
        return programRepo.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, pageable)
                .map(ProgramEntityMapper::toVaultItem);
    }

    @Override
    public Optional<VaultProgram> findByIdAndOwner(UUID id, String ownerUserId) {
        return programRepo.findByIdAndOwnerUserId(id, ownerUserId)
                .map(ProgramEntityMapper::toVaultProgram);
    }

    @Override
    public VaultItem save(VaultProgram program) {
        ProgramJpaEntity entity = programRepo.findById(program.id()).orElse(null);

        if (entity != null) {
            // Update: rebuild content on existing entity, preserving id and immutable fields
            ProgramEntityMapper.rebuildEntityContent(entity, program.program());
            entity.setUpdatedAt(program.updatedAt());
        } else {
            // New: create entity from domain, set metadata fields
            entity = ProgramEntityMapper.toEntity(program.program());
            entity.setId(program.id());
            entity.setOwnerUserId(program.ownerUserId());
            entity.setContentSource(program.contentSource());
            entity.setCreatedAt(program.createdAt());
            entity.setUpdatedAt(program.updatedAt());
        }

        ProgramJpaEntity saved = programRepo.save(entity);
        return ProgramEntityMapper.toVaultItem(saved);
    }

    @Override
    @Transactional
    public void deleteByIdAndOwner(UUID id, String ownerUserId) {
        programRepo.deleteByIdAndOwnerUserId(id, ownerUserId);
    }

    @Override
    public boolean existsByIdAndOwner(UUID id, String ownerUserId) {
        return programRepo.existsByIdAndOwnerUserId(id, ownerUserId);
    }

    @Override
    public Page<VaultItem> search(SearchCriteria criteria, String ownerUserId, Pageable pageable) {
        String query = criteria.hasKeyword() ? criteria.query() : null;
        String focusArea = criteria.hasFocusArea() ? criteria.focusArea() : null;
        String modality = criteria.hasModality() ? criteria.modality() : null;

        return programRepo.searchPrograms(ownerUserId, query, focusArea, modality, pageable)
                .map(ProgramEntityMapper::toVaultItem);
    }
}
