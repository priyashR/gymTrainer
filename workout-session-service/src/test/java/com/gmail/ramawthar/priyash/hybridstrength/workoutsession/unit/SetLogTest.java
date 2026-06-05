package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SetLog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SetLog value object.
 * Tests construction validation — no Spring context, no mocks.
 */
class SetLogTest {

    private static final Instant NOW = Instant.now();

    @Nested
    @DisplayName("Valid construction")
    class ValidConstruction {

        @Test
        @DisplayName("constructor_AllValidFields_CreatesSetLog")
        void constructor_AllValidFields_CreatesSetLog() {
            SetLog setLog = new SetLog(1, new BigDecimal("100.0"), 5, new BigDecimal("8.0"), NOW);

            assertEquals(1, setLog.getSetNumber());
            assertEquals(0, new BigDecimal("100.0").compareTo(setLog.getWeight()));
            assertEquals(5, setLog.getRepetitions());
            assertEquals(0, new BigDecimal("8.0").compareTo(setLog.getRpe()));
            assertEquals(NOW, setLog.getLoggedAt());
        }

        @Test
        @DisplayName("constructor_NullRpe_CreatesSetLogWithNullRpe")
        void constructor_NullRpe_CreatesSetLogWithNullRpe() {
            SetLog setLog = new SetLog(1, new BigDecimal("60.5"), 10, null, NOW);

            assertEquals(1, setLog.getSetNumber());
            assertEquals(0, new BigDecimal("60.5").compareTo(setLog.getWeight()));
            assertEquals(10, setLog.getRepetitions());
            assertNull(setLog.getRpe());
            assertEquals(NOW, setLog.getLoggedAt());
        }

        @Test
        @DisplayName("constructor_MinValidRpe_CreatesSetLog")
        void constructor_MinValidRpe_CreatesSetLog() {
            SetLog setLog = new SetLog(1, new BigDecimal("50"), 8, new BigDecimal("1.0"), NOW);

            assertEquals(0, new BigDecimal("1.0").compareTo(setLog.getRpe()));
        }

        @Test
        @DisplayName("constructor_MaxValidRpe_CreatesSetLog")
        void constructor_MaxValidRpe_CreatesSetLog() {
            SetLog setLog = new SetLog(1, new BigDecimal("50"), 8, new BigDecimal("10.0"), NOW);

            assertEquals(0, new BigDecimal("10.0").compareTo(setLog.getRpe()));
        }

        @Test
        @DisplayName("constructor_HalfPointRpe_CreatesSetLog")
        void constructor_HalfPointRpe_CreatesSetLog() {
            SetLog setLog = new SetLog(1, new BigDecimal("50"), 8, new BigDecimal("7.5"), NOW);

            assertEquals(0, new BigDecimal("7.5").compareTo(setLog.getRpe()));
        }

        @Test
        @DisplayName("constructor_SmallWeight_CreatesSetLog")
        void constructor_SmallWeight_CreatesSetLog() {
            SetLog setLog = new SetLog(1, new BigDecimal("0.01"), 1, null, NOW);

            assertEquals(0, new BigDecimal("0.01").compareTo(setLog.getWeight()));
            assertEquals(1, setLog.getRepetitions());
        }
    }

    @Nested
    @DisplayName("Invalid weight")
    class InvalidWeight {

        @Test
        @DisplayName("constructor_ZeroWeight_ThrowsIllegalArgument")
        void constructor_ZeroWeight_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, BigDecimal.ZERO, 5, null, NOW)
            );
            assertTrue(ex.getMessage().contains("Weight must be greater than zero"));
        }

        @Test
        @DisplayName("constructor_NegativeWeight_ThrowsIllegalArgument")
        void constructor_NegativeWeight_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, new BigDecimal("-10.0"), 5, null, NOW)
            );
            assertTrue(ex.getMessage().contains("Weight must be greater than zero"));
        }

        @Test
        @DisplayName("constructor_NullWeight_ThrowsIllegalArgument")
        void constructor_NullWeight_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, null, 5, null, NOW)
            );
        }
    }

    @Nested
    @DisplayName("Invalid repetitions")
    class InvalidRepetitions {

        @Test
        @DisplayName("constructor_ZeroRepetitions_ThrowsIllegalArgument")
        void constructor_ZeroRepetitions_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, new BigDecimal("100"), 0, null, NOW)
            );
            assertTrue(ex.getMessage().contains("Repetitions must be at least 1"));
        }

        @Test
        @DisplayName("constructor_NegativeRepetitions_ThrowsIllegalArgument")
        void constructor_NegativeRepetitions_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, new BigDecimal("100"), -3, null, NOW)
            );
            assertTrue(ex.getMessage().contains("Repetitions must be at least 1"));
        }
    }

    @Nested
    @DisplayName("Invalid RPE")
    class InvalidRpe {

        @Test
        @DisplayName("constructor_RpeBelowMinimum_ThrowsIllegalArgument")
        void constructor_RpeBelowMinimum_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, new BigDecimal("100"), 5, new BigDecimal("0.5"), NOW)
            );
            assertTrue(ex.getMessage().contains("RPE must be between 1.0 and 10.0"));
        }

        @Test
        @DisplayName("constructor_RpeAboveMaximum_ThrowsIllegalArgument")
        void constructor_RpeAboveMaximum_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, new BigDecimal("100"), 5, new BigDecimal("10.5"), NOW)
            );
            assertTrue(ex.getMessage().contains("RPE must be between 1.0 and 10.0"));
        }

        @Test
        @DisplayName("constructor_RpeNotHalfIncrement_ThrowsIllegalArgument")
        void constructor_RpeNotHalfIncrement_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, new BigDecimal("100"), 5, new BigDecimal("7.3"), NOW)
            );
            assertTrue(ex.getMessage().contains("RPE must be between 1.0 and 10.0 in 0.5 increments"));
        }

        @Test
        @DisplayName("constructor_RpeNotHalfIncrement_7Point2_ThrowsIllegalArgument")
        void constructor_RpeNotHalfIncrement_7Point2_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, new BigDecimal("100"), 5, new BigDecimal("7.2"), NOW)
            );
        }

        @Test
        @DisplayName("constructor_RpeZero_ThrowsIllegalArgument")
        void constructor_RpeZero_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    new SetLog(1, new BigDecimal("100"), 5, new BigDecimal("0.0"), NOW)
            );
        }
    }
}
