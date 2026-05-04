# Implementation Plan: Workout Upload Feature

## Overview

Implement the upload feature across `workout-creator-service` (backend) and `workout-coach-ui` (frontend). The backend adds a new `upload` package following hexagonal architecture, a Flyway migration for `content_source`, two new endpoints, and full test coverage. The frontend adds an `upload` feature module with file picker, preview, JSON editor, and upload state machine.

## Tasks

- [x] 1. Add Flyway migration for `content_source` column
  - Create `V1XX__add_content_source_to_programs.sql` in `src/main/resources/db/migration/` (V100–V199 range)
  - `ALTER TABLE programs ADD COLUMN content_source VARCHAR(20) NOT NULL DEFAULT 'AI_GENERATED'`
  - Backfill existing rows: `UPDATE programs SET content_source = 'AI_GENERATED' WHERE content_source IS NULL`
  - Add `ContentSource` enum (`AI_GENERATED`, `UPLOADED`, `MANUAL`) to `common/model/`
  - Update the existing `Program` JPA entity to map the `content_source` column to `ContentSource`
  - _Requirements: 4.5_

- [x] 2. Implement upload domain layer
  - [x] 2.1 Create `UploadValidationError` record in `upload/domain/`
    - `public record UploadValidationError(String field, String message) {}`
    - Field paths use dot-notation (e.g. `program_structure[0].days[1].blocks[0].movements[0].modality_type`)
    - _Requirements: 2.6, 9.6_

  - [x] 2.2 Create `ParseResult` sealed interface and `UploadedProgram` value object in `upload/domain/`
    - `ParseResult` with `Success(Program program)` and `Failure(List<UploadValidationError> errors)` records
    - `UploadedProgram` wraps a `Program` and a `ContentSource`; it is transient — never persisted directly
    - _Requirements: 2.1, 4.5_

  - [x] 2.3 Implement `UploadParser` in `upload/domain/`
    - Pure Java class — zero framework imports
    - Accepts a raw JSON `String`, returns `ParseResult`
    - Enforce all Upload_Schema constraints from Requirement 1.2:
      - `duration_weeks` must be 1 or 4
      - `version` must be `"1.0"`
      - `equipment_profile` non-empty
      - `program_structure` length equals `duration_weeks`
      - `week_number` unique within `[1, duration_weeks]`
      - `day_number` unique within its week, within `[1, 7]`
      - `blocks` non-empty; `movements` non-empty
      - `modality_type` required on each Movement when parent Day `modality` is `"CrossFit"`
    - Map all Upload_Schema fields to domain model fields per the field mapping table in the design
    - _Requirements: 1.2, 1.3, 2.5, 2.6, 2.7, 2.8, 5.1_
+---------
  - [x] 2.4 Write unit tests for `UploadParser`
    - Test each validation rule in isolation with a specific invalid input
    - Naming: `parse_StateUnderTest_ExpectedBehaviour`
    - Located in `src/test/java/.../unit/upload/UploadParserTest.java`
    - _Requirements: 1.2, 2.5, 2.6, 2.7, 2.8_

  - [x] 2.5 Implement `UploadFormatter` in `upload/domain/`
    - Pure Java class — zero framework imports
    - Accepts a `Program` domain object, returns a canonical JSON `String` conforming to Upload_Schema
    - Map all domain fields back to Upload_Schema fields per the field mapping table in the design
    - _Requirements: 1.3, 5.2_

  - [x] 2.6 Write unit tests for `UploadFormatter`
    - Test that a known `Program` object produces the expected JSON structure
    - Located in `src/test/java/.../unit/upload/UploadFormatterTest.java`
    - _Requirements: 5.2_

  - [x] 2.7 Write property test for parse–format–parse round-trip
    - **Property 5: Parse–format–parse round-trip**
    - **Validates: Requirements 5.3, 5.4**
    - Class: `UploadRoundTripPropertyTest` in `src/test/java/.../property/upload/`
    - `@Property(tries = 100)` — generate valid program JSON with `duration_weeks` randomly from `{1, 4}`, random `CrossFit`/`Hypertrophy` modalities, valid `week_number` and `day_number` sequences
    - Assert `parse(format(parse(json))) ≡ parse(json)` (equivalent `Program` domain objects)
    - _Requirements: 5.3, 5.4_

  - [x] 2.8 Write property test for schema validation
    - **Property 1: Schema validation accepts all valid programs and rejects all invalid ones**
    - **Validates: Requirements 1.2, 2.6**
    - Class: `UploadSchemaValidationPropertyTest` in `src/test/java/.../property/upload/`
    - `@Property(tries = 100)` — valid programs always return `ParseResult.Success`
    - `@Property(tries = 100)` — programs with invalid `duration_weeks` (e.g. 2 or 3) always return `ParseResult.Failure`
    - Cover each constraint from Requirement 1.2 with a targeted invalid-input property
    - _Requirements: 1.2, 2.6_

