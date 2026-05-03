# Design Document — Workout Upload Feature

## Overview

The Workout Upload feature adds a new `upload` package to the `workout-creator-service`, enabling authenticated users to import Programs from JSON files into their Vault. It also adds a corresponding upload UI to `workout-coach-ui`.

The feature introduces two new endpoints:
- `POST /api/v1/uploads/programs` — parse, validate, and persist a Program
- `POST /api/v1/uploads/programs/validate` — validate without persisting (dry-run for the UI preview)

Uploaded Programs are stored in the same Vault tables as AI-generated content, distinguished by a `content_source` column set to `UPLOADED`. Ownership is always resolved from the JWT subject claim — never from client-supplied data.

Once persisted, an uploaded Program is indistinguishable from any other Vault item at the query layer. The existing Vault listing, search, browse, and management endpoints serve all content regardless of how it entered the Vault — uploaded, AI-generated, or manually created. The `content_source` value is metadata only; it does not affect any Vault behaviour.

The `Upload_Parser` and `Upload_Formatter` are inverses of each other and must satisfy a round-trip correctness property: `parse(format(parse(json)))` produces a domain object equivalent to `parse(json)`.

---

## Architecture

The upload feature follows the same hexagonal architecture used by the existing `generation`, `vault`, and `search` packages.

```
upload/
├── domain/
│   ├── UploadedProgram.java          # Value object wrapping Program + source metadata
│   ├── UploadParser.java             # Deserialises JSON → Program domain object
│   └── UploadFormatter.java          # Serialises Program domain object → canonical JSON
├── ports/
│   ├── inbound/
│   │   ├── UploadProgramUseCase.java
│   │   └── ValidateProgramUploadUseCase.java
│   └── outbound/
│       └── UploadProgramRepository.java
├── application/
│   ├── UploadProgramService.java
│   └── ValidateProgramUploadService.java
└── adapters/
    ├── inbound/
    │   ├── UploadController.java
    │   └── dto/
    │       ├── UploadProgramResponse.java
    │       └── ValidateUploadResponse.java
    └── outbound/
        └── JpaUploadProgramRepository.java
```

The domain layer (`UploadParser`, `UploadFormatter`) has zero framework imports. Jackson is used only in the adapter layer for HTTP binding; the parser receives a pre-read `String` or `JsonNode` and works with plain Java.

### Frontend

A new `upload` feature module is added to `workout-coach-ui`:

```
src/features/upload/
├── UploadPage.tsx              # Route-level page component
├── FilePicker.tsx              # File input with .json filter and size hint
├── ProgramPreview.tsx          # Structured preview of parsed program
├── JsonEditor.tsx              # Inline JSON editor (textarea + parse feedback)
├── useUpload.ts                # Hook managing upload state machine
├── uploadApi.ts                # API calls: validate + upload
└── __tests__/
    ├── ProgramPreview.test.tsx
    ├── JsonEditor.test.tsx
    └── useUpload.test.ts
```

A new route `/upload` is added to `App.tsx` under `ProtectedRoute`.

---

## Components and Interfaces

### Backend

#### UploadController

Handles two endpoints. Both require a valid JWT (`Authorization: Bearer <token>`).

```
POST /api/v1/uploads/programs
  Content-Type: application/json
  Body: <program JSON, max 1 MB>
  → 201 Created: UploadProgramResponse
  → 400 Bad Request: ValidationErrorResponse | ErrorResponse
  → 401 Unauthorised: ErrorResponse

POST /api/v1/uploads/programs/validate
  Content-Type: application/json
  Body: <program JSON, max 1 MB>
  → 200 OK: ValidateUploadResponse
  → 401 Unauthorised: ErrorResponse
```

Size enforcement is done via Spring's `spring.servlet.multipart.max-request-size` and a `@RequestBody` size guard — the controller reads the raw body as a `String` and checks `body.length()` before passing to the parser. This ensures the limit is enforced before any JSON parsing begins (Requirement 6.1).

#### UploadProgramUseCase (inbound port)

```java
public interface UploadProgramUseCase {
    UploadProgramResponse upload(String rawJson, String ownerUserId);
}
```

#### ValidateProgramUploadUseCase (inbound port)

```java
public interface ValidateProgramUploadUseCase {
    ValidateUploadResponse validate(String rawJson);
}
```

#### UploadProgramRepository (outbound port)

```java
public interface UploadProgramRepository {
    UploadedProgram save(UploadedProgram program);
}
```

This port is implemented by `JpaUploadProgramRepository`, which delegates to the existing JPA entity infrastructure used by the `vault` package. The `content_source` column is set to `UPLOADED` on every save.

