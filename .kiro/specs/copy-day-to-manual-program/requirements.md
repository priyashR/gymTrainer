# Requirements Document

## Introduction

This feature extends manual program creation to support a new day assignment type: "copy from program." When creating a manual program, users can select a specific day from an existing program in their vault and copy its full structure (label, focus area, modality, warm-up, sections with exercises, cool-down) into the new manual program as a snapshot. The copied day is independent of the source — subsequent changes to or deletion of the source program have no effect on the manual program's copied day. This enables users to remix days from their AI-generated, uploaded, or other manual programs into new custom training plans.

## Glossary

- **Manual_Program**: A user-created program consisting of a name and an ordered list of day assignments, stored with `content_source = 'MANUAL'`.
- **Day_Assignment**: A single entry within a Manual_Program that maps a day number to either an external activity or a copied day snapshot.
- **Copied_Day_Snapshot**: A deep copy of a source program's day structure (label, focus area, modality, warm-up entries, sections with exercises, cool-down entries) stored inline as JSON within the day assignment. Independent of the source after creation.
- **Source_Program**: An existing program in the user's vault from which a day is being copied.
- **Source_Day**: A specific day (identified by week number and day number) within the Source_Program that the user selects to copy.
- **Endpoint**: The REST controller method that handles the copy-day request.
- **Copy_Day_Service**: The application-layer use case responsible for reading the source day, creating the snapshot, and persisting the day assignment.
- **Program_Repository**: The outbound port responsible for reading program data and persisting day assignments to the database.
- **Day_Structure**: The complete content of a training day: label, focus area, modality, warm-up entries, sections (each with name, type, format, time cap, and exercises), and cool-down entries.

## Requirements

### Requirement 1: Support "Copy From Program" Day Assignment Type

**User Story:** As a user creating a manual program, I want to assign a day by copying from an existing program in my vault, so that I can reuse structured training days without rebuilding them from scratch.

#### Acceptance Criteria

1. THE Endpoint SHALL accept a day assignment with `type` equal to `"copied_day"` in addition to the existing `"activity"` type, and SHALL process it through the same validation and persistence pipeline as other assignment types.
2. WHEN a day assignment has `type` equal to `"copied_day"`, THE Endpoint SHALL require `sourceProgramId` (UUID of the source program), `sourceWeekNumber` (integer, minimum value 1), and `sourceDayNumber` (integer, minimum value 1) in the request body.
3. IF a day assignment has `type` equal to `"copied_day"` but `sourceProgramId`, `sourceWeekNumber`, or `sourceDayNumber` is null or missing, THEN THE Endpoint SHALL return HTTP 400 Bad Request with a message identifying the missing field.
4. IF a day assignment has `type` equal to `"copied_day"` and the `sourceProgramId` does not exist in the authenticated user's vault, THEN THE Copy_Day_Service SHALL return HTTP 400 Bad Request with a message indicating the source program was not found.
5. IF a day assignment has `type` equal to `"copied_day"` and the specified `sourceWeekNumber` or `sourceDayNumber` does not exist within the referenced source program, THEN THE Copy_Day_Service SHALL return HTTP 400 Bad Request with a message indicating the referenced week or day was not found in the source program.
6. WHEN a day assignment has `type` equal to `"copied_day"` and all source references are valid, THE Program_Repository SHALL persist the day assignment with the copied snapshot, `sourceProgramId`, `sourceWeekNumber`, and `sourceDayNumber` as provenance metadata.

### Requirement 2: Browse Source Program Days

**User Story:** As a user, I want to see which days are available in a given vault program, so that I can pick the specific day I want to copy.

#### Acceptance Criteria

1. WHEN an authenticated user sends a GET request to `/api/v1/vault/programs/{programId}/days`, THE Endpoint SHALL return HTTP 200 OK with a JSON response containing available days grouped by week, where each day entry includes week number, day number, label, and focus area.
2. WHEN an authenticated user sends a GET request to `/api/v1/vault/programs/{programId}/days` and the program contains no weeks or days, THE Endpoint SHALL return HTTP 200 OK with an empty weeks collection.
3. IF the `programId` path parameter is not a valid UUID, THEN THE Endpoint SHALL return HTTP 400 Bad Request with a message indicating the program ID format is invalid.
4. IF the `programId` does not exist in the authenticated user's vault, THEN THE Endpoint SHALL return HTTP 404 Not Found.
5. THE Endpoint SHALL only return days from programs owned by the authenticated user (ownership verified via JWT subject claim).

### Requirement 3: Validate Source Program and Day Exist

**User Story:** As a user, I want to be told immediately if the program or day I selected no longer exists, so that I can choose a different source.

#### Acceptance Criteria