- [x] 3. Checkpoint — Ensure all domain-layer tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement upload ports and application layer
  - [x] 4.1 Create inbound ports in `upload/ports/inbound/`
    - `UploadProgramUseCase`: `UploadProgramResponse upload(String rawJson, String ownerUserId)`
    - `ValidateProgramUploadUseCase`: `ValidateUploadResponse validate(String rawJson)`
    - _Requirements: 2.1, 9.1_

  - [x] 4.2 Create outbound port in `upload/ports/outbound/`
    - `UploadProgramRepository`: `UploadedProgram save(UploadedProgram program)`
    - _Requirements: 2.1, 4.1_

  - [x] 4.3 Implement `UploadProgramService` in `upload/application/`
    - Implements `UploadProgramUseCase`
    - Annotate with `@Transactional` — full atomicity: if persist fails, nothing is saved
    - Delegate to `UploadParser`; on `ParseResult.Failure` throw `UploadValidationException`
    - Set `owner` from `ownerUserId` parameter (never from client-supplied data)
    - Set `contentSource = UPLOADED` on the `UploadedProgram` before calling `UploadProgramRepository.save()`
    - Map persisted `Program` to `UploadProgramResponse`
    - _Requirements: 2.1, 2.2, 4.1, 4.5, 6.4_

  - [x] 4.4 Write unit tests for `UploadProgramService`
    - Mock `UploadProgramRepository` and `UploadParser`
    - Verify `owner` is set from `ownerUserId`, not any client value
    - Verify `contentSource` is `UPLOADED`
    - Verify `UploadValidationException` is thrown on parse failure
    - Located in `src/test/java/.../unit/upload/UploadProgramServiceTest.java`
    - _Requirements: 2.2, 4.5_

  - [x] 4.5 Implement `ValidateProgramUploadService` in `upload/application/`
    - Implements `ValidateProgramUploadUseCase`
    - Delegates to `UploadParser`; maps `ParseResult` to `ValidateUploadResponse`
    - No repository interaction — nothing is persisted
    - _Requirements: 9.1, 9.2, 9.3_

  - [x] 4.6 Write unit tests for `ValidateProgramUploadService`
    - No mocks needed (pure logic delegation)
    - Verify valid JSON → `ValidateUploadResponse(valid=true, errors=[])`
    - Verify invalid JSON → `ValidateUploadResponse(valid=false, errors=[...])` with at least one error
    - Located in `src/test/java/.../unit/upload/ValidateProgramUploadServiceTest.java`
    - _Requirements: 9.2, 9.3_

- [x] 5. Implement upload adapters (backend)
  - [x] 5.1 Add `UploadValidationException` to `common/exception/`
    - Carries `List<UploadValidationError>`
    - Register handler in `GlobalExceptionHandler` → 400 with `errors` array in platform standard shape
    - _Requirements: 2.6, 9.5, 9.6, 9.7_

  - [x] 5.2 Create response DTOs in `upload/adapters/inbound/dto/`
    - `UploadProgramResponse`: `id`, `programName`, `durationWeeks`, `goal`, `equipmentProfile`, `contentSource` (always `"UPLOADED"`), `createdAt`
    - `ValidateUploadResponse`: `valid`, `errors` (empty list when valid)
    - _Requirements: 2.1, 9.2, 9.3_

  - [x] 5.3 Implement `UploadController` in `upload/adapters/inbound/`
    - `POST /api/v1/uploads/programs` — requires valid JWT; reads body as `String`; checks `body.getBytes(UTF_8).length > 1_048_576` before parsing → 400 if exceeded; delegates to `UploadProgramUseCase`; returns 201
    - `POST /api/v1/uploads/programs/validate` — requires valid JWT; same size check; delegates to `ValidateProgramUploadUseCase`; returns 200
    - Validate `Content-Type: application/json` — return 400 with `"Content-Type must be application/json"` otherwise
    - Validate body is non-empty — return 400 with `"Request body must not be empty"` otherwise
    - Extract `ownerUserId` from JWT subject claim via `SecurityContextHolder`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 6.1, 6.2, 6.3, 9.1, 9.4, 9.5_

  - [x] 5.4 Implement `JpaUploadProgramRepository` in `upload/adapters/outbound/`
    - Implements `UploadProgramRepository`
    - Delegates to the existing JPA entity infrastructure used by the `vault` package
    - Sets `content_source = UPLOADED` on every save
    - Returns the saved `UploadedProgram` (wrapping the persisted `Program` entity)
    - _Requirements: 4.1, 4.2, 4.5_

