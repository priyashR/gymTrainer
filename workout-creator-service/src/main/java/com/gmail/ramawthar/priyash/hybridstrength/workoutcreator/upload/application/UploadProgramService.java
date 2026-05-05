package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.application;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.UploadValidationException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto.UploadProgramResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadedProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.inbound.UploadProgramUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.outbound.UploadProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class UploadProgramService implements UploadProgramUseCase {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final UploadParser uploadParser;
    private final UploadProgramRepository uploadProgramRepository;

    public UploadProgramService(UploadParser uploadParser,
                                UploadProgramRepository uploadProgramRepository) {
        this.uploadParser = uploadParser;
        this.uploadProgramRepository = uploadProgramRepository;
    }

    @Override
    public UploadProgramResponse upload(String rawJson, String ownerUserId) {
        ParseResult result = uploadParser.parse(rawJson);

        if (result instanceof ParseResult.Failure failure) {
            throw new UploadValidationException(failure.errors());
        }

        ParseResult.Success success = (ParseResult.Success) result;

        // ownerUserId is always from the JWT subject claim — never client-supplied
        UploadedProgram toSave = UploadedProgram.forUpload(success.program(), ownerUserId);
        UploadedProgram saved = uploadProgramRepository.save(toSave);

        return toResponse(saved);
    }

    private UploadProgramResponse toResponse(UploadedProgram saved) {
        return new UploadProgramResponse(
                saved.id(),
                saved.program().getName(),
                saved.program().getDurationWeeks(),
                saved.program().getGoal(),
                saved.program().getEquipmentProfile(),
                saved.contentSource().name(),
                saved.createdAt() != null ? ISO_FORMATTER.format(saved.createdAt()) : null
        );
    }
}