#### UploadParser

Pure domain class. Accepts a `String` of raw JSON and returns either a `Program` domain object or a structured list of `UploadValidationError` records.

```java
public sealed interface ParseResult {
    record Success(Program program) implements ParseResult {}
    record Failure(List<UploadValidationError> errors) implements ParseResult {}
}

public class UploadParser {
    public ParseResult parse(String rawJson) { ... }
}
```

Validation logic enforced by the parser:
- `program_metadata.duration_weeks` must be `1` or `4`
- `program_metadata.version` must be `"1.0"`
- `program_metadata.equipment_profile` must have at least one non-empty entry
- `program_structure` length must equal `duration_weeks`
- Each `week_number` unique within `[1, duration_weeks]`
- Each `day_number` unique within its week, within `[1, 7]`
- Each `blocks` array non-empty; each `movements` array non-empty
- `modality_type` required on each Movement when parent Day `modality` is `"CrossFit"`

#### UploadFormatter

Pure domain class. Accepts a `Program` domain object and returns a canonical JSON `String` conforming to the Upload_Schema.

```java
public class UploadFormatter {
    public String format(Program program) { ... }
}
```

#### UploadValidationError

```java
public record UploadValidationError(String field, String message) {}
```

Field paths use dot-notation for nested fields (e.g., `program_structure[0].days[1].blocks[0].movements[0].modality_type`).

### Response DTOs

```java
// 201 response
public record UploadProgramResponse(
    String id,
    String programName,
    int durationWeeks,
    String goal,
    List<String> equipmentProfile,
    String contentSource,   // always "UPLOADED"
    String createdAt
) {}

// 200 response from validate endpoint
public record ValidateUploadResponse(
    boolean valid,
    List<UploadValidationError> errors  // empty list when valid
) {}
```

### Frontend

#### useUpload hook

Manages a state machine with states: `idle → file_selected → previewing → editing → uploading → success | error`.

```typescript
type UploadState =
  | { status: 'idle' }
  | { status: 'file_selected'; file: File }
  | { status: 'previewing'; program: ParsedProgram; rawJson: string }
  | { status: 'editing'; rawJson: string; parseError?: string }
  | { status: 'uploading'; rawJson: string }
  | { status: 'success'; programName: string; programId: string }
  | { status: 'error'; errors: FieldError[] | string };
```

#### uploadApi.ts

```typescript
export async function validateProgram(json: string): Promise<ValidateUploadResponse>
export async function uploadProgram(json: string): Promise<UploadProgramResponse>
```

Both use the shared `apiClient` (Axios instance with JWT interceptor). The `uploadProgram` call sends `Content-Type: application/json` with the raw JSON string as the body.

---

## Data Models

### Domain Model

The upload feature reuses the existing `Program` domain object from the `vault`/`generation` packages. The mapping between the Upload_Schema JSON fields and the domain model is:

| Upload_Schema field | Domain field |
|---|---|
| `program_metadata.program_name` | `Program.name` |
| `program_metadata.duration_weeks` | `Program.durationWeeks` |
| `program_metadata.goal` | `Program.goal` |
| `program_metadata.equipment_profile` | `Program.equipmentProfile` |
| `program_metadata.version` | parsed/validated, not stored on domain object |
| `program_structure[].week_number` | `Week.weekNumber` |
| `program_structure[].days[].day_number` | `Day.dayNumber` |
| `program_structure[].days[].day_label` | `Day.label` |
| `program_structure[].days[].focus_area` | `Day.focusArea` |
| `program_structure[].days[].modality` | `Day.modality` (enum: `CROSSFIT`, `HYPERTROPHY`) |
| `program_structure[].days[].warm_up[]` | `Day.warmUp` (list of `WarmUpEntry`) |
| `program_structure[].days[].blocks[]` | `Day.sections` (list of `Section`) |
| `program_structure[].days[].blocks[].block_type` | `Section.name` |
| `program_structure[].days[].blocks[].format` | `Section.sectionType` (mapped from string) |
| `program_structure[].days[].blocks[].time_cap_minutes` | `Section.timeCap` |
| `program_structure[].days[].blocks[].movements[]` | `Section.exercises` (list of `Exercise`) |
| `program_structure[].days[].blocks[].movements[].exercise_name` | `Exercise.name` |
| `program_structure[].days[].blocks[].movements[].modality_type` | `Exercise.modalityType` (enum: `ENGINE`, `GYMNASTICS`, `WEIGHTLIFTING`, nullable) |
| `program_structure[].days[].blocks[].movements[].prescribed_sets` | `Exercise.sets` |
| `program_structure[].days[].blocks[].movements[].prescribed_reps` | `Exercise.reps` |
| `program_structure[].days[].blocks[].movements[].prescribed_weight` | `Exercise.weight` |
| `program_structure[].days[].blocks[].movements[].rest_interval_seconds` | `Exercise.restSeconds` |
| `program_structure[].days[].blocks[].movements[].notes` | `Exercise.notes` |
| `program_structure[].days[].cool_down[]` | `Day.coolDown` (list of `CoolDownEntry`) |
| `program_structure[].days[].methodology_source` | `Day.methodologySource` |

