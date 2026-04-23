# Requirements Document — Workout Coach UI

## Introduction

The Workout Coach UI is the single frontend application serving all user-facing views for the HybridStrength platform. It is not a Single Page Application and must not use React, Angular, or Vue SPA patterns. The frontend approach is to be decided.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Program**: A structured collection of Workouts spanning one or more weeks
- **Theater Mode**: The distraction-free active workout execution UI
- **Workout_Coach_UI**: The single frontend application serving all user-facing views
- **JWT**: JSON Web Token — a signed token used to authenticate requests to protected endpoints

---

## Requirements

### Requirement 1: Navigation and Home Screen

**User Story:** As a User, I want a clear home screen with quick access to key actions, so that I can start training without friction.

#### Acceptance Criteria

1. WHEN a User logs in, THE Workout_Coach_UI SHALL display the home screen with the following primary actions: "New Workout", "My Performance", and "Workout" (resume or start).
2. WHEN an active Program exists for the User, THE Workout_Coach_UI SHALL display the next scheduled Program day as the "Next Step" indicator on the home screen, showing the Program name, current week number, day number, and Workout name alongside a "Start" action.
3. THE Workout_Coach_UI SHALL be implemented using React 18 with Next.js (App Router, SSR-first); it SHALL NOT be implemented as a client-side Single Page Application where all routing and rendering occurs in the browser.
4. WHEN a User navigates between views, THE Workout_Coach_UI SHALL maintain authentication state without requiring re-login for the duration of the JWT validity period.
