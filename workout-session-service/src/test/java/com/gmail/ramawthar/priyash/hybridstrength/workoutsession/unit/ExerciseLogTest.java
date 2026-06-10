package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SetLog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExerciseLog.addSetLog() method.
 * Tests set log appending and ordering — no Spring context, no mocks.
 */
class ExerciseLogTest {

    private static final Instant BASE_TIME = Instant.parse("2026-01-15T10:00:00Z");

    @Nested
    @DisplayName("addSetLog")
    class AddSetLog {

        @Test
        @DisplayName("addSetLog_SingleSet_AppendsToList")
        void addSetLog_SingleSet_AppendsToList() {
            ExerciseLog exerciseLog = new ExerciseLog(0, "Back Squat");
            SetLog setLog = new SetLog(1, new BigDecimal("100"), 5, new BigDecimal("7.0"), BASE_TIME);

            exerciseLog.addSetLog(setLog);

            List<SetLog> setLogs = exerciseLog.getSetLogs();
            assertEquals(1, setLogs.size());
            assertEquals(1, setLogs.get(0).getSetNumber());
            assertEquals(0, new BigDecimal("100").compareTo(setLogs.get(0).getWeight()));
        }

        @Test
        @DisplayName("addSetLog_MultipleSets_MaintainsChronologicalOrder")
        void addSetLog_MultipleSets_MaintainsChronologicalOrder() {
            ExerciseLog exerciseLog = new ExerciseLog(0, "Bench Press");

            SetLog set1 = new SetLog(1, new BigDecimal("80"), 8, null, BASE_TIME);
            SetLog set2 = new SetLog(2, new BigDecimal("85"), 6, new BigDecimal("7.5"), BASE_TIME.plusSeconds(120));
            SetLog set3 = new SetLog(3, new BigDecimal("90"), 5, new BigDecimal("8.5"), BASE_TIME.plusSeconds(240));

            exerciseLog.addSetLog(set1);
            exerciseLog.addSetLog(set2);
            exerciseLog.addSetLog(set3);

            List<SetLog> setLogs = exerciseLog.getSetLogs();
            assertEquals(3, setLogs.size());

            // Verify chronological order via loggedAt
            assertTrue(setLogs.get(0).getLoggedAt().compareTo(setLogs.get(1).getLoggedAt()) <= 0);
            assertTrue(setLogs.get(1).getLoggedAt().compareTo(setLogs.get(2).getLoggedAt()) <= 0);
        }

        @Test
        @DisplayName("addSetLog_MultipleSets_SetNumbersAreSequential")
        void addSetLog_MultipleSets_SetNumbersAreSequential() {
            ExerciseLog exerciseLog = new ExerciseLog(0, "Deadlift");

            for (int i = 1; i <= 5; i++) {
                SetLog setLog = new SetLog(i, new BigDecimal("120"), 5, null, BASE_TIME.plusSeconds(i * 60L));
                exerciseLog.addSetLog(setLog);
            }

            List<SetLog> setLogs = exerciseLog.getSetLogs();
            assertEquals(5, setLogs.size());

            for (int i = 0; i < 5; i++) {
                assertEquals(i + 1, setLogs.get(i).getSetNumber());
            }
        }

        @Test
        @DisplayName("addSetLog_NullSetLog_ThrowsIllegalArgument")
        void addSetLog_NullSetLog_ThrowsIllegalArgument() {
            ExerciseLog exerciseLog = new ExerciseLog(0, "Squat");

            assertThrows(IllegalArgumentException.class, () ->
                    exerciseLog.addSetLog(null)
            );
        }

        @Test
        @DisplayName("getSetLogs_ReturnsUnmodifiableList")
        void getSetLogs_ReturnsUnmodifiableList() {
            ExerciseLog exerciseLog = new ExerciseLog(0, "Squat");
            SetLog setLog = new SetLog(1, new BigDecimal("100"), 5, null, BASE_TIME);
            exerciseLog.addSetLog(setLog);

            List<SetLog> setLogs = exerciseLog.getSetLogs();

            assertThrows(UnsupportedOperationException.class, () ->
                    setLogs.add(new SetLog(2, new BigDecimal("100"), 5, null, BASE_TIME))
            );
        }

        @Test
        @DisplayName("addSetLog_AfterReconstitution_AppendsToExistingLogs")
        void addSetLog_AfterReconstitution_AppendsToExistingLogs() {
            // Simulate reconstitution from persistence with existing set logs
            SetLog existingSet = new SetLog(1, new BigDecimal("80"), 8, null, BASE_TIME);
            ExerciseLog exerciseLog = new ExerciseLog(0, "OHP", null, false, null, List.of(existingSet));

            SetLog newSet = new SetLog(2, new BigDecimal("85"), 6, new BigDecimal("8.0"), BASE_TIME.plusSeconds(120));
            exerciseLog.addSetLog(newSet);

            List<SetLog> setLogs = exerciseLog.getSetLogs();
            assertEquals(2, setLogs.size());
            assertEquals(1, setLogs.get(0).getSetNumber());
            assertEquals(2, setLogs.get(1).getSetNumber());
        }
    }
}
