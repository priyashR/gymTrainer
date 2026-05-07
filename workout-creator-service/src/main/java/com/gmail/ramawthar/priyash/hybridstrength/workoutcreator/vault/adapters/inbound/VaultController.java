package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.dto.ErrorResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto.PaginatedResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto.VaultItemResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto.VaultProgramDetailResponse;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.SearchCriteria;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultItem;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.VaultProgram;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.ports.inbound.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound adapter for the vault feature — exposes CRUD and search endpoints.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/v1/vault/programs} — list programs (paginated)</li>
 *   <li>{@code GET /api/v1/vault/programs/{id}} — get program detail</li>
 *   <li>{@code PUT /api/v1/vault/programs/{id}} — update program (full JSON replacement)</li>
 *   <li>{@code DELETE /api/v1/vault/programs/{id}} — delete program</li>
 *   <li>{@code POST /api/v1/vault/programs/{id}/copy} — copy program</li>
 *   <li>{@code GET /api/v1/vault/programs/search} — search with filters</li>
 * </ul>
 *
 * <p>All endpoints require a valid JWT. The {@code ownerUserId} is resolved from the JWT subject
 * claim via {@link SecurityContextHolder} — never from client-supplied data.
 */
@RestController
@RequestMapping("/api/v1/vault/programs")
public class VaultController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ListProgramsUseCase listProgramsUseCase;
    private final GetProgramUseCase getProgramUseCase;
    private final UpdateProgramUseCase updateProgramUseCase;
    private final DeleteProgramUseCase deleteProgramUseCase;
    private final CopyProgramUseCase copyProgramUseCase;
    private final SearchProgramsUseCase searchProgramsUseCase;

    public VaultController(ListProgramsUseCase listProgramsUseCase,
                           GetProgramUseCase getProgramUseCase,
                           UpdateProgramUseCase updateProgramUseCase,
                           DeleteProgramUseCase deleteProgramUseCase,
                           CopyProgramUseCase copyProgramUseCase,
                           SearchProgramsUseCase searchProgramsUseCase) {
        this.listProgramsUseCase = listProgramsUseCase;
        this.getProgramUseCase = getProgramUseCase;
        this.updateProgramUseCase = updateProgramUseCase;
        this.deleteProgramUseCase = deleteProgramUseCase;
        this.copyProgramUseCase = copyProgramUseCase;
        this.searchProgramsUseCase = searchProgramsUseCase;
    }

    /**
     * List all programs in the authenticated user's vault, paginated.
     *
     * @param page zero-indexed page number (default 0)
     * @param size page size (default 20, max 100)
     * @return 200 OK with paginated list of {@link VaultItemResponse}
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listPrograms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        ResponseEntity<?> sizeError = validatePageSize(size, request);
        if (sizeError != null) return sizeError;

        String ownerUserId = resolveOwnerUserId();
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        Page<VaultItem> result = listProgramsUseCase.listPrograms(ownerUserId, pageable);

        PaginatedResponse<VaultItemResponse> response =
                PaginatedResponse.from(result, VaultItemResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the full detail of a single program.
     *
     * @param id program UUID
     * @return 200 OK with {@link VaultProgramDetailResponse}, or 403 if not found/not owned
     */
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProgram(@PathVariable String id, HttpServletRequest request) {
        UUID programId = parseUuid(id);
        if (programId == null) {
            return badRequest("Invalid program ID format", request);
        }

        String ownerUserId = resolveOwnerUserId();
        VaultProgram program = getProgramUseCase.getProgram(programId, ownerUserId);

        VaultProgramDetailResponse response = VaultProgramDetailResponse.from(program);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a program's content via full JSON replacement.
     *
     * @param id   program UUID
     * @param body raw JSON conforming to Upload_Schema
     * @return 200 OK with updated {@link VaultItemResponse}, or 400/403 on error
     */
    @PutMapping(path = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateProgram(
            @PathVariable String id,
            @RequestBody String body,
            HttpServletRequest request) {

        UUID programId = parseUuid(id);
        if (programId == null) {
            return badRequest("Invalid program ID format", request);
        }

        String ownerUserId = resolveOwnerUserId();
        VaultItem updated = updateProgramUseCase.updateProgram(programId, body, ownerUserId);

        return ResponseEntity.ok(VaultItemResponse.from(updated));
    }

    /**
     * Delete a program from the vault.
     *
     * @param id program UUID
     * @return 204 No Content on success, or 403 if not found/not owned
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProgram(@PathVariable String id, HttpServletRequest request) {
        UUID programId = parseUuid(id);
        if (programId == null) {
            return badRequest("Invalid program ID format", request);
        }

        String ownerUserId = resolveOwnerUserId();
        deleteProgramUseCase.deleteProgram(programId, ownerUserId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Copy/duplicate a program in the vault.
     *
     * @param id source program UUID
     * @return 201 Created with the new {@link VaultItemResponse}, or 403 if not found/not owned
     */
    @PostMapping(path = "/{id}/copy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> copyProgram(@PathVariable String id, HttpServletRequest request) {
        UUID programId = parseUuid(id);
        if (programId == null) {
            return badRequest("Invalid program ID format", request);
        }

        String ownerUserId = resolveOwnerUserId();
        VaultItem copy = copyProgramUseCase.copyProgram(programId, ownerUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(VaultItemResponse.from(copy));
    }

    /**
     * Search programs in the vault with keyword and optional filters.
     *
     * @param q         keyword query (searches name and goal, case-insensitive)
     * @param focusArea optional filter by day focus area
     * @param modality  optional filter by day modality
     * @param page      zero-indexed page number (default 0)
     * @param size      page size (default 20, max 100)
     * @return 200 OK with paginated search results, or 400 for invalid query
     */
    @GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchPrograms(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String focusArea,
            @RequestParam(required = false) String modality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        ResponseEntity<?> sizeError = validatePageSize(size, request);
        if (sizeError != null) return sizeError;

        String ownerUserId = resolveOwnerUserId();
        SearchCriteria criteria = new SearchCriteria(q, focusArea, modality);
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));

        Page<VaultItem> result = searchProgramsUseCase.searchPrograms(criteria, ownerUserId, pageable);

        PaginatedResponse<VaultItemResponse> response =
                PaginatedResponse.from(result, VaultItemResponse::from);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the authenticated user's ID from the JWT subject claim.
     */
    private String resolveOwnerUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getPrincipal().toString();
    }

    /**
     * Validates that the requested page size does not exceed the maximum.
     *
     * @return a 400 response if size exceeds max, null otherwise
     */
    private ResponseEntity<?> validatePageSize(int size, HttpServletRequest request) {
        if (size > MAX_PAGE_SIZE) {
            return badRequest("Page size must not exceed 100", request);
        }
        return null;
    }

    /**
     * Attempts to parse a string as a UUID.
     *
     * @return the parsed UUID, or null if the format is invalid
     */
    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ResponseEntity<?> badRequest(String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
