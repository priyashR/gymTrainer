# Requirements Document — Vault CRUD and Search

## Parent Spec Reference

This sub-spec implements the following requirements from the `workout-creator-service` main spec:

- **Requirement 2: Workout and Program CRUD (Vault)** — full implementation of save, view, edit, and delete operations with ownership enforcement
- **Requirement 3: Vault Search and Filter** — keyword search and filtering (by focus area and modality) across the User's Vault

---

## Introduction

This feature completes the Vault CRUD functionality in the `workout-creator-service` and connects the upload flow's "Save to Vault" action to the Vault services. It also adds a Search capability so Users can find saved Workouts and Programs. On the frontend, the existing "Workout" link on the Home page becomes an expandable menu with two sub-options: "Continue with Program" and "Search for a workout or program". The Search option uses the Vault search API to fetch and display results.

The Vault currently has only the JPA entity infrastructure (persistence layer) and no domain, ports, or application layers. This feature builds out the full hexagonal stack for the `vault` package: domain objects, inbound/outbound ports, application services, and REST controllers for CRUD and search operations.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Workout**: A single training session definition containing one or more Sections; represented as a 1-week Program with a single Day
- **Program**: A structured collection of Workouts spanning 1 to 4 weeks
- **Section**: A named block within a Day (e.g., Tier 1: Compound, Metcon, Finisher)
- **Exercise**: A single movement within a Section
- **Vault**: A User's personal library of saved Workouts and Programs
- **Vault_Service**: The application-layer service responsible for Vault CRUD operations
- **Search_Service**: The application-layer service responsible for searching and filtering Vault content
- **Workout_Creator_Service**: The microservice responsible for AI generation, Vault CRUD, and search
- **Workout_Coach_UI**: The React SPA frontend providing all user-facing views
- **Content_Source**: Metadata indicating how a Program entered the Vault — one of `AI_GENERATED`, `UPLOADED`, or `MANUAL`
- **Vault_Item**: A summary representation of a Program in the Vault listing (id, name, goal, duration, content source, timestamps)
- **Focus_Area**: The primary training focus of a Day (e.g., Push, Pull, Metcon, Full Body)
- **Modality**: The training style of a Day — either `CrossFit` or `Hypertrophy`

---

## Requirements

### Requirement 1: Vault CRUD — Read Operations

**User Story:** As a User, I want to view all programs in my Vault and see the full details of any individual program, so that I can review my saved training content.

#### Acceptance Criteria

1. WHEN a User submits a GET request to `/api/v1/vault/programs`, THE Workout_Creator_Service SHALL return a paginated list of all Programs owned by the authenticated User, ordered by `created_at` descending (most recent first).

2. THE Workout_Creator_Service SHALL return each Vault_Item in the listing with the following fields: `id`, `name`, `goal`, `durationWeeks`, `equipmentProfile`, `contentSource`, `createdAt`, and `updatedAt`.

3. WHEN a User submits a GET request to `/api/v1/vault/programs/{id}`, THE Workout_Creator_Service SHALL return the full Program details including all weeks, days, sections, exercises, warm-up entries, and cool-down entries.

4. IF a User submits a GET request to `/api/v1/vault/programs/{id}` for a Program that does not exist or is owned by a different User, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.

5. WHEN a User submits a GET request to `/api/v1/vault/programs` without a valid JWT, THE Workout_Creator_Service SHALL return a 401 Unauthorised response.

6. THE Workout_Creator_Service SHALL support pagination query parameters `page` (zero-indexed, default 0) and `size` (default 20, maximum 100) on the Vault listing endpoint.

---

### Requirement 2: Vault CRUD — Update Operations

**User Story:** As a User, I want to update a saved program — either its metadata or its full content via JSON editing with preview — so that I can refine my training plans over time.

#### Acceptance Criteria

1. WHEN a User submits a PUT request to `/api/v1/vault/programs/{id}` with a valid Program JSON body (conforming to the Upload_Schema), THE Workout_Creator_Service SHALL validate the JSON, replace the existing Program content entirely, and return a 200 OK response with the updated Vault_Item.

2. WHEN a Program is updated, THE Workout_Creator_Service SHALL set the `updated_at` timestamp to the current time.

3. IF a User submits a PUT request to `/api/v1/vault/programs/{id}` for a Program owned by a different User, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.

4. IF a User submits a PUT request to `/api/v1/vault/programs/{id}` for a Program that does not exist, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.

5. WHEN a PUT request contains JSON that fails schema validation, THE Workout_Creator_Service SHALL return a 400 Bad Request response with field-level errors using the same error shape as the upload endpoint.

6. WHEN a PUT request is received without a valid JWT, THE Workout_Creator_Service SHALL return a 401 Unauthorised response.

7. THE Workout_Creator_Service SHALL NOT change the `content_source` or `owner_user_id` of a Program during an update; these fields are immutable after creation.

---

### Requirement 3: Vault CRUD — Delete Operations

**User Story:** As a User, I want to delete programs from my Vault, so that I can remove content I no longer need.

#### Acceptance Criteria

