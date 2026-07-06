# Implementation Plan: Copy Day to Manual Program

## Overview

Extend the manual program creation feature to support a new `COPIED_DAY` assignment type. Users can browse days in their existing vault programs and copy a full day structure (label, focus area, modality, warm-up, sections with exercises, cool-down) as an independent JSON snapshot into a new manual program. The implementation spans the backend (workout-creator-service) and frontend (workout-coach-ui), following hexagonal architecture: domain → ports → application → adapters → tests.

## Tasks

- [x] 1. Backend domain model changes
  - [x] 1.1 Extend `DayAssignmentType` enum with `COPIED_DAY` value
    - Add `COPIED_DAY` to the existing `DayAssignmentType` enum in `vault/domain/DayAssignmentType.java`
    - _Requirements: 1.1, 7.1_

  - [x] 1.2 Extend `DayAssignment` record with snapshot and provenance fields
    - Add fields: `snapshotData` (String, nullable), `sourceProgramId` (UUID, nullable), `sourceWeekNumber` (Integer, nullable), `sourceDayNumber` (Integer, nullable)
    - Add backwards-compatible constructor for existing ACTIVITY/WORKOUT usage
    - _Requirements: 1.2, 4.2, 4.5, 7.1_

  - [x] 1.3 Create `DaySummary` record in `vault/domain/`
    - Fields: `weekNumber` (int), `dayNumber` (int), `label` (String), `focusArea` (String)
    - Lightweight DTO for the browse days endpoint
    - _Requirements: 2.1_

- [x] 2. Schema migration and exception classes
  - [x] 2.1 Create Flyway migration `V104__add_copied_day_support.sql`
    - Path: `src/main/resources/db/migration/V104__add_copied_day_support.sql`
    - ALTER `assignment_type` column type to `VARCHAR(30)` to accommodate longer type name
    - ADD columns: `snapshot_data` (JSONB, nullable), `source_program_id` (UUID, nullable), `source_week_number` (INTEGER, nullable), `source_day_number` (INTEGER, nullable)
    - No FK constraint on `source_program_id` — intentional for snapshot independence
    - Add column comments for documentation
    - _Requirements: 5.3, 7.1, 7.3_

  - [x] 2.2 Create `SourceProgramNotFoundException` in `common/exception/`
    - Extends `RuntimeException`
    - Constructor accepts `UUID sourceProgramId`
    - Message: `"Source program not found in vault: " + sourceProgramId`
    - Include `getSourceProgramId()` accessor
    - _Requirements: 1.4, 3.1, 8.1_

  - [x] 2.3 Create `SourceDayNotFoundException` in `common/exception/`
    - Extends `RuntimeException`
    - Constructor accepts `int weekNumber`, `int dayNumber`
    - Message: `"Day not found in source program: week " + weekNumber + ", day " + dayNumber`
    - Include `getWeekNumber()` and `getDayNumber()` accessors
    - _Requirements: 1.5, 3.3, 8.1_

- [x] 3. Inbound port for browsing days
  - [x] 3.1 Create `GetProgramDaysUseCase` interface in `vault/ports/inbound/`
    - Single method: `List<DaySummary> getProgramDays(UUID programId, String ownerUserId)`
    - _Requirements: 2.1, 2.5_

- [x] 4. Service layer changes
  - [x] 4.1 Implement `GetProgramDaysUseCase` in `VaultService`
    - Add `GetProgramDaysUseCase` to the implements clause
    - Load program via `findByIdAndOwner`, throw `ProgramAccessDeniedException` if not found
    - Extract all days from all weeks, map to `DaySummary` with week number, day number, label, focus area
    - Mark `@Transactional(readOnly = true)`
    - _Requirements: 2.1, 2.2, 2.4, 2.5_

  - [x] 4.2 Extend `VaultService.createManualProgram` to handle `COPIED_DAY` assignments
    - For each day with `type == COPIED_DAY`: load source program via `findByIdAndOwner`, throw `SourceProgramNotFoundException` if not found
    - Extract the specified day at (`sourceWeekNumber`, `sourceDayNumber`), throw `SourceDayNotFoundException` if not found
    - Serialize the `Day` domain object to JSON using `ObjectMapper`
    - Construct resolved `DayAssignment` with snapshot JSON and provenance fields
    - Support mixed assignment types (activity + copied_day) in a single request
    - _Requirements: 1.1, 1.4, 1.5, 1.6, 3.1, 3.2, 3.3, 3.5, 4.1, 4.4, 4.5_

