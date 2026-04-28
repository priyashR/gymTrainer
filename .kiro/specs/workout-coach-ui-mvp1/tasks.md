# Implementation Plan: Workout Coach UI MVP1

## Overview

Implement the first frontend deliverable for HybridStrength: a React 18 SPA with Vite, TypeScript, and React Router v6 that provides a working authentication flow (register, login, logout, silent token refresh) connected to the Auth Service, plus a stubbed home screen with placeholder action cards. Implementation follows a bottom-up dependency order: scaffolding → types → API layer → auth state → UI components → routing → tests.

## Tasks

- [x] 1. Scaffold project and install dependencies
  - [x] 1.1 Initialize Vite + React 18 + TypeScript project in `workout-coach-ui/`
    - Run `npm create vite@latest workout-coach-ui -- --template react-ts`
    - Install dependencies: `react-router-dom`, `axios`
    - Install dev dependencies: `vitest`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `jsdom`, `fast-check`, `msw`
    - Configure `vitest` in `vite.config.ts` (jsdom environment)
    - Create the directory structure: `src/pages/auth/`, `src/components/ui/`, `src/components/layout/`, `src/features/auth/`, `src/hooks/`, `src/lib/`, `src/types/`
    - _Requirements: 2.3_

  - [x] 1.2 Configure Vite dev proxy to Auth Service
    - Set up `vite.config.ts` with proxy: `/api` → `http://localhost:8081`
    - Set `changeOrigin: true` and `secure: false`
    - _Requirements: 2.3_

- [x] 2. Define TypeScript types and API client layer
  - [x] 2.1 Create auth type definitions (`src/types/auth.ts`)
    - Define `LoginRequest`, `RegisterRequest`, `AccessTokenResponse`, `RegisterResponse`
    - Define `ApiErrorResponse`, `FieldError`, `ValidationErrorResponse`
    - Define `AuthState` interface
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 2.2 Implement Axios API client with interceptors (`src/lib/apiClient.ts`)
    - Create Axios instance with `baseURL: "/api/v1"`, `withCredentials: true`
    - Implement request interceptor to attach `Authorization: Bearer <token>` header
    - Implement response interceptor: on 401 (excluding `/auth/login`, `/auth/register`, `/auth/refresh`), call refresh endpoint, queue concurrent requests, retry with new token, or clear auth state on failure
    - Export a `setAccessTokenGetter` / `setOnRefreshFailure` mechanism so AuthContext can wire in its state
    - _Requirements: 1.3, 1.4_

  - [x] 2.3 Implement auth API functions (`src/lib/authApi.ts`)
    - `login(data: LoginRequest): Promise<AccessTokenResponse>` — POST `/auth/login`
    - `register(data: RegisterRequest): Promise<RegisterResponse>` — POST `/auth/register`
    - `refresh(): Promise<AccessTokenResponse>` — POST `/auth/refresh`
    - Implement `extractApiError` utility for mapping Axios errors to `{ message, fieldErrors? }`
    - _Requirements: 1.1, 1.2, 1.4_


- [x] 3. Implement auth context and protected route
  - [x] 3.1 Implement AuthContext and AuthProvider (`src/features/auth/AuthContext.tsx`)
    - Create `AuthContextValue` with `accessToken`, `isAuthenticated`, `isLoading`, `login()`, `register()`, `logout()`
    - On mount: attempt silent refresh via `POST /auth/refresh`; set `isLoading = false` when complete
    - `login()`: call login API, store access token in memory, set `isAuthenticated = true`
    - `logout()`: clear in-memory token, set `isAuthenticated = false`, redirect to `/login`
    - Wire the token getter and refresh failure callback into `apiClient`
    - Never persist the access token to localStorage or sessionStorage
    - _Requirements: 1.3, 1.4, 1.5, 2.4_

  - [x] 3.2 Create `useAuth` convenience hook (`src/features/auth/useAuth.ts`)
    - Wraps `useContext(AuthContext)` with a guard that throws if used outside `AuthProvider`
    - _Requirements: 1.3_

  - [x] 3.3 Implement ProtectedRoute component (`src/components/layout/ProtectedRoute.tsx`)
    - If `isLoading`: render a loading indicator
    - If `!isAuthenticated`: redirect to `/login` via `<Navigate to="/login" replace />`
    - If `isAuthenticated`: render `children`
    - _Requirements: 1.6_

  - [x] 3.4 Write property test: Registration client-side validation rejects invalid inputs
    - **Property 1: Registration client-side validation rejects invalid inputs**
    - Test file: `src/features/auth/__tests__/validation.property.test.ts`
    - Use `fast-check` to generate arbitrary strings; verify that invalid emails and passwords < 8 chars produce validation errors, and valid inputs produce none
    - Minimum 100 iterations
    - **Validates: Requirements 1.1**

  - [ ]* 3.5 Write property test: Successful login stores token and sets authenticated state
    - **Property 2: Successful login stores token and sets authenticated state**
    - Test file: `src/features/auth/__tests__/authState.property.test.ts`
    - Use `fast-check` to generate arbitrary `AccessTokenResponse` values; verify that after login, `isAuthenticated === true` and `accessToken` matches the response
    - Minimum 100 iterations
    - **Validates: Requirements 1.3**

  - [ ]* 3.6 Write property test: Logout clears auth state
    - **Property 4: Logout clears auth state**
    - Test file: `src/features/auth/__tests__/authState.property.test.ts`
    - Use `fast-check` to generate arbitrary auth states where `isAuthenticated === true`; verify that after `logout()`, `accessToken` is `null` and `isAuthenticated` is `false`
    - Minimum 100 iterations
    - **Validates: Requirements 1.5**

