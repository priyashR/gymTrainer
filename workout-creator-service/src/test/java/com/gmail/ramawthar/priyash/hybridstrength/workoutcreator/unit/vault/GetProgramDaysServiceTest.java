package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.ProgramAccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DaySummary;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VaultService#getProgramDays(UUID, String)}.
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 */
@ExtendWith(MockitoExtension.class)
class GetProgramDaysServiceTest {

    @Mock
    private VaultProgramRepository vaultProgramRepository;

    @Mock
    private UploadParser uploadParser;

    private VaultService service;

    private static final String OWNER_USER_ID = "user-owner-aaa";
    private static final UUID PROGRAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        service = new VaultService(vaultProgramRepository, uploadParser, new ObjectMapper());
    }

    @Test
    void GetProgramDays_ValidProgram_ReturnsDaySummaries() {
        // Arrange: program with 2 weeks, each containing 2 days
        Day day1 = new Day(1, "Push Day", "Push", Modality.HYPERTROPHY,
                List.of(), List.of(), List.of(), null);
        Day day2 = new Day(2, "Pull Day", "Pull", Modality.STRENGTH,
                List.of(), List.of(), List.of(), null);
        Day day3 = new Day(1, "Legs", "Lower", Modality.HYPERTROPHY,
                List.of(), List.of(), List.of(), null);
        Day day4 = new Day(2, "Upper", "Upper Body", Modality.STRENGTH,
                List.of(), List.of(), List.of(), null);

        Week week1 = new Week(1, List.of(day1, day2));
        Week week2 = new Week(2, List.of(day3, day4));

        Program program = new Program("Test Program", 2, "Build muscle",
                List.of("Barbell", "Dumbbell"), List.of(week1, week2));
        VaultProgram vaultProgram = new VaultProgram(PROGRAM_ID, program, OWNER_USER_ID,
                ContentSource.UPLOADED, CREATED_AT, UPDATED_AT);

        when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_USER_ID))
                .thenReturn(Optional.of(vaultProgram));

        // Act
        List<DaySummary> result = service.getProgramDays(PROGRAM_ID, OWNER_USER_ID);

        // Assert: all 4 days returned with correct week/day numbers, labels, and focus areas
        assertThat(result).hasSize(4);

        assertThat(result).containsExactly(
                new DaySummary(1, 1, "Push Day", "Push"),
                new DaySummary(1, 2, "Pull Day", "Pull"),
                new DaySummary(2, 1, "Legs", "Lower"),
                new DaySummary(2, 2, "Upper", "Upper Body")
        );
    }

    @Test
    void GetProgramDays_EmptyProgram_ReturnsEmptyList() {
        // Arrange: program with no weeks (empty weeks list)
        Program program = new Program("Empty Program", 0, "N/A", List.of(), List.of());
        VaultProgram vaultProgram = new VaultProgram(PROGRAM_ID, program, OWNER_USER_ID,
                ContentSource.MANUAL, CREATED_AT, UPDATED_AT);

        when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_USER_ID))
                .thenReturn(Optional.of(vaultProgram));

        // Act
        List<DaySummary> result = service.getProgramDays(PROGRAM_ID, OWNER_USER_ID);

        // Assert: empty list returned
        assertThat(result).isEmpty();
    }

    @Test
    void GetProgramDays_ProgramNotOwned_ThrowsProgramAccessDeniedException() {
        // Arrange: repository returns empty (program doesn't exist or belongs to another user)
        when(vaultProgramRepository.findByIdAndOwner(PROGRAM_ID, OWNER_USER_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getProgramDays(PROGRAM_ID, OWNER_USER_ID))
                .isInstanceOf(ProgramAccessDeniedException.class)
                .hasMessage("Program not found or access denied");
    }
}
