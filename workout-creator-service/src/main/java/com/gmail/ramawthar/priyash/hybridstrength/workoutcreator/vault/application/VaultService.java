package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.ProgramAccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.UploadValidationException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.SearchCriteria;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service implementing all vault CRUD and search use cases.
 * Orchestrates domain logic, delegates persistence to the outbound port,
 * and reuses {@link UploadParser} for JSON validation on updates.
 */
@Service
@Transactional
public class VaultService implements ListProgramsUseCase, GetProgramUseCase,
        UpdateProgramUseCase, DeleteProgramUseCase, CopyProgramUseCase, SearchProgramsUseCase {

    private final VaultProgramRepository vaultProgramRepository;
    private final UploadParser uploadParser;

    public VaultService(VaultProgramRepository vaultProgramRepository, UploadParser uploadParser) {
        this.vaultProgramRepository = vaultProgramRepository;
        this.uploadParser = uploadParser;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VaultItem> listPrograms(String ownerUserId, Pageable pageable) {
        return vaultProgramRepository.findAllByOwner(ownerUserId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public VaultProgram getProgram(UUID programId, String ownerUserId) {
        return vaultProgramRepository.findByIdAndOwner(programId, ownerUserId)
                .orElseThrow(ProgramAccessDeniedException::new);
    }

    @Override
    public VaultItem updateProgram(UUID programId, String rawJson, String ownerUserId) {
        // Validate the JSON against Upload_Schema
        ParseResult result = uploadParser.parse(rawJson);
        if (result instanceof ParseResult.Failure failure) {
            throw new UploadValidationException(failure.errors());
        }

        ParseResult.Success success = (ParseResult.Success) result;
        Program newProgram = success.program();

        // Find existing program — throws 403 if not found or not owned
        VaultProgram existing = vaultProgramRepository.findByIdAndOwner(programId, ownerUserId)
                .orElseThrow(ProgramAccessDeniedException::new);

        // Replace content while preserving immutable fields (id, ownerUserId, contentSource)
        VaultProgram updated = new VaultProgram(
                existing.id(),
                newProgram,
                existing.ownerUserId(),
                existing.contentSource(),
                existing.createdAt(),
                Instant.now()
        );

        return vaultProgramRepository.save(updated);
    }

    @Override
    public void deleteProgram(UUID programId, String ownerUserId) {
        if (!vaultProgramRepository.existsByIdAndOwner(programId, ownerUserId)) {
            throw new ProgramAccessDeniedException();
        }
        vaultProgramRepository.deleteByIdAndOwner(programId, ownerUserId);
    }

    @Override
    public VaultItem copyProgram(UUID programId, String ownerUserId) {
        VaultProgram original = vaultProgramRepository.findByIdAndOwner(programId, ownerUserId)
                .orElseThrow(ProgramAccessDeniedException::new);

        Program originalProgram = original.program();

        // Deep copy with new name — domain objects are immutable so reconstruction is safe
        Program copiedProgram = new Program(
                originalProgram.getName() + " (Copy)",
                originalProgram.getDurationWeeks(),
                originalProgram.getGoal(),
                originalProgram.getEquipmentProfile(),
                originalProgram.getWeeks()
        );

        Instant now = Instant.now();
        VaultProgram copy = new VaultProgram(
                UUID.randomUUID(),
                copiedProgram,
                ownerUserId,
                ContentSource.MANUAL,
                now,
                now
        );

        return vaultProgramRepository.save(copy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VaultItem> searchPrograms(SearchCriteria criteria, String ownerUserId, Pageable pageable) {
        // Reject empty/blank query when q parameter is explicitly provided
        if (criteria.query() != null && criteria.query().isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }
        return vaultProgramRepository.search(criteria, ownerUserId, pageable);
    }
}
