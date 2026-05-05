package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain;

/**
 * Describes a single schema validation failure from the Upload_Parser.
 * Field paths use dot-notation for nested fields, e.g.:
 * {@code program_structure[0].days[1].blocks[0].movements[0].modality_type}
 */
public record UploadValidationError(String field, String message) {}