### Database Schema

The upload feature adds a `content_source` column to the existing `programs` table (Flyway migration `V1XX__add_content_source_to_programs.sql`). The exact migration number is assigned within the V100–V199 range.

```sql
ALTER TABLE programs
  ADD COLUMN content_source VARCHAR(20) NOT NULL DEFAULT 'AI_GENERATED';

-- Backfill existing rows
UPDATE programs SET content_source = 'AI_GENERATED' WHERE content_source IS NULL;
```

The `ContentSource` enum in Java:

```java
public enum ContentSource {
    AI_GENERATED,
    UPLOADED,
    MANUAL
}
```

No new tables are required — uploaded Programs share the same `programs`, `weeks`, `days`, `sections`, and `exercises` tables as AI-generated content.

**Vault consistency guarantee:** After `JpaUploadProgramRepository.save()` completes, the persisted record is a standard `Program` entity with `contentSource = UPLOADED`. The `UploadedProgram` value object is a transient upload-flow construct only — it exists to carry the parsed domain object and source metadata through the application layer and is never stored or exposed beyond that boundary. All downstream Vault operations (list, get, search, filter, delete) operate on the `Program` entity directly and are completely unaware of how the content entered the Vault. A User browsing or searching their Vault will see uploaded Programs alongside AI-generated and manually created ones with no behavioural difference.

### Frontend Types