1. WHEN a User submits a DELETE request to `/api/v1/vault/programs/{id}`, THE Workout_Creator_Service SHALL remove the Program and all associated weeks, days, sections, exercises, and warm-cool entries from the data store, and return a 204 No Content response.

2. IF a User submits a DELETE request to `/api/v1/vault/programs/{id}` for a Program owned by a different User, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.

3. IF a User submits a DELETE request to `/api/v1/vault/programs/{id}` for a Program that does not exist, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.

4. WHEN a DELETE request is received without a valid JWT, THE Workout_Creator_Service SHALL return a 401 Unauthorised response.

5. WHEN a Program is deleted, THE Workout_Creator_Service SHALL ensure that subsequent GET requests for that Program's id return a 403 Forbidden response.

---

### Requirement 4: Vault Search and Filter

**User Story:** As a User, I want to search and filter my Vault by keyword, focus area, and modality, so that I can quickly find the right workout or program.

#### Acceptance Criteria

1. WHEN a User submits a GET request to `/api/v1/vault/programs/search?q={query}`, THE Workout_Creator_Service SHALL return a paginated list of Programs in the User's Vault whose `name` or `goal` contains the query string (case-insensitive partial match).

2. THE Workout_Creator_Service SHALL only search Programs owned by the authenticated User; Programs belonging to other Users SHALL NOT appear in search results.

3. WHEN the `q` parameter is provided but empty or blank, THE Workout_Creator_Service SHALL return a 400 Bad Request response with the message `"Search query must not be empty"`.

4. WHEN a search returns no matching results, THE Workout_Creator_Service SHALL return a 200 OK response with an empty `content` array and `totalElements: 0`.

5. THE Workout_Creator_Service SHALL support pagination query parameters `page` (zero-indexed, default 0) and `size` (default 20, maximum 100) on the search endpoint.

6. WHEN a search request is received without a valid JWT, THE Workout_Creator_Service SHALL return a 401 Unauthorised response.

7. THE Workout_Creator_Service SHALL return search results ordered by relevance (name match first, then goal match), with ties broken by `created_at` descending.

8. WHEN a User includes a `focusArea` query parameter (e.g., `?focusArea=Push`), THE Workout_Creator_Service SHALL return only Programs that contain at least one Day with a matching `focus_area` value (case-insensitive).

9. WHEN a User includes a `modality` query parameter (e.g., `?modality=CrossFit`), THE Workout_Creator_Service SHALL return only Programs that contain at least one Day with a matching `modality` value (case-insensitive).

10. WHEN multiple filter parameters are applied simultaneously (e.g., `?q=strength&focusArea=Push&modality=Hypertrophy`), THE Workout_Creator_Service SHALL return only Programs that satisfy all applied criteria.

11. WHEN only filter parameters are provided without a `q` parameter, THE Workout_Creator_Service SHALL return all Programs owned by the User that match the specified filters.

---

### Requirement 5: Vault CRUD — Copy/Duplicate

**User Story:** As a User, I want to copy an existing program in my Vault, so that I can make tweaks to the copy without affecting the original.

#### Acceptance Criteria

1. WHEN a User submits a POST request to `/api/v1/vault/programs/{id}/copy`, THE Workout_Creator_Service SHALL create a new Program that is a full deep copy of the source Program (including all weeks, days, sections, exercises, warm-up entries, and cool-down entries) and return a 201 Created response with the new Vault_Item.

2. THE Workout_Creator_Service SHALL assign the copied Program a new unique id, set `created_at` and `updated_at` to the current time, and set the name to the original name suffixed with `" (Copy)"`.

3. THE Workout_Creator_Service SHALL set the `content_source` of the copied Program to `MANUAL`.

4. IF a User submits a copy request for a Program owned by a different User, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.

5. IF a User submits a copy request for a Program that does not exist, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.

6. WHEN a copy request is received without a valid JWT, THE Workout_Creator_Service SHALL return a 401 Unauthorised response.

---

### Requirement 6: Upload Integration with Vault Services

**User Story:** As a User, I want uploaded programs to be fully accessible through the Vault API immediately after upload, so that I can search for and manage them like any other Vault content.

#### Acceptance Criteria

1. WHEN a Program is successfully uploaded via `/api/v1/uploads/programs`, THE Workout_Creator_Service SHALL ensure the Program is immediately retrievable via `GET /api/v1/vault/programs/{id}` using the id returned in the upload response.

2. WHEN a Program is successfully uploaded, THE Workout_Creator_Service SHALL ensure the Program appears in the User's Vault listing at `GET /api/v1/vault/programs`.

3. WHEN a Program is successfully uploaded, THE Workout_Creator_Service SHALL ensure the Program is discoverable via the search endpoint at `GET /api/v1/vault/programs/search` when the query matches the program's name or goal.

4. THE Workout_Creator_Service SHALL treat uploaded Programs identically to AI-generated and manually created Programs for all Vault CRUD and search operations; the `content_source` field SHALL be metadata only and SHALL NOT affect any Vault behaviour.

---

### Requirement 7: Frontend — Workout Menu Restructure

