import React, { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { searchPrograms } from "../lib/vaultApi";
import { FilterChipsRow, type SearchFilters } from "../features/search/FilterChipsRow";
import { ResultsGrid } from "../features/search/ResultsGrid";
import { EmptyState } from "../components/ui/EmptyState";
import type { VaultItem } from "../types/vault";

const DEBOUNCE_MS = 300;

const styles: Record<string, React.CSSProperties> = {
  page: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-md)",
    padding: "var(--spacing-lg)",
    maxWidth: "900px",
    margin: "0 auto",
    width: "100%",
  },
  backLink: {
    display: "inline-flex",
    alignItems: "center",
    gap: "var(--spacing-xs)",
    color: "var(--color-accent)",
    background: "none",
    border: "none",
    cursor: "pointer",
    fontSize: "14px",
    fontWeight: 500,
    padding: 0,
    minHeight: "var(--tap-target-min)",
  },
  searchInput: {
    width: "100%",
    padding: "var(--spacing-md)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-border)",
    background: "var(--color-bg-surface)",
    color: "var(--color-text-primary)",
    fontSize: "16px",
    fontFamily: "var(--font-sans)",
    outline: "none",
    minHeight: "var(--tap-target-preferred)",
    boxSizing: "border-box",
  },
  createProgramLink: {
    display: "inline-flex",
    alignItems: "center",
    gap: "var(--spacing-xs)",
    color: "var(--color-accent)",
    background: "none",
    border: "none",
    cursor: "pointer",
    fontSize: "14px",
    fontWeight: 600,
    padding: "var(--spacing-sm) 0",
    textDecoration: "none",
    minHeight: "var(--tap-target-min)",
  },
  loadingText: {
    color: "var(--color-text-secondary)",
    fontSize: "14px",
    textAlign: "center",
    padding: "var(--spacing-lg)",
  },
};

export const SearchPage: React.FC = () => {
  const navigate = useNavigate();

  const [query, setQuery] = useState("");
  const [filters, setFilters] = useState<SearchFilters>({
    focusArea: "",
    modality: "",
    days: "",
  });
  const [programs, setPrograms] = useState<VaultItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const fetchResults = useCallback(
    async (q: string, f: SearchFilters) => {
      setLoading(true);
      setHasSearched(true);
      try {
        const response = await searchPrograms(
          q || undefined,
          f.focusArea || undefined,
          f.modality || undefined
        );

        let results = response.content;

        // Apply "Days" filter client-side on durationWeeks field
        if (f.days) {
          const daysValue = parseInt(f.days, 10);
          results = results.filter(
            (program) => program.durationWeeks === daysValue
          );
        }

        setPrograms(results);
      } catch {
        setPrograms([]);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  // Debounce search query changes
  useEffect(() => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    debounceTimerRef.current = setTimeout(() => {
      fetchResults(query, filters);
    }, DEBOUNCE_MS);

    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, [query, filters, fetchResults]);

  const handleQueryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
  };

  const handleFiltersChange = (newFilters: SearchFilters) => {
    setFilters(newFilters);
  };

  const handleCardClick = (programId: string) => {
    navigate(`/vault/programs/${programId}`);
  };

  const handleBackClick = () => {
    navigate("/");
  };

  const handleCreateProgramClick = () => {
    navigate("/programs/create");
  };

  return (
    <main style={styles.page} data-testid="search-page">
      {/* BackLink */}
      <button
        type="button"
        style={styles.backLink}
        onClick={handleBackClick}
        aria-label="Back to Home"
        data-testid="back-link"
      >
        ← Home
      </button>

      {/* SearchBar */}
      <input
        type="search"
        placeholder="Search programs..."
        value={query}
        onChange={handleQueryChange}
        style={styles.searchInput}
        aria-label="Search programs"
        data-testid="search-input"
      />

      {/* FilterChipsRow */}
      <FilterChipsRow filters={filters} onChange={handleFiltersChange} />

      {/* CreateProgramLink */}
      <button
        type="button"
        style={styles.createProgramLink}
        onClick={handleCreateProgramClick}
        data-testid="create-program-link"
      >
        + Create Program
      </button>

      {/* Results area */}
      {loading && (
        <p style={styles.loadingText} data-testid="loading-indicator">
          Searching...
        </p>
      )}

      {!loading && hasSearched && programs.length === 0 && (
        <EmptyState
          icon="🔍"
          title="No matching programs found"
          subtitle="Try adjusting your search or filters"
        />
      )}

      {!loading && programs.length > 0 && (
        <ResultsGrid programs={programs} onCardClick={handleCardClick} />
      )}
    </main>
  );
};

export default SearchPage;
