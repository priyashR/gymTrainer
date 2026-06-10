package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.unit;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.RecommendationEngine;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SnapshotParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecommendationEngine edge cases.
 * Tests domain logic in isolation — no Spring context, no mocks.
 *
 * Requirements: 1.2, 1.4
 */
class RecommendationEngineTest {

    private RecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RecommendationEngine();
    }

    // --- Malformed JSON → SnapshotParseException ---

    @Test
    @DisplayName("computeAll: malformed JSON throws SnapshotParseException")
    void computeAll_MalformedJson_ThrowsSnapshotParseException() {
        String malformedJson = "{ this is not valid json !!!";

        assertThrows(SnapshotParseException.class,
                () -> engine.computeAll(malformedJson, 1, 1));
    }

    @Test
    @DisplayName("computeForSection: malformed JSON throws SnapshotParseException")
    void computeForSection_MalformedJson_ThrowsSnapshotParseException() {
        String malformedJson = "not json at all";

        assertThrows(SnapshotParseException.class,
                () -> engine.computeForSection(malformedJson, 1, 1, 0));
    }

    // --- Out-of-bounds section index → empty list (no exercises to produce all-null for) ---

    @Test
    @DisplayName("computeForSection: section index beyond array size returns empty list")
    void computeForSection_SectionIndexOutOfBounds_ReturnsEmptyList() {
        String snapshot = """
                {
                  "weeks": [{
                    "weekNumber": 1,
                    "days": [{
                      "dayNumber": 1,
                      "modality": "HYPERTROPHY",
                      "sections": [
                        {
                          "name": "Main Lifts",
                          "exercises": [
                            { "name": "Squat", "weight": "100kg", "reps": "5", "sets": 5 }
                          ]
                        }
                      ]
                    }]
                  }]
                }
                """;

        List<ExerciseRecommendation> result = engine.computeForSection(snapshot, 1, 1, 99);

        assertTrue(result.isEmpty(), "Out-of-bounds section index should return an empty list");
    }

    @Test
    @DisplayName("computeForSection: negative section index returns empty list")
    void computeForSection_NegativeSectionIndex_ReturnsEmptyList() {
        String snapshot = """
                {
                  "weeks": [{
                    "weekNumber": 1,
                    "days": [{
                      "dayNumber": 1,
                      "modality": "HYPERTROPHY",
                      "sections": [
                        {
                          "name": "Main Lifts",
                          "exercises": [
                            { "name": "Squat", "weight": "80kg", "reps": "8", "sets": 4 }
                          ]
                        }
                      ]
                    }]
                  }]
                }
                """;

        List<ExerciseRecommendation> result = engine.computeForSection(snapshot, 1, 1, -1);

        assertTrue(result.isEmpty(), "Negative section index should return an empty list");
    }

    // --- Out-of-bounds exercise index → all-null recommendation ---
    // Note: The engine iterates exercises from the snapshot array, so an out-of-bounds exercise
    // index scenario occurs when the snapshot has fewer exercises than expected by the session.
    // The engine itself does not receive an exercise index parameter — it iterates the snapshot array.
    // We test by verifying behaviour when the snapshot has no exercises at a position.

    @Test
    @DisplayName("computeAll: week number out of bounds returns empty list")
    void computeAll_WeekNumberOutOfBounds_ReturnsEmptyList() {
        String snapshot = """
                {
                  "weeks": [{
                    "weekNumber": 1,
                    "days": [{
                      "dayNumber": 1,
                      "modality": "HYPERTROPHY",
                      "sections": [
                        {
                          "name": "Main Lifts",
                          "exercises": [
                            { "name": "Squat", "weight": "80kg", "reps": "8", "sets": 4 }
                          ]
                        }
                      ]
                    }]
                  }]
                }
                """;

        List<ExerciseRecommendation> result = engine.computeAll(snapshot, 5, 1);

        assertTrue(result.isEmpty(), "Out-of-bounds week number should return an empty list");
    }

    @Test
    @DisplayName("computeAll: day number out of bounds returns empty list")
    void computeAll_DayNumberOutOfBounds_ReturnsEmptyList() {
        String snapshot = """
                {
                  "weeks": [{
                    "weekNumber": 1,
                    "days": [{
                      "dayNumber": 1,
                      "modality": "HYPERTROPHY",
                      "sections": [
                        {
                          "name": "Main Lifts",
                          "exercises": [
                            { "name": "Squat", "weight": "80kg", "reps": "8", "sets": 4 }
                          ]
                        }
                      ]
                    }]
                  }]
                }
                """;

        List<ExerciseRecommendation> result = engine.computeAll(snapshot, 1, 10);

        assertTrue(result.isEmpty(), "Out-of-bounds day number should return an empty list");
    }

    // --- Empty sections array → empty list ---

    @Test
    @DisplayName("computeAll: empty sections array returns empty list")
    void computeAll_EmptySectionsArray_ReturnsEmptyList() {
        String snapshot = """
                {
                  "weeks": [{
                    "weekNumber": 1,
                    "days": [{
                      "dayNumber": 1,
                      "modality": "HYPERTROPHY",
                      "sections": []
                    }]
                  }]
                }
                """;

        List<ExerciseRecommendation> result = engine.computeAll(snapshot, 1, 1);

        assertTrue(result.isEmpty(), "Empty sections array should return an empty list");
    }

    @Test
    @DisplayName("computeForSection: empty sections array returns empty list")
    void computeForSection_EmptySectionsArray_ReturnsEmptyList() {
        String snapshot = """
                {
                  "weeks": [{
                    "weekNumber": 1,
                    "days": [{
                      "dayNumber": 1,
                      "modality": "HYPERTROPHY",
                      "sections": []
                    }]
                  }]
                }
                """;

        List<ExerciseRecommendation> result = engine.computeForSection(snapshot, 1, 1, 0);

        assertTrue(result.isEmpty(), "Empty sections with section index 0 should return an empty list");
    }

    // --- Null weight in exercise → null in recommendation ---

    @Test
    @DisplayName("computeAll: null weight in exercise results in null prescribedWeight")
    void computeAll_NullWeightInExercise_ReturnsNullWeight() {
        String snapshot = """
                {
                  "weeks": [{
                    "weekNumber": 1,
                    "days": [{
                      "dayNumber": 1,
                      "modality": "HYPERTROPHY",
                      "sections": [
                        {
                          "name": "Accessories",
                          "exercises": [
                            { "name": "Pull-ups", "weight": null, "reps": "8-12", "sets": 3 }
                          ]
                        }
                      ]
                    }]
                  }]
                }
                """;

        List<ExerciseRecommendation> result = engine.computeAll(snapshot, 1, 1);

        assertEquals(1, result.size());
        ExerciseRecommendation rec = result.get(0);
        assertNull(rec.prescribedWeight(), "Null weight in snapshot should produce null prescribedWeight");
        assertEquals("8-12", rec.prescribedReps());
        assertEquals(3, rec.prescribedSets());
    }

    @Test
    @DisplayName("computeAll: missing weight field in exercise results in null prescribedWeight")
    void computeAll_MissingWeightField_ReturnsNullWeight() {
        String snapshot = """
                {
                  "weeks": [{
                    "weekNumber": 1,
                    "days": [{
                      "dayNumber": 1,
                      "modality": "HYPERTROPHY",
                      "sections": [
                        {
                          "name": "Bodyweight",
                          "exercises": [
                            { "name": "Dips", "reps": "10", "sets": 4 }
                          ]
                        }
                      ]
                    }]
                  }]
                }
                """;

        List<ExerciseRecommendation> result = engine.computeAll(snapshot, 1, 1);

        assertEquals(1, result.size());
        ExerciseRecommendation rec = result.get(0);
        assertNull(rec.prescribedWeight(), "Missing weight field should produce null prescribedWeight");
        assertEquals("10", rec.prescribedReps());
        assertEquals(4, rec.prescribedSets());
    }
}
