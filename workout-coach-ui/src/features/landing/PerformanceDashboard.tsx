import React from "react";
import { EmptyState } from "../../components/ui/EmptyState";

export interface TopExerciseEntry {
  exerciseName: string;
  maxWeight: number;
  unit: string;
}

export interface MonthlyFrequencyEntry {
  month: string;
  count: number;
}

export interface PerformanceDashboardProps {
  topExercises: TopExerciseEntry[] | null;
  monthlyFrequency: MonthlyFrequencyEntry[] | null;
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "var(--spacing-md)",
    minHeight: 0,
    flex: 1,
  },
  containerMobile: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-md)",
  },
  section: {
    background: "var(--color-bg-card)",
    borderRadius: "var(--radius-lg)",
    padding: "var(--spacing-md)",
    border: "1px solid var(--color-border)",
    display: "flex",
    flexDirection: "column",
    minHeight: 0,
    overflow: "hidden",
  },
  sectionTitle: {
    fontSize: "13px",
    fontWeight: 600,
    color: "var(--color-text-secondary)",
    margin: "0 0 var(--spacing-sm) 0",
    textTransform: "uppercase" as const,
    letterSpacing: "0.5px",
    flexShrink: 0,
  },
  scrollArea: {
    flex: 1,
    overflowY: "auto",
    minHeight: 0,
  },
  chartContent: {
    display: "flex",
    flexDirection: "column",
    gap: "2px",
  },
  exerciseRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "var(--spacing-xs) 0",
    borderBottom: "1px solid var(--color-border)",
  },
  exerciseName: {
    fontSize: "13px",
    color: "var(--color-text-primary)",
  },
  exerciseWeight: {
    fontSize: "13px",
    fontWeight: 600,
    color: "var(--color-accent)",
  },
  barRow: {
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-sm)",
    padding: "2px 0",
  },
  barLabel: {
    fontSize: "11px",
    color: "var(--color-text-secondary)",
    minWidth: "28px",
    textAlign: "right" as const,
  },
  barTrack: {
    flex: 1,
    height: "10px",
    background: "var(--color-bg-surface)",
    borderRadius: "5px",
    overflow: "hidden",
  },
  barFill: {
    height: "100%",
    background: "var(--color-accent)",
    borderRadius: "5px",
    transition: "width 0.3s ease",
  },
  barValue: {
    fontSize: "11px",
    color: "var(--color-text-secondary)",
    minWidth: "20px",
  },
};

export const PerformanceDashboard: React.FC<PerformanceDashboardProps> = ({
  topExercises,
  monthlyFrequency,
}) => {
  return (
    <div style={styles.container} data-testid="performance-dashboard">
      {/* Top 10 Exercises — left panel */}
      <div style={styles.section} data-testid="top-exercises-section">
        <h3 style={styles.sectionTitle}>Top 10 Exercises by Weight (12 months)</h3>
        {topExercises == null ? (
          <EmptyState
            icon="🏋️"
            title="Performance data coming soon"
            subtitle="Top exercise data will appear once the Progress Tracker is available"
          />
        ) : (
          <div style={styles.scrollArea}>
            <div style={styles.chartContent} role="list" aria-label="Top 10 exercises by weight">
              {topExercises.slice(0, 10).map((entry, index) => (
                <div key={index} style={styles.exerciseRow} role="listitem">
                  <span style={styles.exerciseName}>{entry.exerciseName}</span>
                  <span style={styles.exerciseWeight}>
                    {entry.maxWeight} {entry.unit}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Monthly Frequency — right panel */}
      <div style={styles.section} data-testid="monthly-frequency-section">
        <h3 style={styles.sectionTitle}>Workouts Per Month (12 months)</h3>
        {monthlyFrequency == null ? (
          <EmptyState
            icon="📅"
            title="Performance data coming soon"
            subtitle="Monthly workout data will appear once the Progress Tracker is available"
          />
        ) : (
          <div style={styles.scrollArea}>
            <div
              style={styles.chartContent}
              role="img"
              aria-label="Monthly workout frequency chart"
            >
              {monthlyFrequency.map((entry, index) => {
                const maxCount = Math.max(
                  ...monthlyFrequency.map((e) => e.count),
                  1
                );
                const widthPercent = (entry.count / maxCount) * 100;

                return (
                  <div key={index} style={styles.barRow}>
                    <span style={styles.barLabel}>{entry.month}</span>
                    <div style={styles.barTrack}>
                      <div
                        style={{
                          ...styles.barFill,
                          width: `${widthPercent}%`,
                        }}
                      />
                    </div>
                    <span style={styles.barValue}>{entry.count}</span>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default PerformanceDashboard;
