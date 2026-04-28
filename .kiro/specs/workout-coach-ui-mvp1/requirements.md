# Requirements Document — Workout Coach UI MVP1

## Introduction

The Workout Coach UI MVP1 is the first deliverable slice of the HybridStrength frontend. It delivers a working authentication flow (registration, login, logout, token refresh) connected to the Auth Service, and a stubbed home screen that establishes the application shell and routing structure. All non-auth features on the home screen are placeholder stubs with no backend integration.

It is a React 18 Single Page Application built with Vite, React Router v6, and TypeScript. All rendering is client-side.

---

## Glossary

- **User**: An authenticated individual using the HybridStrength platform
- **Workout_Coach_UI**: The single frontend application serving all user-facing views
- **Auth_Service**: The microservice responsible for User registration, authentication, and authorisation
- **JWT**: JSON Web Token — a signed token used to authenticate requests to protected endpoints

---

## Requirements

### Requirement 1: Authentication Views

**User Story:** As a visitor, I want to register and log in through the UI, so that I can access my personal training data securely.

#### Acceptance Criteria

1. THE Workout_Coach_UI SHALL provide a registration view that collects an email address and password, validates inputs client-side, and submits the request to the Auth_Service registration endpoint.
2. THE Workout_Coach_UI SHALL provide a login view that collects an email and password and submits the request to the Auth_Service login endpoint.
3. WHEN login succeeds, THE Workout_Coach_UI SHALL store the JWT access token in memory and SHALL rely on the HttpOnly refresh cookie set by the Auth_Service for token renewal.
4. WHEN a JWT access token expires, THE Workout_Coach_UI SHALL automatically request a new access token using the refresh cookie before retrying the failed request.
5. WHEN a User clicks "Log out", THE Workout_Coach_UI SHALL clear the in-memory access token, request refresh token invalidation from the Auth_Service, and redirect to the login view.
6. THE Workout_Coach_UI SHALL protect all routes except login and registration behind an authentication guard that redirects unauthenticated Users to the login view.

---

### Requirement 2: Stubbed Home Screen

**User Story:** As a User, I want to see a home screen after logging in with placeholders for key actions, so that the application shell is ready for future features.

#### Acceptance Criteria

1. WHksume or start).
2. EACH primary action on the home screen SHALL be a visible, clickable element that navigates to a stub page displaying a "Coming Soon" message.
3. THE Workout_Coach_UI SHALL be implemented as a React 18 Single Page Application using Vite as the build tool and React Router v6 for client-side routing; all components SHALL be client-rendered.
4. WHEN a User navigates between views, THE Workout_Coach_UI SHALL maintain authentication state without requiring re-login for the duration of the JWT validity period.
