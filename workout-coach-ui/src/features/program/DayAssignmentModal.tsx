import React, { useState } from "react";
import { DayAssignment } from "./DayTile";
import { WorkoutSelector } from "./WorkoutSelector";
import { ActivitySelector } from "./ActivitySelector";

export interface DayAssignmentModalProps {
  isOpen: boolean;
  dayNumber: number;
  onAssign: (assignment: DayAssignment) => void;
  onClose: () => void;
}

type Tab = "workout" | "activity";

const styles: Record<string, React.CSSProperties> = {
  backdrop: {
    position: "fixed",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    background: "rgba(0, 0, 0, 0.7)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 1000,
    padding: "var(--spacing-md)",
  },
  modal: {
    background: "var(--color-bg-surface)",
    borderRadius: "var(--radius-lg)",
    border: "1px solid var(--color-border)",
    width: "100%",
    maxWidth: "540px",
    maxHeight: "80vh",
    display: "flex",
    flexDirection: "column",
    overflow: "hidden",
  },
  header: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: "var(--spacing-md) var(--spacing-lg)",
    borderBottom: "1px solid var(--color-border)",
  },
  title: {
    fontSize: "18px",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    margin: 0,
  },
  closeButton: {
    minWidth: "var(--tap-target-min)",
    minHeight: "var(--tap-target-min)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: "var(--radius-sm)",
    border: "none",
    background: "transparent",
    color: "var(--color-text-secondary)",
    fontSize: "20px",
    cursor: "pointer",
    transition: "color 0.15s ease",
  },
  tabs: {
    display: "flex",
    borderBottom: "1px solid var(--color-border)",
  },
  tab: {
    flex: 1,
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-sm) var(--spacing-md)",
    border: "none",
    background: "transparent",
    color: "var(--color-text-secondary)",
    fontSize: "15px",
    fontWeight: 500,
    cursor: "pointer",
    transition: "color 0.15s ease, border-color 0.15s ease",
    borderBottom: "2px solid transparent",
  },
  tabActive: {
    flex: 1,
    minHeight: "var(--tap-target-min)",
    padding: "var(--spacing-sm) var(--spacing-md)",
    border: "none",
    background: "transparent",
    color: "var(--color-accent)",
    fontSize: "15px",
    fontWeight: 500,
    cursor: "pointer",
    transition: "color 0.15s ease, border-color 0.15s ease",
    borderBottom: "2px solid var(--color-accent)",
  },
  content: {
    flex: 1,
    overflowY: "auto",
    padding: "var(--spacing-md)",
  },
};

export const DayAssignmentModal: React.FC<DayAssignmentModalProps> = ({
  isOpen,
  dayNumber,
  onAssign,
  onClose,
}) => {
  const [activeTab, setActiveTab] = useState<Tab>("workout");

  if (!isOpen) {
    return null;
  }

  const handleWorkoutSelect = (workoutId: string, workoutName: string) => {
    onAssign({
      type: "workout",
      workoutId,
      workoutName,
    });
  };

  const handleActivitySelect = (activityType: string) => {
    onAssign({
      type: "activity",
      activityType,
    });
  };

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      style={styles.backdrop}
      onClick={handleBackdropClick}
      data-testid="day-assignment-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-label={`Assign Day ${dayNumber}`}
    >
      <div style={styles.modal} data-testid="day-assignment-modal">
        <div style={styles.header}>
          <h2 style={styles.title}>Assign Day {dayNumber}</h2>
          <button
            type="button"
            style={styles.closeButton}
            onClick={onClose}
            aria-label="Close modal"
            data-testid="day-assignment-modal-close"
          >
            ✕
          </button>
        </div>

        <div style={styles.tabs} role="tablist">
          <button
            type="button"
            style={activeTab === "workout" ? styles.tabActive : styles.tab}
            onClick={() => setActiveTab("workout")}
            role="tab"
            aria-selected={activeTab === "workout"}
            aria-controls="tab-panel-workout"
            data-testid="tab-workout"
          >
            💪 Workout
          </button>
          <button
            type="button"
            style={activeTab === "activity" ? styles.tabActive : styles.tab}
            onClick={() => setActiveTab("activity")}
            role="tab"
            aria-selected={activeTab === "activity"}
            aria-controls="tab-panel-activity"
            data-testid="tab-activity"
          >
            🏃 Activity
          </button>
        </div>

        <div style={styles.content}>
          {activeTab === "workout" && (
            <div
              id="tab-panel-workout"
              role="tabpanel"
              data-testid="tab-panel-workout"
            >
              <WorkoutSelector onSelect={handleWorkoutSelect} />
            </div>
          )}
          {activeTab === "activity" && (
            <div
              id="tab-panel-activity"
              role="tabpanel"
              data-testid="tab-panel-activity"
            >
              <ActivitySelector onSelect={handleActivitySelect} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DayAssignmentModal;
