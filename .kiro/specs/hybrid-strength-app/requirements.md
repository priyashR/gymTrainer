# Requirements Document

## Introduction

HybridStrength is a unified training platform for hybrid athletes, CrossFitters, and traditional gym-goers. The platform enables users to create AI-generated workouts and programs, execute workouts in a distraction-free "Theater Mode" experience, and track performance over time through a rich progress dashboard. The system is composed of four microservices — User/Auth, Workout Creator, Workout Session, and Progress Tracker — each following hexagonal architecture principles and communicating via REST and RabbitMQ where appropriate.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Admin**: A privileged User with access to administrative functions
- **Workout**: A single training session definition containing one or more Sections
- **Program**: A structured collection of Workouts spanning one or more weeks
- **Section**: A named block within a Workout (e.g., a strength block, an AMRAP, a Tabata interval)
- **Exercise**: A single movement within a Section (e.g., Back Squat, Box Jump)
- **Vault**: A User's personal library of saved Workouts and Programs
- **Theater Mode**: The distraction-free active workout execution UI
- **Session**: A single in-progress or completed execution of a Workout by a User
- **Performance Log**: A record of a User's results for a completed Session
- **1RM**: One-repetition maximum — the maximum weight a User can lift for a single repetition of an Exercise
- **RPE**: Rate of Perceived Exertion — a subjective effort scale (1–10)
- **AMRAP**: As Many Rounds As Possible — a timed Section type
- **EMOM**: Every Minute On the Minute — an interval Section type
- **Tabata**: A specific interval protocol (20s work / 10s rest, 8 rounds)
- **For Time**: A Section type where the User completes a fixed amount of work as fast as possible
- **CrossFit Score**: A result recorded for AMRAP (rounds + reps), EMOM, Tabata, or For Time Sections
- **PR**: Personal Record — the best result a User has achieved for a given Exercise or Workout
- **Benchmark Workout**: A named, standardised Workout used to measure fitness (e.g., "Fran", "Murph")
- **Gemini**: Google Gemini — the external AI service used to generate Workout and Program content
- **Workout_Creator_Service**: The microservice responsible for AI-powered Workout and Program creation and the Vault
- **Session_Service**: The microservice responsible for active Workout execution and Session state management
- **Progress_Tracker_Service**: The microservice responsible for performance analytics and dashboards
- **Auth_Service**: The microservice responsible for User registration, authentication, and authorisation
- **Workout_Coach_UI**: The single frontend application serving all user-facing views

---

## Requirements

### Requirement 1: User Registration and Authentication

**User Story:** As a visitor, I want to register and log in, so that I can access my personal training data securely.

#### Acceptance Criteria

1. THE Auth_Service SHALL provide a registration endpoint that accepts a unique email address and a password of at least 8 characters.
2. WHEN a registration request is received with a duplicate email address, THE Auth_Service SHALL return a 409 Conflict response with a descriptive error message.
3. WHEN a valid login request is received, THE Auth_Service SHALL return a signed JWT access token and a refresh token.
4. WHEN an expired JWT is presented with a valid refresh token, THE Auth_Service SHALL issue a new JWT access token without requiring re-authentication.
5. WHEN an invalid or missing JWT is presented to any protected endpoint, THE Auth_Service SHALL return a 401 Unauthorised response.
6. THE Auth_Service SHALL store passwords using a one-way cryptographic hash (bcrypt with a minimum cost factor of 12).
7. WHEN a User requests a password reset, THE Auth_Service SHALL send a time-limited reset link to the registered email address, valid for no more than 60 minutes.

---

### Requirement 2: Admin User Management

**User Story:** As an Admin, I want to manage User accounts, so that I can maintain platform integrity.

#### Acceptance Criteria

1. THE Auth_Service SHALL expose admin-only endpoints protected by a role-based access control check for the ADMIN role.
2. WHEN an Admin requests a list of Users, THE Auth_Service SHALL return a paginated list of User accounts including email, registration date, and account status.
3. WHEN an Admin deactivates a User account, THE Auth_Service SHALL prevent that User from authenticating until the account is reactivated.
4. IF a non-Admin User attempts to access an admin-only endpoint, THEN THE Auth_Service SHALL return a 403 Forbidden response.

---

### Requirement 3: AI-Powered Workout and Program Generation

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

### Requirement 4: Workout and Program CRUD (Vault)

**User Story:** As a User, I want to save, view, edit, and delete workouts and programs in my personal Vault, so that I can build a reusable training library.

#### Acceptance Criteria

1. WHEN a User saves a generated or manually created Workout, THE Workout_Creator_Service SHALL persist it to the Workout data store associated with that User's identity.
2. THE Workout_Creator_Service SHALL allow a User to update the name, description, and Section content of any Workout owned by that User.
3. WHEN a User deletes a Workout, THE Workout_Creator_Service SHALL remove it from the Vault and SHALL return a 404 Not Found for any subsequent fetch of that Workout.
4. IF a User attempts to read, modify, or delete a Workout or Program owned by a different User, THEN THE Workout_Creator_Service SHALL return a 403 Forbidden response.
5. THE Workout_Creator_Service SHALL only return Workouts and Programs associated with the authenticated User's identity; Workouts and Programs are private and SHALL NOT be visible to other Users.
6. THE Workout_Creator_Service SHALL allow a User to view the full details of any Workout in their Vault, including all Sections and Exercises.

