import React from "react";

export interface DayAssignment {
  type: "workout" | "activity" | "copied_day" | null;
  workoutId?: string;
  workoutName?: string;
  activityType?: string;
  // Copied day fields
  sourceProgramId?: string;
  sourceProgramName?: string;
  sourceWeekNumber?: number;
  sourceDayNumber?: number;
  dayLabel?: string;
  focusArea?: string;
}

export interface DayTileProps {
  dayNumber: number;
  assignment: DayAssignment | null;
  onClick: () => void;
  onRemove: () => void;
}

const styles: Record<string, React.CSSProperties> = {
  tile: {
    background: "var(--color-bg-card)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-border)",
    padding: "var(--spacing-md)",
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-sm)",
    minHeight: "120px",
  },
  dayLabel: {
    fontSize: "13px",
    fontWeight: 600,
    color: "var(--color-text-secondary)",
    textTransform: "uppercase" as const,
    letterSpacing: "0.5px",
    margin: 0,
  },
  assignmentName: {
    fontSize: "15px",
    fontWeight: 500,
    color: "var(--color-text-primary)",
    margin: 0,
    flex: 1,
  },
  noAssignment: {
    fontSize: "14px",
    fontWeight: 400,
    color: "var(--color-text-secondary)",
    fontStyle: "italic",
    margin: 0,
    flex: 1,
  },
  actions: {
    display: "flex",
    gap: "var(--spacing-sm)",
    marginTop: "auto",
  },
  changeButton: {
    minWidth: "var(--tap-target-min)",
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-xs) var(--spacing-sm)",
    borderRadius: "var(--radius-sm)",
    border: "1px solid var(--color-accent)",
    background: "transparent",
    color: "var(--color-accent)",
    fontSize: "13px",
    fontWeight: 500,
    cursor: "pointer",
    transition: "background 0.15s ease",
  },
  removeButton: {
    minWidth: "var(--tap-target-min)",
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-xs) var(--spacing-sm)",
    borderRadius: "var(--radius-sm)",
    border: "1px solid var(--color-error)",
    background: "transparent",
    color: "var(--color-error)",
    fontSize: "13px",
    fontWeight: 500,
    cursor: "pointer",
    transition: "background 0.15s ease",
  },
  assignButton: {
    minWidth: "var(--tap-target-min)",
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-xs) var(--spacing-sm)",
    borderRadius: "var(--radius-sm)",
    border: "1px solid var(--color-accent)",
    background: "transparent",
    color: "var(--color-accent)",
    fontSize: "13px",
    fontWeight: 500,
    cursor: "pointer",
    transition: "background 0.15s ease",
    width: "100%",
  },
  copiedDayBadge: {
    display: "inline-flex",
    alignItems: "center",
    gap: "4px",
    fontSize: "11px",
    fontWeight: 600,
    color: "var(--color-accent)",
    background: "var(--color-accent-subtle, rgba(59, 130, 246, 0.1))",
    borderRadius: "var(--radius-sm)",
    padding: "2px 8px",
    width: "fit-content",
    textTransform: "uppercase" as const,
    letterSpacing: "0.3px",
  },
  copiedDayTile: {
    background: "var(--color-bg-card)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-accent, #3b82f6)",
    borderLeft: "3px solid var(--color-accent, #3b82f6)",
    padding: "var(--spacing-md)",
    display: "flex",
    flexDirection: "column" as const,
    gap: "var(--spacing-sm)",
    minHeight: "120px",
  },
};

function getDisplayName(assignment: DayAssignment): string {
  if (assignment.type === "copied_day") {
    const label = assignment.dayLabel || `Week ${assignment.sourceWeekNumber} Day ${assignment.sourceDayNumber}`;
    return `📋 ${assignment.sourceProgramName} — ${label}`;
  }
  if (assignment.type === "workout" && assignment.workoutName) {
    return assignment.workoutName;
  }
  if (assignment.type === "activity" && assignment.activityType) {
    return assignment.activityType;
  }
  return "No assignment";
}

export const DayTile: React.FC<DayTileProps> = ({
  dayNumber,
  assignment,
  onClick,
  onRemove,
}) => {
  const hasAssignment = assignment !== null && assignment.type !== null;
  const displayName = hasAssignment ? getDisplayName(assignment!) : null;
  const isCopiedDay = assignment?.type === "copied_day";
  const tileStyle = isCopiedDay ? styles.copiedDayTile : styles.tile;

  return (
    <div style={tileStyle} data-testid={`day-tile-${dayNumber}`}>
      <p style={styles.dayLabel}>Day {dayNumber}</p>

      {isCopiedDay && (
        <span style={styles.copiedDayBadge} data-testid={`day-tile-${dayNumber}-copied-badge`}>
          Copied Day
        </span>
      )}

      {hasAssignment ? (
        <p style={styles.assignmentName} data-testid={`day-tile-${dayNumber}-name`}>
          {displayName}
        </p>
      ) : (
        <p style={styles.noAssignment} data-testid={`day-tile-${dayNumber}-empty`}>
          No assignment
        </p>
      )}

      <div style={styles.actions}>
        {hasAssignment ? (
          <>
            <button
              type="button"
              style={styles.changeButton}
              onClick={onClick}
              aria-label={`Change assignment for Day ${dayNumber}`}
              data-testid={`day-tile-${dayNumber}-change`}
            >
              Change
            </button>
            <button
              type="button"
              style={styles.removeButton}
              onClick={onRemove}
              aria-label={`Remove assignment for Day ${dayNumber}`}
              data-testid={`day-tile-${dayNumber}-remove`}
            >
              Remove
            </button>
          </>
        ) : (
          <button
            type="button"
            style={styles.assignButton}
            onClick={onClick}
            aria-label={`Assign workout or activity to Day ${dayNumber}`}
            data-testid={`day-tile-${dayNumber}-assign`}
          >
            Assign
          </button>
        )}
      </div>
    </div>
  );
};

export default DayTile;
