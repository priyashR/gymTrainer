import React, { useEffect, useState } from "react";
import { DayAssignment } from "./DayTile";
import {
  listPrograms,
  getProgramDays,
  WeekDays,
} from "../../lib/vaultApi";
import type { VaultItem } from "../../types/vault";

export interface CopyDaySelectorProps {
  onSelect: (assignment: DayAssignment) => void;
  excludeProgramId?: string;
}

type Step = "programList" | "dayPicker";

interface SelectedProgram {
  id: string;
  name: string;
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
  backButton: {
    display: "inline-flex",
    alignItems: "center",
    gap: "var(--spacing-xs)",
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-xs) var(--spacing-sm)",
    borderRadius: "var(--radius-sm)",
    border: "1px solid var(--color-border)",
    background: "transparent",
    color: "var(--color-text-secondary)",
    fontSize: "13px",
    fontWeight: 500,
    cursor: "pointer",
    transition: "color 0.15s ease, border-color 0.15s ease",
    alignSelf: "flex-start",
  },
  weekHeading: {
    fontSize: "13px",
    fontWeight: 600,
    color: "var(--color-text-secondary)",
    textTransform: "uppercase",
    letterSpacing: "0.5px",
    margin: 0,
    paddingTop: "var(--spacing-sm)",
  },
  dayItem: {
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
  dayLabel: {
    fontSize: "14px",
    fontWeight: 500,
    color: "var(--color-text-primary)",
    margin: 0,
    flex: 1,
  },
  dayFocusArea: {
    fontSize: "12px",
    fontWeight: 400,
    color: "var(--color-text-secondary)",
    margin: 0,
  },
  emptyState: {
    padding: "var(--spacing-lg)",
    color: "var(--color-text-secondary)",
    fontSize: "14px",
    textAlign: "center",
    fontStyle: "italic",
  },
};

export const CopyDaySelector: React.FC<CopyDaySelectorProps> = ({
  onSelect,
  excludeProgramId,
}) => {
  const [step, setStep] = useState<Step>("programList");
  const [programs, setPrograms] = useState<VaultItem[]>([]);
  const [weeks, setWeeks] = useState<WeekDays[]>([]);
  const [selectedProgram, setSelectedProgram] = useState<SelectedProgram | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Fetch programs on mount
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

  const handleProgramSelect = (program: VaultItem) => {
    setSelectedProgram({ id: program.id, name: program.name });
    setStep("dayPicker");
    setLoading(true);
    setError(null);
    setWeeks([]);

    getProgramDays(program.id)
      .then((response) => {
        setWeeks(response.weeks);
        setLoading(false);
      })
      .catch((err) => {
        setError(err?.message || "Failed to load program days");
        setLoading(false);
      });
  };

  const handleDaySelect = (weekNumber: number, dayNumber: number, label: string, focusArea: string) => {
    if (!selectedProgram) return;

    onSelect({
      type: "copied_day",
      sourceProgramId: selectedProgram.id,
      sourceProgramName: selectedProgram.name,
      sourceWeekNumber: weekNumber,
      sourceDayNumber: dayNumber,
      dayLabel: label,
      focusArea: focusArea,
    });
  };

  const handleBack = () => {
    setStep("programList");
    setSelectedProgram(null);
    setWeeks([]);
    setError(null);
  };

  const formatContentSource = (source: string): string => {
    return source.replace(/_/g, " ").toLowerCase();
  };

  // Render program list step
  if (step === "programList") {
    return (
      <div style={styles.container} data-testid="copy-day-selector">
        {loading && (
          <div style={styles.loading} data-testid="copy-day-loading">
            Loading programs…
          </div>
        )}

        {error && (
          <div style={styles.error} data-testid="copy-day-error">
            {error}
          </div>
        )}

        {!loading && !error && programs.length === 0 && (
          <div style={styles.emptyState} data-testid="copy-day-empty">
            No programs available to copy from
          </div>
        )}

        {!loading && !error && programs.length > 0 && (
          <div data-testid="copy-day-program-list">
            {programs.map((program) => (
              <button
                key={program.id}
                type="button"
                style={styles.programItem}
                onClick={() => handleProgramSelect(program)}
                aria-label={`Select program ${program.name}`}
                data-testid={`program-item-${program.id}`}
              >
                <div style={{ flex: 1 }}>
                  <p style={styles.programName}>{program.name}</p>
                  <p style={styles.programSource}>
                    {formatContentSource(program.contentSource)}
                  </p>
                </div>
                <span style={{ color: "var(--color-text-secondary)", fontSize: "18px" }}>›</span>
              </button>
            ))}
          </div>
        )}
      </div>
    );
  }

  // Render day picker step
  return (
    <div style={styles.container} data-testid="copy-day-selector">
      <button
        type="button"
        style={styles.backButton}
        onClick={handleBack}
        aria-label="Back to program list"
        data-testid="copy-day-back-button"
      >
        ← Back
      </button>

      {selectedProgram && (
        <p
          style={{
            fontSize: "15px",
            fontWeight: 600,
            color: "var(--color-text-primary)",
            margin: 0,
          }}
          data-testid="copy-day-selected-program-name"
        >
          {selectedProgram.name}
        </p>
      )}

      {loading && (
        <div style={styles.loading} data-testid="copy-day-loading">
          Loading days…
        </div>
      )}

      {error && (
        <div style={styles.error} data-testid="copy-day-error">
          {error}
        </div>
      )}

      {!loading && !error && weeks.length === 0 && (
        <div style={styles.emptyState} data-testid="copy-day-empty">
          No days available in this program
        </div>
      )}

      {!loading && !error && weeks.length > 0 && (
        <div data-testid="copy-day-day-picker">
          {weeks.map((week) => (
            <div key={week.weekNumber} data-testid={`week-${week.weekNumber}`}>
              <p style={styles.weekHeading}>Week {week.weekNumber}</p>
              {week.days.map((day) => (
                <button
                  key={`${week.weekNumber}-${day.dayNumber}`}
                  type="button"
                  style={styles.dayItem}
                  onClick={() =>
                    handleDaySelect(
                      week.weekNumber,
                      day.dayNumber,
                      day.label,
                      day.focusArea
                    )
                  }
                  aria-label={`Select Day ${day.dayNumber} - ${day.label}`}
                  data-testid={`day-item-${week.weekNumber}-${day.dayNumber}`}
                >
                  <div style={{ flex: 1 }}>
                    <p style={styles.dayLabel}>
                      Day {day.dayNumber} — {day.label}
                    </p>
                    <p style={styles.dayFocusArea}>{day.focusArea}</p>
                  </div>
                </button>
              ))}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default CopyDaySelector;
