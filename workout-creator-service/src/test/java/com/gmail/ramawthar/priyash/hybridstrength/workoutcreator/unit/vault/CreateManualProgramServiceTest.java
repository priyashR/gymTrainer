package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.WorkoutNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.application.VaultService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DayAssignmentType;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.ManualProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound.CreateManualProgramCommand;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.outbound.VaultProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VaultService#createManualProgram(CreateManualProgramCommand)}.
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 */
@ExtendWith(MockitoExtension.class)
class CreateManualProgramServiceTest {

    @Mock
    private VaultProgramRepository vaultProgramRepository;

    @Mock
    private UploadParser uploadParser;

    private VaultService service;

    private static final String OWNER_USER_ID = "user-owner-aaa";
    private static final UUID WORKOUT_ID_1 = UUID.fromString("aaaa1111-1111-1111-1111-111111111111");
    private static final UUID WORKOUT_ID_2 = UUID.fromString("bbbb2222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        service = new VaultService(vaultProgramRepository, uploadParser, new ObjectMapper());
    }

    @Test
    void CreateManualProgram_ValidCommand_ReturnsProgramId() {
        // Arrange: command with a mix of workout and activity day assignments
        List<DayAssignment> days = List.of(
                new DayAssignment(1, DayAssignmentType.WORKOUT, WORKOUT_ID_1, null),
                new DayAssignment(2, DayAssignmentType.ACTIVITY, null, "Swimming"),
                new DayAssignment(3, DayAssignmentType.WORKOUT, WORKOUT_ID_2, null)
        );
        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "My Training Plan", OWNER_USER_ID, days
        );

        when(vaultProgramRepository.existsByIdAndOwner(WORKOUT_ID_1, OWNER_USER_ID)).thenReturn(true);
        when(vaultProgramRepository.existsByIdAndOwner(WORKOUT_ID_2, OWNER_USER_ID)).thenReturn(true);

        // Act
        UUID result = service.createManualProgram(command);

        // Assert: a non-null UUID is returned and saveManualProgram was called
        assertThat(result).isNotNull();

        ArgumentCaptor<ManualProgram> captor = ArgumentCaptor.forClass(ManualProgram.class);
        verify(vaultProgramRepository).saveManualProgram(captor.capture());
        ManualProgram saved = captor.getValue();
        assertThat(saved.id()).isEqualTo(result);
        assertThat(saved.name()).isEqualTo("My Training Plan");
        assertThat(saved.ownerUserId()).isEqualTo(OWNER_USER_ID);
    }

    @Test
    void CreateManualProgram_WorkoutNotInVault_ThrowsWorkoutNotFoundException() {
        // Arrange: one workout doesn't exist in the user's vault
        UUID missingWorkoutId = UUID.fromString("cccc3333-3333-3333-3333-333333333333");
        List<DayAssignment> days = List.of(
                new DayAssignment(1, DayAssignmentType.WORKOUT, WORKOUT_ID_1, null),
                new DayAssignment(2, DayAssignmentType.WORKOUT, missingWorkoutId, null)
        );
        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "My Plan", OWNER_USER_ID, days
        );

        when(vaultProgramRepository.existsByIdAndOwner(WORKOUT_ID_1, OWNER_USER_ID)).thenReturn(true);
        when(vaultProgramRepository.existsByIdAndOwner(missingWorkoutId, OWNER_USER_ID)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.createManualProgram(command))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessageContaining(missingWorkoutId.toString());

        // saveManualProgram should never be called when validation fails
        verify(vaultProgramRepository, never()).saveManualProgram(any());
    }

    @Test
    void CreateManualProgram_WorkoutInOtherUsersVault_ThrowsWorkoutNotFoundException() {
        // Arrange: workout exists but belongs to another user — repository returns false for this user
        List<DayAssignment> days = List.of(
                new DayAssignment(1, DayAssignmentType.WORKOUT, WORKOUT_ID_1, null)
        );
        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "My Plan", OWNER_USER_ID, days
        );

        // The workout exists in another user's vault, but not in OWNER_USER_ID's vault
        when(vaultProgramRepository.existsByIdAndOwner(WORKOUT_ID_1, OWNER_USER_ID)).thenReturn(false);

        // Act & Assert: same exception type — no information leakage about ownership
        assertThatThrownBy(() -> service.createManualProgram(command))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessageContaining(WORKOUT_ID_1.toString());

        verify(vaultProgramRepository, never()).saveManualProgram(any());
    }

    @Test
    void CreateManualProgram_DatabaseError_PropagatesAsRuntimeException() {
        // Arrange: valid command where workouts exist, but save throws
        List<DayAssignment> days = List.of(
                new DayAssignment(1, DayAssignmentType.WORKOUT, WORKOUT_ID_1, null)
        );
        CreateManualProgramCommand command = new CreateManualProgramCommand(
                "My Plan", OWNER_USER_ID, days
        );

        when(vaultProgramRepository.existsByIdAndOwner(WORKOUT_ID_1, OWNER_USER_ID)).thenReturn(true);
        doThrow(new RuntimeException("Database connection lost"))
                .when(vaultProgramRepository).saveManualProgram(any(ManualProgram.class));

        // Act & Assert: RuntimeException propagates without being wrapped
        assertThatThrownBy(() -> service.createManualProgram(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection lost");
    }
}
