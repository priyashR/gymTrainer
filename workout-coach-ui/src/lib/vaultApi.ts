import apiClient from "./apiClient";
import type {
  PaginatedResponse,
  VaultItem,
  VaultProgramDetail,
} from "../types/vault";

/**
 * List all programs in the authenticated user's vault (paginated).
 */
export function listPrograms(
  page = 0,
  size = 20
): Promise<PaginatedResponse<VaultItem>> {
  return apiClient
    .get<PaginatedResponse<VaultItem>>("/vault/programs", {
      params: { page, size },
    })
    .then((res) => res.data);
}

/**
 * Get full program detail by ID.
 */
export function getProgram(id: string): Promise<VaultProgramDetail> {
  return apiClient
    .get<VaultProgramDetail>(`/vault/programs/${id}`)
    .then((res) => res.data);
}

/**
 * Update a program with full JSON replacement (Upload_Schema format).
 */
export function updateProgram(id: string, json: string): Promise<VaultItem> {
  return apiClient
    .put<VaultItem>(`/vault/programs/${id}`, json, {
      headers: { "Content-Type": "application/json" },
    })
    .then((res) => res.data);
}

/**
 * Delete a program from the vault.
 */
export function deleteProgram(id: string): Promise<void> {
  return apiClient.delete(`/vault/programs/${id}`).then(() => undefined);
}

/**
 * Copy/duplicate a program in the vault.
 */
export function copyProgram(id: string): Promise<VaultItem> {
  return apiClient
    .post<VaultItem>(`/vault/programs/${id}/copy`)
    .then((res) => res.data);
}

/**
 * Search programs in the vault with optional filters.
 */
export function searchPrograms(
  q?: string,
  focusArea?: string,
  modality?: string,
  page = 0,
  size = 20
): Promise<PaginatedResponse<VaultItem>> {
  return apiClient
    .get<PaginatedResponse<VaultItem>>("/vault/programs/search", {
      params: {
        ...(q ? { q } : {}),
        ...(focusArea ? { focusArea } : {}),
        ...(modality ? { modality } : {}),
        page,
        size,
      },
    })
    .then((res) => res.data);
}
