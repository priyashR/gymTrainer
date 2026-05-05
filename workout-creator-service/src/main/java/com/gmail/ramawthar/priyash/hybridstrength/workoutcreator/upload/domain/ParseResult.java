package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;

import java.util.List;

/**
 * The result of parsing a raw JSON upload string.
 * Either a successfully parsed {@link Program} or a list of validation errors.
 */
public sealed interface ParseResult {

    /**
     * The JSON was valid and mapped to a domain object successfully.
     */
    record Success(Program program) implements ParseResult {}

    /**
     * The JSON failed one or more schema validation rules.
     * The errors list is non-empty and contains at least one entry per violated constraint.
     */
    record Failure(List<UploadValidationError> errors) implements ParseResult {}
}
