import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { createElement } from 'react';
import type { ReactNode } from 'react';
import axios from 'axios';
import { useUpload } from '../useUpload';

// Requirements: 7.8, 7.9, 7.10, 7.11, 7.12

vi.mock('../uploadApi', () => ({
  uploadProgram: vi.fn(),
}));

import { uploadProgram } from '../uploadApi';

const mockedNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

function wrapper({ children }: { children: ReactNode }) {
  return createElement(MemoryRouter, null, children);
}

// Helper: create a valid program JSON string
const validProgramJson = JSON.stringify({
  program_metadata: {
    program_name: 'Test Program',
    duration_weeks: 1,
    goal: 'Hypertrophy',
    equipment_profile: ['Barbell', 'Dumbbells'],
    version: '1.0',
  },
  program_structure: [
    {
      week_number: 1,
      days: [
        {
          day_number: 1,
          day_label: 'Monday',
          focus_area: 'Push',
          modality: 'Hypertrophy',
          warm_up: [{ movement: 'Arm circles', instruction: '30 seconds' }],
          blocks: [
            {
              block_type: 'Tier 1: Compound',
              format: 'Sets/Reps',
              movements: [
                {
                  exercise_name: 'Bench Press',
                  prescribed_sets: 4,
                  prescribed_reps: '8-10',
                  prescribed_weight: '75% 1RM',
                },
              ],
            },
          ],
          cool_down: [{ movement: 'Stretching', instruction: '5 minutes' }],
        },
      ],
    },
  ],
});

// Helper: create a mock File with given content
function createJsonFile(content: string, name = 'program.json'): File {
  return new File([content], name, { type: 'application/json' });
}

// Helper: create an AxiosError-like object
function createAxiosError(status: number, data: unknown) {
  const error = new Error('Request failed') as Error & {
    isAxiosError: boolean;
    response: { status: number; data: unknown; statusText: string; headers: object; config: object };
    config: object;
    toJSON: () => object;
  };
  error.isAxiosError = true;
  error.response = {
    status,
    data,
    statusText: status === 400 ? 'Bad Request' : 'Unauthorized',
    headers: {},
    config: {},
  };
  error.config = {};
  error.toJSON = () => ({});
  return error;
}

