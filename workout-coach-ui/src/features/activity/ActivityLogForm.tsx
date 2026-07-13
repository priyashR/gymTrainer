import React from "react";

export type DistanceUnit = "km" | "miles";

export interface ActivityLogFormProps {
  duration: string;
  calories: string;
  distance: string;
  distanceUnit: DistanceUnit;
  notes: string;
  onDurationChange: (value: string) => void;
  onCaloriesChange: (value: string) => void;
  onDistanceChange: (value: string) => void;
  onDistanceUnitChange: (unit: DistanceUnit) => void;
  onNotesChange: (value: string) => void;
}

const styles: Record<string, React.CSSProperties> = {
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-md)",
  },
  fieldGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-xs)",
  },
  label: {
    fontSize: "13px",
    fontWeight: 500,
    color: "var(--color-text-secondary)",
  },
  input: {
    width: "100%",
    padding: "var(--spacing-sm) var(--spacing-md)",
    fontSize: "16px",
    fontFamily: "var(--font-sans)",
    background: "var(--color-bg-surface)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-sm)",
    color: "var(--color-text-primary)",
    minHeight: "var(--tap-target-min)",
    boxSizing: "border-box",
  },
  distanceRow: {
    display: "flex",
    gap: "var(--spacing-sm)",
    alignItems: "stretch",
  },
  distanceInput: {
    flex: 1,
    padding: "var(--spacing-sm) var(--spacing-md)",
    fontSize: "16px",
    fontFamily: "var(--font-sans)",
    background: "var(--color-bg-surface)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-sm)",
    color: "var(--color-text-primary)",
    minHeight: "var(--tap-target-min)",
    boxSizing: "border-box",
  },
  unitToggle: {
    display: "flex",
    borderRadius: "var(--radius-sm)",
    overflow: "hidden",
    border: "1px solid var(--color-border)",
  },
  unitButton: {
    padding: "var(--spacing-sm) var(--spacing-md)",
    fontSize: "14px",
    fontWeight: 500,
    fontFamily: "var(--font-sans)",
    border: "none",
    cursor: "pointer",
    minWidth: "var(--tap-target-min)",
    minHeight: "var(--tap-target-min)",
    transition: "background 0.15s ease, color 0.15s ease",
    background: "var(--color-bg-surface)",
    color: "var(--color-text-secondary)",
  },
  unitButtonActive: {
    background: "var(--color-accent)",
    color: "var(--color-fab-text)",
  },
  textarea: {
    width: "100%",
    padding: "var(--spacing-sm) var(--spacing-md)",
    fontSize: "16px",
    fontFamily: "var(--font-sans)",
    background: "var(--color-bg-surface)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-sm)",
    color: "var(--color-text-primary)",
    minHeight: "88px",
    resize: "vertical",
    boxSizing: "border-box",
  },
};

export const ActivityLogForm: React.FC<ActivityLogFormProps> = ({
  duration,
  calories,
  distance,
  distanceUnit,
  notes,
  onDurationChange,
  onCaloriesChange,
  onDistanceChange,
  onDistanceUnitChange,
  onNotesChange,
}) => {
  return (
    <div style={styles.form} data-testid="activity-log-form">
      {/* Duration Field */}
      <div style={styles.fieldGroup}>
        <label htmlFor="activity-duration" style={styles.label}>
          Duration (minutes)
        </label>
        <input
          id="activity-duration"
          type="number"
          min="0"
          placeholder="0"
          value={duration}
          onChange={(e) => onDurationChange(e.target.value)}
          style={styles.input}
          data-testid="duration-input"
          aria-label="Duration in minutes"
        />
      </div>

      {/* Calories Field */}
      <div style={styles.fieldGroup}>
        <label htmlFor="activity-calories" style={styles.label}>
          Calories (kcal)
        </label>
        <input
          id="activity-calories"
          type="number"
          min="0"
          placeholder="0"
          value={calories}
          onChange={(e) => onCaloriesChange(e.target.value)}
          style={styles.input}
          data-testid="calories-input"
          aria-label="Calories in kcal"
        />
      </div>

      {/* Distance Field */}
      <div style={styles.fieldGroup}>
        <label htmlFor="activity-distance" style={styles.label}>
          Distance
        </label>
        <div style={styles.distanceRow}>
          <input
            id="activity-distance"
            type="number"
            min="0"
            step="0.1"
            placeholder="0"
            value={distance}
            onChange={(e) => onDistanceChange(e.target.value)}
            style={styles.distanceInput}
            data-testid="distance-input"
            aria-label="Distance value"
          />
          <div
            style={styles.unitToggle}
            role="group"
            aria-label="Distance unit"
          >
            <button
              type="button"
              style={{
                ...styles.unitButton,
                ...(distanceUnit === "km" ? styles.unitButtonActive : {}),
              }}
              onClick={() => onDistanceUnitChange("km")}
              aria-pressed={distanceUnit === "km"}
              data-testid="unit-km"
            >
              km
            </button>
            <button
              type="button"
              style={{
                ...styles.unitButton,
                ...(distanceUnit === "miles" ? styles.unitButtonActive : {}),
              }}
              onClick={() => onDistanceUnitChange("miles")}
              aria-pressed={distanceUnit === "miles"}
              data-testid="unit-miles"
            >
              miles
            </button>
          </div>
        </div>
      </div>

      {/* Notes Field */}
      <div style={styles.fieldGroup}>
        <label htmlFor="activity-notes" style={styles.label}>
          Notes
        </label>
        <textarea
          id="activity-notes"
          placeholder="Any additional notes..."
          value={notes}
          onChange={(e) => onNotesChange(e.target.value)}
          style={styles.textarea}
          data-testid="notes-textarea"
          aria-label="Notes"
        />
      </div>
    </div>
  );
};

export default ActivityLogForm;
