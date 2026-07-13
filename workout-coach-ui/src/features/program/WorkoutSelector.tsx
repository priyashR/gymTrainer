import React, { useCallback, useEffect, useRef, useState } from "react";
import { searchPrograms } from "../../lib/vaultApi";
import type { VaultItem } from "../../types/vault";

export interface WorkoutSelectorProps {
  onSelect: (workoutId: string, workoutName: string) => void;
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-md)",
    padding: "var(--spacing-md)",
  },
  searchInput: {
    width: "100%",
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-sm)",
    border: "1px solid var(--color-border)",
    background: "var(--color-bg-surface)",
    color: "var(--color-text-primary)",
    fontSize: "15px",
    fontFamily: "var(--font-sans)",
    outline: "none",
    minHeight: "var(--tap-target-min)",
    boxSizing: "border-box",
  },
  list: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-sm)",
    overflowY: "auto",
    maxHeight: "320px",
  },
  card: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    background: "var(--color-bg-card)",
    borderRadius: "var(--radius-sm)",
    border: "1px solid var(--color-border)",
    padding: "var(--spacing-sm) var(--spacing-md)",
    gap: "var(--spacing-sm)",
  },
  cardInfo: {
    display: "flex",
    flexDirection: "column",
    gap: "2px",
    flex: 1,
    minWidth: 0,
  },
  cardName: {
    fontSize: "15px",
    fontWeight: 500,
    color: "var(--color-text-primary)",
    margin: 0,
    overflow: "hidden",
    textOverflow: "ellipsis",
    whiteSpace: "nowrap",
  },
  cardMeta: {
    fontSize: "12px",
    color: "var(--color-text-secondary)",
    margin: 0,
  },
  assignButton: {
    minWidth: "var(--tap-target-min)",
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-xs) var(--spacing-md)",
    borderRadius: "var(--radius-sm)",
    border: "none",
    background: "var(--color-accent)",
    color: "var(--color-bg-primary)",
    fontSize: "13px",
    fontWeight: 600,
    cursor: "pointer",
    flexShrink: 0,
    transition: "background 0.15s ease",
  },
  loadingText: {
    fontSize: "14px",
    color: "var(--color-text-secondary)",
    textAlign: "center",
    padding: "var(--spacing-lg)",
    margin: 0,
  },
  errorText: {
    fontSize: "14px",
    color: "var(--color-error)",
    textAlign: "center",
    padding: "var(--spacing-lg)",
    margin: 0,
  },
  emptyText: {
    fontSize: "14px",
    color: "var(--color-text-secondary)",
    textAlign: "center",
    padding: "var(--spacing-lg)",
    margin: 0,
    fontStyle: "italic",
  },
};

export const WorkoutSelector: React.FC<WorkoutSelectorProps> = ({ onSelect }) => {
  const [query, setQuery] = useState("");
  const [workouts, setWorkouts] = useState<VaultItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const fetchWorkouts = useCallback(async (searchQuery: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await searchPrograms(
        searchQuery || undefined,
        undefined,
        undefined,
        0,
        50
      );
      setWorkouts(response.content);
    } catch {
      setError("Failed to load workouts. Please try again.");
      setWorkouts([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Initial fetch on mount
  useEffect(() => {
    fetchWorkouts("");
  }, [fetchWorkouts]);

  // Debounced search
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);

    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    debounceRef.current = setTimeout(() => {
      fetchWorkouts(value);
    }, 300);
  };

  // Cleanup debounce on unmount
  useEffect(() => {
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, []);

  return (
    <div style={styles.container} data-testid="workout-selector">
      <input
        type="text"
        placeholder="Search workouts..."
        value={query}
        onChange={handleSearchChange}
        style={styles.searchInput}
        aria-label="Search workouts"
        data-testid="workout-selector-search"
      />

      <div style={styles.list} data-testid="workout-selector-list">
        {isLoading && (
          <p style={styles.loadingText} data-testid="workout-selector-loading">
            Loading workouts...
          </p>
        )}

        {error && !isLoading && (
          <p style={styles.errorText} data-testid="workout-selector-error">
            {error}
          </p>
        )}

        {!isLoading && !error && workouts.length === 0 && (
          <p style={styles.emptyText} data-testid="workout-selector-empty">
            No workouts found
          </p>
        )}

        {!isLoading &&
          !error &&
          workouts.map((workout) => (
            <div
              key={workout.id}
              style={styles.card}
              data-testid={`workout-card-${workout.id}`}
            >
              <div style={styles.cardInfo}>
                <p style={styles.cardName}>{workout.name}</p>
                {workout.goal && (
                  <p style={styles.cardMeta}>{workout.goal}</p>
                )}
              </div>
              <button
                type="button"
                style={styles.assignButton}
                onClick={() => onSelect(workout.id, workout.name)}
                aria-label={`Assign ${workout.name}`}
                data-testid={`workout-assign-${workout.id}`}
              >
                Assign
              </button>
            </div>
          ))}
      </div>
    </div>
  );
};

export default WorkoutSelector;