- [x] 5. Checkpoint - Ensure domain, ports, and service compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Entity and repository changes
  - [x] 6.1 Extend `DayAssignmentJpaEntity` with new columns
    - Add fields: `snapshotData` (String, `@Column(columnDefinition = "jsonb")`), `sourceProgramId` (UUID), `sourceWeekNumber` (Integer), `sourceDayNumber` (Integer)
    - Update mapping methods (toDomain, fromDomain) to handle the new fields
    - _Requirements: 7.1, 7.3_

  - [x] 6.2 Update `JpaVaultProgramRepository.saveManualProgram` to persist snapshot data
    - When persisting `COPIED_DAY` assignments, store `snapshotData`, `sourceProgramId`, `sourceWeekNumber`, `sourceDayNumber` in the entity
    - Ensure atomic persistence within the existing transaction
    - _Requirements: 4.4, 7.1_

  - [x] 6.3 Update `JpaVaultProgramRepository` read path to load snapshot data
    - When loading day assignments, map snapshot and provenance fields back to the domain `DayAssignment`
    - Handle null snapshot gracefully for ACTIVITY-type assignments
    - _Requirements: 6.1, 7.2, 7.4_

- [x] 7. Controller and DTO changes
  - [x] 7.1 Extend `DayAssignmentRequest` DTO with source fields
    - Add fields: `sourceProgramId` (String, nullable), `sourceWeekNumber` (Integer, nullable), `sourceDayNumber` (Integer, nullable)
    - _Requirements: 1.2_

  - [x] 7.2 Create `ProgramDaysResponse` DTO in `vault/adapters/inbound/dto/`
    - Records: `ProgramDaysResponse(List<WeekDays> weeks)`, `WeekDays(int weekNumber, List<DayEntry> days)`, `DayEntry(int dayNumber, String label, String focusArea)`
    - Static factory `from(List<DaySummary>)` that groups days by week number
    - _Requirements: 2.1_

  - [x] 7.3 Add `GET /{id}/days` endpoint to `VaultController`
    - Parse and validate UUID path parameter
    - Resolve owner from JWT, call `getProgramDaysUseCase.getProgramDays()`
    - Return 200 OK with `ProgramDaysResponse`
    - Return 400 for invalid UUID, 404 for not-found/not-owned
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 7.4 Update `validateDayAssignments` in `VaultController` to accept `"copied_day"` type
    - Validate conditional fields: `sourceProgramId` required and non-blank, `sourceWeekNumber` >= 1, `sourceDayNumber` >= 1
    - Return 400 with descriptive message for invalid/missing fields
    - Validate unrecognised type returns 400 listing valid types (`activity`, `copied_day`)
    - _Requirements: 1.2, 1.3, 3.4, 8.3_

  - [x] 7.5 Extend `VaultProgramDetailResponse.DayAssignmentResponse` for copied_day data
    - Add fields: `snapshotData` (Object, deserialized JSON), `sourceProgramId` (String), `sourceWeekNumber` (Integer), `sourceDayNumber` (Integer)
    - Update `from(DayAssignment)` mapping to deserialize snapshot JSON to Object for Jackson serialization
    - _Requirements: 6.1, 6.2, 6.3_

  - [x] 7.6 Add exception handlers to `GlobalExceptionHandler`
    - Handle `SourceProgramNotFoundException` → 400 Bad Request with "Source program not found"
    - Handle `SourceDayNotFoundException` → 400 Bad Request with exception message
    - _Requirements: 1.4, 1.5, 3.1, 3.3, 8.1_