---

### Requirement 5: Vault Search and Filter

**User Story:** As a User, I want to search and filter my Vault, so that I can quickly find the right workout for today.

#### Acceptance Criteria

1. WHEN a User submits a search query, THE Workout_Creator_Service SHALL return all Workouts and Programs in the User's Vault whose name or description contains the query string (case-insensitive).
2. THE Workout_Creator_Service SHALL support filtering Vault results by the following dimensions: estimated duration, required equipment, training goal, primary muscle group, and Workout type (Strength, CrossFit, Hybrid).
3. WHEN multiple filters are applied simultaneously, THE Workout_Creator_Service SHALL return only Workouts and Programs that satisfy all applied filters.
4. WHEN a search or filter returns no results, THE Workout_Creator_Service SHALL return an empty list and a 200 OK response.

---

### Requirement 6: Active Workout — Theater Mode

**User Story:** As a User, I want a distraction-free workout execution interface, so that I can focus entirely on training without navigating menus.

#### Acceptance Criteria

1. WHEN a User starts a Workout, THE Workout_Coach_UI SHALL enter Theater Mode, displaying only the current Section, its Exercises, and the active timer.
2. THE Session_Service SHALL detect the Section type and configure the timer accordingly: countdown for AMRAP, stopwatch for Strength, and interval for Tabata and EMOM.
3. WHILE a Session is active, THE Workout_Coach_UI SHALL display navigation controls to move to the next or previous Section.
4. WHEN a User checks off an Exercise within a Section, THE Session_Service SHALL record the completion and SHALL automatically start the rest timer for that Exercise, using the default duration defined in the Exercise definition.
5. THE Workout_Coach_UI SHALL allow a User to adjust the rest timer duration during an active Session; the adjusted duration SHALL apply to the current rest period only and SHALL NOT modify the Exercise definition.
6. WHEN the rest timer expires, THE Workout_Coach_UI SHALL display a visual and audible notification to resume the next Exercise.
7. WHILE a Session is active, THE Workout_Coach_UI SHALL display a "Next Up" indicator showing the name of the next Exercise within the current Section and, where applicable, the name of the next Section.
8. WHILE a Session is active, THE Session_Service SHALL persist the current Session state to the Workout session data store after each Section completion, so that the Session can be resumed if the application closes.
9. WHEN a User reopens the application with an incomplete Session, THE Workout_Coach_UI SHALL offer a "Resume Session" action that restores the Session to the last persisted state.

---

### Requirement 7: Live Performance Logging

**User Story:** As a User, I want to log my performance during a workout, so that my results are captured accurately for progress tracking.

#### Acceptance Criteria

1. WHEN a User completes a Strength Exercise set, THE Session_Service SHALL accept and persist the weight lifted, number of repetitions, and RPE value for that set.
2. WHEN a User completes an AMRAP, EMOM, or For Time Section, THE Session_Service SHALL accept and persist the CrossFit Score (rounds completed, additional reps, and total time where applicable).
3. DURING an active AMRAP Section, THE Workout_Coach_UI SHALL display a tap-based round counter that increments the round count with each tap, and SHALL provide a field for the User to log additional reps at the end of the Section.
4. WHEN a User ends a Session, THE Session_Service SHALL mark the Session as complete and SHALL publish a SessionCompleted event to the message broker for consumption by the Progress_Tracker_Service.
5. IF a User attempts to end a Session with no logged data, THEN THE Session_Service SHALL prompt the User to confirm before marking the Session as complete.

---

### Requirement 8: Session State and Program Progression

**User Story:** As a User, I want the app to track where I am in my program, so that I always know what to do next without manual tracking.

#### Acceptance Criteria

1. WHEN a User completes a Session that is part of a Program, THE Session_Service SHALL advance the Program's current day pointer to the next scheduled Workout.
2. WHEN a User views the home screen, THE Workout_Coach_UI SHALL display the next scheduled Program Workout as a "Next Step" indicator, showing the Program name, current week number, day number, and Workout name alongside a "Start" action.
3. WHEN a User completes the final Session of a Program, THE Session_Service SHALL mark the Program as complete and SHALL notify the User via the UI.
4. THE Session_Service SHALL allow a User to start a new standalone Workout or a new Program independently of any active Program.
5. WHILE a Program is active, THE Session_Service SHALL allow a User to continue the current Program from the home screen with a single action.
6. WHEN a User navigates away from a Workout Details screen or cancels before starting a Session, THE Session_Service SHALL NOT advance or terminate the User's active Program state.

---

### Requirement 9: Progress Dashboard

**User Story:** As a User, I want a performance dashboard, so that I can understand my training trends and results at a glance.

#### Acceptance Criteria