**User Story:** As a User, I want the "Workout" option on the Home page to expand into sub-options, so that I can choose between continuing a program or searching for content.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL render the "Workout" item on the Home page as an expandable menu (matching the existing "New Workout" expandable pattern).

2. WHEN a User expands the "Workout" menu, THE Workout_Coach_UI SHALL display two sub-options: "Continue with Program" and "Search for a workout or program".

3. WHEN a User selects "Continue with Program", THE Workout_Coach_UI SHALL navigate to the `/workout/continue` route.

4. WHEN a User selects "Search for a workout or program", THE Workout_Coach_UI SHALL navigate to the `/vault/search` route.

5. THE Workout_Coach_UI SHALL maintain the existing "New Workout" expandable menu with its current sub-options ("Ask Gemini" and "Upload Program") unchanged.

---

### Requirement 8: Frontend — Vault Search Page

**User Story:** As a User, I want a search page where I can type a query and apply filters to find matching programs from my Vault, so that I can find and select content to use.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a search page at the `/vault/search` route with a text input field, filter controls for focus area and modality, and a search button.

2. WHEN a User submits a search query, THE Workout_Coach_UI SHALL call `GET /api/v1/vault/programs/search` with the appropriate query parameters and display the results as a list of Vault_Items showing `name`, `goal`, `durationWeeks`, and `contentSource`.

3. WHILE a search request is in progress, THE Workout_Coach_UI SHALL display a loading indicator.

4. WHEN the search returns no results, THE Workout_Coach_UI SHALL display a message: "No programs found matching your search."

5. WHEN a User selects a search result, THE Workout_Coach_UI SHALL navigate to the program detail view at `/vault/programs/{id}`.

6. WHEN the search endpoint returns a 401 Unauthorised response, THE Workout_Coach_UI SHALL redirect the User to the login screen.

7. THE Workout_Coach_UI SHALL debounce search input by 300ms to avoid excessive API calls while the User is typing.

8. THE Workout_Coach_UI SHALL provide a "Focus Area" dropdown filter with options including: Push, Pull, Metcon, Full Body, and a blank "All" default.

9. THE Workout_Coach_UI SHALL provide a "Modality" dropdown filter with options: All, CrossFit, Hypertrophy.

10. WHEN a User changes a filter value, THE Workout_Coach_UI SHALL immediately re-execute the search with the updated filter parameters.

---

### Requirement 9: Frontend — Program Detail View

**User Story:** As a User, I want to view the full details of a program from my Vault with a drill-down into each day, edit it via JSON with preview, copy it, or delete it, so that I can fully manage my training content.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a program detail page at the `/vault/programs/{id}` route that displays the Program metadata (name, goal, duration, equipment profile, content source) at the top level.

2. WHEN the detail page loads, THE Workout_Coach_UI SHALL call `GET /api/v1/vault/programs/{id}` and render the program metadata followed by a collapsible week breakdown; each week shows its days as expandable items.

3. WHEN a User expands a Day, THE Workout_Coach_UI SHALL display the day's focus area, modality, warm-up entries, all sections with their exercises (sets, reps, weight, notes), and cool-down entries.

4. WHILE the program detail request is in progress, THE Workout_Coach_UI SHALL display a loading indicator.

5. WHEN the detail endpoint returns a 403 Forbidden response, THE Workout_Coach_UI SHALL display a message: "Program not found or access denied." and provide a link back to the search page.

6. THE Workout_Coach_UI SHALL display a "Delete" action on the program detail page that, when confirmed by the User, calls `DELETE /api/v1/vault/programs/{id}` and navigates back to the search page on success.

7. THE Workout_Coach_UI SHALL display an "Edit JSON" action on the program detail page that opens an inline JSON editor pre-populated with the program's current content in Upload_Schema format.

8. WHEN a User edits the JSON and selects "Preview", THE Workout_Coach_UI SHALL parse the edited JSON client-side and display a structured preview of the changes; if the JSON is invalid, THE Workout_Coach_UI SHALL display an inline parse error without clearing the editor content.

9. WHEN a User selects "Save Changes" from the edit/preview view, THE Workout_Coach_UI SHALL submit the updated JSON to `PUT /api/v1/vault/programs/{id}` and refresh the detail view on success.

10. WHEN the update endpoint returns a 400 Bad Request response with validation errors, THE Workout_Coach_UI SHALL display the field-level errors and return the User to the JSON editor.

11. THE Workout_Coach_UI SHALL display a "Copy" action on the program detail page that calls `POST /api/v1/vault/programs/{id}/copy` and navigates to the newly created copy's detail page on success.

---

### Requirement 10: Frontend — Upload Success Navigation

**User Story:** As a User, I want to navigate directly to my newly uploaded program in the Vault after a successful upload, so that I can verify it was saved correctly.

#### Acceptance Criteria

1. WHEN the upload endpoint returns a 201 Created response, THE Workout_Coach_UI SHALL display a "View in Vault" link that navigates to `/vault/programs/{id}` using the id from the upload response.

2. THE Workout_Coach_UI SHALL retain the existing success confirmation message showing the program name alongside the new "View in Vault" navigation action.