- [x] 8. Checkpoint - Ensure backend compiles and existing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Backend unit tests
  - [x] 9.1 Write unit tests for `VaultService.createManualProgram` with copied_day
    - Test class: `CopyDayServiceTest` in `src/test/java/.../workoutcreator/unit/vault/`
    - Tests: `CreateManualProgram_WithCopiedDay_PersistsSnapshotAndProvenance`, `CreateManualProgram_SourceProgramNotFound_ThrowsSourceProgramNotFoundException`, `CreateManualProgram_SourceDayNotFound_ThrowsSourceDayNotFoundException`, `CreateManualProgram_MixedTypes_ProcessesAllCorrectly`
    - Mock `VaultProgramRepository` with Mockito
    - _Requirements: 1.4, 1.5, 1.6, 3.1, 3.3, 4.1_

  - [x] 9.2 Write unit tests for `VaultService.getProgramDays`
    - Tests: `GetProgramDays_ValidProgram_ReturnsDaySummaries`, `GetProgramDays_EmptyProgram_ReturnsEmptyList`, `GetProgramDays_ProgramNotOwned_ThrowsProgramAccessDeniedException`
    - _Requirements: 2.1, 2.2, 2.4_

  - [x] 9.3 Write unit tests for controller validation logic
    - Tests: `ValidateDayAssignments_CopiedDayMissingSourceProgramId_Returns400`, `ValidateDayAssignments_CopiedDayNonPositiveWeekNumber_Returns400`, `ValidateDayAssignments_UnknownType_Returns400WithValidTypesList`, `ValidateDayAssignments_ValidCopiedDay_PassesValidation`
    - _Requirements: 1.3, 3.4, 8.3_

- [x] 10. Backend property-based tests
  - [x] 10.1 Write property test: Snapshot serialization round-trip
    - **Property 1: Snapshot serialization round-trip**
    - **Validates: Requirements 4.2, 7.2**
    - Test class: `CopyDayPropertyTest` in `src/test/java/.../workoutcreator/property/vault/`
    - Generate arbitrary `Day` objects with random strings for label/focusArea, random Modality enum, random-length lists of WarmCoolEntry/Section/Exercise with nullable fields randomly set to null
    - Serialize to JSON via ObjectMapper, deserialize back, verify all fields identical
    - `@Property(tries = 100)`

  - [x] 10.2 Write property test: Conditional required field validation for copied_day
    - **Property 2: Conditional required field validation for copied_day**
    - **Validates: Requirements 1.2, 1.3**
    - Generate `DayAssignmentRequest` with type="copied_day" and randomly null out one or more of sourceProgramId, sourceWeekNumber, sourceDayNumber
    - Verify controller validation returns 400
    - `@Property(tries = 100)`

  - [x] 10.3 Write property test: Non-existent source program is rejected
    - **Property 3: Non-existent source program is rejected**
    - **Validates: Requirements 1.4, 3.1, 3.2**
    - Generate random UUIDs as sourceProgramId, mock repository to return `Optional.empty()`
    - Verify `SourceProgramNotFoundException` is thrown
    - `@Property(tries = 100)`

  - [x] 10.4 Write property test: Non-existent week/day combination is rejected
    - **Property 4: Non-existent week/day combination is rejected**
    - **Validates: Requirements 1.5, 3.3**
    - Generate programs with known structure, reference week/day combos outside that structure
    - Verify `SourceDayNotFoundException` is thrown
    - `@Property(tries = 100)`

  - [x] 10.5 Write property test: Non-positive week/day numbers are rejected
    - **Property 5: Non-positive week/day numbers are rejected**
    - **Validates: Requirements 3.4**
    - Generate `sourceWeekNumber` or `sourceDayNumber` values <= 0 (zero and negatives)
    - Verify controller validation returns 400
    - `@Property(tries = 100)`

  - [x] 10.6 Write property test: Unrecognised assignment type is rejected
    - **Property 6: Unrecognised assignment type is rejected**
    - **Validates: Requirements 8.3**
    - Generate arbitrary strings that are NOT "activity" or "copied_day" as type value
    - Verify controller validation returns 400 listing valid types
    - `@Property(tries = 100)`

  - [x] 10.7 Write property test: Browse days returns correct structure
    - **Property 7: Browse days returns correct structure**
    - **Validates: Requirements 2.1, 2.2**
    - Generate programs with 1–5 weeks, 1–7 days per week
    - Call `getProgramDays`, verify every day appears exactly once with correct week number, day number, label, and focus area
    - `@Property(tries = 100)`

