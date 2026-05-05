import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import type { ParsedProgram, UploadProgramResponse } from '../../types/upload';
import type { FieldError } from '../../types/auth';
import { uploadProgram } from './uploadApi';

// ---------------------------------------------------------------------------
// State machine types
// ---------------------------------------------------------------------------

export type UploadState =
  | { status: 'idle' }
  | { status: 'file_selected'; file: File }
  | { status: 'previewing'; program: ParsedProgram; rawJson: string }
  | { status: 'editing'; rawJson: string; parseError?: string }
  | { status: 'uploading'; rawJson: string }
  | { status: 'success'; programName: string; programId: string }
  | { status: 'error'; errors: FieldError[] | string };

// ---------------------------------------------------------------------------
// Hook
// ---------------------------------------------------------------------------

export interface UseUploadReturn {
  state: UploadState;
  /** Called when the user selects a file via the FilePicker. */
  onFileSelected: (file: File) => void;
  /** Called when the user clicks "Edit JSON" from the preview. */
  onEditJson: () => void;
  /** Called when the user clicks "Preview" from the JSON editor. */
  onPreview: (rawJson: string) => void;
  /** Called when the user clicks "Save to Vault". */
  onSave: () => void;
  /** Called when the user wants to start over. */
  onReset: () => void;
}

/**
 * Manages the upload feature state machine:
 *   idle → file_selected → previewing → editing → uploading → success | error
 *
 * The 401 case is handled transparently by the apiClient interceptor, which
 * calls onRefreshFailure → navigate('/login'). The hook only needs to handle
 * 400 and network errors explicitly.
 */
export function useUpload(): UseUploadReturn {
  const [state, setState] = useState<UploadState>({ status: 'idle' });
  const navigate = useNavigate();

  // -------------------------------------------------------------------------
  // File selected — parse client-side and move to previewing
  // -------------------------------------------------------------------------
  const onFileSelected = useCallback((file: File) => {
    setState({ status: 'file_selected', file });

    const reader = new FileReader();
    reader.onload = (e) => {
      const rawJson = e.target?.result as string;
      try {
        const program = JSON.parse(rawJson) as ParsedProgram;
        setState({ status: 'previewing', program, rawJson });
      } catch {
        // Invalid JSON — drop straight into editor with an inline error
        setState({
          status: 'editing',
          rawJson,
          parseError: 'The selected file is not valid JSON. Please fix the content below.',
        });
      }
    };
    reader.onerror = () => {
      setState({
        status: 'error',
        errors: 'Failed to read the selected file. Please try again.',
      });
    };
    reader.readAsText(file);
  }, []);

  // -------------------------------------------------------------------------
  // Edit JSON — transition from previewing to editing
  // -------------------------------------------------------------------------
  const onEditJson = useCallback(() => {
    setState((prev) => {
      if (prev.status === 'previewing') {
        return { status: 'editing', rawJson: prev.rawJson };
      }
      return prev;
    });
  }, []);

  // -------------------------------------------------------------------------
  // Preview — re-parse edited JSON and refresh the structured preview
  // -------------------------------------------------------------------------
  const onPreview = useCallback((rawJson: string) => {
    try {
      const program = JSON.parse(rawJson) as ParsedProgram;
      setState({ status: 'previewing', program, rawJson });
    } catch {
      // Keep editor open with inline error — do NOT clear content
      setState({ status: 'editing', rawJson, parseError: 'Invalid JSON — please fix the syntax and try again.' });
    }
  }, []);

  // -------------------------------------------------------------------------
  // Save — submit to backend
  // -------------------------------------------------------------------------
  const onSave = useCallback(() => {
    setState((prev) => {
      if (prev.status !== 'previewing' && prev.status !== 'editing') return prev;
      const rawJson = prev.rawJson;

      // Kick off the async upload outside of setState
      (async () => {
        try {
          const response: UploadProgramResponse = await uploadProgram(rawJson);
          setState({
            status: 'success',
            programName: response.programName,
            programId: response.id,
          });
        } catch (err: unknown) {
          if (axios.isAxiosError(err)) {
            const status = err.response?.status;

            // 401 — the apiClient interceptor already attempted a refresh.
            // If we reach here the refresh also failed and onRefreshFailure
            // has been called (which navigates to /login). Nothing more to do.
            if (status === 401) {
              navigate('/login', { replace: true });
              return;
            }

            const data = err.response?.data;

            // 400 with field-level errors array
            if (status === 400 && data?.errors && Array.isArray(data.errors)) {
              setState({
                status: 'error',
                errors: data.errors as FieldError[],
              });
              return;
            }

            // 400 with a single message string
            if (status === 400 && data?.message) {
              setState({ status: 'error', errors: data.message as string });
              return;
            }

            // Any other HTTP error
            setState({
              status: 'error',
              errors: data?.message ?? 'An unexpected error occurred. Please try again.',
            });
          } else {
            // Network / timeout error
            setState({
              status: 'error',
              errors: 'Network error. Please check your connection and try again.',
            });
          }
        }
      })();

      return { status: 'uploading', rawJson };
    });
  }, [navigate]);

  // -------------------------------------------------------------------------
  // Reset — return to idle
  // -------------------------------------------------------------------------
  const onReset = useCallback(() => {
    setState({ status: 'idle' });
  }, []);

  return { state, onFileSelected, onEditJson, onPreview, onSave, onReset };
}
