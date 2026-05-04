package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;

import java.time.Instant;

/**
 * Value object carrying a {@link Program} and its upload metadata through the
 * application layer.
 * <p>
 * Before persistence: {@code id} and {@code createdAt} are {@code null}.
 * After {@code UploadProgramRepository.save()} returns, the adapter populates
 * all fields from the persisted entity so the application service can build
 * the full {@code UploadProgramResponse} without touching JPA types.
 */
public record UploadedProgram(
        String id,
        Program program,
        String ownerUserId,
        ContentSource contentSource,
        Instant createdAt
) {
    /**
     * Factory for the pre-persistence state — id and createdAt are not yet known.
     */
    public static UploadedProgram forUpload(Program program, String ownerUserId) {
        return new UploadedProgram(null, program, ownerUserId, ContentSource.UPLOADED, null);
    }
}
