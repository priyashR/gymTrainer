package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.property;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.ExerciseRecommendation;

import net.jqwik.api.*;

import java.util.Objects;

/**
 * Property-based tests for ExerciseRecommendation value object.
 * Tests Property 4 from the design document.
 */
class ExerciseRecommendationPropertyTest {

    // --- Generators ---

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just(" "),
                Arbitraries.just("  "),
                Arbitraries.just("\t"),
                Arbitraries.just("\n"),
                Arbitraries.just(" \t\n "),
                Arbitraries.strings()
                        .withChars(' ', '\t', '\n', '\r')
                        .ofMinLength(1)
                        .ofMaxLength(10)
        );
    }

    @Provide
    Arbitrary<Integer> zeroOrNegativeSets() {
        return Arbitraries.integers().between(-1000, 0);
    }

    @Provide
    Arbitrary<Integer> setsAbove100() {
        return Arbitraries.integers().between(101, 10000);
    }

    @Provide
    Arbitrary<Integer> validSectionIndex() {
        return Arbitraries.integers().between(0, 20);
    }

    @Provide
    Arbitrary<Integer> validExerciseIndex() {
        return Arbitraries.integers().between(0, 20);
    }

    // --- Property 4: Value object rejects invalid construction ---

    // Feature: theater-mode-recommendations, Property 4: Value object rejects invalid construction

    /**
     * Property 4: Value object rejects invalid construction — blank prescribedWeight.
     *
     * For any prescribedWeight value that is blank (whitespace-only or empty),
     * constructing an ExerciseRecommendation SHALL throw IllegalArgumentException.
     *
     * Validates: Requirements 7.4
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 4: Value object rejects invalid construction — blank prescribedWeight")
    void blankPrescribedWeight_throwsIllegalArgumentException(
            @ForAll("validSectionIndex") int sectionIndex,
            @ForAll("validExerciseIndex") int exerciseIndex,
            @ForAll("blankStrings") String blankWeight) {

        boolean exceptionThrown = false;
        try {
            new ExerciseRecommendation(sectionIndex, exerciseIndex, blankWeight, null, null);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        assert exceptionThrown :
                "Expected IllegalArgumentException for blank prescribedWeight=\""
                        + blankWeight.replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }

    /**
     * Property 4: Value object rejects invalid construction — blank prescribedReps.
     *
     * For any prescribedReps value that is blank (whitespace-only or empty),
     * constructing an ExerciseRecommendation SHALL throw IllegalArgumentException.
     *
     * Validates: Requirements 7.4
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 4: Value object rejects invalid construction — blank prescribedReps")
    void blankPrescribedReps_throwsIllegalArgumentException(
            @ForAll("validSectionIndex") int sectionIndex,
            @ForAll("validExerciseIndex") int exerciseIndex,
            @ForAll("blankStrings") String blankReps) {

        boolean exceptionThrown = false;
        try {
            new ExerciseRecommendation(sectionIndex, exerciseIndex, null, blankReps, null);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        assert exceptionThrown :
                "Expected IllegalArgumentException for blank prescribedReps=\""
                        + blankReps.replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }

    /**
     * Property 4: Value object rejects invalid construction — zero or negative prescribedSets.
     *
     * For any prescribedSets value that is ≤ 0, constructing an ExerciseRecommendation
     * SHALL throw IllegalArgumentException.
     *
     * Validates: Requirements 7.4
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 4: Value object rejects invalid construction — zero or negative sets")
    void zeroOrNegativePrescribedSets_throwsIllegalArgumentException(
            @ForAll("validSectionIndex") int sectionIndex,
            @ForAll("validExerciseIndex") int exerciseIndex,
            @ForAll("zeroOrNegativeSets") int invalidSets) {

        boolean exceptionThrown = false;
        try {
            new ExerciseRecommendation(sectionIndex, exerciseIndex, null, null, invalidSets);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        assert exceptionThrown :
                "Expected IllegalArgumentException for prescribedSets=" + invalidSets;
    }

    /**
     * Property 4: Value object rejects invalid construction — prescribedSets above 100.
     *
     * For any prescribedSets value that is > 100, constructing an ExerciseRecommendation
     * SHALL throw IllegalArgumentException.
     *
     * Validates: Requirements 7.4
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 4: Value object rejects invalid construction — sets above 100")
    void setsAbove100_throwsIllegalArgumentException(
            @ForAll("validSectionIndex") int sectionIndex,
            @ForAll("validExerciseIndex") int exerciseIndex,
            @ForAll("setsAbove100") int invalidSets) {

        boolean exceptionThrown = false;
        try {
            new ExerciseRecommendation(sectionIndex, exerciseIndex, null, null, invalidSets);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        assert exceptionThrown :
                "Expected IllegalArgumentException for prescribedSets=" + invalidSets;
    }

    // --- Property 5: Serialization round-trip ---

    // Feature: theater-mode-recommendations, Property 5: Serialization round-trip

    @Provide
    Arbitrary<String> validNonBlankString() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('-', '.', ' ', '%')
                .ofMinLength(1)
                .ofMaxLength(50)
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<Integer> validSets() {
        return Arbitraries.integers().between(1, 100);
    }

    @Provide
    Arbitrary<ExerciseRecommendation> validRecommendations() {
        Arbitrary<Integer> sectionIdx = Arbitraries.integers().between(0, 20);
        Arbitrary<Integer> exerciseIdx = Arbitraries.integers().between(0, 20);
        Arbitrary<String> nonBlank = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('-', '.', ' ', '%')
                .ofMinLength(1)
                .ofMaxLength(50)
                .filter(s -> !s.isBlank());
        Arbitrary<String> nullableWeight = Arbitraries.oneOf(
                Arbitraries.just(null),
                nonBlank
        );
        Arbitrary<String> nullableReps = Arbitraries.oneOf(
                Arbitraries.just(null),
                nonBlank
        );
        Arbitrary<Integer> nullableSets = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.integers().between(1, 100)
        );

        return Combinators.combine(sectionIdx, exerciseIdx, nullableWeight, nullableReps, nullableSets)
                .as(ExerciseRecommendation::new);
    }

    /**
     * Property 5: Serialization round-trip.
     *
     * For any valid ExerciseRecommendation, serializing to JSON via Jackson ObjectMapper
     * and deserializing back SHALL produce an object with identical field values
     * (null-equality for null fields, value-equality for non-null fields).
     *
     * Validates: Requirements 7.6
     */
    @Property(tries = 100)
    @Label("Feature: theater-mode-recommendations, Property 5: Serialization round-trip")
    void serializationRoundTrip_preservesFieldValues(
            @ForAll("validRecommendations") ExerciseRecommendation original) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String json = mapper.writeValueAsString(original);
        ExerciseRecommendation deserialized = mapper.readValue(json, ExerciseRecommendation.class);

        assert original.sectionIndex() == deserialized.sectionIndex() :
                "sectionIndex mismatch: " + original.sectionIndex() + " != " + deserialized.sectionIndex();
        assert original.exerciseIndex() == deserialized.exerciseIndex() :
                "exerciseIndex mismatch: " + original.exerciseIndex() + " != " + deserialized.exerciseIndex();
        assert Objects.equals(original.prescribedWeight(), deserialized.prescribedWeight()) :
                "prescribedWeight mismatch: " + original.prescribedWeight() + " != " + deserialized.prescribedWeight();
        assert Objects.equals(original.prescribedReps(), deserialized.prescribedReps()) :
                "prescribedReps mismatch: " + original.prescribedReps() + " != " + deserialized.prescribedReps();
        assert Objects.equals(original.prescribedSets(), deserialized.prescribedSets()) :
                "prescribedSets mismatch: " + original.prescribedSets() + " != " + deserialized.prescribedSets();
    }
}