- [x] 6. Checkpoint — Ensure all backend unit and property tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Write backend integration tests
  - [x] 7.1 Create `UploadIntegrationTest` in `src/test/java/.../integration/upload/`
    - `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`
    - Flyway migrations run on startup; connects to local dev PostgreSQL
    - Happy path: valid 1-week program → 201, program appears in vault listing
    - Happy path: valid 4-week program → 201
    - Content source: uploaded program has `contentSource = UPLOADED` in vault listing
    - Size limit: body > 1 MB → 400 before parsing
    - Wrong `Content-Type` → 400
    - Invalid JSON → 400
    - Schema violation (mismatched weeks) → 400 with field error
    - No JWT → 401
    - Validate endpoint: valid JSON → `{ valid: true }`, vault count unchanged
    - Validate endpoint: invalid JSON → `{ valid: false, errors: [...] }`, vault count unchanged
    - Cleanup via `@Transactional` rollback or explicit `DELETE` in `@AfterEach`
    - _Requirements: 2.1, 2.3, 2.4, 2.5, 2.6, 4.2, 4.5, 6.1, 6.2, 9.1, 9.2, 9.3, 9.4_

  - [x] 7.2 Write property test for upload atomicity
    - **Property 4: Upload atomicity**
    - **Validates: Requirements 6.4**
    - Class: `UploadAtomicityPropertyTest` in `src/test/java/.../property/upload/`
    - `@Property(tries = 100)` — for any invalid program JSON, vault count before and after the failed request must be equal
    - _Requirements: 6.4_

  - [x] 7.3 Write property test for upload persistence invariant
    - **Property 2: Upload persistence invariant**
    - **Validates: Requirements 2.1, 2.2, 4.1, 4.2, 4.5**
    - Class: `UploadPersistencePropertyTest` in `src/test/java/.../property/upload/`
    - `@Property(tries = 100)` — for any valid program JSON with a valid JWT, persisted Program has `owner` = JWT subject, `contentSource = UPLOADED`, and appears in vault listing
    - _Requirements: 2.1, 2.2, 4.1, 4.2, 4.5_

  - [x] 7.4 Write property test for cross-user access denial
    - **Property 3: Cross-user access is denied**
    - **Validates: Requirements 4.3, 4.4**
    - `@Property(tries = 100)` — for any Program uploaded by user A, a request from user B receives 403
    - _Requirements: 4.3, 4.4_

  - [x] 7.5 Write property test for error response shape
    - **Property 6: Error responses conform to platform standard shape**
    - **Validates: Requirements 9.5, 9.7**
    - `@Property(tries = 100)` — for any error response from upload endpoints, body contains `status`, `error`, `message`/`errors`, `path`, `timestamp` and no stack traces or internal identifiers
    - _Requirements: 9.5, 9.7_

  - [x] 7.6 Write property test for validate endpoint correctness
    - **Property 7: Validate endpoint correctness**
    - **Validates: Requirements 9.2, 9.3**
    - `@Property(tries = 100)` — validate endpoint returns `{ valid: true }` iff JSON passes all schema rules; returns `{ valid: false, errors: [...] }` with ≥1 error otherwise; nothing persisted in either case
    - _Requirements: 9.2, 9.3_

- [ ] 8. Checkpoint — Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Add frontend TypeScript types and API client
  - [ ] 9.1 Create `src/types/upload.ts` in `workout-coach-ui`
    - Define all types from the design: `ProgramMetadata`, `WarmCoolEntry`, `Movement`, `Block`, `Day`, `Week`, `ParsedProgram`, `UploadProgramResponse`, `ValidateUploadResponse`
    - _Requirements: 7.3, 7.9, 7.10_

  - [ ] 9.2 Create `src/features/upload/uploadApi.ts`
    - `validateProgram(json: string): Promise<ValidateUploadResponse>` — POST to `/api/v1/uploads/programs/validate`
    - `uploadProgram(json: string): Promise<UploadProgramResponse>` — POST to `/api/v1/uploads/programs` with `Content-Type: application/json`
    - Both use the shared `apiClient` (Axios instance with JWT interceptor)
    - _Requirements: 7.7, 7.12_

