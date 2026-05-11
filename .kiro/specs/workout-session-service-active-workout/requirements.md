# Requirements Document — Workout Session Service: Active Workout

## Introduction

This sub-spec covers the core active workout execution capabilities of the Workout Session Service: Theater Mode (distraction-free workout UI with timers and state persistence), Session State and Program Progression (advancing through multi-week programs), Vault-Initiated Workouts (starting sessions from the Vault search), Theater Mode UI (React components and routes), and Service Integration (data ownership, cross-service communication, and schema management). These requirements form a cohesive, independently implementable slice that delivers the primary session execution flow end-to-end.

The service runs on port 8083, follows hexagonal architecture, and communicates with the Workout Creator Service via REST to fetch workout and program definitions. Real-time session updates are pushed to the frontend via Spring WebSocket + STOMP.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Workout**: A single training session definition containing one or more Sections, retrieved from the Workout Creator Service
- **Program**: A structured collection of Workouts spanning one or more weeks, retrieved from the Workout Creator Service
- **Section**: A named block within a Workout (e.g., a strength block, an AMRAP, a Tabata interval)
- **Exercise**: A single movement within a Section (e.g., Back Squat, Box Jump)
- **Theater_Mode**: The distraction-free active workout execution UI that displays only the current Section, Exercises, and active timer
- **Session**: A single in-progress or completed execution of a Workout by a User
- **Standalone_Session**: A Session started from the Vault for a single Workout day that does not affect the User's active Program_Day_Pointer
- **AMRAP**: As Many Rounds As Possible — a timed Section type using a countdown timer
- **EMOM**: Every Minute On the Minute — an interval Section type
- **Tabata**: A specific interval protocol (20s work / 10s rest, 8 rounds)
- **Strength**: A Section type where the User performs sets of an Exercise with rest periods, using a stopwatch timer
- **Rest_Timer**: A countdown timer that begins after an Exercise is checked off, using the default duration from the Exercise definition
- **Session_Service**: The microservice responsible for active Workout execution and Session state management (port 8083)
- **Workout_Creator_Service**: The microservice responsible for AI-powered Workout and Program creation and the Vault, exposing endpoints at `/api/v1/vault/programs` and `/api/v1/vault/programs/{id}`
- **Workout_Coach_UI**: The React 18 SPA frontend serving all user-facing views
- **SessionCompleted**: A domain event published to RabbitMQ when a User completes a Session
- **Program_Day_Pointer**: A persistent marker indicating the User's current position within a Program (week and day)
- **Flyway**: The schema migration tool managing the Session_Service PostgreSQL schema (migration range V200–V299)
- **Vault**: The searchable collection of saved Workouts and Programs owned by the Workout_Creator_Service
- **Theater_Mode_Route**: The dedicated React Router route (`/workout/session/:sessionId`) that renders the Theater_Mode experience
- **Section_Navigator**: The UI component within Theater_Mode that allows the User to move between Sections
- **Exercise_Checklist**: The UI component within Theater_Mode that displays Exercises in the current Section with checkoff controls
- **Next_Up_Indicator**: The UI component within Theater_Mode that shows the upcoming Exercise or Section name

---

## Requirements

### Requirement 1: Active Workout — Theater Mode

**User Story:** As a User, I want a distraction-free workout execution interface, so that I can focus entirely on training without navigating menus.

#### Acceptance Criteria

