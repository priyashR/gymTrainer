package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.EnrollmentStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.SkipRecord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ProgramEnrollment domain object.
 * Tests domain logic in isolation — no Spring context, no mocks.
 */
class ProgramEnrollmentTest {

    private static final String USER_ID = "user-456";
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final String PROGRAM_NAME = "Strength Builder 12-Week";

    // --- Test helpers ---

    private ProgramEnrollment createActiveEnrollment(int totalWeeks, int totalDaysPerWeek) {
        return ProgramEnrollment.create(
                UUID.randomUUID(), USER_ID, PROGRAM_ID, PROGRAM_NAME,
                totalWeeks, totalDaysPerWeek, Instant.now()
        );
    }

    private ProgramEnrollment createEnrollmentAtPosition(int currentWeek, int currentDay,
                                                         int totalWeeks, int totalDaysPerWeek) {
        return new ProgramEnrollment.Builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .programId(PROGRAM_ID)
                .programName(PROGRAM_NAME)
                .currentWeek(currentWeek)
                .currentDay(currentDay)
                .totalWeeks(totalWeeks)
                .totalDaysPerWeek(totalDaysPerWeek)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .skips(new java.util.ArrayList<>())
                .build();
    }

    // --- Tests ---

    @Nested
    @DisplayName("ProgramEnrollment.create")
    class Create {

        @Test
        @DisplayName("create_ValidInputs_CreatesActiveEnrollmentAtDay1")
        void create_ValidInputs_CreatesActiveEnrollmentAtDay1() {
            Instant now = Instant.now();

            ProgramEnrollment enrollment = ProgramEnrollment.create(
                    UUID.randomUUID(), USER_ID, PROGRAM_ID, PROGRAM_NAME,
                    4, 5, now
            );

            assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus());
            assertEquals(1, enrollment.getCurrentWeek());
            assertEquals(1, enrollment.getCurrentDay());
            assertEquals(4, enrollment.getTotalWeeks());
            assertEquals(5, enrollment.getTotalDaysPerWeek());
            assertEquals(now, enrollment.getEnrolledAt());
            assertNull(enrollment.getCompletedAt());
            assertTrue(enrollment.getSkips().isEmpty());
        }