- [ ] 10. Implement frontend upload feature module
  - [ ] 10.1 Create `src/features/upload/useUpload.ts`
    - State machine: `idle → file_selected → previewing → editing → uploading → success | error`
    - `UploadState` union type as defined in the design
    - Handle all API error shapes: 401 → redirect to `/login`; 400 with `errors` array → `error` state with `FieldError[]`; 400 with `message` → `error` state with string; network error → generic message
    - Client-side JSON parse errors caught with `try/catch` around `JSON.parse()`, displayed inline
    - _Requirements: 7.3, 7.5, 7.6, 7.8, 7.9, 7.10, 7.11, 7.12_

  - [ ]* 10.2 Write unit tests for `useUpload`
    - Located in `src/features/upload/__tests__/useUpload.test.ts`
    - Verify button is disabled during upload (Property 8)
    - Verify transition to `error` state on 400 response
    - Verify transition to `success` state on 201 response
    - Verify redirect on 401
    - _Requirements: 7.8, 7.9, 7.10, 7.11, 7.12_

  - [ ] 10.3 Create `src/features/upload/FilePicker.tsx`
    - File input accepting only `.json` extension
    - Display 1 MB size hint adjacent to the control
    - Reject non-`.json` files before any request is made
    - _Requirements: 7.1, 7.2_

  - [ ] 10.4 Create `src/features/upload/ProgramPreview.tsx`
    - Display `program_name`, `goal`, `duration_weeks`, `equipment_profile` from `program_metadata`
    - Collapsible breakdown of each week, day, and block with movement names, sets, reps, weight
    - "Save to Vault" and "Edit JSON" action buttons
    - Disable "Save to Vault" and show loading indicator while upload is in progress
    - _Requirements: 7.3, 7.4, 7.8_

  - [ ]* 10.5 Write unit tests for `ProgramPreview`
    - Located in `src/features/upload/__tests__/ProgramPreview.test.tsx`
    - Verify `program_name`, `goal`, `duration_weeks`, `equipment_profile` are rendered
    - Verify week/day/block breakdown is rendered
    - Verify "Save to Vault" and "Edit JSON" buttons are present
    - _Requirements: 7.3, 7.4_

  - [ ]* 10.6 Write property test for preview metadata display
    - **Property 9: Client-side preview displays required metadata fields**
    - **Validates: Requirements 7.3**
    - Using `@fast-check/vitest` with `numRuns: 100`
    - For any valid `ProgramMetadata` object, the rendered preview always displays `program_name`, `goal`, `duration_weeks`, and `equipment_profile`
    - _Requirements: 7.3_

  - [ ] 10.7 Create `src/features/upload/JsonEditor.tsx`
    - Textarea pre-populated with raw JSON content
    - "Preview" button to re-parse and refresh the structured preview
    - Display inline parse error on invalid JSON without clearing editor content
    - _Requirements: 7.5, 7.6_

  - [ ]* 10.8 Write unit tests for `JsonEditor`
    - Located in `src/features/upload/__tests__/JsonEditor.test.tsx`
    - Verify pre-population with raw JSON
    - Verify inline parse error shown on invalid JSON without clearing content
    - _Requirements: 7.5, 7.6_

  - [ ] 10.9 Create `src/features/upload/UploadPage.tsx`
    - Route-level page component composing `FilePicker`, `ProgramPreview`, and `JsonEditor`
    - Driven by `useUpload` state machine
    - Display confirmation message with `program_name` and navigation to Vault on success (201)
    - _Requirements: 7.3, 7.4, 7.5, 7.9_

  - [ ] 10.10 Register `/upload` route in `App.tsx` under `ProtectedRoute`
    - Add `<Route path="/upload" element={<ProtectedRoute><UploadPage /></ProtectedRoute>} />`
    - _Requirements: 7.1_

- [ ] 11. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik (backend, min 100 tries) and `@fast-check/vitest` (frontend, `numRuns: 100`)
- `UploadedProgram` is a transient value object — it is never stored or exposed beyond the application layer
- After `JpaUploadProgramRepository.save()`, the persisted record is a plain `Program` entity; all existing Vault operations are source-agnostic
- Integration tests connect to the local dev PostgreSQL instance — no Testcontainers
