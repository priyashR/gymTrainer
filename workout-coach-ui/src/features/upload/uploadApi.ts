import apiClient from '../../lib/apiClient';
import type { UploadProgramResponse, ValidateUploadResponse } from '../../types/upload';

/**
 * Validates a program JSON string against the Upload_Schema without persisting anything.
 *
 * POST /api/v1/uploads/programs/validate
 *
 * Returns { valid: true } when the JSON passes all schema rules, or
 * { valid: false, errors: [...] } with field-level errors otherwise.
 * Nothing is written to the Vault regardless of the result.
 */
export function validateProgram(json: string): Promise<ValidateUploadResponse> {
  return apiClient
    .post<ValidateUploadResponse>('/uploads/programs/validate', json, {
      headers: { 'Content-Type': 'application/json' },
    })
    .then((res) => res.data);
}

/**
 * Uploads a program JSON string to the authenticated user's Vault.
 *
 * POST /api/v1/uploads/programs
 *
 * On success (201 Created) returns the persisted program metadata.
 * The caller is responsible for handling error responses — the shared
 * apiClient interceptor will automatically attempt a token refresh on 401
 * and call onRefreshFailure (which redirects to /login) if the refresh fails.
 */
export function uploadProgram(json: string): Promise<UploadProgramResponse> {
  return apiClient
    .post<UploadProgramResponse>('/uploads/programs', json, {
      headers: { 'Content-Type': 'application/json' },
    })
    .then((res) => res.data);
}
