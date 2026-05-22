import apiClient from "./apiClient";
import type {
  AdvanceSectionRequest,
  CompleteExerciseRequest,
  EnrollmentResponse,
  EnrollRequest,
  SessionResponse,
  StartSessionRequest,
} from "../types/session";

// --- Session Endpoints (/api/v1/sessions) ---

/**
 * Start a new workout session.
 */
export function startSession(
  request: StartSessionRequest
): Promise<SessionResponse> {
  return apiClient
    .post<SessionResponse>("/sessions", request)
    .then((res) => res.data);
}

/**
 * Get session state by ID.
 */
export function getSession(sessionId: string): Promise<SessionResponse> {
  return apiClient
    .get<SessionResponse>(`/sessions/${sessionId}`)
    .then((res) => res.data);
}

/**
 * Get the user's currently active session, or null if none exists.
 */
export function getActiveSession(): Promise<SessionResponse | null> {
  return apiClient
    .get<SessionResponse>("/sessions/active", {
      validateStatus: (status) => status === 200 || status === 204,
    })
    .then((res) => (res.status === 204 ? null : res.data));
}

/**
 * Mark an exercise as complete within a session.
 */
export function completeExercise(
  sessionId: string,
  request: CompleteExerciseRequest
): Promise<SessionResponse> {
  return apiClient
    .patch<SessionResponse>(`/sessions/${sessionId}/exercises`, request)
    .then((res) => res.data);
}

/**
 * Navigate to a different section within a session.
 */
export function advanceSection(
  sessionId: string,
  request: AdvanceSectionRequest
): Promise<SessionResponse> {
  return apiClient
    .patch<SessionResponse>(`/sessions/${sessionId}/section`, request)
    .then((res) => res.data);
}

/**
 * Pause the active session.
 */
export function pauseSession(sessionId: string): Promise<SessionResponse> {
  return apiClient
    .post<SessionResponse>(`/sessions/${sessionId}/pause`)
    .then((res) => res.data);
}

/**
 * Resume a paused session.
 */
export function resumeSession(sessionId: string): Promise<SessionResponse> {
  return apiClient
    .post<SessionResponse>(`/sessions/${sessionId}/resume`)
    .then((res) => res.data);
}

/**
 * End/complete a session with whatever progress has been logged.
 */
export function endSession(sessionId: string): Promise<SessionResponse> {
  return apiClient
    .post<SessionResponse>(`/sessions/${sessionId}/end`)
    .then((res) => res.data);
}

// --- Enrollment Endpoints (/api/v1/enrollments) ---

/**
 * Enroll in a program (replaces any existing active enrollment).
 */
export function enrollProgram(
  request: EnrollRequest
): Promise<EnrollmentResponse> {
  return apiClient
    .post<EnrollmentResponse>("/enrollments", request)
    .then((res) => res.data);
}

/**
 * Get the user's active program enrollment, or null if none exists.
 */
export function getActiveEnrollment(): Promise<EnrollmentResponse | null> {
  return apiClient
    .get<EnrollmentResponse>("/enrollments/active", {
      validateStatus: (status) => status === 200 || status === 204,
    })
    .then((res) => (res.status === 204 ? null : res.data));
}