1. WHEN a User starts a Workout, THE Workout_Coach_UI SHALL enter Theater_Mode, displaying only the current Section, its Exercises, and the active timer.
2. THE Session_Service SHALL detect the Section type and configure the timer accordingly: countdown for AMRAP, stopwatch for Strength, and interval for Tabata and EMOM.
3. WHILE a Session is active, THE Workout_Coach_UI SHALL display navigation controls to move to the next or previous Section.
4. WHEN a User checks off an Exercise within a Section, THE Session_Service SHALL record the completion and SHALL automatically start the Rest_Timer for that Exercise, using the default duration defined in the Exercise definition.
5. THE Workout_Coach_UI SHALL allow a User to adjust the Rest_Timer duration during an active Session; the adjusted duration SHALL apply to the current rest period only and SHALL NOT modify the Exercise definition.
6. WHEN the Rest_Timer expires, THE Workout_Coach_UI SHALL display a visual and audible notification to resume the next Exercise.
7. WHILE a Session is active, THE Workout_Coach_UI SHALL display a "Next Up" indicator showing the name of the next Exercise within the current Section and, where applicable, the name of the next Section.
8. WHILE a Session is active, THE Session_Service SHALL persist the current Session state to the Workout session data store after each Section completion, so that the Session can be resumed if the application closes.
9. WHEN a User reopens the application with an incomplete Session, THE Workout_Coach_UI SHALL offer a "Resume Session" action that restores the Session to the last persisted state.
10. WHILE a Session is active, THE Workout_Coach_UI SHALL display a "Pause Workout" action; WHEN activated, THE Session_Service SHALL persist the current Session state and stop all active timers, allowing the User to resume later from the same point.
11. WHILE a Session is active, THE Workout_Coach_UI SHALL display an "End Workout" action; WHEN activated, THE Workout_Coach_UI SHALL prompt the User to confirm, and upon confirmation THE Session_Service SHALL mark the Session as complete with whatever progress has been logged up to that point.

---

### Requirement 2: Session State and Program Progression

**User Story:** As a User, I want the app to track where I am in my program, so that I always know what to do next without manual tracking.

#### Acceptance Criteria

1. WHEN a User completes a Session that is part of a Program, THE Session_Service SHALL advance the Program_Day_Pointer to the next scheduled Workout.
2. WHEN a User views the home screen, THE Workout_Coach_UI SHALL display the next scheduled Program Workout as a "Next Step" indicator, showing the Program name, current week number, day number, and Workout name alongside a "Start" action.
3. WHEN a User completes the final Session of a Program, THE Session_Service SHALL mark the Program as complete and SHALL notify the User via the UI.
4. THE Session_Service SHALL allow a User to start a new standalone Workout or a new Program independently of any active Program.
5. WHILE a Program is active, THE Session_Service SHALL allow a User to continue the current Program from the home screen with a single action.
6. WHEN a User navigates away from a Workout Details screen or cancels before starting a Session, THE Session_Service SHALL NOT advance or terminate the User's active Program state.
7. WHEN a User requests to skip the current Program day, THE Session_Service SHALL advance the Program_Day_Pointer to the next scheduled Workout without requiring the User to complete the current day's Session.
8. WHEN a User skips a Program day, THE Session_Service SHALL record the skip event with the skipped day's identifier and timestamp, so that the User's history reflects the gap.

---

### Requirement 3: Vault-Initiated Workouts

**User Story:** As a User, I want to start a workout or program directly from the Vault search results, so that I can quickly begin training without navigating back to the home screen.

#### Acceptance Criteria

1. WHEN a User selects a single Workout day from the Vault, THE Workout_Coach_UI SHALL offer a "Start Standalone" action that begins a Standalone_Session for that Workout.
2. WHEN a User starts a Standalone_Session from the Vault, THE Session_Service SHALL NOT modify the User's active Program_Day_Pointer.
3. WHEN a User selects a Program from the Vault, THE Workout_Coach_UI SHALL present a choice between "Start as Standalone Day" (selecting a single day to execute) and "Start New Program" (replacing the current active Program).
4. WHEN a User chooses "Start New Program" from the Vault and an active Program exists, THE Session_Service SHALL end the current active Program, mark it as replaced, and set the selected Program as the new active Program with the Program_Day_Pointer at day one.
5. WHEN a User chooses "Start as Standalone Day" from a Program in the Vault, THE Session_Service SHALL begin a Standalone_Session for the selected day without ending or modifying the current active Program.
6. IF a User chooses "Start New Program" while an active Program exists, THEN THE Workout_Coach_UI SHALL display a confirmation prompt informing the User that the current Program will be ended before proceeding.
7. WHEN a User starts a new Program from the Vault and no active Program exists, THE Session_Service SHALL set the selected Program as the active Program with the Program_Day_Pointer at day one without requiring confirmation.

