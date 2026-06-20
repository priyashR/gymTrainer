import React from "react";

export interface ResumeWorkoutCardProps {
  sessionId: string;
  workoutName: string;
  status: "active" | "paused";
  progress: number;
  onResume: (sessionId: string) => void;
}

const styles: Record<string, React.CSSProperties> = {
  card: {
    background: "linear-gradient(135deg, #1e1e1e 0%, #2a2a2a 50%, #1a3a4a 100%)",
    borderRadius: "var(--radius-lg)",
    padding: "var(--spacing-lg)",
    border: "1px solid var(--color-border)",
    boxShadow: "0 4px 16px rgba(0, 0, 0, 0.3)",
  },
  header: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: "var(--spacing-md)",
  },
  workoutName: {
    fontSize: "18px",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    margin: 0,
  },
  statusBadge: {
    fontSize: "12px",
    fontWeight: 500,
    padding: "var(--spacing-xs) var(--spacing-sm)",
    borderRadius: "var(--radius-sm)",
    textTransform: "uppercase" as const,
    letterSpacing: "0.5px",
  },
  statusActive: {
    background: "rgba(102, 187, 106, 0.15)",
    color: "var(--color-success)",
  },
  statusPaused: {
    background: "rgba(255, 167, 38, 0.15)",
    color: "var(--color-warning)",
  },
  progressContainer: {
    marginBottom: "var(--spacing-md)",
  },
  progressLabel: {
    fontSize: "13px",
    color: "var(--color-text-secondary)",
    marginBottom: "var(--spacing-xs)",
  },
  progressBar: {
    width: "100%",
    height: "6px",
    borderRadius: "3px",
    background: "var(--color-bg-primary)",
    overflow: "hidden",
  },
  progressFill: {
    height: "100%",
    borderRadius: "3px",
    background: "var(--color-accent)",
    transition: "width 0.3s ease",
  },
  resumeButton: {
    width: "100%",
    padding: "var(--spacing-md)",
    borderRadius: "var(--radius-md)",
    border: "none",
    background: "var(--color-accent)",
    color: "var(--color-fab-text)",
    fontSize: "16px",
    fontWeight: 600,
    cursor: "pointer",
    minHeight: "var(--tap-target-preferred)",
    transition: "background 0.15s ease",
  },
};

export const ResumeWorkoutCard: React.FC<ResumeWorkoutCardProps> = ({
  sessionId,
  workoutName,
  status,
  progress,
  onResume,
}) => {
  const clampedProgress = Math.max(0, Math.min(100, progress));

  const badgeStyle: React.CSSProperties = {
    ...styles.statusBadge,
    ...(status === "active" ? styles.statusActive : styles.statusPaused),
  };

  return (
    <div style={styles.card} data-testid="resume-workout-card">
      <div style={styles.header}>
        <h3 style={styles.workoutName}>{workoutName}</h3>
        <span style={badgeStyle} aria-label={`Status: ${status}`}>
          {status}
        </span>
      </div>

      <div style={styles.progressContainer}>
        <div style={styles.progressLabel}>{clampedProgress}% complete</div>
        <div
          style={styles.progressBar}
          role="progressbar"
          aria-valuenow={clampedProgress}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={`Workout progress: ${clampedProgress}%`}
        >
          <div
            style={{ ...styles.progressFill, width: `${clampedProgress}%` }}
          />
        </div>
      </div>

      <button
        style={styles.resumeButton}
        onClick={() => onResume(sessionId)}
        type="button"
        aria-label={`Resume workout: ${workoutName}`}
      >
        Resume Workout
      </button>
    </div>
  );
};

export default ResumeWorkoutCard;
