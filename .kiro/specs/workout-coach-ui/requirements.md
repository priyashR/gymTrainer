# Requirements Document — Workout Coach UI

## Introduction

The Workout Coach UI is the single frontend application serving all user-facing views for the HybridStrength platform. It is a React 18 Single Page Application built with Vite and React Router v6 for client-side routing. All rendering is client-side.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Program**: A structured collection of Workouts spanning one or more weeks
- **Theater Mode**: The distraction-free active workout execution UI
- **Workout_Coach_UI**: The single frontend application serving all user-facing views
- **JWT**: JSON Web Token — a signed token used to authenticate requests to protected endpoints
- **Vault**: A User's personal library of saved Workouts and Programs
- **Section**: A named block within a Workout (e.g., a strength block, an AMRAP, a Tabata interval)
- **Exercise**: A single movement within a Section (e.g., Back Squat, Box Jump)
- **RPE**: Rate of Perceived Exertion — a subjective effort scale (1–10)
- **CrossFit Score**: A result recorded for AMRAP (rounds + reps), EMOM, Tabata, or For Time Sections
- **PR**: Personal Record — the best result a User has achieved for a given Exercise or Workout
d
---

## Requirements

### Requirement 1: Navigation and Home Screen

**User Story:** As a User, I want a clear home screen with quick access to key actions, so that I can start training without friction.

#### Acceptance Criteria

1. WHEN a User logs in, THE Workout_Coach_UI SHALL display the home screen with the following primary actions: "New Workout", "My Performance", and "Workout" (resume or start).
2. WHEN an active Program exists for the User, THE Workout_Coach_UI SHALL display the next scheduled Program day as the "Next Step" indicator on the home screen, showing the Program name, current week number, day number, and Workout name alongside a "Start" action.
3. THE Workout_Coach_UI SHALL be implemented as a React 18 Single Page Application using Vite as the build tool and React Router v6 for client-side routing; all components SHALL be client-rendered.
4. WHEN a User navigates between views, THE Workout_Coach_UI SHALL maintain authentication state without requiring re-login for the duration of the JWT validity period.


---

### Requirement 2: Authentication Views

**User Story:** As a visitor, I want to register and log in through the UI, so that I can access my personal training data securely.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a registration view that collects an email address and password, validates inputs client-side, and submits the request to the Auth_Service registration endpoint.
2. THE Workout_Coach_UI SHALL provide a login view that collects an email and password and submits the request to the Auth_Service login endpoint.
3. WHEN login succeeds, THE Workout_Coach_UI SHALL store the JWT access token in memory and SHALL rely on the HttpOnly refresh cookie set by the Auth_Service for token renewal.
4. WHEN a JWT access token expires, THE Workout_Coach_UI SHALL automatically request a new access token using the refresh cookie before retrying the failed request.
5. WHEN a User clicks "Log out", THE Workout_Coach_UI SHALL clear the in-memory access token, request refresh token invalidation from the Auth_Service, and redirect to the login view.
6. THE Workout_Coach_UI SHALL protect all routes except login and registration behind an authentication guard that redirects unauthenticated Users to the login view.


---

### Requirement 3: AI Workout and Program Generation

**User Story:** As a User, I want to describe a workout in natural language and receive a generated result, so that I can start training without manual planning.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a generation view where a User can enter a natural language description and select a scope of "day", "week", or "4-week".
2. WHEN a User submits a generation request, THE Workout_Coach_UI SHALL display a loading state and SHALL submit the request to the Workout_Creator_Service generation endpoint.
3. WHEN the Workout_Creator_Service returns a generated Workout or Program, THE Workout_Coach_UI SHALL display the result in a structured, readable format showing all Sections and Exercises.
4. WHEN a generated result is displayed, THE Workout_Coach_UI SHALL provide a "Save to Vault" action that persists the Workout or Program via the Workout_Creator_Service.
5. WHEN a generated result is displayed, THE Workout_Coach_UI SHALL provide a "Try Again" action that discards the current result and submits a new generation request with the same description.
6. IF the Workout_Creator_Service returns an error, THE Workout_Coach_UI SHALL display a user-friendly error message and allow the User to retry.


---

### Requirement 4: Vault — Browse, Search, and Manage

**User Story:** As a User, I want to browse, search, and manage my saved workouts and programs, so that I can quickly find and reuse my training content.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a Vault view that displays a paginated list of the User's saved Workouts and Programs, fetched from the Workout_Creator_Service.
2. THE Workout_Coach_UI SHALL provide a search input that filters Vault results by name or description as the User types.
3. THE Workout_Coach_UI SHALL provide filter controls for estimated duration, required equipment, training goal, primary muscle group, and Workout type (Strength, CrossFit, Hybrid).
4. WHEN a User selects a Workout or Program from the Vault list, THE Workout_Coach_UI SHALL navigate to a detail view showing all Sections, Exercises, and metadata.
5. THE Workout_Coach_UI SHALL provide edit and delete actions on the Workout detail view; delete SHALL require confirmation before submitting to the Workout_Creator_Service.
6. WHEN a search or filter returns no results, THE Workout_Coach_UI SHALL display an empty state message.


