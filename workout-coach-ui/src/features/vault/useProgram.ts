import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import type { VaultProgramDetail, VaultItem } from '../../types/vault';
import type { FieldError } from '../../types/auth';
import { getProgram, updateProgram, deleteProgram, copyProgram } from '../../lib/vaultApi';

// ---------------------------------------------------------------------------
// State types
// ---------------------------------------------------------------------------

export type ProgramState =
  | { status: 'loading' }
  | { status: 'loaded'; program: VaultProgramDetail }
  | { status: 'forbidden' }
  | { status: 'error'; message: string }
  | { status: 'updating' }
  | { status: 'deleting' }
  | { status: 'copying' };

export interface UseProgramReturn {
  state: ProgramState;
  program: VaultProgramDetail | null;
  /** Reload the program detail from the API. */
  reload: () => void;
  /** Update the program with new JSON content. Returns validation errors on 400. */
  onUpdate: (json: string) => Promise<{ success: boolean; errors?: FieldError[] | string }>;
  /** Delete the program. Navigates to search on success. */
  onDelete: () => Promise<void>;
  /** Copy the program. Navigates to the new copy's detail page on success. */
  onCopy: () => Promise<void>;
}

// ---------------------------------------------------------------------------
// Hook
// ---------------------------------------------------------------------------

/**
 * Fetches program detail and provides update/delete/copy operations.
 * Handles 403 and 401 responses per requirements.
 *
 * Requirements: 9.2, 9.5, 9.6
 */
export function useProgram(programId: string): UseProgramReturn {
  const navigate = useNavigate();
  const [state, setState] = useState<ProgramState>({ status: 'loading' });
  const [program, setProgram] = useState<VaultProgramDetail | null>(null);

  // -------------------------------------------------------------------------
  // Fetch program detail
  // -------------------------------------------------------------------------
  const fetchProgram = useCallback(async () => {
    setState({ status: 'loading' });
    try {
      const data = await getProgram(programId);
      setProgram(data);
      setState({ status: 'loaded', program: data });
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        if (err.response?.status === 403) {
          setState({ status: 'forbidden' });
          return;
        }
        setState({ status: 'error', message: err.response?.data?.message ?? 'Failed to load program.' });
      } else {
        setState({ status: 'error', message: 'Network error. Please check your connection.' });
      }
    }
  }, [programId, navigate]);

  useEffect(() => {
    fetchProgram();
  }, [fetchProgram]);

  // -------------------------------------------------------------------------
  // Update program
  // -------------------------------------------------------------------------
  const onUpdate = useCallback(
    async (json: string): Promise<{ success: boolean; errors?: FieldError[] | string }> => {
      setState({ status: 'updating' });
      try {
        await updateProgram(programId, json);
        // Reload the program detail after successful update
        await fetchProgram();
        return { success: true };
      } catch (err: unknown) {
        if (axios.isAxiosError(err)) {
          if (err.response?.status === 401) {
            navigate('/login', { replace: true });
            return { success: false };
          }
          if (err.response?.status === 400) {
            const data = err.response.data;
            // Restore loaded state so user can continue editing
            if (program) {
              setState({ status: 'loaded', program });
            }
            if (data?.errors && Array.isArray(data.errors)) {
              return { success: false, errors: data.errors as FieldError[] };
            }
            return { success: false, errors: data?.message ?? 'Validation failed.' };
          }
          if (program) {
            setState({ status: 'loaded', program });
          }
          return { success: false, errors: err.response?.data?.message ?? 'Update failed.' };
        }
        if (program) {
          setState({ status: 'loaded', program });
        }
        return { success: false, errors: 'Network error. Please check your connection.' };
      }
    },
    [programId, navigate, fetchProgram, program]
  );

  // -------------------------------------------------------------------------
  // Delete program
  // -------------------------------------------------------------------------
  const onDelete = useCallback(async () => {
    setState({ status: 'deleting' });
    try {
      await deleteProgram(programId);
      navigate('/vault/search', { replace: true });
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
      }
      // Restore loaded state on failure
      if (program) {
        setState({ status: 'loaded', program });
      }
    }
  }, [programId, navigate, program]);

  // -------------------------------------------------------------------------
  // Copy program
  // -------------------------------------------------------------------------
  const onCopy = useCallback(async () => {
    setState({ status: 'copying' });
    try {
      const copy: VaultItem = await copyProgram(programId);
      navigate(`/vault/programs/${copy.id}`);
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
      }
      // Restore loaded state on failure
      if (program) {
        setState({ status: 'loaded', program });
      }
    }
  }, [programId, navigate, program]);

  return { state, program, reload: fetchProgram, onUpdate, onDelete, onCopy };
}
