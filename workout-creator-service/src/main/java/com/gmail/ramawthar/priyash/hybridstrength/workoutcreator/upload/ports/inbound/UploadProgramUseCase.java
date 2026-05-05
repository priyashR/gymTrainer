package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto.UploadProgramResponse;

/**
 * Inbound port: parse, validate, and persist an uploaded Program JSON.
 * The {@code ownerUserId} MUST be resolved from the JWT subject claim by the caller —
 * never from client-supplied data.
 */
public interface UploadProgramUseCase {
    UploadProgramResponse upload(String rawJson, String ownerUserId);
}
