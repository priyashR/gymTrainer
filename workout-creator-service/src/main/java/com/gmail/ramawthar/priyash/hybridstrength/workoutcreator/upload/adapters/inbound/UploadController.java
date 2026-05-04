package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto.UploadProgramResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.adapters.inbound.dto.ValidateUploadResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.inbound.UploadProgramUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.ports.inbound.ValidateProgramUploadUseCase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Inbound adapter for the upload feature.
 *
 * <p>Two endpoints:
 * <ul>
 *   <li>{@code POST /api/v1/uploads/programs} — parse, validate, persist; returns 201</li>
 *   <li>{@code POST /api/v1/uploads/programs/validate} — validate only, no persistence; returns 200</li>
 * </ul>
 *
 * <p>Both require a valid JWT. The {@code ownerUserId} is always resolved from the JWT subject
 * claim via {@link SecurityContextHolder} — never from client-supplied data.
 */
@RestController
@RequestMapping("/api/v1/uploads/programs")
public class UploadController {

    private static final int MAX_BODY_BYTES = 1_048_576; // 1 MB

    private final UploadProgramUseCase uploadProgramUseCase;
    private final ValidateProgramUploadUseCase validateProgramUploadUseCase;

    public UploadController(UploadProgramUseCase uploadProgramUseCase,
                            ValidateProgramUploadUseCase validateProgramUploadUseCase) {
        this.uploadProgramUseCase = uploadProgramUseCase;
        this.validateProgramUploadUseCase = validateProgramUploadUseCase;
    }

    /**
     * Upload a Program JSON and persist it to the authenticated user's Vault.
     *
     * @return 201 Created with {@link UploadProgramResponse}
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadProgram(@RequestBody String body, HttpServletRequest request) {
        ResponseEntity<?> guardError = guardRequest(body, request);
        if (guardError != null) return guardError;

        String ownerUserId = resolveOwnerUserId();
        UploadProgramResponse response = uploadProgramUseCase.upload(body, ownerUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Validate a Program JSON against the Upload_Schema without persisting anything.
     *
     * @return 200 OK with {@link ValidateUploadResponse}
     */
    @PostMapping(path = "/validate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateProgram(@RequestBody String body, HttpServletRequest request) {
        ResponseEntity<?> guardError = guardRequest(body, request);
        if (guardError != null) return guardError;

        ValidateUploadResponse response = validateProgramUploadUseCase.validate(body);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Runs pre-parse guards: empty body, size limit.
     * Content-Type is enforced by Spring via {@code consumes} on the mapping — a mismatch
     * results in a 415 by default, but the spec requires 400 with a specific message.
     * That case is handled by the {@link UnsupportedMediaTypeHandler} companion class.
     *
     * @return a 400 {@link ResponseEntity} if a guard fails, {@code null} otherwise
     */
    private ResponseEntity<?> guardRequest(String body, HttpServletRequest request) {
        if (body == null || body.isBlank()) {
            return badRequest("Request body must not be empty", request);
        }
        if (body.getBytes(StandardCharsets.UTF_8).length > MAX_BODY_BYTES) {
            return badRequest("File size exceeds the maximum allowed limit of 1 MB", request);
        }
        return null;
    }

    private ResponseEntity<?> badRequest(String message, HttpServletRequest request) {
        var body = new com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto.ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                java.time.Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Extracts the authenticated user's ID from the JWT subject claim.
     * The JWT filter populates the {@link SecurityContextHolder} with the user's UUID as principal.
     */
    private String resolveOwnerUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getPrincipal().toString();
    }
}
