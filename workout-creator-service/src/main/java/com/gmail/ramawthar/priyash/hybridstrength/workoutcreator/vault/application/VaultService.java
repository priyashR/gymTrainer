package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.ProgramAccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.SourceDayNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.SourceProgramNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.UploadValidationException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.WorkoutNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Day;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignmentType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DaySummary;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.ManualProgram;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application service implementing all vault CRUD and search use cases.
 * Orchestrates domain logic, delegates persistence to the outbound port,
 * and reuses {@link UploadParser} for JSON validation on updates.
 */
@Service
@Transactional
public class VaultService implements ListProgramsUseCase, GetProgramUseCase,
        UpdateProgramUseCase, DeleteProgramUseCase, CopyProgramUseCase, SearchProgramsUseCase,
        CreateManualProgramUseCase, GetProgramDaysUseCase {

    private final VaultProgramRepository vaultProgramRepository;
    private final UploadParser uploadParser;
    private final ObjectMapper objectMapper;

    public VaultService(VaultProgramRepository vaultProgramRepository, UploadParser uploadParser,
                        ObjectMapper objectMapper) {
        this.vaultProgramRepository = vaultProgramRepository;
        this.uploadParser = uploadParser;
        this.objectMapper = objectMapper;
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

    @Override
    public UUID createManualProgram(CreateManualProgramCommand command) {
        List<DayAssignment> resolvedAssignments = new ArrayList<>();

        for (DayAssignment day : command.dayAssignments()) {
            if (day.type() == DayAssignmentType.COPIED_DAY) {
                // Validate source program exists and belongs to user
                VaultProgram sourceProgram = vaultProgramRepository
                        .findByIdAndOwner(day.sourceProgramId(), command.ownerUserId())
                        .orElseThrow(() -> new SourceProgramNotFoundException(day.sourceProgramId()));

                // Find the specified week and day
                Day sourceDay = findDay(sourceProgram, day.sourceWeekNumber(), day.sourceDayNumber());

                // Serialize the day structure to JSON snapshot
                String snapshotJson;
                try {
                    snapshotJson = objectMapper.writeValueAsString(sourceDay);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize source day to JSON", e);
                }

                resolvedAssignments.add(new DayAssignment(
                        day.dayNumber(), DayAssignmentType.COPIED_DAY,
                        null, null, snapshotJson,
                        day.sourceProgramId(), day.sourceWeekNumber(), day.sourceDayNumber()
                ));
            } else if (day.type() == DayAssignmentType.WORKOUT) {
                // Verify workout-type day assignments reference workouts in the user's vault
                if (!vaultProgramRepository.existsByIdAndOwner(day.workoutId(), command.ownerUserId())) {
                    throw new WorkoutNotFoundException(day.workoutId());
                }
                resolvedAssignments.add(day);
            } else {
                // ACTIVITY type — pass through as-is
                resolvedAssignments.add(day);
            }
        }

        // Construct domain object
        Instant now = Instant.now();
        UUID programId = UUID.randomUUID();
        ManualProgram manualProgram = new ManualProgram(
                programId,
                command.programName(),
                command.ownerUserId(),
                ContentSource.MANUAL,
                resolvedAssignments,
                now,
                now
        );

        // Persist via outbound port
        vaultProgramRepository.saveManualProgram(manualProgram);

        return programId;
    }

    /**
     * Finds a specific day within a vault program by week number and day number.
     *
     * @param program    the source vault program containing weeks and days
     * @param weekNumber the 1-based week number to find
     * @param dayNumber  the 1-based day number within the week to find
     * @return the Day domain object at the specified position
     * @throws SourceDayNotFoundException if the week/day combination does not exist
     */
    private Day findDay(VaultProgram program, int weekNumber, int dayNumber) {
        return program.program().getWeeks().stream()
                .filter(w -> w.getWeekNumber() == weekNumber)
                .flatMap(w -> w.getDays().stream())
                .filter(d -> d.getDayNumber() == dayNumber)
                .findFirst()
                .orElseThrow(() -> new SourceDayNotFoundException(weekNumber, dayNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DaySummary> getProgramDays(UUID programId, String ownerUserId) {
        VaultProgram program = vaultProgramRepository.findByIdAndOwner(programId, ownerUserId)
                .orElseThrow(ProgramAccessDeniedException::new);

        return program.program().getWeeks().stream()
                .flatMap(week -> week.getDays().stream()
                        .map(day -> new DaySummary(week.getWeekNumber(), day.getDayNumber(),
                                day.getLabel(), day.getFocusArea())))
                .toList();
    }
}
