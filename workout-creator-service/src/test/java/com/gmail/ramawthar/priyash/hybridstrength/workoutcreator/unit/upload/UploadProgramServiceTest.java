package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.unit.upload;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception.UploadValidationException;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Program;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto.UploadProgramResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.application.UploadProgramService;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.ParseResult;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadValidationError;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadedProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.outbound.UploadProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UploadProgramService}.
 * Naming convention: MethodName_StateUnderTest_ExpectedBehaviour
 */
@ExtendWith(MockitoExtension.class)
class UploadProgramServiceTest {

    @Mock
    private UploadParser uploadParser;

    @Mock
    private UploadProgramRepository uploadProgramRepository;

    private UploadProgramService service;

    private static final String RAW_JSON = "{\"program_metadata\":{}}";
    private static final String OWNER_USER_ID = "user-abc-123";

    @BeforeEach
    void setUp() {
        service = new UploadProgramService(uploadParser, uploadProgramRepository);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Program minimalProgram() {
        return new Program("Test Program", 1, "Hypertrophy", List.of("Barbell"), List.of());
    }

    private UploadedProgram savedUploadedProgram(Program program, String ownerUserId) {
        return new UploadedProgram(
                "saved-id-999",
                program,
                ownerUserId,
                ContentSource.UPLOADED,
                Instant.parse("2026-05-04T10:00:00Z")
        );
    }

    // ── ownership ─────────────────────────────────────────────────────────────

    @Test
    void upload_ValidJson_SetsOwnerFromOwnerUserIdParameter() {
        Program program = minimalProgram();
        when(uploadParser.parse(RAW_JSON)).thenReturn(new ParseResult.Success(program));
        when(uploadProgramRepository.save(any())).thenReturn(savedUploadedProgram(program, OWNER_USER_ID));

        service.upload(RAW_JSON, OWNER_USER_ID);

        ArgumentCaptor<UploadedProgram> captor = ArgumentCaptor.forClass(UploadedProgram.class);
        verify(uploadProgramRepository).save(captor.capture());
        assertThat(captor.getValue().ownerUserId()).isEqualTo(OWNER_USER_ID);
    }

    @Test
    void upload_ValidJson_DoesNotUseAnyClientSuppliedOwner() {
        // Simulate a different userId being passed — the service must use only ownerUserId
        String differentUserId = "attacker-user-999";
        Program program = minimalProgram();
        when(uploadParser.parse(RAW_JSON)).thenReturn(new ParseResult.Success(program));
        when(uploadProgramRepository.save(any())).thenReturn(savedUploadedProgram(program, differentUserId));

        service.upload(RAW_JSON, differentUserId);

        ArgumentCaptor<UploadedProgram> captor = ArgumentCaptor.forClass(UploadedProgram.class);
        verify(uploadProgramRepository).save(captor.capture());
        // The owner on the persisted object must match exactly what was passed in
        assertThat(captor.getValue().ownerUserId()).isEqualTo(differentUserId);
        // And must NOT be the hardcoded test owner
        assertThat(captor.getValue().ownerUserId()).isNotEqualTo(OWNER_USER_ID);
    }

    // ── content source ────────────────────────────────────────────────────────

    @Test
    void upload_ValidJson_SetsContentSourceToUploaded() {
        Program program = minimalProgram();
        when(uploadParser.parse(RAW_JSON)).thenReturn(new ParseResult.Success(program));
        when(uploadProgramRepository.save(any())).thenReturn(savedUploadedProgram(program, OWNER_USER_ID));

        service.upload(RAW_JSON, OWNER_USER_ID);

        ArgumentCaptor<UploadedProgram> captor = ArgumentCaptor.forClass(UploadedProgram.class);
        verify(uploadProgramRepository).save(captor.capture());
        assertThat(captor.getValue().contentSource()).isEqualTo(ContentSource.UPLOADED);
    }

    @Test
    void upload_ValidJson_ResponseContentSourceIsUploaded() {
        Program program = minimalProgram();
        when(uploadParser.parse(RAW_JSON)).thenReturn(new ParseResult.Success(program));
        when(uploadProgramRepository.save(any())).thenReturn(savedUploadedProgram(program, OWNER_USER_ID));

        UploadProgramResponse response = service.upload(RAW_JSON, OWNER_USER_ID);

        assertThat(response.contentSource()).isEqualTo("UPLOADED");
    }

    // ── parse failure ─────────────────────────────────────────────────────────

    @Test
    void upload_ParseFailure_ThrowsUploadValidationException() {
        List<UploadValidationError> errors = List.of(
                new UploadValidationError("program_metadata.duration_weeks", "must be 1 or 4")
        );
        when(uploadParser.parse(RAW_JSON)).thenReturn(new ParseResult.Failure(errors));

        assertThatThrownBy(() -> service.upload(RAW_JSON, OWNER_USER_ID))
                .isInstanceOf(UploadValidationException.class);
    }

    @Test
    void upload_ParseFailure_ExceptionCarriesValidationErrors() {
        List<UploadValidationError> errors = List.of(
                new UploadValidationError("program_metadata.duration_weeks", "must be 1 or 4"),
                new UploadValidationError("program_metadata.version", "must be \"1.0\"")
        );
        when(uploadParser.parse(RAW_JSON)).thenReturn(new ParseResult.Failure(errors));

        assertThatThrownBy(() -> service.upload(RAW_JSON, OWNER_USER_ID))
                .isInstanceOf(UploadValidationException.class)
                .satisfies(ex -> {
                    UploadValidationException uve = (UploadValidationException) ex;
                    assertThat(uve.getErrors()).hasSize(2);
                    assertThat(uve.getErrors()).extracting(UploadValidationError::field)
                            .containsExactlyInAnyOrder(
                                    "program_metadata.duration_weeks",
                                    "program_metadata.version");
                });
    }

    @Test
    void upload_ParseFailure_RepositoryIsNeverCalled() {
        when(uploadParser.parse(RAW_JSON)).thenReturn(
                new ParseResult.Failure(List.of(
                        new UploadValidationError("$", "Uploaded file is not valid JSON")
                ))
        );

        assertThatThrownBy(() -> service.upload(RAW_JSON, OWNER_USER_ID))
                .isInstanceOf(UploadValidationException.class);

        verifyNoInteractions(uploadProgramRepository);
    }

    // ── response mapping ──────────────────────────────────────────────────────

    @Test
    void upload_ValidJson_ResponseContainsProgramName() {
        Program program = minimalProgram();
        when(uploadParser.parse(RAW_JSON)).thenReturn(new ParseResult.Success(program));
        when(uploadProgramRepository.save(any())).thenReturn(savedUploadedProgram(program, OWNER_USER_ID));

        UploadProgramResponse response = service.upload(RAW_JSON, OWNER_USER_ID);

        assertThat(response.programName()).isEqualTo("Test Program");
    }

    @Test
    void upload_ValidJson_ResponseContainsSavedId() {
        Program program = minimalProgram();
        when(uploadParser.parse(RAW_JSON)).thenReturn(new ParseResult.Success(program));
        when(uploadProgramRepository.save(any())).thenReturn(savedUploadedProgram(program, OWNER_USER_ID));

        UploadProgramResponse response = service.upload(RAW_JSON, OWNER_USER_ID);

        assertThat(response.id()).isEqualTo("saved-id-999");
    }

    @Test
    void upload_ValidJson_ResponseCreatedAtIsIso8601Formatted() {
        Program program = minimalProgram();
        when(uploadParser.parse(RAW_JSON)).thenReturn(new ParseResult.Success(program));
        when(uploadProgramRepository.save(any())).thenReturn(savedUploadedProgram(program, OWNER_USER_ID));

        UploadProgramResponse response = service.upload(RAW_JSON, OWNER_USER_ID);

        assertThat(response.createdAt()).isEqualTo("2026-05-04T10:00:00Z");
    }
}