- [x] 11. Checkpoint - Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Frontend type and API updates
  - [x] 12.1 Extend `DayAssignment` type in `src/types/vault.ts`
    - Add `"copied_day"` to the type union
    - Add optional fields: `sourceProgramId`, `sourceProgramName`, `sourceWeekNumber`, `sourceDayNumber`, `dayLabel`, `focusArea`, `snapshotData`
    - _Requirements: 1.1, 6.1, 11.1_

  - [x] 12.2 Add `getProgramDays` API function in `src/lib/vaultApi.ts`
    - Function signature: `getProgramDays(programId: string): Promise<ProgramDaysResponse>`
    - Call `GET /api/v1/vault/programs/{programId}/days` with auth header
    - Define response types: `ProgramDaysResponse`, `WeekDays`, `DayEntry`
    - _Requirements: 2.1, 10.1_

- [x] 13. Frontend modal and component changes
  - [x] 13.1 Update `DayAssignmentModal` to replace "Workout" tab with "Copy Day" tab
    - Remove the "💪 Workout" tab and `WorkoutSelector` import
    - Add "📋 Copy Day" tab
    - Set default active tab to "🏃 Activity"
    - Tab type becomes: `"activity" | "copyDay"`
    - _Requirements: 9.1, 13.1, 13.2_

  - [x] 13.2 Create `CopyDaySelector` component in `src/features/program/`
    - Props: `onSelect: (assignment: DayAssignment) => void`, `excludeProgramId?: string`
    - Two-step flow: program list → day picker with back button
    - States: `"programList"` and `"dayPicker"`
    - Fetch programs from `GET /api/v1/vault/programs` on mount
    - _Requirements: 9.2, 9.3, 9.4, 10.5_

  - [x] 13.3 Create `ProgramPicker` sub-component in `src/features/program/`
    - Renders list of programs with name and content source
    - Excludes current program by ID
    - Shows loading indicator and error state
    - Calls `onProgramSelect` when a program is clicked
    - _Requirements: 9.2, 9.3, 9.4_

  - [x] 13.4 Create `DayPicker` sub-component in `src/features/program/`
    - Fetches days from `GET /api/v1/vault/programs/{programId}/days`
    - Displays days grouped by week (week number heading, day entries below)
    - Each entry shows day number, label, focus area
    - Shows loading indicator and error/empty state
    - Calls `onDaySelect` with the selected day info
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 13.5 Update `DayTile` component for copied_day rendering
    - Update `getDisplayName` to handle `type === "copied_day"`: show `📋 {sourceProgramName} — {dayLabel}`
    - Add visual badge/icon to distinguish copied day tiles from activity tiles
    - _Requirements: 11.1, 11.2_

  - [x] 13.6 Update `ProgramDetailPage` to render copied day snapshot
    - Add branch for `type === "copied_day"` in day assignment rendering
    - Render full snapshot structure (warm-up, sections with exercises, cool-down) using existing exercise display components
    - Show provenance: "Copied from [Program Name] — Week X, Day Y" or "Deleted Program" if source is gone
    - _Requirements: 12.1, 12.2, 12.3_