```typescript
// src/types/upload.ts

export interface ProgramMetadata {
  program_name: string;
  duration_weeks: 1 | 4;
  goal: string;
  equipment_profile: string[];
  version: '1.0';
}

export interface WarmCoolEntry {
  movement: string;
  instruction: string;
}

export interface Movement {
  exercise_name: string;
  modality_type?: 'Engine' | 'Gymnastics' | 'Weightlifting';
  prescribed_sets: number;
  prescribed_reps: string;
  prescribed_weight?: string;
  rest_interval_seconds?: number;
  notes?: string;
}

export interface Block {
  block_type: string;
  format: string;
  time_cap_minutes?: number;
  movements: Movement[];
}

export interface Day {
  day_number: number;
  day_label: string;
  focus_area: string;
  modality: 'CrossFit' | 'Hypertrophy';
  warm_up: WarmCoolEntry[];
  blocks: Block[];
  cool_down: WarmCoolEntry[];
  methodology_source?: string;
}

export interface Week {
  week_number: number;
  days: Day[];
}

export interface ParsedProgram {
  program_metadata: ProgramMetadata;
  program_structure: Week[];
}

export interface UploadProgramResponse {
  id: string;
  programName: string;
  durationWeeks: number;
  goal: string;
  equipmentProfile: string[];
  contentSource: 'UPLOADED';
  createdAt: string;
}

export interface ValidateUploadResponse {
  valid: boolean;
  errors: Array<{ field: string; message: string }>;
}
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Schema validation accepts all valid programs and rejects all invalid ones

*For any* Program JSON object, the `UploadParser` should accept it if and only if it satisfies all Upload_Schema constraints: `duration_weeks` is 1 or 4, `version` is `"1.0"`, `equipment_profile` is non-empty, `program_structure` length equals `duration_weeks`, all `week_number` values are unique and within range, all `day_number` values are unique within their week, all `blocks` arrays are non-empty, all `movements` arrays are non-empty, and `modality_type` is present on each Movement when the parent Day modality is `CrossFit`.

**Validates: Requirements 1.2, 2.6**

### Property 2: Upload persistence invariant

*For any* valid Program JSON submitted to `POST /api/v1/uploads/programs` with a valid JWT, the persisted Program should have its `owner` set to the JWT subject claim (not any client-supplied value), its `contentSource` set to `UPLOADED`, and it should appear in the authenticated user's Vault listing response.

**Validates: Requirements 2.1, 2.2, 4.1, 4.2, 4.5**

### Property 3: Cross-user access is denied

*For any* Program uploaded by user A, a request from user B (different JWT subject) to read, modify, or delete that Program should receive a 403 Forbidden response.

**Validates: Requirements 4.3, 4.4**

### Property 4: Upload atomicity

*For any* upload request that fails validation (schema error, size violation, malformed JSON), the Vault should contain exactly the same Programs after the failed request as it did before — no partial state is persisted.

**Validates: Requirements 6.4**

### Property 5: Parse–format–parse round-trip

*For any* valid Program upload JSON (including programs with `duration_weeks` of 1 and 4, and days with both `CrossFit` and `Hypertrophy` modalities), parsing then formatting then parsing should produce a Program domain object equivalent to the one produced by the first parse: `parse(format(parse(json))) ≡ parse(json)`.

**Validates: Requirements 5.3, 5.4**

### Property 6: Error responses conform to platform standard shape

*For any* error response from the upload endpoints (`/api/v1/uploads/programs` and `/api/v1/uploads/programs/validate`), the response body should contain `status`, `error`, `message` (or `errors` array for validation failures), `path`, and `timestamp` fields, and should contain no stack traces, internal class names, SQL, or internal identifiers.

**Validates: Requirements 9.5, 9.7**

### Property 7: Validate endpoint correctness

*For any* Program JSON, the `POST /api/v1/uploads/programs/validate` endpoint should return `{ "valid": true }` if and only if the JSON passes all Upload_Schema validation rules, and `{ "valid": false, "errors": [...] }` with at least one error entry otherwise — without persisting anything to the Vault.

**Validates: Requirements 9.2, 9.3**

### Property 8: Upload button disabled during in-progress request

*For any* upload state where a request is in flight, the "Save to Vault" button in the UI should be disabled and a loading indicator should be visible, preventing duplicate submissions.

**Validates: Requirements 7.8**

### Property 9: Client-side preview displays required metadata fields

*For any* valid Program JSON selected via the file picker, the rendered preview component should display `program_name`, `goal`, `duration_weeks`, and `equipment_profile` from `program_metadata`.

**Validates: Requirements 7.3**

---

## Error Handling

### Backend

| Condition | HTTP Status | Response shape |
|---|---|---|
| Missing or invalid JWT | 401 | `ErrorResponse` with `message: "Unauthorised"` |
| `Content-Type` not `application/json` | 400 | `ErrorResponse` with `message: "Content-Type must be application/json"` |
| Empty request body | 400 | `ErrorResponse` with `message: "Request body must not be empty"` |
| Body exceeds 1 MB | 400 | `ErrorResponse` with `message: "File size exceeds the maximum allowed limit of 1 MB"` |
| Body is not valid JSON | 400 | `ErrorResponse` with `message: "Uploaded file is not valid JSON"` |
| JSON fails schema validation | 400 | `ValidationErrorResponse` with `errors` array, each entry having `field` (dot-notation) and `message` |
| Accessing another user's resource | 403 | `ErrorResponse` with `message: "Forbidden"` |
| Unexpected server error | 500 | `ErrorResponse` with generic message — no internal detail |

All responses use the platform standard shape defined in `api-standards.md`. The `GlobalExceptionHandler` in `common/exception/` handles all cases. A new `UploadValidationException` is added to the exception hierarchy, carrying the list of `UploadValidationError` records.

Size enforcement is applied before JSON parsing. Spring's `spring.servlet.multipart.max-request-size` is set to `1MB`. Additionally, the controller reads the body as a `String` and checks `body.getBytes(StandardCharsets.UTF_8).length > 1_048_576` before invoking the use case, returning 400 immediately if exceeded.

Upload operations are wrapped in a `@Transactional` boundary in the application service. If any step after parsing fails (e.g., a database error during persist), the transaction rolls back and nothing is persisted.

### Frontend

The `useUpload` hook handles all API error shapes:

- `401` → calls `onRefreshFailure` via the existing Axios interceptor, which redirects to `/login`
- `400` with `errors` array → transitions to `{ status: 'error', errors: FieldError[] }`, opens JSON editor
- `400` with `message` → transitions to `{ status: 'error', errors: string }`, displays message
- Network error → transitions to `{ status: 'error', errors: 'Network error. Please try again.' }`

Client-side JSON parse errors (from the file picker or editor) are caught with a `try/catch` around `JSON.parse()` and displayed inline without clearing the editor content.

---

## Testing Strategy

### Unit Tests (JUnit 5 + Mockito)

Located in `src/test/java/.../unit/upload/`.

- `UploadParser` — test each validation rule in isolation with specific invalid inputs (wrong `duration_weeks`, mismatched `program_structure` length, missing `modality_type` on CrossFit day, empty `blocks`, etc.)
- `UploadFormatter` — test that a known `Program` object produces the expected JSON structure
- `UploadProgramService` — mock `UploadProgramRepository`, verify ownership is set from the provided `ownerUserId` and `contentSource` is `UPLOADED`
- `ValidateProgramUploadService` — mock nothing (pure logic), verify it delegates to `UploadParser` and maps results correctly

Naming convention: `MethodName_StateUnderTest_ExpectedBehaviour`

### Property-Based Tests (jqwik, minimum 100 tries)

Located in `src/test/java/.../property/upload/`.

**`UploadRoundTripPropertyTest`** — implements Property 5:
```java
// Feature: workout-creator-service-upload, Property 5: parse-format-parse round-trip
@Property(tries = 100)
void parseFormatParse_validProgram_producesEquivalentDomainObject(
    @ForAll @From("validProgramJson") String json) {
    ParseResult first = parser.parse(json);
    assertThat(first).isInstanceOf(ParseResult.Success.class);
    Program p1 = ((ParseResult.Success) first).program();
    String formatted = formatter.format(p1);
    ParseResult second = parser.parse(formatted);
    assertThat(second).isInstanceOf(ParseResult.Success.class);
    Program p2 = ((ParseResult.Success) second).program();
    assertThat(p2).isEqualTo(p1);
}
```

The `validProgramJson` arbitrary generates programs with:
- `duration_weeks` randomly chosen from `{1, 4}`
- Days with randomly chosen `modality` (`CrossFit` or `Hypertrophy`)
- `modality_type` present on all movements when modality is `CrossFit`, absent when `Hypertrophy`
- Random but valid `week_number` and `day_number` sequences

**`UploadSchemaValidationPropertyTest`** — implements Property 1:
```java
// Feature: workout-creator-service-upload, Property 1: schema validation
@Property(tries = 100)
void parse_invalidDurationWeeks_returnsFailure(@ForAll @IntRange(min = 2, max = 3) int weeks) { ... }