        @Test
        @DisplayName("create_ZeroTotalWeeks_ThrowsIllegalArgument")
        void create_ZeroTotalWeeks_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    ProgramEnrollment.create(UUID.randomUUID(), USER_ID, PROGRAM_ID, PROGRAM_NAME,
                            0, 5, Instant.now())
            );
        }

        @Test
        @DisplayName("create_ZeroDaysPerWeek_ThrowsIllegalArgument")
        void create_ZeroDaysPerWeek_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    ProgramEnrollment.create(UUID.randomUUID(), USER_ID, PROGRAM_ID, PROGRAM_NAME,
                            4, 0, Instant.now())
            );
        }
    }

    @Nested
    @DisplayName("advanceDay")
    class AdvanceDay {

        @Test
        @DisplayName("advanceDay_MiddleOfWeek_IncrementsDayByOne")
        void advanceDay_MiddleOfWeek_IncrementsDayByOne() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(2, 3, 4, 5);

            enrollment.advanceDay();

            assertEquals(2, enrollment.getCurrentWeek());
            assertEquals(4, enrollment.getCurrentDay());
            assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus());
        }

        @Test
        @DisplayName("advanceDay_LastDayOfWeek_RollsOverToNextWeek")
        void advanceDay_LastDayOfWeek_RollsOverToNextWeek() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(2, 5, 4, 5);

            enrollment.advanceDay();

            assertEquals(3, enrollment.getCurrentWeek());
            assertEquals(1, enrollment.getCurrentDay());
            assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus());
        }

        @Test
        @DisplayName("advanceDay_LastDayOfLastWeek_MarksCompleted")
        void advanceDay_LastDayOfLastWeek_MarksCompleted() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(4, 5, 4, 5);

            enrollment.advanceDay();

            assertEquals(EnrollmentStatus.COMPLETED, enrollment.getStatus());
            assertNotNull(enrollment.getCompletedAt());
        }

        @Test
        @DisplayName("advanceDay_FirstDayFirstWeek_AdvancesToDay2")
        void advanceDay_FirstDayFirstWeek_AdvancesToDay2() {
            ProgramEnrollment enrollment = createActiveEnrollment(4, 5);

            enrollment.advanceDay();

            assertEquals(1, enrollment.getCurrentWeek());
            assertEquals(2, enrollment.getCurrentDay());
        }

        @Test
        @DisplayName("advanceDay_CompletedEnrollment_ThrowsIllegalState")
        void advanceDay_CompletedEnrollment_ThrowsIllegalState() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(4, 5, 4, 5);
            enrollment.advanceDay(); // marks COMPLETED

            assertThrows(IllegalStateException.class, enrollment::advanceDay);
        }

        @Test
        @DisplayName("advanceDay_ReplacedEnrollment_ThrowsIllegalState")
        void advanceDay_ReplacedEnrollment_ThrowsIllegalState() {
            ProgramEnrollment enrollment = createActiveEnrollment(4, 5);
            enrollment.markReplaced();

            assertThrows(IllegalStateException.class, enrollment::advanceDay);
        }

        @Test
        @DisplayName("advanceDay_SingleWeekProgram_MarksCompletedOnLastDay")
        void advanceDay_SingleWeekProgram_MarksCompletedOnLastDay() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(1, 3, 1, 3);

            enrollment.advanceDay();

            assertEquals(EnrollmentStatus.COMPLETED, enrollment.getStatus());
        }
    }

    @Nested
    @DisplayName("skipDay")
    class SkipDay {

        @Test
        @DisplayName("skipDay_MiddleOfWeek_AdvancesAndCreatesSkipRecord")
        void skipDay_MiddleOfWeek_AdvancesAndCreatesSkipRecord() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(2, 3, 4, 5);
            Instant now = Instant.now();

            enrollment.skipDay(now);

            // Day pointer advanced
            assertEquals(2, enrollment.getCurrentWeek());
            assertEquals(4, enrollment.getCurrentDay());

            // Skip record created
            assertEquals(1, enrollment.getSkips().size());
            SkipRecord record = enrollment.getSkips().get(0);
            assertEquals(2, record.getWeekNumber());
            assertEquals(3, record.getDayNumber());
            assertEquals(now, record.getSkippedAt());
        }

        @Test
        @DisplayName("skipDay_LastDayOfWeek_RollsOverAndCreatesSkipRecord")
        void skipDay_LastDayOfWeek_RollsOverAndCreatesSkipRecord() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(2, 5, 4, 5);
            Instant now = Instant.now();

            enrollment.skipDay(now);

            assertEquals(3, enrollment.getCurrentWeek());
            assertEquals(1, enrollment.getCurrentDay());

            SkipRecord record = enrollment.getSkips().get(0);
            assertEquals(2, record.getWeekNumber());
            assertEquals(5, record.getDayNumber());
        }

        @Test
        @DisplayName("skipDay_LastDayOfLastWeek_MarksCompletedAndCreatesSkipRecord")
        void skipDay_LastDayOfLastWeek_MarksCompletedAndCreatesSkipRecord() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(4, 5, 4, 5);
            Instant now = Instant.now();

            enrollment.skipDay(now);

            assertEquals(EnrollmentStatus.COMPLETED, enrollment.getStatus());
            assertEquals(1, enrollment.getSkips().size());
            SkipRecord record = enrollment.getSkips().get(0);
            assertEquals(4, record.getWeekNumber());
            assertEquals(5, record.getDayNumber());
        }

        @Test
        @DisplayName("skipDay_MultipleSkips_AccumulatesRecords")
        void skipDay_MultipleSkips_AccumulatesRecords() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(1, 1, 4, 5);
            Instant first = Instant.now();
            Instant second = first.plusSeconds(60);

            enrollment.skipDay(first);
            enrollment.skipDay(second);

            assertEquals(2, enrollment.getSkips().size());
            assertEquals(1, enrollment.getSkips().get(0).getWeekNumber());
            assertEquals(1, enrollment.getSkips().get(0).getDayNumber());
            assertEquals(1, enrollment.getSkips().get(1).getWeekNumber());
            assertEquals(2, enrollment.getSkips().get(1).getDayNumber());
        }

        @Test
        @DisplayName("skipDay_NullTimestamp_ThrowsIllegalArgument")
        void skipDay_NullTimestamp_ThrowsIllegalArgument() {
            ProgramEnrollment enrollment = createActiveEnrollment(4, 5);

            assertThrows(IllegalArgumentException.class, () ->
                    enrollment.skipDay(null)
            );
        }

        @Test
        @DisplayName("skipDay_CompletedEnrollment_ThrowsIllegalState")
        void skipDay_CompletedEnrollment_ThrowsIllegalState() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(4, 5, 4, 5);
            enrollment.advanceDay(); // marks COMPLETED

            assertThrows(IllegalStateException.class, () ->
                    enrollment.skipDay(Instant.now())
            );
        }
    }

    @Nested
    @DisplayName("markReplaced")
    class MarkReplaced {

        @Test
        @DisplayName("markReplaced_ActiveEnrollment_TransitionsToReplaced")
        void markReplaced_ActiveEnrollment_TransitionsToReplaced() {
            ProgramEnrollment enrollment = createActiveEnrollment(4, 5);

            enrollment.markReplaced();

            assertEquals(EnrollmentStatus.REPLACED, enrollment.getStatus());
        }

        @Test
        @DisplayName("markReplaced_CompletedEnrollment_ThrowsIllegalState")
        void markReplaced_CompletedEnrollment_ThrowsIllegalState() {
            ProgramEnrollment enrollment = createEnrollmentAtPosition(4, 5, 4, 5);
            enrollment.advanceDay(); // marks COMPLETED

            assertThrows(IllegalStateException.class, enrollment::markReplaced);
        }

        @Test
        @DisplayName("markReplaced_AlreadyReplaced_ThrowsIllegalState")
        void markReplaced_AlreadyReplaced_ThrowsIllegalState() {
            ProgramEnrollment enrollment = createActiveEnrollment(4, 5);
            enrollment.markReplaced();

            assertThrows(IllegalStateException.class, enrollment::markReplaced);
        }
    }

    @Nested
    @DisplayName("markCompleted")
    class MarkCompleted {

        @Test
        @DisplayName("markCompleted_ActiveEnrollment_TransitionsToCompleted")
        void markCompleted_ActiveEnrollment_TransitionsToCompleted() {
            ProgramEnrollment enrollment = createActiveEnrollment(4, 5);
            Instant now = Instant.now();

            enrollment.markCompleted(now);

            assertEquals(EnrollmentStatus.COMPLETED, enrollment.getStatus());
            assertEquals(now, enrollment.getCompletedAt());
        }

        @Test
        @DisplayName("markCompleted_NullTimestamp_ThrowsIllegalArgument")
        void markCompleted_NullTimestamp_ThrowsIllegalArgument() {
            ProgramEnrollment enrollment = createActiveEnrollment(4, 5);

            assertThrows(IllegalArgumentException.class, () ->
                    enrollment.markCompleted(null)
            );
        }
    }
}