1. WHEN a User navigates to the Progress section, THE Progress_Tracker_Service SHALL return a dashboard summary including: total Sessions completed, total volume lifted (kg), total training time, and number of PRs achieved in the last 30 days.
2. THE Progress_Tracker_Service SHALL calculate and expose the estimated 1RM for any Exercise with sufficient logged data, using the Epley formula (weight × (1 + reps / 30)).
3. WHEN a User views Exercise stats for a specific Exercise, THE Progress_Tracker_Service SHALL return a chronological history of logged sets including date, weight, reps, RPE, and estimated 1RM.
4. WHEN a User views Workout stats for a specific Workout, THE Progress_Tracker_Service SHALL return a chronological history of Sessions for that Workout including date, total volume, and CrossFit Scores where applicable.
5. WHEN a User's logged performance for an Exercise exceeds their previous best weight, reps, or estimated 1RM, THE Progress_Tracker_Service SHALL emit a PR notification event for display in the Workout_Coach_UI.
6. WHEN a User views a historical Session, THE Progress_Tracker_Service SHALL display the comparison label "Last time you did [weight]kg for [reps] reps" for each Exercise where prior data exists.

---

### Requirement 10: CrossFit and Benchmark Tracking

**User Story:** As a CrossFitter, I want to track my benchmark workout results, so that I can measure my fitness progress against standardised tests.

#### Acceptance Criteria

1. THE Progress_Tracker_Service SHALL maintain a catalogue of Benchmark Workouts including The Girls (e.g., Fran, Grace, Helen) and Hero WODs.
2. WHEN a User completes a Session for a Benchmark Workout, THE Progress_Tracker_Service SHALL record the result in a dedicated Benchmark history for that User.
3. WHEN a User views their Benchmark history, THE Progress_Tracker_Service SHALL return all recorded results for each Benchmark Workout in chronological order.
4. THE Progress_Tracker_Service SHALL track AMRAP scores (rounds + reps), For Time scores (total time in seconds), and EMOM completion rates as distinct metric types.

---

### Requirement 11: Muscle Activation Heat Map

**User Story:** As a User, I want to see a visual muscle activation map, so that I can identify imbalances and ensure balanced training.

#### Acceptance Criteria

1. WHEN a User views the muscle activation heat map, THE Progress_Tracker_Service SHALL calculate the relative training volume per primary muscle group across all Sessions in the selected time window (default: last 30 days).
2. THE Progress_Tracker_Service SHALL return muscle activation data as a percentage of total volume per muscle group, enabling the Workout_Coach_UI to render a proportional heat map overlay.
3. THE Progress_Tracker_Service SHALL support time window selection of 7 days, 30 days, and 90 days for the heat map calculation.

---

### Requirement 12: Workout Coach UI — Navigation and Home Screen

**User Story:** As a User, I want a clear home screen with quick access to key actions, so that I can start training without friction.

#### Acceptance Criteria

1. WHEN a User logs in, THE Workout_Coach_UI SHALL display the home screen with the following primary actions: "New Workout", "My Performance", and "Workout" (resume or start).
2. WHEN an active Program exists for the User, THE Workout_Coach_UI SHALL display the next scheduled Program day as the "Next Step" indicator on the home screen.
3. THE Workout_Coach_UI SHALL be implemented using React 18 with Next.js (App Router, SSR-first); it SHALL NOT be implemented as a client-side Single Page Application where all routing and rendering occurs in the browser.
4. WHEN a User navigates between views, THE Workout_Coach_UI SHALL maintain authentication state without requiring re-login for the duration of the JWT validity period.

---

### Requirement 13: Data Integrity and Service Isolation

**User Story:** As a platform operator, I want each microservice to own its data, so that services can be deployed and scaled independently.

#### Acceptance Criteria

1. THE Workout_Creator_Service SHALL own and manage the Workout data store exclusively; no other service SHALL read from or write directly to it.
2. THE Session_Service SHALL retrieve Workout and Program definitions by calling the Workout_Creator_Service API; THE Session_Service SHALL NOT query the Workout data store directly.
3. THE Session_Service SHALL own and manage the Workout session data store and SHALL publish domain events to RabbitMQ for cross-service communication.
4. THE Progress_Tracker_Service SHALL consume SessionCompleted events from RabbitMQ and SHALL maintain its own read-optimised Performance data store.
5. THE Auth_Service SHALL own and manage the User data store; all other services SHALL validate identity via JWT verification only.
6. WHEN a SessionCompleted event cannot be delivered to RabbitMQ, THE Session_Service SHALL retry delivery using an exponential backoff strategy with a maximum of 5 attempts before logging the failure for manual intervention.

---

### Requirement 14: Schema Management and Database Standards

**User Story:** As a developer, I want database schemas managed through versioned migrations, so that schema changes are reproducible and auditable across environments.

#### Acceptance Criteria

1. THE Workout_Creator_Service, Session_Service, Progress_Tracker_Service, and Auth_Service SHALL each manage their own PostgreSQL schema using Flyway versioned migration scripts.
2. WHEN a service starts, THE service SHALL apply any pending Flyway migrations before accepting traffic.
3. IF a Flyway migration fails on startup, THEN THE service SHALL halt startup and SHALL log the migration error with sufficient detail to identify the failing script.
