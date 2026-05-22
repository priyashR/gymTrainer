package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.AccessDeniedException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.exception.EnrollmentNotFoundException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.application.ProgressionService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.EnrollmentStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.outbound.EnrollmentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProgressionService application service.
 * EnrollmentRepository is mocked — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class ProgressionServiceTest {

    private static final String USER_ID = "user-prog-123";
    private static final String OTHER_USER_ID = "user-other";
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final String PROGRAM_NAME = "Hypertrophy 8-Week";

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private ProgressionService progressionService;

    @BeforeEach
    void setUp() {
        progressionService = new ProgressionService(enrollmentRepository);
    }

    // --- Test helpers ---

    private ProgramEnrollment createActiveEnrollment(String userId, int currentWeek, int currentDay,
                                                     int totalWeeks, int totalDaysPerWeek) {
        return new ProgramEnrollment.Builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .programId(PROGRAM_ID)
                .programName(PROGRAM_NAME)
                .currentWeek(currentWeek)
                .currentDay(currentDay)
                .totalWeeks(totalWeeks)
                .totalDaysPerWeek(totalDaysPerWeek)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .skips(new ArrayList<>())
                .build();
    }

    // --- Tests ---

    @Nested
    @DisplayName("enrollProgram")
    class EnrollProgram {

        @Test
        @DisplayName("enrollProgram_NoExistingEnrollment_CreatesNewAtDay1")
        void enrollProgram_NoExistingEnrollment_CreatesNewAtDay1() {
            when(enrollmentRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
            when(enrollmentRepository.save(any(ProgramEnrollment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ProgramEnrollment result = progressionService.enrollProgram(
                    USER_ID, PROGRAM_ID, PROGRAM_NAME, 8, 5
            );

            assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
            assertEquals(1, result.getCurrentWeek());
            assertEquals(1, result.getCurrentDay());
            assertEquals(8, result.getTotalWeeks());
            assertEquals(5, result.getTotalDaysPerWeek());
            assertEquals(USER_ID, result.getUserId());
            assertEquals(PROGRAM_ID, result.getProgramId());
            assertEquals(PROGRAM_NAME, result.getProgramName());

            // Only one save call (the new enrollment)
            verify(enrollmentRepository, times(1)).save(any(ProgramEnrollment.class));
        }

        @Test
        @DisplayName("enrollProgram_ExistingActiveEnrollment_ReplacesExistingAndCreatesNew")
        void enrollProgram_ExistingActiveEnrollment_ReplacesExistingAndCreatesNew() {
            ProgramEnrollment existing = createActiveEnrollment(USER_ID, 3, 4, 6, 5);
            when(enrollmentRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(enrollmentRepository.save(any(ProgramEnrollment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UUID newProgramId = UUID.randomUUID();
            ProgramEnrollment result = progressionService.enrollProgram(
                    USER_ID, newProgramId, "New Program", 12, 4
            );

            // Verify existing was marked as REPLACED and saved
            assertEquals(EnrollmentStatus.REPLACED, existing.getStatus());

            // Verify new enrollment is at day 1
            assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
            assertEquals(1, result.getCurrentWeek());
            assertEquals(1, result.getCurrentDay());
            assertEquals(newProgramId, result.getProgramId());

            // Two saves: one for the replaced enrollment, one for the new one
            verify(enrollmentRepository, times(2)).save(any(ProgramEnrollment.class));
        }

        @Test
        @DisplayName("enrollProgram_ExistingActiveEnrollment_ExistingMarkedReplaced")
        void enrollProgram_ExistingActiveEnrollment_ExistingMarkedReplaced() {
            ProgramEnrollment existing = createActiveEnrollment(USER_ID, 2, 3, 4, 5);
            when(enrollmentRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(enrollmentRepository.save(any(ProgramEnrollment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<ProgramEnrollment> captor = ArgumentCaptor.forClass(ProgramEnrollment.class);

            progressionService.enrollProgram(USER_ID, PROGRAM_ID, PROGRAM_NAME, 8, 5);

            verify(enrollmentRepository, times(2)).save(captor.capture());

            // First save should be the replaced enrollment
            ProgramEnrollment firstSaved = captor.getAllValues().get(0);
            assertEquals(EnrollmentStatus.REPLACED, firstSaved.getStatus());
            assertEquals(existing.getId(), firstSaved.getId());

            // Second save should be the new enrollment
            ProgramEnrollment secondSaved = captor.getAllValues().get(1);
            assertEquals(EnrollmentStatus.ACTIVE, secondSaved.getStatus());
            assertEquals(1, secondSaved.getCurrentWeek());
            assertEquals(1, secondSaved.getCurrentDay());
        }
    }

    @Nested
    @DisplayName("advanceDay")
    class AdvanceDay {

        @Test
        @DisplayName("advanceDay_MiddleOfWeek_IncrementsDayByOne")
        void advanceDay_MiddleOfWeek_IncrementsDayByOne() {
            ProgramEnrollment enrollment = createActiveEnrollment(USER_ID, 2, 3, 4, 5);
            UUID enrollmentId = enrollment.getId();

            when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(ProgramEnrollment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ProgramEnrollment result = progressionService.advanceDay(enrollmentId, USER_ID);

            assertEquals(2, result.getCurrentWeek());
            assertEquals(4, result.getCurrentDay());
            verify(enrollmentRepository).save(enrollment);
        }

        @Test
        @DisplayName("advanceDay_LastDayOfWeek_RollsOverToNextWeek")
        void advanceDay_LastDayOfWeek_RollsOverToNextWeek() {
            ProgramEnrollment enrollment = createActiveEnrollment(USER_ID, 2, 5, 4, 5);
            UUID enrollmentId = enrollment.getId();

            when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(ProgramEnrollment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ProgramEnrollment result = progressionService.advanceDay(enrollmentId, USER_ID);

            assertEquals(3, result.getCurrentWeek());
            assertEquals(1, result.getCurrentDay());
        }

        @Test
        @DisplayName("advanceDay_LastDayOfLastWeek_MarksCompleted")
        void advanceDay_LastDayOfLastWeek_MarksCompleted() {
            ProgramEnrollment enrollment = createActiveEnrollment(USER_ID, 4, 5, 4, 5);
            UUID enrollmentId = enrollment.getId();

            when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(ProgramEnrollment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ProgramEnrollment result = progressionService.advanceDay(enrollmentId, USER_ID);

            assertEquals(EnrollmentStatus.COMPLETED, result.getStatus());
        }

        @Test
        @DisplayName("advanceDay_EnrollmentNotFound_ThrowsEnrollmentNotFound")
        void advanceDay_EnrollmentNotFound_ThrowsEnrollmentNotFound() {
            UUID enrollmentId = UUID.randomUUID();
            when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

            assertThrows(EnrollmentNotFoundException.class, () ->
                    progressionService.advanceDay(enrollmentId, USER_ID)
            );
        }

        @Test
        @DisplayName("advanceDay_WrongUser_ThrowsAccessDenied")
        void advanceDay_WrongUser_ThrowsAccessDenied() {
            ProgramEnrollment enrollment = createActiveEnrollment(USER_ID, 2, 3, 4, 5);
            UUID enrollmentId = enrollment.getId();

            when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

            assertThrows(AccessDeniedException.class, () ->
                    progressionService.advanceDay(enrollmentId, OTHER_USER_ID)
            );
        }
    }

    @Nested
    @DisplayName("skipDay")
    class SkipDay {

        @Test
        @DisplayName("skipDay_ValidRequest_AdvancesAndCreatesSkipRecord")
        void skipDay_ValidRequest_AdvancesAndCreatesSkipRecord() {
            ProgramEnrollment enrollment = createActiveEnrollment(USER_ID, 2, 3, 4, 5);
            UUID enrollmentId = enrollment.getId();

            when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(ProgramEnrollment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ProgramEnrollment result = progressionService.skipDay(enrollmentId, USER_ID);

            // Day advanced
            assertEquals(2, result.getCurrentWeek());
            assertEquals(4, result.getCurrentDay());

            // Skip record created
            assertEquals(1, result.getSkips().size());
            assertEquals(2, result.getSkips().get(0).getWeekNumber());
            assertEquals(3, result.getSkips().get(0).getDayNumber());
            assertNotNull(result.getSkips().get(0).getSkippedAt());

            verify(enrollmentRepository).save(enrollment);
        }

        @Test
        @DisplayName("skipDay_WrongUser_ThrowsAccessDenied")
        void skipDay_WrongUser_ThrowsAccessDenied() {
            ProgramEnrollment enrollment = createActiveEnrollment(USER_ID, 2, 3, 4, 5);
            UUID enrollmentId = enrollment.getId();

            when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

            assertThrows(AccessDeniedException.class, () ->
                    progressionService.skipDay(enrollmentId, OTHER_USER_ID)
            );
        }

        @Test
        @DisplayName("skipDay_EnrollmentNotFound_ThrowsEnrollmentNotFound")
        void skipDay_EnrollmentNotFound_ThrowsEnrollmentNotFound() {
            UUID enrollmentId = UUID.randomUUID();
            when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

            assertThrows(EnrollmentNotFoundException.class, () ->
                    progressionService.skipDay(enrollmentId, USER_ID)
            );
        }
    }

    @Nested
    @DisplayName("getActiveEnrollment")
    class GetActiveEnrollment {

        @Test
        @DisplayName("getActiveEnrollment_EnrollmentExists_ReturnsEnrollment")
        void getActiveEnrollment_EnrollmentExists_ReturnsEnrollment() {
            ProgramEnrollment enrollment = createActiveEnrollment(USER_ID, 3, 2, 8, 5);
            when(enrollmentRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(enrollment));

            Optional<ProgramEnrollment> result = progressionService.getActiveEnrollment(USER_ID);

            assertTrue(result.isPresent());
            assertEquals(enrollment.getId(), result.get().getId());
            assertEquals(3, result.get().getCurrentWeek());
            assertEquals(2, result.get().getCurrentDay());
        }

        @Test
        @DisplayName("getActiveEnrollment_NoEnrollment_ReturnsEmpty")
        void getActiveEnrollment_NoEnrollment_ReturnsEmpty() {
            when(enrollmentRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());

            Optional<ProgramEnrollment> result = progressionService.getActiveEnrollment(USER_ID);

            assertTrue(result.isEmpty());
        }
    }
}
