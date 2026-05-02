# Requirements Document — Workout Creator Service

## Introduction

The Workout Creator Service is responsible for AI-powered Workout and Program generation via Google Gemini, and for managing each User's personal Vault of saved Workouts and Programs. It is a standalone microservice following hexagonal architecture principles and exclusively owns the Workout data store.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Workout**: A single training session definition containing one or more Sections
- **Program**: A structured collection of Workouts spanning one or more weeks
- **Section**: A named block within a Workout (e.g., a strength block, an AMRAP, a Tabata interval)
- **Exercise**: A single movement within a Section (e.g., Back Squat, Box Jump)
- **Vault**: A User's personal library of saved Workouts and Programs
- **Gemini**: Google Gemini — the external AI service used to generate Workout and Program content
- **Workout_Creator_Service**: The microservice responsible for AI-powered Workout and Program creation and the Vault
- **Workout_Coach_UI**: The single frontend application serving all user-facing views

---

## Requirements

### Requirement 1: AI-Powered Workout and Program Generation

**User Story:** As a User, I want to describe a workout or program in natural language and have it generated for me, so that I can train without spending time on manual planning.

#### Acceptance Criteria

1. WHEN a User submits a natural language description with a scope of "day", "week", or "4-week", THE Workout_Creator_Service SHALL send a structured prompt to Gemini and return a generated Workout or Program.
2. THE Workout_Creator_Service SHALL parse the Gemini response into a structured Workout or Program domain object conforming to the internal data model.
3. THE Workout_Creator_Service SHALL format a Workout or Program domain object back into a human-readable text representation (pretty-print).
4. FOR ALL valid Workout domain objects, parsing then formatting then parsing SHALL produce an equivalent Workout domain object (round-trip property).
5. IF the Gemini service returns an error or times out after 10 seconds, THEN THE Workout_Creator_Service SHALL return a descriptive error response to the caller and SHALL NOT persist a partial Workout.
6. WHEN a generated Workout or Program is returned to the User, THE Workout_Coach_UI SHALL display a "Try Again" action that discards the result and submits a new generation request with the same description.
7. THE Workout_Creator_Service SHALL support the following Section types in generated content: Strength, AMRAP, EMOM, Tabata, For Time, and Accessory.

---

### Requirement 2: Workout and Program CRUD (Vault)

**User Story:** As a User, I want to save, view, edit, and delete workouts and programs in my personal Vault, so that I can build a reusable training library.

#### Acceptance Criteria

1. WHEN a User saves a generated or manually created Workout, THE Workout_Creator_Service SHALL persist it to the Workout data store associated with that User's identity.
2. THE Workout_Creator_Service SHALL allow a User to update the name, description, and Section content of any Workout owned by that User.
3. WHEN a User deletes a Workout, THE Workout_Creator_Service SHALL remove it from the Vault and SHALL return a 404 Not Found for any subsequent fetch of that Workout.
4. IF a User attempts to read, modify, or delete a Workout or Program owned by a different User, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.
5. THE Workout_Creator_Service SHALL only return Workouts and Programs associated with the authenticated User's identity; Workouts and Programs are private and SHALL NOT be visible to other Users.
6. THE Workout_Creator_Service SHALL allow a User to view the full details of any Workout in their Vault, including all Sections and Exercises.

---

### Requirement 3: Vault Search and Filter

**User Story:** As a User, I want to search and filter my Vault, so that I can quickly find the right workout for today.

#### Acceptance Criteria

1. WHEN a User submits a search query, THE Workout_Creator_Service SHALL return all Workouts and Programs in the User's Vault whose name or description contains the query string (case-insensitive).
2. THE Workout_Creator_Service SHALL support filtering Vault results by the following dimensions: estimated duration, required equipment, training goal, primary muscle group, and Workout type (Strength, CrossFit, Hybrid).
3. WHEN multiple filters are applied simultaneously, THE Workout_Creator_Service SHALL return only Workouts and Programs that satisfy all applied filters.
4. WHEN a search or filter returns no results, THE Workout_Creator_Service SHALL return an empty list and a 200 OK response.

---

