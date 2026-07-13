package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.vault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.SourceDayNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.SourceProgramNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.*;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignmentType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.ManualProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound.CreateManualProgramCommand;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VaultService#createManualProgram(CreateManualProgramCommand)}
 * focusing on the COPIED_DAY assignment type.
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 */
@ExtendWith(MockitoExtension.class)
class CopyDayServiceTest {

    @Mock
    private VaultProgramRepository vaultProgramRepository;

    @Mock
    private UploadParser uploadParser;

    private VaultService service;
    private ObjectMapper objectMapper;

    private static final String OWNER_USER_ID = "user-owner-copy-test";
    private static final UUID SOURCE_PROGRAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new VaultService(vaultProgramRepository, uploadParser, objectMapper);
    }

    @Test
    void CreateManualProgram_WithCopiedDay_PersistsSnapshotAndProvenance() throws Exception {
        // Arrange: source program with a known day structure
        Exercise exercise = new Exercise("Bench Press", null, 4, "8-10", "80kg", 120, "Pause at bottom");
        Section section = new Section("Tier 1: Compound", SectionType.STRENGTH, "Sets/Reps", null, List.of(exercise));
        WarmCoolEntry warmUp = new WarmCoolEntry("Arm Circles", "20 each direction");
        WarmCoolEntry coolDown = new WarmCoolEntry("Chest Stretch", "30 seconds each side");
        Day sourceDay = new Day(1, "Push Day", "Push", Modality.HYPERTROPHY,
                List.of(warmUp), List.of(section), List.of(coolDown), null);
        Week week = new Week(1, List.of(sourceDay));
        Program program = new Program("Source Program", 1, "Muscle gain", List.of("Barbell", "Bench"), List.of(week));

        VaultProgram sourceVaultProgram = new VaultProgram(
                SOURCE_PROGRAM_ID, program, OWNER_USER_ID,
                ContentSource.AI_GENERATED, Instant.now(), Instant.now()
        );

        when(vaultProgramRepository.findByIdAndOwner(SOURCE_PROGRAM_ID, OWNER_USER_ID))
                .thenReturn(Optional.of(sourceVaultProgram));

        // Command with a single copied_day assignment
        DayAssignment copiedDayAssignment = new DayAssignment(
                1, DayAssignmentType.COPIED_DAY, null, null, null,
                SOURCE_PROGRAM_ID, 1, 1
        );
        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "My Copied Plan", OWNER_USER_ID, List.of(copiedDayAssignment)
        );

        // Act
        UUID result = service.createManualProgram(command);

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<ManualProgram> captor = ArgumentCaptor.forClass(ManualProgram.class);
        verify(vaultProgramRepository).saveManualProgram(captor.capture());
        ManualProgram saved = captor.getValue();

        assertThat(saved.id()).isEqualTo(result);
        assertThat(saved.name()).isEqualTo("My Copied Plan");
        assertThat(saved.ownerUserId()).isEqualTo(OWNER_USER_ID);
        assertThat(saved.dayAssignments()).hasSize(1);

        DayAssignment savedDay = saved.dayAssignments().get(0);
        assertThat(savedDay.dayNumber()).isEqualTo(1);
        assertThat(savedDay.type()).isEqualTo(DayAssignmentType.COPIED_DAY);
        assertThat(savedDay.sourceProgramId()).isEqualTo(SOURCE_PROGRAM_ID);
        assertThat(savedDay.sourceWeekNumber()).isEqualTo(1);
        assertThat(savedDay.sourceDayNumber()).isEqualTo(1);

        // Verify snapshot contains the serialized day structure
        assertThat(savedDay.snapshotData()).isNotNull();
        JsonNode snapshot = objectMapper.readTree(savedDay.snapshotData());
        assertThat(snapshot.get("label").asText()).isEqualTo("Push Day");
        assertThat(snapshot.get("focusArea").asText()).isEqualTo("Push");
        assertThat(snapshot.get("modality").asText()).isEqualTo("HYPERTROPHY");
        assertThat(snapshot.get("warmUp")).hasSize(1);
        assertThat(snapshot.get("sections")).hasSize(1);
        assertThat(snapshot.get("coolDown")).hasSize(1);
        // Verify exercise details in snapshot
        JsonNode exerciseNode = snapshot.get("sections").get(0).get("exercises").get(0);
        assertThat(exerciseNode.get("name").asText()).isEqualTo("Bench Press");
        assertThat(exerciseNode.get("sets").asInt()).isEqualTo(4);
    }

    @Test
    void CreateManualProgram_SourceProgramNotFound_ThrowsSourceProgramNotFoundException() {
        // Arrange: source program does not exist in user's vault
        UUID nonExistentProgramId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        when(vaultProgramRepository.findByIdAndOwner(nonExistentProgramId, OWNER_USER_ID))
                .thenReturn(Optional.empty());

        DayAssignment copiedDayAssignment = new DayAssignment(
                1, DayAssignmentType.COPIED_DAY, null, null, null,
                nonExistentProgramId, 1, 1
        );
        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "My Plan", OWNER_USER_ID, List.of(copiedDayAssignment)
        );

        // Act & Assert
        assertThatThrownBy(() -> service.createManualProgram(command))
                .isInstanceOf(SourceProgramNotFoundException.class)
                .hasMessageContaining(nonExistentProgramId.toString());

        verify(vaultProgramRepository, never()).saveManualProgram(any());
    }

    @Test
    void CreateManualProgram_SourceDayNotFound_ThrowsSourceDayNotFoundException() {
        // Arrange: source program exists but referenced week/day combination does not
        Day day1 = new Day(1, "Push Day", "Push", Modality.HYPERTROPHY,
                List.of(), List.of(), List.of(), null);
        Week week1 = new Week(1, List.of(day1));
        Program program = new Program("Source", 1, "Strength", List.of(), List.of(week1));

        VaultProgram sourceVaultProgram = new VaultProgram(
                SOURCE_PROGRAM_ID, program, OWNER_USER_ID,
                ContentSource.AI_GENERATED, Instant.now(), Instant.now()
        );

        when(vaultProgramRepository.findByIdAndOwner(SOURCE_PROGRAM_ID, OWNER_USER_ID))
                .thenReturn(Optional.of(sourceVaultProgram));

        // Reference week 2, day 3 which doesn't exist in the source program
        DayAssignment copiedDayAssignment = new DayAssignment(
                1, DayAssignmentType.COPIED_DAY, null, null, null,
                SOURCE_PROGRAM_ID, 2, 3
        );
        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "My Plan", OWNER_USER_ID, List.of(copiedDayAssignment)
        );

        // Act & Assert
        assertThatThrownBy(() -> service.createManualProgram(command))
                .isInstanceOf(SourceDayNotFoundException.class)
                .hasMessageContaining("week 2")
                .hasMessageContaining("day 3");

        verify(vaultProgramRepository, never()).saveManualProgram(any());
    }

    @Test
    void CreateManualProgram_MixedTypes_ProcessesAllCorrectly() throws Exception {
        // Arrange: command with both ACTIVITY and COPIED_DAY assignments
        Exercise exercise = new Exercise("Squat", null, 5, "5", "100kg", 180, null);
        Section section = new Section("Main Lift", SectionType.STRENGTH, "Sets/Reps", null, List.of(exercise));
        Day sourceDay = new Day(2, "Leg Day", "Legs", Modality.STRENGTH,
                List.of(), List.of(section), List.of(), null);
        Week week = new Week(1, List.of(
                new Day(1, "Push Day", "Push", Modality.HYPERTROPHY, List.of(), List.of(), List.of(), null),
                sourceDay
        ));
        Program program = new Program("Source Program", 1, "Strength", List.of("Barbell"), List.of(week));

        VaultProgram sourceVaultProgram = new VaultProgram(
                SOURCE_PROGRAM_ID, program, OWNER_USER_ID,
                ContentSource.AI_GENERATED, Instant.now(), Instant.now()
        );

        when(vaultProgramRepository.findByIdAndOwner(SOURCE_PROGRAM_ID, OWNER_USER_ID))
                .thenReturn(Optional.of(sourceVaultProgram));

        List<DayAssignment> days = List.of(
                new DayAssignment(1, DayAssignmentType.ACTIVITY, null, "Running"),
                new DayAssignment(2, DayAssignmentType.COPIED_DAY, null, null, null,
                        SOURCE_PROGRAM_ID, 1, 2),
                new DayAssignment(3, DayAssignmentType.ACTIVITY, null, "Swimming")
        );
        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "Mixed Plan", OWNER_USER_ID, days
        );

        // Act
        UUID result = service.createManualProgram(command);

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<ManualProgram> captor = ArgumentCaptor.forClass(ManualProgram.class);
        verify(vaultProgramRepository).saveManualProgram(captor.capture());
        ManualProgram saved = captor.getValue();

        assertThat(saved.dayAssignments()).hasSize(3);

        // Day 1: ACTIVITY - passed through as-is
        DayAssignment day1 = saved.dayAssignments().get(0);
        assertThat(day1.type()).isEqualTo(DayAssignmentType.ACTIVITY);
        assertThat(day1.activityType()).isEqualTo("Running");
        assertThat(day1.snapshotData()).isNull();
        assertThat(day1.sourceProgramId()).isNull();

        // Day 2: COPIED_DAY - resolved with snapshot and provenance
        DayAssignment day2 = saved.dayAssignments().get(1);
        assertThat(day2.type()).isEqualTo(DayAssignmentType.COPIED_DAY);
        assertThat(day2.sourceProgramId()).isEqualTo(SOURCE_PROGRAM_ID);
        assertThat(day2.sourceWeekNumber()).isEqualTo(1);
        assertThat(day2.sourceDayNumber()).isEqualTo(2);
        assertThat(day2.snapshotData()).isNotNull();

        // Verify the snapshot contains the correct day
        JsonNode snapshot = objectMapper.readTree(day2.snapshotData());
        assertThat(snapshot.get("label").asText()).isEqualTo("Leg Day");
        assertThat(snapshot.get("focusArea").asText()).isEqualTo("Legs");
        assertThat(snapshot.get("sections")).hasSize(1);
        assertThat(snapshot.get("sections").get(0).get("exercises").get(0).get("name").asText()).isEqualTo("Squat");

        // Day 3: ACTIVITY - passed through as-is
        DayAssignment day3 = saved.dayAssignments().get(2);
        assertThat(day3.type()).isEqualTo(DayAssignmentType.ACTIVITY);
        assertThat(day3.activityType()).isEqualTo("Swimming");
        assertThat(day3.snapshotData()).isNull();
    }
}
