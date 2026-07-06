import React, { useEffect, useState } from "react";
import { getProgramDays, WeekDays } from "../../lib/vaultApi";

export interface DayPickerSelection {
  weekNumber: number;
  dayNumber: number;
  label: string;
  focusArea: string;
}

export interface DayPickerProps {
  programId: string;
  onDaySelect: (day: DayPickerSelection) => void;
  onBack?: () => void;
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

export const DayPicker: React.FC<DayPickerProps> = ({
  programId,
  onDaySelect,
  onBack,
}) => {
  const [weeks, setWeeks] = useState<WeekDays[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setWeeks([]);

    getProgramDays(programId)
      .then((response) => {
        if (!cancelled) {
          setWeeks(response.weeks);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err?.message || "Failed to load program days");
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [programId]);

  const handleDaySelect = (
    weekNumber: number,
    dayNumber: number,
    label: string,
    focusArea: string
  ) => {
    onDaySelect({ weekNumber, dayNumber, label, focusArea });
  };

  return (
    <div style={styles.container} data-testid="day-picker">
      {onBack && (
        <button
          type="button"
          style={styles.backButton}
          onClick={onBack}
          aria-label="Back to program list"
          data-testid="day-picker-back-button"
        >
          ← Back
        </button>
      )}

      {loading && (
        <div style={styles.loading} data-testid="day-picker-loading">
          Loading days…
        </div>
      )}

      {error && (
        <div style={styles.error} data-testid="day-picker-error">
          {error}
        </div>
      )}

      {!loading && !error && weeks.length === 0 && (
        <div style={styles.emptyState} data-testid="day-picker-empty">
          No days available in this program
        </div>
      )}

      {!loading && !error && weeks.length > 0 && (
        <div data-testid="day-picker-list">
          {weeks.map((week) => (
            <div key={week.weekNumber} data-testid={`day-picker-week-${week.weekNumber}`}>
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
                  data-testid={`day-picker-item-${week.weekNumber}-${day.dayNumber}`}
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

export default DayPicker;