---

### Requirement 5: Theater Mode — Active Workout Execution

**User Story:** As a User, I want a distraction-free workout execution interface, so that I can focus entirely on training.

#### Acceptance Criteria

1. WHEN a User starts a Workout, THE Workout_Coach_UI SHALL enter Theater Mode, displaying only the current Section, its Exercises, and the active timer.
2. THE Workout_Coach_UI SHALL configure the timer display based on Section type: countdown for AMRAP, stopwatch for Strength, and interval for Tabata and EMOM.
3. THE Workout_Coach_UI SHALL display navigation controls to move to the next or previous Section within the active Session.
4. WHEN a User checks off an Exercise, THE Workout_Coach_UI SHALL record the completion via the Session_Service and SHALL start the rest timer using the default duration defined in the Exercise definition.
5. THE Workout_Coach_UI SHALL allow a User to adjust the rest timer duration during an active Session; the adjusted duration SHALL apply to the current rest period only.
6. WHEN the rest timer expires, THE Workout_Coach_UI SHALL display a visual and audible notification to resume the next Exercise.
7. THE Workout_Coach_UI SHALL display a "Next Up" indicator showing the name of the next Exercise within the current Section and, where applicable, the next Section name.
8. WHEN a User reopens the application with an incomplete Session, THE Workout_Coach_UI SHALL offer a "Resume Session" action that restores the Session to the last persisted state via the Session_Service.


---

### Requirement 6: Performance Logging

**User Story:** As a User, I want to log my performance during a workout, so that my results are captured accurately for progress tracking.

#### Acceptance Criteria

1. DURING an active Strength Exercise, THE Workout_Coach_UI SHALL provide input fields for weight, repetitions, and RPE for each set, and SHALL submit the data to the Session_Service.
2. DURING an active AMRAP Section, THE Workout_Coach_UI SHALL display a tap-based round counter that increments with each tap, and SHALL provide a field for additional reps at the end of the Section.
3. DURING an active For Time Section, THE Workout_Coach_UI SHALL display a running stopwatch and SHALL allow the User to record the total completion time.
4. WHEN a User ends a Session, THE Workout_Coach_UI SHALL submit the completion to the Session_Service and display a Session summary showing all logged data.
5. IF a User attempts to end a Session with no logged data, THE Workout_Coach_UI SHALL display a confirmation prompt before submitting.


---

### Requirement 7: Progress Dashboard

**User Story:** As a User, I want a performance dashboard, so that I can understand my training trends and results at a glance.

#### Acceptance Criteria

1. WHEN a User navigates to the "My Performance" section, THE Workout_Coach_UI SHALL display a dashboard summary including total Sessions completed, total volume lifted, total training time, and number of PRs achieved in the last 30 days, fetched from the Progress_Tracker_Service.
2. THE Workout_Coach_UI SHALL provide an Exercise stats view showing a chronological history of logged sets including date, weight, reps, RPE, and estimated 1RM.
3. THE Workout_Coach_UI SHALL provide a Workout stats view showing a chronological history of Sessions for a specific Workout including date, total volume, and CrossFit Scores where applicable.
4. WHEN a PR notification event is received, THE Workout_Coach_UI SHALL display a celebratory notification to the User.
5. WHEN a User views a historical Session, THE Workout_Coach_UI SHALL display the comparison label "Last time you did [weight]kg for [reps] reps" for each Exercise where prior data exists.


---

### Requirement 8: CrossFit and Benchmark Tracking

**User Story:** As a CrossFitter, I want to track my benchmark workout results, so that I can measure my fitness progress against standardised tests.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a Benchmarks view that displays the catalogue of Benchmark Workouts (The Girls, Hero WODs) fetched from the Progress_Tracker_Service.
2. WHEN a User selects a Benchmark Workout, THE Workout_Coach_UI SHALL display all of the User's recorded results for that Benchmark in chronological order, including AMRAP scores, For Time scores, and EMOM completion rates.
3. WHEN a User completes a Session for a Benchmark Workout, THE Workout_Coach_UI SHALL highlight the result as a PR if it exceeds the User's previous best for that Benchmark.


---

### Requirement 9: Muscle Activation Heat Map

**User Story:** As a User, I want to see a visual muscle activation map, so that I can identify imbalances and ensure balanced training.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a heat map view that renders a body diagram with muscle groups shaded proportionally to their training volume, using data fetched from the Progress_Tracker_Service.
2. THE Workout_Coach_UI SHALL provide time window controls allowing the User to select 7 days, 30 days, or 90 days for the heat map calculation; the default SHALL be 30 days.
3. WHEN a User changes the time window, THE Workout_Coach_UI SHALL re-fetch the muscle activation data and update the heat map without a full page reload.