package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.application;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto.ValidateUploadResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.inbound.ValidateProgramUploadUseCase;
import org.springframework.stereotype.Service;

/**
 * Validates a Program JSON against the Upload_Schema without persisting anything.
 * Pure delegation to {@link UploadParser} — no repository interaction.
 */
@Service
public class ValidateProgramUploadService implements ValidateProgramUploadUseCase {

    private final UploadParser uploadParser;

    public ValidateProgramUploadService(UploadParser uploadParser) {
        this.uploadParser = uploadParser;
    }

    @Override
    public ValidateUploadResponse validate(String rawJson) {
        ParseResult result = uploadParser.parse(rawJson);
        if (result instanceof ParseResult.Failure failure) {
            return ValidateUploadResponse.invalid(failure.errors());
        }
        return ValidateUploadResponse.ok();
    }
}