1. WHEN a day assignment references a `sourceProgramId` that does not exist in the authenticated user's vault, THE Copy_Day_Service SHALL return HTTP 400 Bad Request with a message indicating the specified source program was not found.
2. WHEN a day assignment references a `sourceProgramId` that exists in another user's vault but not the authenticated user's vault, THE Copy_Day_Service SHALL return HTTP 400 Bad Request with an error message identical to criterion 1 (source program not found) to avoid leaking resource existence.
3. WHEN a day assignment references a `sourceWeekNumber` and `sourceDayNumber` combination that does not exist within the specified source program, THE Copy_Day_Service SHALL return HTTP 400 Bad Request with a message indicating the specified day was not found in the source program.
4. WHEN a day assignment has a `sourceWeekNumber` less than 1 or a `sourceDayNumber` less than 1, THE Copy_Day_Service SHALL return HTTP 400 Bad Request with a message indicating the value must be a positive integer.
5. IF multiple day assignments in the same request reference non-existent source programs or days, THEN THE Copy_Day_Service SHALL validate all copied_day assignments and return errors for the first invalid assignment encountered (fail-fast).

### Requirement 4: Deep Copy Day Structure as Snapshot

**User Story:** As a user, I want the copied day to include the full workout structure (warm-up, exercises, cool-down), so that I have a complete training day without needing to refer back to the source.

#### Acceptance Criteria

1. WHEN the Copy_Day_Service processes a valid `"copied_day"` assignment, THE Copy_Day_Service SHALL read the complete Day_Structure from the source program's specified week and day, including all nested collections (warm-up entries, sections with exercises, and cool-down entries).
2. THE Copy_Day_Service SHALL create a Copied_Day_Snapshot containing: label, focus area, modality, warm-up entries (each with movement and instruction), sections (each with name, section type, format, time cap in seconds or null if uncapped, and exercises each with name, modality type, sets, reps, weight or null, rest seconds or null, and notes or null), and cool-down entries (each with movement and instruction). Nullable fields SHALL be preserved as null in the snapshot rather than omitted.
3. WHEN the source day contains empty warm-up, empty sections, or empty cool-down lists, THE Copy_Day_Service SHALL preserve them as empty arrays in the Copied_Day_Snapshot.
4. THE Program_Repository SHALL persist the Copied_Day_Snapshot as serialized JSON (maximum 1 MB) in the `day_assignments` table's `snapshot_data` column, linked to the parent Manual_Program. The snapshot write and the day assignment row insert SHALL be persisted atomically within a single transaction.
5. THE Program_Repository SHALL store `source_program_id`, `source_week_number`, and `source_day_number` in the day assignment row for provenance tracking purposes only.

### Requirement 5: Snapshot Independence from Source

**User Story:** As a user, I want my copied day to remain exactly as it was when I copied it, even if the source program is later edited or deleted.

#### Acceptance Criteria

1. WHEN the source program's day structure is modified after the copy operation, THE Manual_Program's Copied_Day_Snapshot SHALL retain the same Day_Structure field values (label, focus area, modality, warm-up entries, sections with exercises, cool-down entries) as were captured at the time of the copy operation.
2. WHEN the source program is deleted after the copy operation, THE Manual_Program's Copied_Day_Snapshot SHALL remain retrievable via the program detail endpoint and SHALL return the full Day_Structure without error.
3. THE `day_assignments` table's `source_program_id` column SHALL NOT define a foreign key constraint to the `programs` table, ensuring that deletion or modification of the source program does not cascade to or alter the day assignment row.
4. WHEN the source program is deleted after the copy operation, THE day assignment row SHALL retain the original `source_program_id`, `source_week_number`, and `source_day_number` values unchanged for provenance reference.

### Requirement 6: Return Copied Day in Program Detail Response

**User Story:** As a user viewing my manual program, I want to see the full day structure for copied days, so that I can review the exercises and plan my training.

#### Acceptance Criteria

1. WHEN the program detail endpoint returns a Manual_Program containing a `"copied_day"` assignment, THE response SHALL include the `type` field set to `"copied_day"` and the deserialized Copied_Day_Snapshot with the full Day_Structure (label, focus area, modality, warm-up entries with movement and instruction, sections with name, section type, format, time cap, and exercises with name, modality type, sets, reps, weight, rest seconds, and notes, cool-down entries with movement and instruction).
2. THE response for a `"copied_day"` assignment SHALL include `sourceProgramId`, `sourceWeekNumber`, and `sourceDayNumber` as metadata fields containing the original values stored at copy time, regardless of whether the source program still exists.
3. IF the source program no longer exists at the time of retrieval, THEN THE program detail endpoint SHALL still return HTTP 200 with the full Copied_Day_Snapshot and the original provenance metadata values unchanged.

### Requirement 7: Persist Copied Day Assignment

**User Story:** As a user, I want the copy operation to be durable, so that my manual program retains the copied day across sessions.

#### Acceptance Criteria

