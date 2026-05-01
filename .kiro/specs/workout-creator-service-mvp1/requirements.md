# Requirements Document — Workout Creator Service MVP1

## Introduction

MVP1 of the Workout Creator Service delivers AI-powered Workout and Program generation via Google Gemini. It establishes the core domain model (Workouts, Programs, Sections, Exercises), the Gemini integration with structured prompting, parse/format round-trip correctness for Workout domain objects, and the Flyway-managed PostgreSQL schema that underpins all future MVPs.

Out of scope for MVP1: Vault CRUD (Requirement 2), Search and Filter (Requirement 3), and the "Try Again" UI action (a frontend concern handled by the workout-coach-ui spec). UI controls (radio buttons, checkboxes) are also out of scope — the API request model supports the fields the frontend will use.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform, identified by the JWT subject claim
- **Workout**: A single training session definition containing one or more Sections
- **Program**: A structured collection of Workouts spanning one or more weeks
- **Section**: A named block within a Workout representing a distinct training phase (e.g., a strength block, an AMRAP, a Tabata interval)
- **Section_Type**: An enumeration of supported Section kinds — Strength, AMRAP, EMOM, Tabata, For_Time, and Accessory
- **Exercise**: A single movement within a Section (e.g., Back Squat, Box Jump), including prescribed sets, reps, weight, and rest
- **Training_Style**: An enumeration of supported training methodologies — CrossFit, Hypertrophy, and Strength. A generation request includes one or more Training_Style values depending on the Generation_Scope
- **Gemini**: Google Gemini — the external AI service used to generate Workout and Program content
- **Workout_Creator_Service**: The Spring Boot microservice responsible for AI-powered Workout and Program creation
- **Generation_Scope**: The temporal scope of a generation request — "day" (single Workout), "week" (7-day Program), or "4-week" (28-day Program)
- **Structured_Prompt**: A prompt template sent to Gemini that includes the User's natural language description, the target Generation_Scope, the selected Training_Style values, and schema constraints for the expected response format
- **Workout_Parser**: The component that transforms a Gemini text response into a structured Workout or Program domain object
- **Workout_Formatter**: The component that transforms a Workout or Program domain object into a human-readable text representation (pretty-print)
- **Flyway**: The schema migration tool used to version-control and apply PostgreSQL DDL changes on service startup
- **Raw_Gemini_Response**: The unprocessed text returned by Gemini before parsing, always included in the API response regardless of parsing success or failure

---

## Requirements

### Requirement 1: AI-Powered Workout and Program Generation

**User Story:** As a User, I want to describe a workout or program in natural language, select a training style, and have it generated for me, so that I can train without spending time on manual planning.

#### Acceptance Criteria

1. WHEN a User submits a natural language description with a Generation_Scope of "day", "week", or "4-week" and one or more Training_Style values, THE Workout_Creator_Service SHALL send a Structured_Prompt to Gemini and return a generated Workout (for "day") or Program (for "week" or "4-week").
2. THE Workout_Creator_Service SHALL validate the generation request, requiring a non-blank description, a valid Generation_Scope, and at least one valid Training_Style; IF the request is invalid, THEN THE Workout_Creator_Service SHALL return a 400 Bad Request response with field-level validation errors.
3. THE Workout_Creator_Service SHALL expose the generation endpoint at `POST /api/v1/workouts/generate`, accepting a JSON request body containing the description, scope, and trainingStyles fields, and SHALL require a valid JWT in the Authorization header.
4. WHEN the Generation_Scope is "day", THE Workout_Creator_Service SHALL require exactly one Training_Style in the trainingStyles list; IF more than one Training_Style is provided for a "day" scope, THEN THE Workout_Creator_Service SHALL return a 400 Bad Request response.
5. WHEN the Generation_Scope is "week" or "4-week", THE Workout_Creator_Service SHALL accept one or more Training_Style values in the trainingStyles list, and the generated Program may include Workouts of different Training_Styles across the days.
6. THE Workout_Creator_Service SHALL include the selected Training_Style values in the Structured_Prompt sent to Gemini so that the generated content reflects the requested training methodology.

---

### Requirement 2: Gemini Response Parsing

**User Story:** As a User, I want the AI-generated content to be structured and consistent, so that I can read and follow the workout without confusion.

#### Acceptance Criteria

1. THE Workout_Parser SHALL parse the Gemini response into a structured Workout or Program domain object conforming to the internal data model.
2. IF the Gemini response does not conform to the expected structure, THEN THE Workout_Parser SHALL return a descriptive parsing error rather than producing a malformed domain object.
3. THE Workout_Creator_Service SHALL sanitise all text content returned from Gemini before including it in the response to the caller.