describe('useUpload', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('initial state', () => {
    it('should start in idle state', () => {
      const { result } = renderHook(() => useUpload(), { wrapper });
      expect(result.current.state).toEqual({ status: 'idle' });
    });
  });

  describe('file selection and parsing', () => {
    it('should transition to previewing state when a valid JSON file is selected', async () => {
      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      if (result.current.state.status === 'previewing') {
        expect(result.current.state.program.program_metadata.program_name).toBe('Test Program');
        expect(result.current.state.rawJson).toBe(validProgramJson);
      }
    });

    it('should transition to editing state with parseError when file contains invalid JSON', async () => {
      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile('{ invalid json }');

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('editing');
      });

      if (result.current.state.status === 'editing') {
        expect(result.current.state.parseError).toBeDefined();
        expect(result.current.state.rawJson).toBe('{ invalid json }');
      }
    });
  });

  describe('Property 8: button disabled during upload', () => {
    it('should transition to uploading state when onSave is called from previewing', async () => {
      vi.mocked(uploadProgram).mockImplementation(
        () => new Promise(() => {}) // Never resolves — simulates in-flight request
      );

      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onSave();
      });

      // State should be 'uploading' — the UI uses this to disable the button
      expect(result.current.state.status).toBe('uploading');
      if (result.current.state.status === 'uploading') {
        expect(result.current.state.rawJson).toBe(validProgramJson);
      }
    });
  });

  describe('Requirement 7.9: success state on 201 response', () => {
    it('should transition to success state with programName and programId on successful upload', async () => {
      vi.mocked(uploadProgram).mockResolvedValue({
        id: 'prog-123',
        programName: 'Test Program',
        durationWeeks: 1,
        goal: 'Hypertrophy',
        equipmentProfile: ['Barbell', 'Dumbbells'],
        contentSource: 'UPLOADED',
        createdAt: '2026-05-05T10:00:00Z',
      });

      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onSave();
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('success');
      });

      if (result.current.state.status === 'success') {
        expect(result.current.state.programName).toBe('Test Program');
        expect(result.current.state.programId).toBe('prog-123');
      }
    });
  });

  describe('Requirement 7.10: error state on 400 with errors array', () => {
    it('should transition to error state with field errors on 400 response with errors array', async () => {
      const fieldErrors = [
        { field: 'program_metadata.duration_weeks', message: 'must be 1 or 4' },
        { field: 'program_structure', message: 'number of weeks does not match duration_weeks' },
      ];

      vi.mocked(uploadProgram).mockRejectedValue(
        createAxiosError(400, { errors: fieldErrors })
      );

      vi.spyOn(axios, 'isAxiosError').mockReturnValue(true);

      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onSave();
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('error');
      });

      if (result.current.state.status === 'error') {
        expect(Array.isArray(result.current.state.errors)).toBe(true);
        expect(result.current.state.errors).toEqual(fieldErrors);
      }
    });
  });

  describe('Requirement 7.11: error state on 400 with single message', () => {
    it('should transition to error state with string message on 400 response with message', async () => {
      vi.mocked(uploadProgram).mockRejectedValue(
        createAxiosError(400, { message: 'File size exceeds the maximum allowed limit of 1 MB' })
      );

      vi.spyOn(axios, 'isAxiosError').mockReturnValue(true);

      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onSave();
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('error');
      });

      if (result.current.state.status === 'error') {
        expect(result.current.state.errors).toBe('File size exceeds the maximum allowed limit of 1 MB');
      }
    });
  });

  describe('Requirement 7.12: redirect on 401', () => {
    it('should navigate to /login on 401 response', async () => {
      vi.mocked(uploadProgram).mockRejectedValue(
        createAxiosError(401, { message: 'Unauthorised' })
      );

      vi.spyOn(axios, 'isAxiosError').mockReturnValue(true);

      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onSave();
      });

      await waitFor(() => {
        expect(mockedNavigate).toHaveBeenCalledWith('/login', { replace: true });
      });
    });
  });

  describe('network error handling', () => {
    it('should transition to error state with network error message on non-Axios error', async () => {
      vi.mocked(uploadProgram).mockRejectedValue(new Error('Network Error'));
      vi.spyOn(axios, 'isAxiosError').mockReturnValue(false);

      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onSave();
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('error');
      });

      if (result.current.state.status === 'error') {
        expect(result.current.state.errors).toBe(
          'Network error. Please check your connection and try again.'
        );
      }
    });
  });

  describe('edit and preview transitions', () => {
    it('should transition from previewing to editing when onEditJson is called', async () => {
      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onEditJson();
      });

      expect(result.current.state.status).toBe('editing');
      if (result.current.state.status === 'editing') {
        expect(result.current.state.rawJson).toBe(validProgramJson);
        expect(result.current.state.parseError).toBeUndefined();
      }
    });

    it('should transition back to previewing when onPreview is called with valid JSON', async () => {
      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onEditJson();
      });

      act(() => {
        result.current.onPreview(validProgramJson);
      });

      expect(result.current.state.status).toBe('previewing');
    });

    it('should stay in editing state with parseError when onPreview is called with invalid JSON', async () => {
      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onEditJson();
      });

      act(() => {
        result.current.onPreview('{ broken json');
      });

      expect(result.current.state.status).toBe('editing');
      if (result.current.state.status === 'editing') {
        expect(result.current.state.parseError).toBeDefined();
        expect(result.current.state.rawJson).toBe('{ broken json');
      }
    });
  });

  describe('reset', () => {
    it('should return to idle state when onReset is called', async () => {
      const { result } = renderHook(() => useUpload(), { wrapper });
      const file = createJsonFile(validProgramJson);

      act(() => {
        result.current.onFileSelected(file);
      });

      await waitFor(() => {
        expect(result.current.state.status).toBe('previewing');
      });

      act(() => {
        result.current.onReset();
      });

      expect(result.current.state).toEqual({ status: 'idle' });
    });
  });
});
