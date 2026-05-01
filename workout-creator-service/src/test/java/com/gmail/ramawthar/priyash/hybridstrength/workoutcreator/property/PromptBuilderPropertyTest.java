package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationScope;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.PromptBuilder;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.TrainingStyle;

import net.jqwik.api.*;

import java.util.List;

/**
 * Property-based tests for PromptBuilder.
 * <p>
 * Feature: workout-creator-service-mvp1, Property 4: Prompt contains all requested training styles
 * Validates: Requirements 1.6
 */
class PromptBuilderPropertyTest {

    // ── Providers ─────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> descriptions() {
        return Arbitraries.of(
                "Heavy back squat day",
                "Full body hypertrophy session",
                "CrossFit metcon with gymnastics",
                "Upper lower split for strength",
                "Quick conditioning finisher",
                "Leg day with accessory work",
                "Push pull legs program",
                "Olympic lifting focus");
    }

    @Provide
    Arbitrary<GenerationScope> scopes() {
        return Arbitraries.of(GenerationScope.values());
    }

    @Provide
    Arbitrary<List<TrainingStyle>> nonEmptyTrainingStyles() {
        return Arbitraries.of(TrainingStyle.values())
                .list()
                .ofMinSize(1)
                .ofMaxSize(3)
                .uniqueElements();
    }

    // ── Property: prompt contains every requested training style name ─

    // Feature: workout-creator-service-mvp1, Property 4: Prompt contains all requested training styles
    @Property(tries = 100)
    void promptContainsAllRequestedTrainingStyleNames(
            @ForAll("descriptions") String description,
            @ForAll("scopes") GenerationScope scope,
            @ForAll("nonEmptyTrainingStyles") List<TrainingStyle> trainingStyles) {

        String prompt = PromptBuilder.buildPrompt(description, scope, trainingStyles);

        for (TrainingStyle style : trainingStyles) {
            assert prompt.contains(style.name())
                    : "Prompt must contain training style '" + style.name()
                    + "' but got:\n" + prompt;
        }
    }

    // ── Property: prompt contains the description ─────────────────────

    // Feature: workout-creator-service-mvp1, Property 4: Prompt contains all requested training styles
    @Property(tries = 100)
    void promptContainsTheUserDescription(
            @ForAll("descriptions") String description,
            @ForAll("scopes") GenerationScope scope,
            @ForAll("nonEmptyTrainingStyles") List<TrainingStyle> trainingStyles) {

        String prompt = PromptBuilder.buildPrompt(description, scope, trainingStyles);

        assert prompt.contains(description.trim())
                : "Prompt must contain the user description '" + description
                + "' but got:\n" + prompt;
    }

    // ── Property: prompt contains the scope name ──────────────────────

    // Feature: workout-creator-service-mvp1, Property 4: Prompt contains all requested training styles
    @Property(tries = 100)
    void promptContainsTheScopeName(
            @ForAll("descriptions") String description,
            @ForAll("scopes") GenerationScope scope,
            @ForAll("nonEmptyTrainingStyles") List<TrainingStyle> trainingStyles) {

        String prompt = PromptBuilder.buildPrompt(description, scope, trainingStyles);

        assert prompt.contains(scope.name())
                : "Prompt must contain scope '" + scope.name()
                + "' but got:\n" + prompt;
    }

    // ── Property: prompt is non-empty for any valid input ─────────────

    // Feature: workout-creator-service-mvp1, Property 4: Prompt contains all requested training styles
    @Property(tries = 100)
    void promptIsNonEmptyForValidInput(
            @ForAll("descriptions") String description,
            @ForAll("scopes") GenerationScope scope,
            @ForAll("nonEmptyTrainingStyles") List<TrainingStyle> trainingStyles) {

        String prompt = PromptBuilder.buildPrompt(description, scope, trainingStyles);

        assert prompt != null && !prompt.isBlank()
                : "Prompt must not be null or blank for valid input";
    }
}