- [x] 14. Checkpoint - Ensure frontend compiles without errors
  - Ensure all tests pass, ask the user if questions arise.

- [x] 15. Frontend tests
  - [x] 15.1 Write unit tests for `DayAssignmentModal` tab changes
    - Verify only "Activity" and "Copy Day" tabs render (no "Workout" tab)
    - Verify default active tab is "Activity"
    - _Requirements: 9.1, 13.1, 13.2_

  - [x] 15.2 Write unit tests for `CopyDaySelector`
    - Test loading state, program list rendering, program selection transitions to day picker
    - Test that current program is excluded from list
    - Test back button returns to program list
    - Test `onSelect` is called with correct copied_day assignment data
    - _Requirements: 9.2, 9.3, 10.4, 10.5_

  - [x] 15.3 Write unit tests for `DayTile` with copied_day
    - Test renders source program name and day label
    - Test distinct visual treatment (icon/badge)
    - _Requirements: 11.1, 11.2_

  - [x] 15.4 Write unit tests for `ProgramDetailPage` copied day rendering
    - Test full snapshot structure rendering (warm-up, sections, exercises, cool-down)
    - Test provenance display with existing program and with deleted program
    - _Requirements: 12.1, 12.2, 12.3_

- [x] 16. Backend integration tests
  - [x]* 16.1 Write integration tests for copy day endpoint flows
    - Test class: `CopyDayIntegrationTest` in `src/test/java/.../workoutcreator/integration/vault/`
    - Tests:
      - `POST /api/v1/vault/programs` with copied_day → 201, verify day_assignments row has snapshot_data
      - `POST /api/v1/vault/programs` with copied_day referencing non-existent source → 400
      - `GET /api/v1/vault/programs/{id}/days` → 200 with correct structure
      - `GET /api/v1/vault/programs/{id}/days` for non-owned program → 404
      - `GET /api/v1/vault/programs/{id}` for manual program with copied_day → 200 with full snapshot in response
      - Delete source program → GET target manual program still returns 200 with snapshot intact (independence)
    - Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`
    - _Requirements: 1.1, 1.4, 2.1, 2.4, 5.1, 5.2, 6.1, 6.3_

- [x] 17. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation after each major layer
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The implementation follows hexagonal architecture: domain → ports → application → adapters → tests
- The Flyway migration uses V104 (next in the V100-V199 range after existing V103)
- No FK constraint on `source_program_id` — this is intentional for snapshot independence (Requirement 5.3)
- The `DayAssignment` record's backwards-compatible constructor ensures existing ACTIVITY code paths remain unchanged
- Frontend changes reuse existing exercise display components from `ProgramDetailPage` for rendering snapshot structure

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.3", "2.2", "2.3"] },
    { "id": 1, "tasks": ["1.2", "2.1", "3.1"] },
    { "id": 2, "tasks": ["4.1", "4.2"] },
    { "id": 3, "tasks": ["6.1", "7.1", "7.2"] },
    { "id": 4, "tasks": ["6.2", "6.3", "7.3", "7.4", "7.5", "7.6"] },
    { "id": 5, "tasks": ["9.1", "9.2", "9.3"] },
    { "id": 6, "tasks": ["10.1", "10.2", "10.3", "10.4", "10.5", "10.6", "10.7"] },
    { "id": 7, "tasks": ["12.1", "12.2"] },
    { "id": 8, "tasks": ["13.1", "13.5", "13.6"] },
    { "id": 9, "tasks": ["13.2"] },
    { "id": 10, "tasks": ["13.3", "13.4"] },
    { "id": 11, "tasks": ["15.1", "15.2", "15.3", "15.4"] },
    { "id": 12, "tasks": ["16.1"] }
  ]
}
```