1. THE Program_Repository SHALL persist each `"copied_day"` day assignment with its `dayNumber`, `assignment_type` set to `"COPIED_DAY"`, the serialized snapshot as JSON in the `snapshot_data` column, and provenance fields (`source_program_id`, `source_week_number`, `source_day_number`) where provenance fields are stored without foreign key constraints to the source program.
2. FOR ALL persisted Copied_Day_Snapshot records, reading the day assignment back and deserializing the `snapshot_data` SHALL produce a Day_Structure with identical values for all fields defined in Requirement 4 criterion 2 (label, focus area, modality, warm-up entries, sections with exercises, cool-down entries) compared to the originally copied structure (round-trip consistency).
3. THE `day_assignments` table schema SHALL support `assignment_type = 'COPIED_DAY'` alongside the existing `'ACTIVITY'` type, with `snapshot_data` and provenance columns defined as nullable, such that existing ACTIVITY day assignments remain readable and correctly typed after the schema migration.
4. WHEN a day assignment with `assignment_type` of `'ACTIVITY'` is read after the schema migration, THE Program_Repository SHALL return the assignment with null `snapshot_data` and null provenance fields without error.

### Requirement 8: Error Handling for Copy Day Operations

**User Story:** As a user, I want clear error messages when the copy operation fails, so that I can understand and correct the issue.

#### Acceptance Criteria

1. IF an unexpected database error occurs during the copy operation, THEN THE Copy_Day_Service SHALL return HTTP 500 Internal Server Error with a generic message that does not expose internal details.
2. IF the `snapshot_data` column contains corrupted or unparseable JSON when reading a day assignment, THEN THE Program_Repository SHALL log the error at ERROR level and return HTTP 500 Internal Server Error with a message indicating the snapshot could not be loaded.
3. IF the request body contains an unrecognised `type` value (not `"activity"` or `"copied_day"`), THEN THE Endpoint SHALL return HTTP 400 Bad Request with a message listing the valid types.

### Requirement 9: "Copy from Program" Tab in Day Assignment Modal

**User Story:** As a user, I want a "Copy from Program" option in the day assignment modal, so that I can pick a day from my existing programs when building a manual program.

#### Acceptance Criteria

1. THE DayAssignmentModal component SHALL display a third tab labelled "📋 Copy Day" alongside the existing "💪 Workout" and "🏃 Activity" tabs.
2. WHEN the user selects the "Copy Day" tab, THE modal SHALL display a list of the user's vault programs (fetched from `GET /api/v1/vault/programs`), showing each program's name and content source.
3. THE program list SHALL exclude the manual program currently being created (if it has already been saved) to prevent self-referencing.
4. THE program list SHALL display a loading indicator while the API call is in progress and an error message if the call fails.

### Requirement 10: Program Day Picker

**User Story:** As a user, I want to browse and select a specific day from a program, so that I can copy exactly the training day I want.

#### Acceptance Criteria

1. WHEN the user selects a program from the list, THE modal SHALL fetch available days from `GET /api/v1/vault/programs/{programId}/days` and display them grouped by week.
2. EACH day entry SHALL display: week number, day number, label (e.g. "Monday"), and focus area (e.g. "Push").
3. THE day picker SHALL display a loading indicator while fetching days and an error message if the program has no days or the call fails.
4. WHEN the user selects a specific day, THE modal SHALL call `onAssign` with a day assignment of type `"copied_day"` containing `sourceProgramId`, `sourceWeekNumber`, `sourceDayNumber`, and the source program's name and day label for display purposes.
5. THE user SHALL be able to navigate back to the program list without losing context (back button within the modal).

### Requirement 11: Display Copied Day in Program Builder

**User Story:** As a user, I want to see what I've copied onto a day in the program builder, so that I can confirm it's the right training day before saving.

#### Acceptance Criteria

1. WHEN a day tile in the Create Program page has a `"copied_day"` assignment, THE tile SHALL display the source program name, the day label (e.g. "Push — Week 1 Day 1"), and the focus area.
2. THE day tile for a copied day SHALL be visually distinct from activity-type tiles (e.g. different icon or badge indicating it's a copied workout day).
3. WHEN the user taps a day tile with a `"copied_day"` assignment, THE modal SHALL open with the option to reassign (replace with a different activity or copied day) or clear the assignment.

### Requirement 12: Display Copied Day Detail in Program View

**User Story:** As a user viewing a saved manual program, I want to see the full exercise structure of copied days, so that I can review my training plan.

#### Acceptance Criteria

1. WHEN the program detail page renders a day assignment with `type` equal to `"copied_day"`, THE page SHALL display the full Day_Structure from the snapshot: label, focus area, modality, warm-up entries, sections with exercises (name, sets, reps, weight, rest), and cool-down entries.
2. THE copied day display SHALL show provenance metadata: "Copied from [Program Name] — Week X, Day Y" where the program name is resolved from `sourceProgramId` if the program still exists, or displayed as "Deleted Program" if it no longer exists.
3. THE exercise display format SHALL match the existing exercise display used in the program detail page for AI-generated and uploaded programs (consistent look and feel).

### Requirement 13: Remove "Workout" Tab from Day Assignment Modal

**User Story:** As a user, I only want to assign activities or copy days from existing programs, so that the interface is not confusing with an unsupported option.

#### Acceptance Criteria

1. THE DayAssignmentModal component SHALL NOT display the "💪 Workout" tab, as workout-type day assignments are no longer supported by the backend.
2. THE default active tab when the modal opens SHALL be "🏃 Activity".
