package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.CrossFitScore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CrossFitScore value object.
 * Tests construction validation — no Spring context, no mocks.
 */
class CrossFitScoreTest {

    private static final Instant NOW = Instant.now();

    @Nested
    @DisplayName("Valid construction")
    class ValidConstruction {

        @Test
        @DisplayName("constructor_AmrapScore_CreatesWithNullTime")
        void constructor_AmrapScore_CreatesWithNullTime() {
            CrossFitScore score = new CrossFitScore(5, 12, null, NOW);

            assertEquals(5, score.getRounds());
            assertEquals(12, score.getAdditionalReps());
            assertNull(score.getTotalTimeSeconds());
            assertEquals(NOW, score.getLoggedAt());
        }

        @Test
        @DisplayName("constructor_ForTimeScore_CreatesWithTime")
        void constructor_ForTimeScore_CreatesWithTime() {
            CrossFitScore score = new CrossFitScore(3, 0, 720, NOW);

            assertEquals(3, score.getRounds());
            assertEquals(0, score.getAdditionalReps());
            assertEquals(720, score.getTotalTimeSeconds());
            assertEquals(NOW, score.getLoggedAt());
        }

        @Test
        @DisplayName("constructor_ZeroRoundsAndReps_CreatesValidScore")
        void constructor_ZeroRoundsAndReps_CreatesValidScore() {
            CrossFitScore score = new CrossFitScore(0, 0, null, NOW);

            assertEquals(0, score.getRounds());
            assertEquals(0, score.getAdditionalReps());
        }

        @Test
        @DisplayName("constructor_MinimalTimeSeconds_CreatesValidScore")
        void constructor_MinimalTimeSeconds_CreatesValidScore() {
            CrossFitScore score = new CrossFitScore(1, 0, 1, NOW);

            assertEquals(1, score.getTotalTimeSeconds());
        }
    }

    @Nested
    @DisplayName("Invalid rounds")
    class InvalidRounds {

        @Test
        @DisplayName("constructor_NegativeRounds_ThrowsIllegalArgument")
        void constructor_NegativeRounds_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new CrossFitScore(-1, 5, null, NOW)
            );
            assertTrue(ex.getMessage().contains("Rounds must be non-negative"));
        }

        @Test
        @DisplayName("constructor_LargeNegativeRounds_ThrowsIllegalArgument")
        void constructor_LargeNegativeRounds_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CrossFitScore(-100, 0, null, NOW)
            );
        }
    }

    @Nested
    @DisplayName("Invalid additionalReps")
    class InvalidAdditionalReps {

        @Test
        @DisplayName("constructor_NegativeAdditionalReps_ThrowsIllegalArgument")
        void constructor_NegativeAdditionalReps_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new CrossFitScore(3, -1, null, NOW)
            );
            assertTrue(ex.getMessage().contains("Additional reps must be non-negative"));
        }

        @Test
        @DisplayName("constructor_LargeNegativeAdditionalReps_ThrowsIllegalArgument")
        void constructor_LargeNegativeAdditionalReps_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CrossFitScore(3, -50, null, NOW)
            );
        }
    }

    @Nested
    @DisplayName("Invalid totalTimeSeconds")
    class InvalidTotalTimeSeconds {

        @Test
        @DisplayName("constructor_ZeroTotalTimeSeconds_ThrowsIllegalArgument")
        void constructor_ZeroTotalTimeSeconds_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new CrossFitScore(3, 5, 0, NOW)
            );
            assertTrue(ex.getMessage().contains("Total time must be greater than zero"));
        }

        @Test
        @DisplayName("constructor_NegativeTotalTimeSeconds_ThrowsIllegalArgument")
        void constructor_NegativeTotalTimeSeconds_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new CrossFitScore(3, 5, -10, NOW)
            );
            assertTrue(ex.getMessage().contains("Total time must be greater than zero"));
        }
    }

    @Nested
    @DisplayName("Invalid loggedAt")
    class InvalidLoggedAt {

        @Test
        @DisplayName("constructor_NullLoggedAt_ThrowsIllegalArgument")
        void constructor_NullLoggedAt_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CrossFitScore(3, 5, null, null)
            );
        }
    }
}
