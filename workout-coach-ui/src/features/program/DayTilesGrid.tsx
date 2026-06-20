import React from "react";
import { DayTile, DayAssignment } from "./DayTile";

export interface DayTilesGridProps {
  days: DayAssignment[];
  onDayTap: (index: number) => void;
  onAddDay: () => void;
  onRemoveDay: (index: number) => void;
}

const styles: Record<string, React.CSSProperties> = {
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
    gap: "var(--spacing-md)",
    width: "100%",
  },
  addDayTile: {
    background: "transparent",
    borderRadius: "var(--radius-md)",
    border: "2px dashed var(--color-border)",
    padding: "var(--spacing-md)",
    display: "flex",
    flexDirection: "column" as const,
    alignItems: "center",
    justifyContent: "center",
    minHeight: "120px",
    cursor: "pointer",
    transition: "border-color 0.15s ease, background 0.15s ease",
    color: "var(--color-accent)",
    fontSize: "14px",
    fontWeight: 500,
    gap: "var(--spacing-sm)",
    minWidth: "var(--tap-target-min)",
  },
  addDayIcon: {
    fontSize: "28px",
    lineHeight: 1,
  },
};

export const DayTilesGrid: React.FC<DayTilesGridProps> = ({
  days,
  onDayTap,
  onAddDay,
  onRemoveDay,
}) => {
  return (
    <div style={styles.grid} data-testid="day-tiles-grid">
      {days.map((assignment, index) => (
        <DayTile
          key={index}
          dayNumber={index + 1}
          assignment={assignment}
          onClick={() => onDayTap(index)}
          onRemove={() => onRemoveDay(index)}
        />
      ))}
      <button
        type="button"
        style={styles.addDayTile}
        onClick={onAddDay}
        aria-label="Add a new day to the program"
        data-testid="add-day-tile"
      >
        <span style={styles.addDayIcon}>+</span>
        <span>Add Day</span>
      </button>
    </div>
  );
};

export default DayTilesGrid;