### Requirement 3b: Workout and Program File Upload (via UI)

**User Story:** As a User, I want to upload a Workout or Program JSON file through the app, so that I can import training content I created externally into my Vault.

#### Acceptance Criteria

1. WHEN a User submits a POST request to `/api/v1/uploads/programs` with a valid Program JSON file not exceeding 1 MB, THE Workout_Creator_Service SHALL parse, validate, persist, and return a 201 Created response with the saved Program.
2. THE Workout_Creator_Service SHALL associate the uploaded content with the authenticated User's identity from the JWT subject claim.
3. THE Workout_Creator_Service SHALL enforce a maximum upload file size of 1 MB and reject non-JSON content types with a 400 Bad Request.
4. THE Workout_Creator_Service SHALL expose a validate-only endpoint at `/api/v1/uploads/programs/validate` that checks the schema without persisting anything.
5. THE Workout_Coach_UI SHALL provide a file picker, client-side preview, and optional JSON editor before submission.

> Full upload schema, field constraints, UI behaviour, and round-trip property are specified in the `workout-creator-service-upload` sub-spec.

---

### Requirement 3c: Workout and Program Ingest via Email [TODO]

> **TODO**: This requirement is deferred. Implementation should begin only after the file upload feature (Requirement 3b) is complete and stable.

**User Story:** As a User, I want to email a Workout or Program JSON to the app's dedicated address, so that I can import training content without logging into the UI.

#### Acceptance Criteria

1. THE platform SHALL provide a dedicated inbound email address (e.g. `vault@hybridstrength.app`) that the Workout_Creator_Service monitors for incoming messages.
2. WHEN an email is received at the inbound address, THE Workout_Creator_Service SHALL identify the sending User by matching the `From` address against the email address registered as the User's username in the Auth Service; IF no matching User is found, THE Workout_Creator_Service SHALL discard the message and SHALL NOT send a reply.
3. THE Workout_Creator_Service SHALL accept the Program JSON payload either as the plain-text or HTML body of the email, or as a `.json` file attachment; IF both are present, the attachment SHALL take precedence.
4. WHEN the extracted JSON passes all Upload_Schema validation rules, THE Workout_Creator_Service SHALL persist the Program to the identified User's Vault and SHALL send a confirmation email to the source address containing the program name and a link to view it in the Vault.
5. WHEN the extracted JSON fails schema validation, THE Workout_Creator_Service SHALL send an error email to the source address listing each failing field path and its associated message, and SHALL NOT persist any partial content.
6. WHEN the email body and any attachments contain no parseable JSON, THE Workout_Creator_Service SHALL send an error email to the source address with the message `"No valid JSON content found in your email. Please include a JSON body or attach a .json file."`.
7. WHEN the JSON payload exceeds 1 MB, THE Workout_Creator_Service SHALL send an error email to the source address with the message `"Attached file exceeds the maximum allowed size of 1 MB."` and SHALL NOT attempt to parse the content.
8. THE Workout_Creator_Service SHALL apply the same ownership and visibility rules to email-ingested content as to uploaded and AI-generated content; ingested Programs SHALL be recorded with content source `EMAIL_INGESTED`.
9. THE Workout_Creator_Service SHALL process each inbound email at most once; duplicate delivery SHALL be handled idempotently using the email message identifier.
10. THE Workout_Creator_Service SHALL NOT expose the inbound email processing mechanism to unauthenticated callers; the email polling or webhook handler SHALL be an internal adapter with no public REST endpoint.

---

### Requirement 4: Data Ownership and Schema Management

**User Story:** As a platform operator, I want the Workout Creator Service to own its data exclusively, so that it can be deployed and scaled independently.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL own and manage the Workout data store exclusively; no other service SHALL read from or write directly to it.
2. THE Workout_Creator_Service SHALL manage its PostgreSQL schema using Flyway versioned migration scripts.
3. WHEN the Workout_Creator_Service starts, it SHALL apply any pending Flyway migrations before accepting traffic.
4. IF a Flyway migration fails on startup, THEN THE Workout_Creator_Service SHALL halt startup and SHALL log the migration error with sufficient detail to identify the failing script.
