import React, { useState } from "react";

const ACTIVITY_TYPES = [
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

export interface ActivitySelectorProps {
  onSelect: (activityType: string) => void;
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-md)",
    width: "100%",
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(100px, 1fr))",
    gap: "var(--spacing-sm)",
    width: "100%",
  },
  tile: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    gap: "var(--spacing-xs)",
    minWidth: "var(--tap-target-min)",
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-sm)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-border)",
    background: "var(--color-bg-card)",
    cursor: "pointer",
    transition: "border-color 0.15s ease, background 0.15s ease",
    color: "var(--color-text-primary)",
    fontSize: "13px",
    fontWeight: 500,
    textAlign: "center",
  },
  tileSelected: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    gap: "var(--spacing-xs)",
    minWidth: "var(--tap-target-min)",
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-sm)",
    borderRadius: "var(--radius-md)",
    border: "2px solid var(--color-accent)",
    background: "var(--color-bg-surface)",
    cursor: "pointer",
    transition: "border-color 0.15s ease, background 0.15s ease",
    color: "var(--color-accent)",
    fontSize: "13px",
    fontWeight: 500,
    textAlign: "center",
  },
  emoji: {
    fontSize: "28px",
    lineHeight: 1,
  },
  name: {
    fontSize: "12px",
    fontWeight: 500,
    margin: 0,
  },
  customInputContainer: {
    display: "flex",
    gap: "var(--spacing-sm)",
    alignItems: "center",
    width: "100%",
  },
  customInput: {
    flex: 1,
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-sm)",
    border: "1px solid var(--color-border)",
    background: "var(--color-bg-surface)",
    color: "var(--color-text-primary)",
    fontSize: "14px",
    fontFamily: "var(--font-sans)",
    outline: "none",
  },
  submitButton: {
    minWidth: "var(--tap-target-min)",
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-sm) var(--spacing-md)",
    borderRadius: "var(--radius-sm)",
    border: "none",
    background: "var(--color-accent)",
    color: "var(--color-fab-text)",
    fontSize: "14px",
    fontWeight: 600,
    cursor: "pointer",
    transition: "background 0.15s ease",
  },
};

export const ActivitySelector: React.FC<ActivitySelectorProps> = ({
  onSelect,
}) => {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [customName, setCustomName] = useState("");

  const handleTileClick = (activity: (typeof ACTIVITY_TYPES)[number]) => {
    if (activity.id === "other") {
      setSelectedId("other");
    } else {
      setSelectedId(activity.id);
      onSelect(`${activity.emoji} ${activity.name}`);
    }
  };

  const handleCustomSubmit = () => {
    const trimmed = customName.trim();
    if (trimmed) {
      onSelect(trimmed);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      handleCustomSubmit();
    }
  };

  return (
    <div style={styles.container} data-testid="activity-selector">
      <div style={styles.grid} data-testid="activity-selector-grid">
        {ACTIVITY_TYPES.map((activity) => (
          <button
            key={activity.id}
            type="button"
            style={
              selectedId === activity.id ? styles.tileSelected : styles.tile
            }
            onClick={() => handleTileClick(activity)}
            aria-label={`Select ${activity.name} activity`}
            aria-pressed={selectedId === activity.id}
            data-testid={`activity-tile-${activity.id}`}
          >
            <span style={styles.emoji} aria-hidden="true">
              {activity.emoji}
            </span>
            <span style={styles.name}>{activity.name}</span>
          </button>
        ))}
      </div>

      {selectedId === "other" && (
        <div
          style={styles.customInputContainer}
          data-testid="custom-activity-input-container"
        >
          <input
            type="text"
            style={styles.customInput}
            placeholder="Enter custom activity name"
            value={customName}
            onChange={(e) => setCustomName(e.target.value)}
            onKeyDown={handleKeyDown}
            aria-label="Custom activity name"
            data-testid="custom-activity-input"
            autoFocus
          />
          <button
            type="button"
            style={styles.submitButton}
            onClick={handleCustomSubmit}
            disabled={!customName.trim()}
            aria-label="Confirm custom activity"
            data-testid="custom-activity-submit"
          >
            Confirm
          </button>
        </div>
      )}
    </div>
  );
};

export default ActivitySelector;
