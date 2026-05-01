package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.generation.adapters.inbound.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that when the generation scope is DAY, exactly one training style is provided.
 */
@Documented
@Constraint(validatedBy = DayScopeTrainingStylesValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDayScopeTrainingStyles {

    String message() default "day scope requires exactly one training style";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
