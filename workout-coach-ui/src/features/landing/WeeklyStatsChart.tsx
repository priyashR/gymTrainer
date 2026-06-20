import React from "react";
import { EmptyState } from "../../components/ui/EmptyState";

export interface WeeklyStatsChartProps {
  completedCount: number | null;
  goalCount: number | null;
  dailyData: (boolean | number)[] | null;
}

const DAY_LABELS = ["M", "T", "W", "T", "F", "S", "S"] as const;

const styles: Record<string, React.CSSProperties> = {
  container: {
    background: "var(--color-bg-card)",
    borderRadius: "var(--radius-lg)",
    padding: "var(--spacing-lg)",
    border: "1px solid var(--color-border)",
    display: "flex",
    flexDirection: "column",
    height: "100%",
    boxSizing: "border-box",
  },
  header: {
    display: "flex",
    alignItems: "baseline",
    justifyContent: "space-between",
    marginBottom: "var(--spacing-md)",
  },
  title: {
    fontSize: "16px",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    margin: 0,
  },
  summary: {
    fontSize: "14px",
    color: "var(--color-text-secondary)",
    margin: 0,
  },
  chartArea: {
    display: "flex",
    alignItems: "flex-end",
    justifyContent: "space-between",
    gap: "var(--spacing-sm)",
    flex: 1,
    paddingTop: "var(--spacing-sm)",
  },
  barGroup: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    flex: 1,
    height: "100%",
    justifyContent: "flex-end",
  },
  bar: {
    width: "100%",
    maxWidth: "32px",
    borderRadius: "4px 4px 0 0",
    minHeight: "8px",
    transition: "height 0.2s ease",
  },
  barCompleted: {
    background: "var(--color-accent)",
  },
  barRemaining: {
    background: "transparent",
    border: "2px solid var(--color-border)",
  },
  dayLabel: {
    fontSize: "12px",
    color: "var(--color-text-secondary)",
    marginTop: "var(--spacing-xs)",
    textAlign: "center",
  },
};

export const WeeklyStatsChart: React.FC<WeeklyStatsChartProps> = ({
  completedCount,
  goalCount,
  dailyData,
}) => {
  const isDataUnavailable =
    completedCount == null || goalCount == null || dailyData == null;

  if (isDataUnavailable) {
    return (
      <div style={styles.container} data-testid="weekly-stats-chart">
        <EmptyState
          icon="📊"
          title="Weekly stats not available"
          subtitle="Data will appear once tracking is active"
        />
      </div>
    );
  }

  return (
    <div style={styles.container} data-testid="weekly-stats-chart">
      <div style={styles.header}>
        <h3 style={styles.title}>This Week</h3>
        <p style={styles.summary}>
          {completedCount} of {goalCount}
        </p>
      </div>

      <div
        style={styles.chartArea}
        role="img"
        aria-label={`Weekly progress: ${completedCount} of ${goalCount} workouts completed`}
      >
        {DAY_LABELS.map((label, index) => {
          const value = dailyData[index];
          const isCompleted = Boolean(value);
          const barHeight = isCompleted ? "100%" : "40%";

          return (
            <div key={index} style={styles.barGroup}>
              <div
                style={{
                  ...styles.bar,
                  ...(isCompleted ? styles.barCompleted : styles.barRemaining),
                  height: barHeight,
                }}
                data-testid={`bar-${index}`}
                aria-label={`${label}: ${isCompleted ? "completed" : "remaining"}`}
              />
              <span style={styles.dayLabel}>{label}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default WeeklyStatsChart;