- [ ] 4. Checkpoint — Core auth logic
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement page components
  - [ ] 5.1 Implement Login page (`src/pages/auth/Login.tsx`)
    - Email and password fields with client-side validation (email format, password non-empty)
    - Submit via `useAuth().login()`
    - Display inline field errors and error banner for server errors (invalid credentials, network errors)
    - Link to registration page
    - On success: redirect to `/`
    - If already authenticated: redirect to `/`
    - _Requirements: 1.2, 1.3_

  - [ ] 5.2 Implement Registration page (`src/pages/auth/Register.tsx`)
    - Email and password fields with client-side validation (email format, password ≥ 8 characters)
    - Submit via `useAuth().register()`
    - Display inline field errors and error banner for server errors (duplicate email, validation errors)
    - Link to login page
    - On success: redirect to `/login` with a success message
    - If already authenticated: redirect to `/`
    - _Requirements: 1.1_

  - [ ] 5.3 Implement Home page (`src/pages/Home.tsx`)
    - Display three action cards: "New Workout", "My Performance", "Workout"
    - Each card is a `<Link>` to `/new-workout`, `/my-performance`, `/workout` respectively
    - Show user email and a logout button
    - _Requirements: 2.1, 2.2_

  - [ ] 5.4 Implement ComingSoon stub page (`src/pages/ComingSoon.tsx`)
    - Accept a `title` prop or read from route params/state
    - Display "Coming Soon" message and a link back to home
    - _Requirements: 2.2_

  - [ ]* 5.5 Write property test: Protected routes redirect unauthenticated users
    - **Property 5: Protected routes redirect unauthenticated users**
    - Test file: `src/components/layout/__tests__/protectedRoute.property.test.ts`
    - Use `fast-check` to generate arbitrary route paths (excluding `/login` and `/register`); verify that `ProtectedRoute` redirects to `/login` when `isAuthenticated === false` and `isLoading === false`
    - Minimum 100 iterations
    - **Validates: Requirements 1.6**

  - [ ]* 5.6 Write property test: Auth state persists across navigation
    - **Property 6: Auth state persists across navigation**
    - Test file: `src/features/auth/__tests__/authState.property.test.ts`
    - Use `fast-check` to generate pairs of protected route paths; verify that navigating between them does not change `isAuthenticated` or `accessToken`
    - Minimum 100 iterations
    - **Validates: Requirements 2.4**

- [ ] 6. Wire up routing in App.tsx
  - [ ] 6.1 Implement `src/main.tsx` with `BrowserRouter` and `ReactDOM.createRoot`
    - _Requirements: 2.3_

  - [ ] 6.2 Implement `src/App.tsx` with route definitions
    - Wrap all routes in `AuthProvider`
    - Public routes: `/login` → `Login`, `/register` → `Register` (redirect to `/` if authenticated)
    - Protected routes (wrapped in `ProtectedRoute`): `/` → `Home`, `/new-workout` → `ComingSoon`, `/my-performance` → `ComingSoon`, `/workout` → `ComingSoon`
    - _Requirements: 1.6, 2.1, 2.2, 2.3_

- [ ] 7. Checkpoint — Full UI wired
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Implement interceptor property test and unit tests
  - [ ]* 8.1 Write property test: 401 interceptor triggers refresh and retries
    - **Property 3: 401 interceptor triggers refresh and retries the original request**
    - Test file: `src/lib/__tests__/interceptor.property.test.ts`
    - Use `fast-check` to generate arbitrary protected endpoint paths (excluding auth endpoints); verify that a 401 response triggers a refresh call and the original request is retried with the new token
    - Minimum 100 iterations
    - **Validates: Requirements 1.4**

  - [ ]* 8.2 Write unit tests for Login page
    - Test successful submission and redirect
    - Test display of server error (invalid credentials)
    - Test client-side validation (empty fields)
    - _Requirements: 1.2_

  - [ ]* 8.3 Write unit tests for Registration page
    - Test successful submission and redirect to `/login`
    - Test display of server error (duplicate email)
    - Test client-side validation (invalid email, short password)
    - _Requirements: 1.1_

  - [ ]* 8.4 Write unit tests for Home page
    - Verify three action cards render with correct labels and links
    - Verify logout button calls `logout()`
    - _Requirements: 2.1, 2.2_

  - [ ]* 8.5 Write unit tests for ComingSoon page
    - Verify "Coming Soon" message renders
    - Verify back/home link is present
    - _Requirements: 2.2_

  - [ ]* 8.6 Write unit tests for AuthContext
    - Test login sets token and `isAuthenticated`
    - Test logout clears token
    - Test silent refresh on mount (success and failure paths)
    - _Requirements: 1.3, 1.4, 1.5_

  - [ ]* 8.7 Write unit tests for `extractApiError` utility
    - Test parsing `ApiErrorResponse` shape
    - Test parsing `ValidationErrorResponse` shape
    - Test handling network errors (no response)
    - _Requirements: 1.1, 1.2_

- [ ] 9. Final checkpoint — All tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document (Properties 1–6)
- Unit tests validate specific examples and edge cases
- E2E tests (Playwright) are not included in this task list — they require a running Auth Service and are better suited for a follow-up task once the UI is functional
- The access token is never persisted to localStorage or sessionStorage (security requirement)
- `withCredentials: true` is critical on the Axios instance for HttpOnly cookie handling