---

### Requirement 3: Workout Formatting (Pretty-Print)

**User Story:** As a developer, I want to format Workout domain objects back into human-readable text, so that the system can produce consistent textual representations for display and debugging.

#### Acceptance Criteria

1. THE Workout_Formatter SHALL format a Workout or Program domain object into a human-readable text representation.
2. FOR ALL valid Workout domain objects, parsing the formatted text then formatting the parsed result SHALL produce text equivalent to the original formatted output (round-trip property).
3. THE Workout_Formatter SHALL include all Section names, Section_Types, Exercise names, prescribed sets, reps, weight, and rest periods in the formatted output.

---

### Requirement 4: Section Type Support

**User Story:** As a User, I want my generated workouts to include a variety of training styles with appropriate timing information, so that I can follow diverse programming methodologies with proper rest guidance.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL support the following Section_Types in generated content: Strength, AMRAP, EMOM, Tabata, For_Time, and Accessory.
2. WHEN a Section is generated, THE Workout_Creator_Service SHALL assign exactly one Section_Type to that Section.
3. WHEN a Section has a Section_Type of AMRAP, THE Workout_Creator_Service SHALL include a time cap in minutes for that Section.
4. WHEN a Section has a Section_Type of EMOM, THE Workout_Creator_Service SHALL include the interval duration in seconds and the total number of rounds for that Section.
5. WHEN a Section has a Section_Type of Tabata, THE Workout_Creator_Service SHALL include the work interval in seconds, rest interval in seconds, and total number of rounds for that Section.
6. WHEN a Section has a Section_Type of For_Time, THE Workout_Creator_Service SHALL include a time cap in minutes for that Section.
7. WHEN a Section has a Section_Type of Strength or Accessory, THE Workout_Creator_Service SHALL include a recommended rest timer duration in seconds for each Exercise in that Section.
8. WHEN a Section has a Section_Type of AMRAP, EMOM, Tabata, or For_Time, THE Workout_Creator_Service SHALL NOT include a per-exercise rest timer duration, as these Section_Types define their own timing structure.

---

### Requirement 5: Generation Response Structure

**User Story:** As a User, I want to see both the raw AI output and the structured result, so that I can still read the workout even if parsing fails.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL include the Raw_Gemini_Response text in every successful generation response alongside the parsed Workout or Program domain object.
2. IF the Workout_Parser fails to parse the Gemini response into a valid domain object, THEN THE Workout_Creator_Service SHALL return a 200 OK response containing the Raw_Gemini_Response text and a human-readable parsing error message.
3. IF the Workout_Parser fails to parse the Gemini response, THEN THE Workout_Creator_Service SHALL NOT return a 500 Internal Server Error; the response SHALL remain a 200 OK with the raw text and error details so the User can read what Gemini returned.
4. WHEN parsing succeeds, THE generation response SHALL contain the Raw_Gemini_Response text, the parsed Workout or Program domain object, and a null parsing error field.

---

### Requirement 6: Gemini Error Handling

**User Story:** As a User, I want clear feedback when AI generation fails, so that I know the system encountered a problem and I can try again.

#### Acceptance Criteria

1. IF the Gemini service returns an error, THEN THE Workout_Creator_Service SHALL return a 502 Bad Gateway response with a human-readable error message and SHALL NOT persist a partial Workout or Program.
2. IF the Gemini service does not respond within 10 seconds, THEN THE Workout_Creator_Service SHALL abort the request and return a 502 Bad Gateway response indicating a timeout.
3. THE Workout_Creator_Service SHALL wrap all Gemini API calls with a Resilience4j circuit breaker to prevent cascading failures.
4. WHILE the Gemini circuit breaker is in the open state, THE Workout_Creator_Service SHALL return a 502 Bad Gateway response immediately without attempting to call Gemini.

---

### Requirement 7: Data Ownership and Schema Management

**User Story:** As a platform operator, I want the Workout Creator Service to own its data exclusively and manage schema changes via Flyway, so that it can be deployed and scaled independently.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL own and manage the Workout data store exclusively; no other service SHALL read from or write directly to it.
2. THE Workout_Creator_Service SHALL manage its PostgreSQL schema using Flyway versioned migration scripts in the V100–V199 range.
3. WHEN the Workout_Creator_Service starts, THE Workout_Creator_Service SHALL apply any pending Flyway migrations before accepting traffic.
4. IF a Flyway migration fails on startup, THEN THE Workout_Creator_Service SHALL halt startup and SHALL log the migration error with sufficient detail to identify the failing script.
5. THE Flyway migrations SHALL create tables for Workouts, Programs, Sections, and Exercises with appropriate foreign key relationships and indexes.
