package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.RecommendationEngine;

import net.jqwik.api.*;
import net.jqwik.api.Tuple.Tuple3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Property-based tests for RecommendationEngine.
 * Tests Properties 1, 2, 3, and 6 from the design document.
 */
class RecommendationEnginePropertyTest {

    private final RecommendationEngine engine = new RecommendationEngine();

    // --- Generators ---

    @Provide
    Arbitrary<String> validWeight() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .withCharRange('A', 'Z')
                        .withCharRange('0', '9')
                        .withChars('-', '.', ' ', '%', 'k', 'g')
                        .ofMinLength(1)
                        .ofMaxLength(50)
                        .filter(s -> !s.isBlank())
        );
    }

    @Provide
    Arbitrary<String> validReps() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.strings()
                        .withCharRange('0', '9')
                        .withChars('-', ' ')
                        .ofMinLength(1)
                        .ofMaxLength(20)
                        .filter(s -> !s.isBlank())
        );
    }

    @Provide
    Arbitrary<Integer> validSets() {
        // 0 means "unspecified" (engine converts to null), 1-100 are valid values
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<Integer> sectionCount() {
        return Arbitraries.integers().between(1, 5);
    }

    @Provide
    Arbitrary<Integer> exerciseCount() {
        return Arbitraries.integers().between(1, 8);
    }

    // --- Snapshot builder helper ---

    /**
     * Represents an exercise in a generated snapshot for verification.
     */
    record GeneratedExercise(String weight, String reps, int sets) {}

    /**
     * Represents a generated snapshot with its exercises organized by section for verification.
     */
    record GeneratedSnapshot(String json, List<List<GeneratedExercise>> sections) {}

    /**
     * Builds a valid HYPERTROPHY workout snapshot JSON with the given structure.
     */
    private GeneratedSnapshot buildHypertrophySnapshot(
            int numSections,
            int exercisesPerSection,
            List<List<Tuple3<String, String, Integer>>> exerciseData) {

        List<List<GeneratedExercise>> allSections = new ArrayList<>();
        StringBuilder json = new StringBuilder();
        json.append("{\"weeks\":[{\"weekNumber\":1,\"days\":[{\"dayNumber\":1,\"modality\":\"HYPERTROPHY\",\"sections\":[");

        for (int s = 0; s < numSections; s++) {
            List<GeneratedExercise> sectionExercises = new ArrayList<>();
            if (s > 0) json.append(",");
            json.append("{\"name\":\"Section ").append(s).append("\",\"type\":\"STRENGTH\",\"exercises\":[");

            List<Tuple3<String, String, Integer>> sectionData = exerciseData.get(s);
            for (int e = 0; e < sectionData.size(); e++) {
                Tuple3<String, String, Integer> ex = sectionData.get(e);
                String weight = ex.get1();
                String reps = ex.get2();
                int sets = ex.get3();

                sectionExercises.add(new GeneratedExercise(weight, reps, sets));

                if (e > 0) json.append(",");
                json.append("{\"name\":\"Exercise ").append(e).append("\"");
                if (weight != null) {
                    json.append(",\"weight\":\"").append(escapeJson(weight)).append("\"");
                } else {
                    json.append(",\"weight\":null");
                }
                if (reps != null) {
                    json.append(",\"reps\":\"").append(escapeJson(reps)).append("\"");
                } else {
                    json.append(",\"reps\":null");
                }
                json.append(",\"sets\":").append(sets);
                json.append(",\"restSeconds\":90}");
            }

            json.append("]}");
            allSections.add(sectionExercises);
        }

        json.append("]}]}]}");
        return new GeneratedSnapshot(json.toString(), allSections);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Builds a valid CROSSFIT workout snapshot JSON with the given structure.
     */
    private GeneratedSnapshot buildCrossfitSnapshot(
            int numSections,
            int exercisesPerSection,
            List<List<Tuple3<String, String, Integer>>> exerciseData) {

        List<List<GeneratedExercise>> allSections = new ArrayList<>();
        StringBuilder json = new StringBuilder();
        json.append("{\"weeks\":[{\"weekNumber\":1,\"days\":[{\"dayNumber\":1,\"modality\":\"CROSSFIT\",\"sections\":[");

        for (int s = 0; s < numSections; s++) {
            List<GeneratedExercise> sectionExercises = new ArrayList<>();
            if (s > 0) json.append(",");
            json.append("{\"name\":\"Section ").append(s).append("\",\"type\":\"WOD\",\"exercises\":[");

            List<Tuple3<String, String, Integer>> sectionData = exerciseData.get(s);
            for (int e = 0; e < sectionData.size(); e++) {
                Tuple3<String, String, Integer> ex = sectionData.get(e);
                String weight = ex.get1();
                String reps = ex.get2();
                int sets = ex.get3();

                sectionExercises.add(new GeneratedExercise(weight, reps, sets));

                if (e > 0) json.append(",");
                json.append("{\"name\":\"Exercise ").append(e).append("\"");
                if (weight != null) {
                    json.append(",\"weight\":\"").append(escapeJson(weight)).append("\"");
                } else {
                    json.append(",\"weight\":null");
                }
                if (reps != null) {
                    json.append(",\"reps\":\"").append(escapeJson(reps)).append("\"");
                } else {
                    json.append(",\"reps\":null");
                }
                json.append(",\"sets\":").append(sets);
                json.append(",\"restSeconds\":60}");
            }

            json.append("]}");
            allSections.add(sectionExercises);
        }

        json.append("]}]}]}");
        return new GeneratedSnapshot(json.toString(), allSections);
    }

    // --- Property 1: HYPERTROPHY extraction preserves snapshot values ---

    // Feature: theater-mode-recommendations, Property 1: HYPERTROPHY extraction preserves snapshot values

    /**
     * Property 1: HYPERTROPHY extraction preserves snapshot values.
     *
     * For any valid workout snapshot containing a HYPERTROPHY day with arbitrary exercises
     * (each having a name, weight string or null, reps string or null, and sets integer),
     * the RecommendationEngine.computeAll(...) SHALL produce an ExerciseRecommendation at each
     * (sectionIndex, exerciseIndex) where:
     * - prescribedWeight equals the exercise's weight field from the snapshot (null if absent/null)
     * - prescribedReps equals the exercise's reps field from the snapshot (null if absent/null)
     * - prescribedSets equals the exercise's sets field if > 0, or null if sets == 0
     *
     * Validates: Requirements 1.1, 1.3, 2.1, 2.2, 2.3, 2.4
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 1: HYPERTROPHY extraction preserves snapshot values")
    void hypertrophyExtraction_preservesSnapshotValues(
            @ForAll("sectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection,
            @ForAll("validWeight") String weight,
            @ForAll("validReps") String reps,
            @ForAll("validSets") int sets) {

        // Build a uniform snapshot where all exercises have the same parameters
        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                sectionData.add(Tuple.of(weight, reps, sets));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildHypertrophySnapshot(numSections, exercisesPerSection, exerciseData);
        List<ExerciseRecommendation> results = engine.computeAll(snapshot.json(), 1, 1);

        // Verify correct number of recommendations
        int expectedCount = numSections * exercisesPerSection;
        assert results.size() == expectedCount :
                "Expected " + expectedCount + " recommendations but got " + results.size();

        // Verify each recommendation matches the snapshot values
        for (ExerciseRecommendation rec : results) {
            // Weight: should be preserved as-is (or null if snapshot had null)
            // Engine treats blank as null, so if weight was non-null and non-blank it's preserved
            String expectedWeight = weight;
            if (expectedWeight != null && expectedWeight.length() > 50) {
                expectedWeight = expectedWeight.substring(0, 50);
            }
            assert Objects.equals(expectedWeight, rec.prescribedWeight()) :
                    "prescribedWeight mismatch at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]: "
                            + "expected=" + expectedWeight + " actual=" + rec.prescribedWeight();

            // Reps: should be preserved as-is (or null if snapshot had null)
            String expectedReps = reps;
            if (expectedReps != null && expectedReps.length() > 50) {
                expectedReps = expectedReps.substring(0, 50);
            }
            assert Objects.equals(expectedReps, rec.prescribedReps()) :
                    "prescribedReps mismatch at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]: "
                            + "expected=" + expectedReps + " actual=" + rec.prescribedReps();

            // Sets: 0 → null, > 0 → preserved (clamped to 100 if > 100)
            Integer expectedSets = sets == 0 ? null : Math.min(sets, 100);
            assert Objects.equals(expectedSets, rec.prescribedSets()) :
                    "prescribedSets mismatch at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]: "
                            + "expected=" + expectedSets + " actual=" + rec.prescribedSets();
        }
    }

    /**
     * Property 1 (variant): HYPERTROPHY extraction with varied exercises per section.
     *
     * Generates snapshots with different parameter values for each exercise within a section,
     * ensuring the engine correctly maps each exercise's fields to the right recommendation index.
     *
     * Validates: Requirements 1.1, 1.3, 2.1, 2.2, 2.3, 2.4
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 1: HYPERTROPHY extraction — varied exercises map correctly")
    void hypertrophyExtraction_variedExercises_mapCorrectly(
            @ForAll("sectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection) {

        // Generate unique data for each exercise using deterministic but varied values
        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                String w = (s + e) % 3 == 0 ? null : ((s * 10 + e * 5 + 40) + "kg");
                String r = (s + e) % 4 == 0 ? null : ((e + 6) + "-" + (e + 10));
                int st = (s + e) % 5 == 0 ? 0 : (e + 1);
                sectionData.add(Tuple.of(w, r, st));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildHypertrophySnapshot(numSections, exercisesPerSection, exerciseData);
        List<ExerciseRecommendation> results = engine.computeAll(snapshot.json(), 1, 1);

        // Verify correct total count
        int expectedCount = numSections * exercisesPerSection;
        assert results.size() == expectedCount :
                "Expected " + expectedCount + " recommendations but got " + results.size();

        // Verify each recommendation matches its specific exercise data
        int resultIdx = 0;
        for (int s = 0; s < numSections; s++) {
            for (int e = 0; e < exercisesPerSection; e++) {
                ExerciseRecommendation rec = results.get(resultIdx);
                GeneratedExercise expected = snapshot.sections().get(s).get(e);

                assert rec.sectionIndex() == s :
                        "sectionIndex mismatch: expected=" + s + " actual=" + rec.sectionIndex();
                assert rec.exerciseIndex() == e :
                        "exerciseIndex mismatch: expected=" + e + " actual=" + rec.exerciseIndex();

                // Weight: null preserved, non-null preserved (truncated if > 50)
                String expectedWeight = expected.weight();
                if (expectedWeight != null && expectedWeight.length() > 50) {
                    expectedWeight = expectedWeight.substring(0, 50);
                }
                assert Objects.equals(expectedWeight, rec.prescribedWeight()) :
                        "prescribedWeight mismatch at [" + s + "," + e + "]: "
                                + "expected=" + expectedWeight + " actual=" + rec.prescribedWeight();

                // Reps: null preserved, non-null preserved (truncated if > 50)
                String expectedReps = expected.reps();
                if (expectedReps != null && expectedReps.length() > 50) {
                    expectedReps = expectedReps.substring(0, 50);
                }
                assert Objects.equals(expectedReps, rec.prescribedReps()) :
                        "prescribedReps mismatch at [" + s + "," + e + "]: "
                                + "expected=" + expectedReps + " actual=" + rec.prescribedReps();

                // Sets: 0 → null, > 0 → preserved (clamped to 100)
                Integer expectedSets = expected.sets() == 0 ? null : Math.min(expected.sets(), 100);
                assert Objects.equals(expectedSets, rec.prescribedSets()) :
                        "prescribedSets mismatch at [" + s + "," + e + "]: "
                                + "expected=" + expectedSets + " actual=" + rec.prescribedSets();

                resultIdx++;
            }
        }
    }

    /**
     * Property 1 (invariant): HYPERTROPHY reps range strings are preserved as-is.
     *
     * When a HYPERTROPHY exercise has a reps value containing a range (e.g. "8-10"),
     * the engine SHALL preserve the range string without interpreting it.
     *
     * Validates: Requirements 2.2
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 1: HYPERTROPHY reps range strings preserved as-is")
    void hypertrophyExtraction_repsRangeStrings_preservedAsIs(
            @ForAll("sectionCount") int numSections) {

        // Generate exercises with various range-style reps strings
        String[] repsRanges = {"8-10", "10-12", "5-8", "1-3", "15-20", "6-8"};

        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < repsRanges.length; e++) {
                sectionData.add(Tuple.of("80kg", repsRanges[e], 4));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildHypertrophySnapshot(numSections, repsRanges.length, exerciseData);
        List<ExerciseRecommendation> results = engine.computeAll(snapshot.json(), 1, 1);

        int resultIdx = 0;
        for (int s = 0; s < numSections; s++) {
            for (int e = 0; e < repsRanges.length; e++) {
                ExerciseRecommendation rec = results.get(resultIdx);
                assert Objects.equals(repsRanges[e], rec.prescribedReps()) :
                        "Reps range string not preserved at [" + s + "," + e + "]: "
                                + "expected=\"" + repsRanges[e] + "\" actual=\"" + rec.prescribedReps() + "\"";
                resultIdx++;
            }
        }
    }

    /**
     * Property 1 (invariant): HYPERTROPHY sets==0 maps to null in recommendation.
     *
     * When a HYPERTROPHY exercise has a sets value of zero, the engine SHALL treat it
     * as unspecified and return null for prescribedSets.
     *
     * Validates: Requirements 2.3
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 1: HYPERTROPHY sets==0 maps to null")
    void hypertrophyExtraction_setsZero_mapsToNull(
            @ForAll("sectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection) {

        // All exercises have sets = 0
        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                sectionData.add(Tuple.of("60kg", "10", 0));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildHypertrophySnapshot(numSections, exercisesPerSection, exerciseData);
        List<ExerciseRecommendation> results = engine.computeAll(snapshot.json(), 1, 1);

        for (ExerciseRecommendation rec : results) {
            assert rec.prescribedSets() == null :
                    "Expected null prescribedSets for sets=0 but got " + rec.prescribedSets()
                            + " at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]";
        }
    }

    /**
     * Property 1 (invariant): HYPERTROPHY positive sets values are preserved.
     *
     * When a HYPERTROPHY exercise has a sets value greater than zero,
     * the engine SHALL return that integer value as-is in the recommendation.
     *
     * Validates: Requirements 2.4
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 1: HYPERTROPHY positive sets values preserved")
    void hypertrophyExtraction_positiveSets_preserved(
            @ForAll("sectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection,
            @ForAll @net.jqwik.api.constraints.IntRange(min = 1, max = 100) int setsValue) {

        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                sectionData.add(Tuple.of("70kg", "8-10", setsValue));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildHypertrophySnapshot(numSections, exercisesPerSection, exerciseData);
        List<ExerciseRecommendation> results = engine.computeAll(snapshot.json(), 1, 1);

        for (ExerciseRecommendation rec : results) {
            assert Objects.equals(setsValue, rec.prescribedSets()) :
                    "Expected prescribedSets=" + setsValue + " but got " + rec.prescribedSets()
                            + " at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]";
        }
    }

    // --- Property 2: CROSSFIT extraction returns only weight ---

    // Feature: theater-mode-recommendations, Property 2: CROSSFIT extraction returns only weight

    /**
     * Property 2: CROSSFIT extraction returns only weight.
     *
     * For any valid workout snapshot containing a CROSSFIT day with arbitrary exercises
     * (each having a weight string or null, reps string, and sets integer),
     * the RecommendationEngine.computeAll(...) SHALL produce an ExerciseRecommendation at each
     * (sectionIndex, exerciseIndex) where:
     * - prescribedWeight equals the exercise's weight field from the snapshot (null if absent/null)
     * - prescribedReps is always null regardless of the snapshot's reps value
     * - prescribedSets is always null regardless of the snapshot's sets value
     *
     * Validates: Requirements 3.1, 3.2, 3.3
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 2: CROSSFIT extraction returns only weight")
    void crossfitExtraction_returnsOnlyWeight(
            @ForAll("sectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection,
            @ForAll("validWeight") String weight,
            @ForAll("validReps") String reps,
            @ForAll("validSets") int sets) {

        // Build a CROSSFIT snapshot where all exercises have the same parameters
        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                sectionData.add(Tuple.of(weight, reps, sets));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildCrossfitSnapshot(numSections, exercisesPerSection, exerciseData);
        List<ExerciseRecommendation> results = engine.computeAll(snapshot.json(), 1, 1);

        // Verify correct number of recommendations
        int expectedCount = numSections * exercisesPerSection;
        assert results.size() == expectedCount :
                "Expected " + expectedCount + " recommendations but got " + results.size();

        // Verify each recommendation: only weight is populated, reps and sets are always null
        for (ExerciseRecommendation rec : results) {
            // Weight: should be preserved as-is (null if snapshot weight was null, truncated if > 50)
            String expectedWeight = weight;
            if (expectedWeight != null && expectedWeight.length() > 50) {
                expectedWeight = expectedWeight.substring(0, 50);
            }
            assert Objects.equals(expectedWeight, rec.prescribedWeight()) :
                    "prescribedWeight mismatch at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]: "
                            + "expected=" + expectedWeight + " actual=" + rec.prescribedWeight();

            // Reps: MUST always be null for CROSSFIT regardless of snapshot reps value
            assert rec.prescribedReps() == null :
                    "prescribedReps must be null for CROSSFIT but got \"" + rec.prescribedReps()
                            + "\" at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]";

            // Sets: MUST always be null for CROSSFIT regardless of snapshot sets value
            assert rec.prescribedSets() == null :
                    "prescribedSets must be null for CROSSFIT but got " + rec.prescribedSets()
                            + " at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]";
        }
    }

    /**
     * Property 2 (variant): CROSSFIT extraction with varied exercises per section.
     *
     * Generates CROSSFIT snapshots with different weight values for each exercise,
     * ensuring the engine correctly maps each exercise's weight and always nullifies reps/sets.
     *
     * Validates: Requirements 3.1, 3.2, 3.3
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 2: CROSSFIT extraction — varied exercises map correctly")
    void crossfitExtraction_variedExercises_mapCorrectly(
            @ForAll("sectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection) {

        // Generate unique data for each exercise with varied weights, reps, and sets
        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                // Some exercises have null weight, some have a value
                String w = (s + e) % 3 == 0 ? null : ((s * 10 + e * 5 + 40) + "kg");
                // Reps and sets should be ignored for CROSSFIT but we generate non-null values
                String r = (e + 6) + "-" + (e + 10);
                int st = e + 3;
                sectionData.add(Tuple.of(w, r, st));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildCrossfitSnapshot(numSections, exercisesPerSection, exerciseData);
        List<ExerciseRecommendation> results = engine.computeAll(snapshot.json(), 1, 1);

        // Verify correct total count
        int expectedCount = numSections * exercisesPerSection;
        assert results.size() == expectedCount :
                "Expected " + expectedCount + " recommendations but got " + results.size();

        // Verify each recommendation matches its specific exercise weight only
        int resultIdx = 0;
        for (int s = 0; s < numSections; s++) {
            for (int e = 0; e < exercisesPerSection; e++) {
                ExerciseRecommendation rec = results.get(resultIdx);
                GeneratedExercise expected = snapshot.sections().get(s).get(e);

                assert rec.sectionIndex() == s :
                        "sectionIndex mismatch: expected=" + s + " actual=" + rec.sectionIndex();
                assert rec.exerciseIndex() == e :
                        "exerciseIndex mismatch: expected=" + e + " actual=" + rec.exerciseIndex();

                // Weight: null preserved, non-null preserved (truncated if > 50)
                String expectedWeight = expected.weight();
                if (expectedWeight != null && expectedWeight.length() > 50) {
                    expectedWeight = expectedWeight.substring(0, 50);
                }
                assert Objects.equals(expectedWeight, rec.prescribedWeight()) :
                        "prescribedWeight mismatch at [" + s + "," + e + "]: "
                                + "expected=" + expectedWeight + " actual=" + rec.prescribedWeight();

                // Reps: MUST always be null for CROSSFIT
                assert rec.prescribedReps() == null :
                        "prescribedReps must be null for CROSSFIT but got \"" + rec.prescribedReps()
                                + "\" at [" + s + "," + e + "]";

                // Sets: MUST always be null for CROSSFIT
                assert rec.prescribedSets() == null :
                        "prescribedSets must be null for CROSSFIT but got " + rec.prescribedSets()
                                + " at [" + s + "," + e + "]";

                resultIdx++;
            }
        }
    }

    /**
     * Property 2 (invariant): CROSSFIT null weight produces all-null recommendation.
     *
     * When a CROSSFIT exercise has a null weight, all recommendation fields
     * (weight, reps, sets) must be null.
     *
     * Validates: Requirements 3.2
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 2: CROSSFIT null weight produces all-null recommendation")
    void crossfitExtraction_nullWeight_producesAllNullRecommendation(
            @ForAll("sectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection) {

        // All exercises have null weight but non-null reps and sets
        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                sectionData.add(Tuple.of(null, "10-12", 5));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildCrossfitSnapshot(numSections, exercisesPerSection, exerciseData);
        List<ExerciseRecommendation> results = engine.computeAll(snapshot.json(), 1, 1);

        for (ExerciseRecommendation rec : results) {
            assert rec.prescribedWeight() == null :
                    "prescribedWeight must be null when snapshot weight is null, but got \""
                            + rec.prescribedWeight() + "\" at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]";
            assert rec.prescribedReps() == null :
                    "prescribedReps must be null for CROSSFIT but got \"" + rec.prescribedReps()
                            + "\" at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]";
            assert rec.prescribedSets() == null :
                    "prescribedSets must be null for CROSSFIT but got " + rec.prescribedSets()
                            + " at [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "]";
        }
    }

    /**
     * Property 2 (invariant): CROSSFIT non-blank weight is preserved as-is.
     *
     * When a CROSSFIT exercise has a non-blank weight string, the engine SHALL preserve
     * the weight string as-is without transformation (only truncation at 50 chars applies).
     *
     * Validates: Requirements 3.3
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 2: CROSSFIT non-blank weight preserved as-is")
    void crossfitExtraction_nonBlankWeight_preservedAsIs(
            @ForAll("sectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection) {

        // Generate exercises with various weight string formats
        String[] weights = {"60kg", "135lbs", "bodyweight", "60% 1RM", "24kg KB", "95lbs"};

        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                String w = weights[(s * exercisesPerSection + e) % weights.length];
                sectionData.add(Tuple.of(w, "AMRAP", 1));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildCrossfitSnapshot(numSections, exercisesPerSection, exerciseData);
        List<ExerciseRecommendation> results = engine.computeAll(snapshot.json(), 1, 1);

        int resultIdx = 0;
        for (int s = 0; s < numSections; s++) {
            for (int e = 0; e < exercisesPerSection; e++) {
                ExerciseRecommendation rec = results.get(resultIdx);
                GeneratedExercise expected = snapshot.sections().get(s).get(e);

                assert Objects.equals(expected.weight(), rec.prescribedWeight()) :
                        "Weight string not preserved at [" + s + "," + e + "]: "
                                + "expected=\"" + expected.weight() + "\" actual=\"" + rec.prescribedWeight() + "\"";

                assert rec.prescribedReps() == null :
                        "prescribedReps must be null for CROSSFIT but got \"" + rec.prescribedReps()
                                + "\" at [" + s + "," + e + "]";

                assert rec.prescribedSets() == null :
                        "prescribedSets must be null for CROSSFIT but got " + rec.prescribedSets()
                                + " at [" + s + "," + e + "]";

                resultIdx++;
            }
        }
    }

    // --- Property 3: Output domain invariants ---

    // Feature: theater-mode-recommendations, Property 3: Output domain invariants

    /**
     * Generator for weight strings that may exceed 50 characters to exercise truncation.
     */
    @Provide
    Arbitrary<String> boundaryWeight() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                // Normal valid weights (1-50 chars)
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .withCharRange('A', 'Z')
                        .withCharRange('0', '9')
                        .withChars('-', '.', ' ', '%', 'k', 'g')
                        .ofMinLength(1)
                        .ofMaxLength(50)
                        .filter(s -> !s.isBlank()),
                // Long weights (51-80 chars) to test truncation
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .withCharRange('0', '9')
                        .withChars('k', 'g')
                        .ofMinLength(51)
                        .ofMaxLength(80)
                        .filter(s -> !s.isBlank())
        );
    }

    /**
     * Generator for reps strings that may exceed 50 characters to exercise truncation.
     */
    @Provide
    Arbitrary<String> boundaryReps() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                // Normal valid reps (1-50 chars)
                Arbitraries.strings()
                        .withCharRange('0', '9')
                        .withChars('-', ' ')
                        .ofMinLength(1)
                        .ofMaxLength(50)
                        .filter(s -> !s.isBlank()),
                // Long reps (51-80 chars) to test truncation
                Arbitraries.strings()
                        .withCharRange('0', '9')
                        .withChars('-', ' ')
                        .ofMinLength(51)
                        .ofMaxLength(80)
                        .filter(s -> !s.isBlank())
        );
    }

    /**
     * Generator for sets values that may exceed 100 to exercise clamping.
     */
    @Provide
    Arbitrary<Integer> boundarySets() {
        return Arbitraries.oneOf(
                // 0 means unspecified (engine converts to null)
                Arbitraries.just(0),
                // Valid range [1, 100]
                Arbitraries.integers().between(1, 100),
                // Beyond max (101-200) to test clamping
                Arbitraries.integers().between(101, 200)
        );
    }

    /**
     * Property 3: Output domain invariants.
     *
     * For any valid workout snapshot with mixed modalities (HYPERTROPHY and CROSSFIT),
     * every ExerciseRecommendation produced by the engine SHALL satisfy:
     * - prescribedSets is either null or an integer in [1, 100]
     * - prescribedReps is either null or a non-blank string of at most 50 characters
     * - prescribedWeight is either null or a non-blank string of at most 50 characters
     *
     * Validates: Requirements 7.1, 7.2, 7.3
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 3: Output domain invariants")
    void outputDomainInvariants_allFieldsSatisfyConstraints(
            @ForAll("sectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection,
            @ForAll("boundaryWeight") String weight,
            @ForAll("boundaryReps") String reps,
            @ForAll("boundarySets") int sets) {

        // Build a HYPERTROPHY snapshot and verify output invariants
        List<List<Tuple3<String, String, Integer>>> hypertrophyData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                sectionData.add(Tuple.of(weight, reps, sets));
            }
            hypertrophyData.add(sectionData);
        }

        GeneratedSnapshot hypertrophySnapshot = buildHypertrophySnapshot(numSections, exercisesPerSection, hypertrophyData);
        List<ExerciseRecommendation> hypertrophyResults = engine.computeAll(hypertrophySnapshot.json(), 1, 1);

        for (ExerciseRecommendation rec : hypertrophyResults) {
            assertOutputDomainInvariants(rec, "HYPERTROPHY");
        }

        // Build a CROSSFIT snapshot with the same data and verify output invariants
        List<List<Tuple3<String, String, Integer>>> crossfitData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                sectionData.add(Tuple.of(weight, reps, sets));
            }
            crossfitData.add(sectionData);
        }

        GeneratedSnapshot crossfitSnapshot = buildCrossfitSnapshot(numSections, exercisesPerSection, crossfitData);
        List<ExerciseRecommendation> crossfitResults = engine.computeAll(crossfitSnapshot.json(), 1, 1);

        for (ExerciseRecommendation rec : crossfitResults) {
            assertOutputDomainInvariants(rec, "CROSSFIT");
        }
    }

    /**
     * Asserts that the given ExerciseRecommendation satisfies all output domain invariants.
     * - prescribedSets: null or integer in [1, 100]
     * - prescribedReps: null or non-blank string ≤ 50 characters
     * - prescribedWeight: null or non-blank string ≤ 50 characters
     */
    private void assertOutputDomainInvariants(ExerciseRecommendation rec, String modality) {
        // Requirement 7.1: prescribedSets is null or in [1, 100]
        Integer sets = rec.prescribedSets();
        assert sets == null || (sets >= 1 && sets <= 100) :
                modality + " [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "] "
                        + "prescribedSets must be null or in [1,100] but got " + sets;

        // Requirement 7.2: prescribedReps is null or non-blank string ≤ 50 chars
        String reps = rec.prescribedReps();
        assert reps == null || (!reps.isBlank() && reps.length() <= 50) :
                modality + " [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "] "
                        + "prescribedReps must be null or non-blank ≤ 50 chars but got "
                        + (reps == null ? "null" : "\"" + reps + "\" (len=" + reps.length() + ")");

        // Requirement 7.3: prescribedWeight is null or non-blank string ≤ 50 chars
        String weight = rec.prescribedWeight();
        assert weight == null || (!weight.isBlank() && weight.length() <= 50) :
                modality + " [" + rec.sectionIndex() + "," + rec.exerciseIndex() + "] "
                        + "prescribedWeight must be null or non-blank ≤ 50 chars but got "
                        + (weight == null ? "null" : "\"" + weight + "\" (len=" + weight.length() + ")");
    }

    // --- Property 6: WebSocket message scoped to current section ---

    // Feature: theater-mode-recommendations, Property 6: WebSocket message scoped to current section

    /**
     * Generator for multi-section snapshots: produces at least 2 sections.
     */
    @Provide
    Arbitrary<Integer> multiSectionCount() {
        return Arbitraries.integers().between(2, 6);
    }

    /**
     * Property 6: WebSocket message scoped to current section.
     *
     * For any session with multiple sections and any valid currentSectionIndex value,
     * the recommendations returned by computeForSection(snapshot, week, day, currentSectionIndex)
     * SHALL contain only ExerciseRecommendation objects whose sectionIndex equals currentSectionIndex.
     * No recommendations for other sections shall be present.
     *
     * Validates: Requirements 5.1, 5.3
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 6: WebSocket message scoped to current section")
    void webSocketMessageScoping_returnsOnlyCurrentSectionRecommendations(
            @ForAll("multiSectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection) {

        // Generate a snapshot with multiple sections, each containing exercises with varied data
        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                String w = ((s + 1) * 10 + e * 5) + "kg";
                String r = (e + 5) + "-" + (e + 8);
                int st = e + 2;
                sectionData.add(Tuple.of(w, r, st));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildHypertrophySnapshot(numSections, exercisesPerSection, exerciseData);

        // Pick an arbitrary valid currentSectionIndex in [0, numSections - 1]
        for (int currentSectionIndex = 0; currentSectionIndex < numSections; currentSectionIndex++) {
            List<ExerciseRecommendation> results = engine.computeForSection(
                    snapshot.json(), 1, 1, currentSectionIndex);

            // Assert that ALL returned recommendations have sectionIndex == currentSectionIndex
            for (ExerciseRecommendation rec : results) {
                assert rec.sectionIndex() == currentSectionIndex :
                        "Expected sectionIndex=" + currentSectionIndex + " but got sectionIndex="
                                + rec.sectionIndex() + " at exerciseIndex=" + rec.exerciseIndex()
                                + " (numSections=" + numSections + ")";
            }

            // Assert that no recommendations for other sections are present
            // (implicitly covered by the above, but also verify count matches expected section size)
            assert results.size() == exercisesPerSection :
                    "Expected " + exercisesPerSection + " recommendations for section " + currentSectionIndex
                            + " but got " + results.size();
        }
    }

    /**
     * Property 6 (variant): WebSocket message scoping with CROSSFIT modality.
     *
     * Validates the same scoping property for CROSSFIT snapshots to ensure modality
     * does not affect section scoping behavior.
     *
     * Validates: Requirements 5.1, 5.3
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 6: WebSocket message scoped to current section — CROSSFIT")
    void webSocketMessageScoping_crossfit_returnsOnlyCurrentSectionRecommendations(
            @ForAll("multiSectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection) {

        // Generate a CROSSFIT snapshot with multiple sections
        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                String w = ((s + 1) * 15 + e * 5) + "kg";
                String r = "AMRAP";
                int st = 1;
                sectionData.add(Tuple.of(w, r, st));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildCrossfitSnapshot(numSections, exercisesPerSection, exerciseData);

        // Pick an arbitrary valid currentSectionIndex in [0, numSections - 1]
        for (int currentSectionIndex = 0; currentSectionIndex < numSections; currentSectionIndex++) {
            List<ExerciseRecommendation> results = engine.computeForSection(
                    snapshot.json(), 1, 1, currentSectionIndex);

            // Assert that ALL returned recommendations have sectionIndex == currentSectionIndex
            for (ExerciseRecommendation rec : results) {
                assert rec.sectionIndex() == currentSectionIndex :
                        "CROSSFIT: Expected sectionIndex=" + currentSectionIndex + " but got sectionIndex="
                                + rec.sectionIndex() + " at exerciseIndex=" + rec.exerciseIndex()
                                + " (numSections=" + numSections + ")";
            }

            // Assert correct count for the section
            assert results.size() == exercisesPerSection :
                    "CROSSFIT: Expected " + exercisesPerSection + " recommendations for section "
                            + currentSectionIndex + " but got " + results.size();
        }
    }

    /**
     * Property 6 (variant): WebSocket message scoping with arbitrary random sectionIndex selection.
     *
     * Uses jqwik to randomly select the currentSectionIndex rather than iterating all,
     * confirming the property holds for any arbitrary valid index.
     *
     * Validates: Requirements 5.1, 5.3
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 6: WebSocket message scoped — random section selection")
    void webSocketMessageScoping_randomSectionIndex_returnsOnlyThatSection(
            @ForAll("multiSectionCount") int numSections,
            @ForAll("exerciseCount") int exercisesPerSection) {

        // Generate a snapshot with multiple sections
        List<List<Tuple3<String, String, Integer>>> exerciseData = new ArrayList<>();
        for (int s = 0; s < numSections; s++) {
            List<Tuple3<String, String, Integer>> sectionData = new ArrayList<>();
            for (int e = 0; e < exercisesPerSection; e++) {
                String w = (s % 2 == 0) ? ((s * 20 + e * 10) + "kg") : null;
                String r = (e % 2 == 0) ? ((e + 3) + "-" + (e + 6)) : null;
                int st = (s + e) % 3 == 0 ? 0 : (e + 1);
                sectionData.add(Tuple.of(w, r, st));
            }
            exerciseData.add(sectionData);
        }

        GeneratedSnapshot snapshot = buildHypertrophySnapshot(numSections, exercisesPerSection, exerciseData);

        // Pick a random valid currentSectionIndex
        int currentSectionIndex = (int) (Math.random() * numSections);

        List<ExerciseRecommendation> results = engine.computeForSection(
                snapshot.json(), 1, 1, currentSectionIndex);

        // Primary assertion: ALL recommendations are scoped to the requested section
        for (ExerciseRecommendation rec : results) {
            assert rec.sectionIndex() == currentSectionIndex :
                    "Expected all recommendations to have sectionIndex=" + currentSectionIndex
                            + " but found sectionIndex=" + rec.sectionIndex();
        }

        // Secondary assertion: results should not be empty (section has exercises)
        assert !results.isEmpty() :
                "Expected non-empty recommendations for section " + currentSectionIndex
                        + " with " + exercisesPerSection + " exercises";
    }
}