---

### Requirement 4: Theater Mode UI

**User Story:** As a User, I want a dedicated Theater Mode interface with timers, section navigation, exercise checkoff, and session resume, so that I have a seamless, focused workout experience in the browser.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL register a protected route at `/workout/session/:sessionId` that renders the Theater_Mode experience.
2. WHEN the Theater_Mode_Route is loaded, THE Workout_Coach_UI SHALL fetch the active Session state from the Session_Service and render the current Section with its Exercises.
3. THE Workout_Coach_UI SHALL render a Section_Navigator component that displays the current Section name, a progress indicator (e.g., "Section 2 of 4"), and previous/next navigation buttons.
4. WHEN the User is on the first Section, THE Section_Navigator SHALL disable the "Previous" button; WHEN the User is on the last Section, THE Section_Navigator SHALL disable the "Next" button.
5. THE Workout_Coach_UI SHALL render an Exercise_Checklist component that lists each Exercise in the current Section with a checkbox control, the prescribed sets and reps, and a visual completion state.
6. WHEN a User checks off an Exercise, THE Exercise_Checklist SHALL visually mark the Exercise as complete and THE Workout_Coach_UI SHALL display the Rest_Timer countdown overlay.
7. THE Workout_Coach_UI SHALL render a timer component appropriate to the Section type: a countdown timer for AMRAP Sections, a stopwatch timer for Strength Sections, and an interval timer (work/rest cycles) for Tabata and EMOM Sections.
8. WHILE the Rest_Timer is active, THE Workout_Coach_UI SHALL display the remaining seconds prominently and SHALL provide a "Skip Rest" button to dismiss the timer early.
9. THE Workout_Coach_UI SHALL render a Next_Up_Indicator component that displays the name of the next Exercise in the current Section, or the name of the next Section when the current Section is complete.
10. WHEN all Exercises in all Sections are complete, THE Workout_Coach_UI SHALL display a "Finish Workout" action that triggers Session completion via the Session_Service.
11. WHEN the User navigates to the Theater_Mode_Route and an incomplete Session exists for the given sessionId, THE Workout_Coach_UI SHALL restore the Session to the last persisted state, including the current Section index and Exercise completion status.
12. THE Workout_Coach_UI SHALL display a "Leave Workout" action within Theater_Mode; WHEN activated, THE Workout_Coach_UI SHALL prompt the User to confirm before navigating away, and SHALL persist the current Session state before exiting.
13. THE Workout_Coach_UI SHALL connect to the Session_Service WebSocket endpoint and update the UI in real time when Session state changes are pushed from the server.

---

### Requirement 5: Service Integration and Data Ownership

**User Story:** As a platform operator, I want the Session Service to own its data and integrate cleanly with other services, so that it can be deployed and scaled independently.

#### Acceptance Criteria

1. THE Session_Service SHALL retrieve Workout and Program definitions by calling the Workout_Creator_Service API at `/api/v1/vault/programs` and `/api/v1/vault/programs/{id}`; THE Session_Service SHALL NOT query the Workout data store directly.
2. THE Session_Service SHALL own and manage the Workout session data store exclusively; no other service SHALL read from or write directly to it.
3. WHEN a SessionCompleted event cannot be delivered to RabbitMQ, THE Session_Service SHALL retry delivery using an exponential backoff strategy with a maximum of 5 attempts before logging the failure for manual intervention.
4. THE Session_Service SHALL manage its PostgreSQL schema using Flyway versioned migration scripts in the range V200–V299.
5. WHEN the Session_Service starts, it SHALL apply any pending Flyway migrations before accepting traffic.
6. IF a Flyway migration fails on startup, THEN THE Session_Service SHALL halt startup and SHALL log the migration error with sufficient detail to identify the failing script.

---