@Property(tries = 100)
void parse_validProgram_returnsSuccess(@ForAll @From("validProgramJson") String json) { ... }
```

**`UploadAtomicityPropertyTest`** — implements Property 4 (integration-level, but defined here as a property spec):
- For any invalid program JSON, vault count before and after the failed request must be equal.

### Integration Tests (@SpringBootTest)

Located in `src/test/java/.../integration/upload/`.

Uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`, connecting to the local dev PostgreSQL instance. Flyway migrations run on startup.

Test cases:
- Happy path: valid 1-week program → 201, program appears in vault listing
- Happy path: valid 4-week program → 201
- Ownership: upload as user A, attempt read as user B → 403
- Content source: uploaded program has `contentSource = UPLOADED` in vault listing
- Size limit: body > 1 MB → 400 before parsing
- Wrong Content-Type → 400
- Invalid JSON → 400
- Schema violation (mismatched weeks) → 400 with field error
- No JWT → 401
- Validate endpoint: valid JSON → `{ valid: true }`, no vault change
- Validate endpoint: invalid JSON → `{ valid: false, errors: [...] }`, no vault change
- Atomicity: failed upload leaves vault unchanged

Each test cleans up with `@Transactional` rollback or explicit `DELETE` in `@AfterEach`.

### Frontend Tests (Vitest + React Testing Library)

Located in `src/features/upload/__tests__/`.

**Unit tests:**
- `ProgramPreview.test.tsx` — renders `program_name`, `goal`, `duration_weeks`, `equipment_profile`; renders week/day/block breakdown; shows "Save to Vault" and "Edit JSON" buttons (Property 9, examples for 7.3, 7.4)
- `JsonEditor.test.tsx` — pre-populates with raw JSON; shows inline parse error on invalid JSON without clearing content (example for 7.5, 7.6)
- `useUpload.test.ts` — disables button during upload (Property 8); transitions to error state on 400; transitions to success state on 201; redirects on 401 (examples for 7.8, 7.9, 7.10, 7.11, 7.12)

**Property-based tests** (fast-check via `@fast-check/vitest`):
```typescript
// Feature: workout-creator-service-upload, Property 9: preview displays metadata
it.prop([fc.record({ program_name: fc.string(), goal: fc.string(), ... })])(
  'preview always shows required metadata fields',
  (metadata) => { ... }
)
```

**Property-based testing library for frontend:** `@fast-check/vitest` (fast-check integration for Vitest). Minimum 100 runs per property (`numRuns: 100`).
