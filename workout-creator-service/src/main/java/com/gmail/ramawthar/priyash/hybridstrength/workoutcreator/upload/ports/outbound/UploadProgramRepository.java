package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadedProgram;

/**
 * Outbound port: persist an uploaded Program to the Vault data store.
 * The adapter implementation sets {@code content_source = UPLOADED} on every save.
 */
public interface UploadProgramRepository {
    UploadedProgram save(UploadedProgram program);
}
