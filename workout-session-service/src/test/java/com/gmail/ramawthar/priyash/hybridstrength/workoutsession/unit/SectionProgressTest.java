package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.CrossFitScore;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseLog;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SectionProgress CrossFit score and round counter functionality.
 * Tests domain logic in isolation — no Spring context, no mocks.
 */
class SectionProgressTest {

    private static final Instant NOW = Instant.now();

    private SectionProgress createAmrapSection() {
        return new SectionProgress(0, "AMRAP 20", SectionType.AMRAP,
                List.of(new ExerciseLog(0, "Pull-ups"), new ExerciseLog(1, "Push-ups")));
    }

    @Nested
    @DisplayName("setCrossFitScore")
    class SetCrossFitScore {

        @Test
        @DisplayName("setCrossFitScore_FirstScore_SetsScore")
        void setCrossFitScore_FirstScore_SetsScore() {
            SectionProgress section = createAmrapSection();
            CrossFitScore score = new CrossFitScore(5, 12, null, NOW);

            section.setCrossFitScore(score);

            assertNotNull(section.getCrossFitScore());
            assertEquals(5, section.getCrossFitScore().getRounds());
            assertEquals(12, section.getCrossFitScore().getAdditionalReps());
        }

        @Test
        @DisplayName("setCrossFitScore_SecondScore_OverwritesPrevious")
        void setCrossFitScore_SecondScore_OverwritesPrevious() {
            SectionProgress section = createAmrapSection();
            CrossFitScore firstScore = new CrossFitScore(3, 8, null, NOW);
            CrossFitScore secondScore = new CrossFitScore(5, 12, null, NOW.plusSeconds(60));

            section.setCrossFitScore(firstScore);
            section.setCrossFitScore(secondScore);

            CrossFitScore result = section.getCrossFitScore();
            assertNotNull(result);
            assertEquals(5, result.getRounds());
            assertEquals(12, result.getAdditionalReps());
            assertEquals(NOW.plusSeconds(60), result.getLoggedAt());
        }

        @Test
        @DisplayName("setCrossFitScore_NullScore_ClearsExistingScore")
        void setCrossFitScore_NullScore_ClearsExistingScore() {
            SectionProgress section = createAmrapSection();
            section.setCrossFitScore(new CrossFitScore(3, 8, null, NOW));

            section.setCrossFitScore(null);

            assertNull(section.getCrossFitScore());
        }

        @Test
        @DisplayName("getCrossFitScore_NoScoreSet_ReturnsNull")
        void getCrossFitScore_NoScoreSet_ReturnsNull() {
            SectionProgress section = createAmrapSection();

            assertNull(section.getCrossFitScore());
        }
    }

    @Nested
    @DisplayName("roundCount")
    class RoundCount {

        @Test
        @DisplayName("getRoundCount_Default_ReturnsZero")
        void getRoundCount_Default_ReturnsZero() {
            SectionProgress section = createAmrapSection();

            assertEquals(0, section.getRoundCount());
        }

        @Test
        @DisplayName("setRoundCount_Increment_UpdatesCount")
        void setRoundCount_Increment_UpdatesCount() {
            SectionProgress section = createAmrapSection();

            section.setRoundCount(1);
            assertEquals(1, section.getRoundCount());

            section.setRoundCount(2);
            assertEquals(2, section.getRoundCount());

            section.setRoundCount(3);
            assertEquals(3, section.getRoundCount());
        }

        @Test
        @DisplayName("setRoundCount_Decrement_UpdatesCount")
        void setRoundCount_Decrement_UpdatesCount() {
            SectionProgress section = createAmrapSection();
            section.setRoundCount(5);

            section.setRoundCount(4);
            assertEquals(4, section.getRoundCount());

            section.setRoundCount(0);
            assertEquals(0, section.getRoundCount());
        }

        @Test
        @DisplayName("setRoundCount_ReconstitutedWithRoundCount_PreservesValue")
        void setRoundCount_ReconstitutedWithRoundCount_PreservesValue() {
            CrossFitScore score = new CrossFitScore(3, 8, null, NOW);
            SectionProgress section = new SectionProgress(0, "AMRAP 20", SectionType.AMRAP,
                    List.of(new ExerciseLog(0, "Pull-ups")), false, score, 7);

            assertEquals(7, section.getRoundCount());
            assertNotNull(section.getCrossFitScore());
            assertEquals(3, section.getCrossFitScore().getRounds());
        }
    }
}
