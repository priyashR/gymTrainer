import React, { useState } from "react";

export interface ActivityType {
  id: string;
  emoji: string;
  name: string;
}

const ACTIVITY_TYPES: readonly ActivityType[] = [
  { id: "soccer", emoji: "⚽", name: "Soccer" },
  { id: "squash", emoji: "🏸", name: "Squash" },
  { id: "running", emoji: "🏃", name: "Running" },
  { id: "padel", emoji: "🎾", name: "Padel" },
  { id: "golf", emoji: "⛳", name: "Golf" },
  { id: "swimming", emoji: "🏊", name: "Swimming" },
  { id: "cycling", emoji: "🚴", name: "Cycling" },
  { id: "hiking", emoji: "🥾", name: "Hiking" },
  { id: "basketball", emoji: "🏀", name: "Basketball" },
  { id: "tennis", emoji: "🎾", name: "Tennis" },
  { id: "other", emoji: "✏️", name: "Other" },
] as const;

export { ACTIVITY_TYPES };

export interface ActivityTypeGridProps {
  selectedType: string | null;
  onSelect: (activityType: string) => void;
}

const styles: Record<string, React.CSSProperties> = {
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(3, 1fr)",
    gap: "var(--spacing-md)",
  },
  tile: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    gap: "var(--spacing-sm)",
    padding: "var(--spacing-md)",
    background: "var(--color-bg-card)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-border)",
    cursor: "pointer",
    minHeight: "var(--tap-target-min)",
    minWidth: "var(--tap-target-min)",
    transition: "background 0.15s ease, border-color 0.15s ease",
  },
  tileSelected: {
    background: "var(--color-bg-surface)",
    borderColor: "var(--color-accent)",
  },
  emoji: {
    fontSize: "28px",
    lineHeight: 1,
  },
  name: {
    fontSize: "13px",
    fontWeight: 500,
    color: "var(--color-text-primary)",
    textAlign: "center",
    margin: 0,
  },
  customInputWrapper: {
    gridColumn: "1 / -1",
    marginTop: "var(--spacing-sm)",
  },
  customInput: {
    width: "100%",
    padding: "var(--spacing-sm) var(--spacing-md)",
    fontSize: "14px",
    fontFamily: "var(--font-sans)",
    background: "var(--color-bg-surface)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-sm)",
    color: "var(--color-text-primary)",
    minHeight: "var(--tap-target-min)",
    boxSizing: "border-box",
  },
};

export const ActivityTypeGrid: React.FC<ActivityTypeGridProps> = ({
  selectedType,
  onSelect,
}) => {
  const [customName, setCustomName] = useState("");

  const isOtherSelected = selectedType === "other";

  const handleTileClick = (activity: ActivityType) => {
    onSelect(activity.id);
  };

  const handleCustomNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setCustomName(value);
    if (value.trim()) {
      onSelect(value.trim());
    }
  };

  return (
    <div data-testid="activity-type-grid">
      <div style={styles.grid} role="group" aria-label="Activity types">
        {ACTIVITY_TYPES.map((activity) => {
          const isSelected =
            selectedType === activity.id ||
            (activity.id === "other" && isOtherSelected);

          return (
            <button
              key={activity.id}
              type="button"
              style={{
                ...styles.tile,
                ...(isSelected ? styles.tileSelected : {}),
              }}
              onClick={() => handleTileClick(activity)}
              aria-label={activity.name}
              aria-pressed={isSelected}
              data-testid={`activity-tile-${activity.id}`}
            >
              <span style={styles.emoji} aria-hidden="true">
                {activity.emoji}
              </span>
              <span style={styles.name}>{activity.name}</span>
            </button>
          );
        })}

        {isOtherSelected && (
          <div style={styles.customInputWrapper}>
            <input
              type="text"
              placeholder="Enter activity name"
              value={customName}
              onChange={handleCustomNameChange}
              style={styles.customInput}
              aria-label="Custom activity name"
              data-testid="custom-activity-input"
              autoFocus
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default ActivityTypeGrid;
