package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto.ValidateUploadResponse;

/**
 * Inbound port: validate a Program JSON against the Upload_Schema without persisting anything.
 */
public interface ValidateProgramUploadUseCase {
    ValidateUploadResponse validate(String rawJson);
}
