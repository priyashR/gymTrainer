package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.validation;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.dto.GenerateRequest;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.domain.GenerationScope;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that DAY scope requests contain exactly one training style.
 * <p>
 * Non-DAY scopes or null fields are considered valid here — other annotations
 * handle null/empty checks on individual fields.
 */
public class DayScopeTrainingStylesValidator
        implements ConstraintValidator<ValidDayScopeTrainingStyles, GenerateRequest> {

    @Override
    public boolean isValid(GenerateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        // Skip if scope or trainingStyles is null — @NotNull / @NotEmpty handle those
        if (request.scope() == null || request.trainingStyles() == null) {
            return true;
        }
        if (request.scope() == GenerationScope.DAY && request.trainingStyles().size() != 1) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "day scope requires exactly one training style")
                    .addPropertyNode("trainingStyles")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
