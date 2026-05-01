package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.property;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto.GenerateRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationScope;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.TrainingStyle;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import net.jqwik.api.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link GenerateRequest} validation.
 * <p>
 * Feature: workout-creator-service-mvp1
 * <ul>
 *   <li>Property 2: Invalid requests are rejected</li>
 *   <li>Property 3: DAY scope requires exactly one training style</li>
 * </ul>
 * Validates: Requirements 1.2, 1.4
 * <p>
 * No Spring context — uses Jakarta Bean Validation programmatically.
 */
class RequestValidationPropertyTest {

    private static final Validator validator;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ── Generators ────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> validDescriptions() {
        return Arbitraries.of(
                "Heavy back squat day",
                "Full body conditioning session",
                "Upper body hypertrophy focus",
                "CrossFit-style metcon with gymnastics");
    }

    @Provide
    Arbitrary<String> blankDescriptions() {
        return Arbitraries.of("", "   ", "\t", "\n", "  \t\n  ");
    }

    @Provide
    Arbitrary<GenerationScope> validScopes() {
        return Arbitraries.of(GenerationScope.values());
    }

    @Provide
    Arbitrary<List<TrainingStyle>> validNonEmptyStyles() {
        return Arbitraries.of(TrainingStyle.values())
                .list()
                .ofMinSize(1)
                .ofMaxSize(3)
                .uniqueElements();
    }

    @Provide
    Arbitrary<List<TrainingStyle>> multipleStyles() {
        return Arbitraries.of(TrainingStyle.values())
                .list()
                .ofMinSize(2)
                .ofMaxSize(3)
                .uniqueElements();
    }

    @Provide
    Arbitrary<GenerateRequest> validRequests() {
        return Combinators.combine(
                validDescriptions(),
                validScopes(),
                validNonEmptyStyles()
        ).as(GenerateRequest::new)
                .filter(req -> {
                    // Exclude DAY scope with != 1 style (that's a separate property)
                    if (req.scope() == GenerationScope.DAY) {
                        return req.trainingStyles().size() == 1;
                    }
                    return true;
                });
    }

    // ── Property 2: Invalid requests are rejected ─────────────────────

    // Feature: workout-creator-service-mvp1, Property 2: Invalid requests are rejected
    @Property(tries = 100)
    void blankDescriptionIsRejected(
            @ForAll("blankDescriptions") String description,
            @ForAll("validScopes") GenerationScope scope,
            @ForAll("validNonEmptyStyles") List<TrainingStyle> styles) {

        GenerateRequest request = new GenerateRequest(description, scope, styles);
        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        boolean hasDescriptionViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"));

        assertTrue(hasDescriptionViolation,
                "Blank description must produce a validation violation on 'description' field");
    }

    // Feature: workout-creator-service-mvp1, Property 2: Invalid requests are rejected
    @Property(tries = 100)
    void nullDescriptionIsRejected(
            @ForAll("validScopes") GenerationScope scope,
            @ForAll("validNonEmptyStyles") List<TrainingStyle> styles) {

        GenerateRequest request = new GenerateRequest(null, scope, styles);
        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        boolean hasDescriptionViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"));

        assertTrue(hasDescriptionViolation,
                "Null description must produce a validation violation on 'description' field");
    }

    // Feature: workout-creator-service-mvp1, Property 2: Invalid requests are rejected
    @Property(tries = 100)
    void nullScopeIsRejected(
            @ForAll("validDescriptions") String description,
            @ForAll("validNonEmptyStyles") List<TrainingStyle> styles) {

        GenerateRequest request = new GenerateRequest(description, null, styles);
        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        boolean hasScopeViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("scope"));

        assertTrue(hasScopeViolation,
                "Null scope must produce a validation violation on 'scope' field");
    }

    // Feature: workout-creator-service-mvp1, Property 2: Invalid requests are rejected
    @Property(tries = 100)
    void emptyTrainingStylesIsRejected(
            @ForAll("validDescriptions") String description,
            @ForAll("validScopes") GenerationScope scope) {

        GenerateRequest request = new GenerateRequest(description, scope, Collections.emptyList());
        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        boolean hasStylesViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("trainingStyles"));

        assertTrue(hasStylesViolation,
                "Empty trainingStyles must produce a validation violation on 'trainingStyles' field");
    }

    // Feature: workout-creator-service-mvp1, Property 2: Invalid requests are rejected
    @Property(tries = 100)
    void nullTrainingStylesIsRejected(
            @ForAll("validDescriptions") String description,
            @ForAll("validScopes") GenerationScope scope) {

        GenerateRequest request = new GenerateRequest(description, scope, null);
        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        boolean hasStylesViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("trainingStyles"));

        assertTrue(hasStylesViolation,
                "Null trainingStyles must produce a validation violation on 'trainingStyles' field");
    }

    // Feature: workout-creator-service-mvp1, Property 2: Invalid requests are rejected
    @Property(tries = 100)
    void validRequestHasNoViolations(
            @ForAll("validRequests") GenerateRequest request) {

        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(),
                "A valid request must have no validation violations, but got: " + violations);
    }

    // ── Property 3: DAY scope requires exactly one training style ─────

    // Feature: workout-creator-service-mvp1, Property 3: DAY scope requires exactly one training style
    @Property(tries = 100)
    void dayScopeWithMultipleStylesIsRejected(
            @ForAll("validDescriptions") String description,
            @ForAll("multipleStyles") List<TrainingStyle> styles) {

        GenerateRequest request = new GenerateRequest(description, GenerationScope.DAY, styles);
        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        boolean hasDayScopeViolation = violations.stream()
                .anyMatch(v -> v.getMessage().contains("day scope requires exactly one training style"));

        assertTrue(hasDayScopeViolation,
                "DAY scope with " + styles.size() + " training styles must be rejected");
    }

    // Feature: workout-creator-service-mvp1, Property 3: DAY scope requires exactly one training style
    @Property(tries = 100)
    void dayScopeWithEmptyStylesIsRejected(
            @ForAll("validDescriptions") String description) {

        GenerateRequest request = new GenerateRequest(
                description, GenerationScope.DAY, Collections.emptyList());
        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        boolean hasStylesViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("trainingStyles"));

        assertTrue(hasStylesViolation,
                "DAY scope with empty trainingStyles must be rejected");
    }

    // Feature: workout-creator-service-mvp1, Property 3: DAY scope requires exactly one training style
    @Property(tries = 100)
    void dayScopeWithExactlyOneStyleIsValid(
            @ForAll("validDescriptions") String description,
            @ForAll TrainingStyle style) {

        GenerateRequest request = new GenerateRequest(
                description, GenerationScope.DAY, List.of(style));
        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(),
                "DAY scope with exactly one training style must be valid, but got: " + violations);
    }

    @Provide
    Arbitrary<GenerationScope> nonDayScopes() {
        return Arbitraries.of(GenerationScope.WEEK, GenerationScope.FOUR_WEEK);
    }

    // Feature: workout-creator-service-mvp1, Property 3: DAY scope requires exactly one training style
    @Property(tries = 100)
    void nonDayScopeAcceptsMultipleStyles(
            @ForAll("validDescriptions") String description,
            @ForAll("nonDayScopes") GenerationScope scope,
            @ForAll("multipleStyles") List<TrainingStyle> styles) {

        GenerateRequest request = new GenerateRequest(description, scope, styles);
        Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(),
                scope + " scope with multiple training styles must be valid, but got: " + violations);
    }
}
