import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import type { PaginatedResponse, VaultItem } from '../../types/vault';
import { searchPrograms } from '../../lib/vaultApi';

// ---------------------------------------------------------------------------
// State types
// ---------------------------------------------------------------------------

export interface VaultSearchState {
  query: string;
  focusArea: string; // '' means "All"
  modality: string;  // '' means "All"
  results: VaultItem[];
  totalElements: number;
  loading: boolean;
  error: string | null;
  searched: boolean; // whether at least one search has been executed
}

export interface UseVaultSearchReturn {
  state: VaultSearchState;
  setQuery: (query: string) => void;
  setFocusArea: (focusArea: string) => void;
  setModality: (modality: string) => void;
  executeSearch: () => void;
}

// ---------------------------------------------------------------------------
// Hook
// ---------------------------------------------------------------------------

/**
 * Manages vault search state with 300ms debounce on query input and
 * immediate re-execution on filter changes.
 *
 * Requirements: 8.7, 8.10
 */
export function useVaultSearch(): UseVaultSearchReturn {
  const navigate = useNavigate();

  const [state, setState] = useState<VaultSearchState>({
    query: '',
    focusArea: '',
    modality: '',
    results: [],
    totalElements: 0,
    loading: false,
    error: null,
    searched: false,
  });

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const latestRef = useRef(state);
  latestRef.current = state;

  // -------------------------------------------------------------------------
  // Core search execution
  // -------------------------------------------------------------------------
  const doSearch = useCallback(
    async (query: string, focusArea: string, modality: string) => {
      // If no query and no filters, don't search
      if (!query.trim() && !focusArea && !modality) {
        setState((prev) => ({
          ...prev,
          results: [],
          totalElements: 0,
          loading: false,
          error: null,
          searched: false,
        }));
        return;
      }

      setState((prev) => ({ ...prev, loading: true, error: null }));

      try {
        const response: PaginatedResponse<VaultItem> = await searchPrograms(
          query.trim() || undefined,
          focusArea || undefined,
          modality || undefined,
          0,
          20
        );
        setState((prev) => ({
          ...prev,
          results: response.content,
          totalElements: response.totalElements,
          loading: false,
          searched: true,
        }));
      } catch (err: unknown) {
        if (axios.isAxiosError(err)) {
          if (err.response?.status === 401) {
            navigate('/login', { replace: true });
            return;
          }
          setState((prev) => ({
            ...prev,
            loading: false,
            error: err.response?.data?.message ?? 'Search failed. Please try again.',
            searched: true,
          }));
        } else {
          setState((prev) => ({
            ...prev,
            loading: false,
            error: 'Network error. Please check your connection.',
            searched: true,
          }));
        }
      }
    },
    [navigate]
  );

  // -------------------------------------------------------------------------
  // Debounced search trigger (on query change)
  // -------------------------------------------------------------------------
  const scheduleDebouncedSearch = useCallback(
    (query: string, focusArea: string, modality: string) => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
      debounceRef.current = setTimeout(() => {
        doSearch(query, focusArea, modality);
      }, 300);
    },
    [doSearch]
  );

  // -------------------------------------------------------------------------
  // Public setters
  // -------------------------------------------------------------------------
  const setQuery = useCallback(
    (query: string) => {
      setState((prev) => ({ ...prev, query }));
      scheduleDebouncedSearch(query, latestRef.current.focusArea, latestRef.current.modality);
    },
    [scheduleDebouncedSearch]
  );

  const setFocusArea = useCallback(
    (focusArea: string) => {
      setState((prev) => ({ ...prev, focusArea }));
      // Immediate re-execution on filter change (Requirement 8.10)
      doSearch(latestRef.current.query, focusArea, latestRef.current.modality);
    },
    [doSearch]
  );

  const setModality = useCallback(
    (modality: string) => {
      setState((prev) => ({ ...prev, modality }));
      // Immediate re-execution on filter change (Requirement 8.10)
      doSearch(latestRef.current.query, latestRef.current.focusArea, modality);
    },
    [doSearch]
  );

  const executeSearch = useCallback(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }
    doSearch(latestRef.current.query, latestRef.current.focusArea, latestRef.current.modality);
  }, [doSearch]);

  // Cleanup debounce on unmount
  useEffect(() => {
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, []);

  return { state, setQuery, setFocusArea, setModality, executeSearch };
}
