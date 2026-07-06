import React, { useEffect, useState } from "react";
import { listPrograms } from "../../lib/vaultApi";
import type { VaultItem } from "../../types/vault";

export interface ProgramPickerProps {
  onProgramSelect: (program: VaultItem) => void;
  excludeProgramId?: string;
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-md)",
    width: "100%",
  },
  loading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "var(--spacing-lg)",
    color: "var(--color-text-secondary)",
    fontSize: "14px",
  },
  error: {
    padding: "var(--spacing-md)",
    borderRadius: "var(--radius-md)",
    background: "var(--color-error-subtle, rgba(239, 68, 68, 0.1))",
    color: "var(--color-error)",
    fontSize: "14px",
    textAlign: "center",
  },
  programItem: {
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-md)",
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-border)",
    background: "var(--color-bg-card)",
    cursor: "pointer",
    transition: "border-color 0.15s ease, background 0.15s ease",
    width: "100%",
    textAlign: "left",
  },
  programName: {
    fontSize: "15px",
    fontWeight: 500,
    color: "var(--color-text-primary)",
    margin: 0,
    flex: 1,
  },
  programSource: {
    fontSize: "12px",
    fontWeight: 400,
    color: "var(--color-text-secondary)",
    margin: 0,
    textTransform: "capitalize",
  },
  emptyState: {
    padding: "var(--spacing-lg)",
    color: "var(--color-text-secondary)",
    fontSize: "14px",
    textAlign: "center",
    fontStyle: "italic",
  },
};

function formatContentSource(source: string): string {
  return source.replace(/_/g, " ").toLowerCase();
}

export const ProgramPicker: React.FC<ProgramPickerProps> = ({
  onProgramSelect,
  excludeProgramId,
}) => {
  const [programs, setPrograms] = useState<VaultItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    listPrograms(0, 100)
      .then((response) => {
        if (!cancelled) {
          const filtered = excludeProgramId
            ? response.content.filter((p) => p.id !== excludeProgramId)
            : response.content;
          setPrograms(filtered);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err?.message || "Failed to load programs");
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [excludeProgramId]);

  if (loading) {
    return (
      <div style={styles.container} data-testid="program-picker">
        <div style={styles.loading} data-testid="program-picker-loading" role="status">
          Loading programs…
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.container} data-testid="program-picker">
        <div style={styles.error} data-testid="program-picker-error" role="alert">
          {error}
        </div>
      </div>
    );
  }

  if (programs.length === 0) {
    return (
      <div style={styles.container} data-testid="program-picker">
        <div style={styles.emptyState} data-testid="program-picker-empty">
          No programs available to copy from
        </div>
      </div>
    );
  }

  return (
    <div style={styles.container} data-testid="program-picker">
      <div data-testid="program-picker-list" role="list" aria-label="Program list">
        {programs.map((program) => (
          <button
            key={program.id}
            type="button"
            style={styles.programItem}
            onClick={() => onProgramSelect(program)}
            aria-label={`Select program ${program.name}`}
            data-testid={`program-picker-item-${program.id}`}
            role="listitem"
          >
            <div style={{ flex: 1 }}>
              <p style={styles.programName}>{program.name}</p>
              <p style={styles.programSource}>
                {formatContentSource(program.contentSource)}
              </p>
            </div>
            <span style={{ color: "var(--color-text-secondary)", fontSize: "18px" }} aria-hidden="true">›</span>
          </button>
        ))}
      </div>
    </div>
  );
};

export default ProgramPicker;
