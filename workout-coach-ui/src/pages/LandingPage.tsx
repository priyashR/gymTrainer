import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import apiClient from "../lib/apiClient";
import { ResumeWorkoutCard } from "../features/landing/ResumeWorkoutCard";
import { WeeklyStatsChart } from "../features/landing/WeeklyStatsChart";
import { PerformanceDashboard } from "../features/landing/PerformanceDashboard";
import { QuickActionsGrid } from "../features/landing/QuickActionsGrid";
import { FABButton } from "../components/ui/FABButton";
import { useWeeklyStats } from "../hooks/useWeeklyStats";
import { usePerformanceData } from "../hooks/usePerformanceData";

interface ActiveSession {
  sessionId: string;
  workoutName: string;
  status: "active" | "paused";
  progress: number;
}

const styles: Record<string, React.CSSProperties> = {
  page: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gridTemplateRows: "auto auto 1fr",
    gap: "var(--spacing-md)",
    padding: "var(--spacing-md) var(--spacing-lg)",
    maxWidth: "1400px",
    margin: "0 auto",
    width: "100%",
    height: "100vh",
    overflow: "hidden",
  },
  pageMobile: {
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-md)",
    padding: "var(--spacing-md)",
    paddingBottom: "calc(var(--fab-size) + var(--spacing-xl) + var(--spacing-lg))",
    width: "100%",
    overflow: "auto",
  },
  topBar: {
    gridColumn: "1 / -1",
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: "var(--spacing-md)",
    flexWrap: "wrap",
  },
  heading: {
    fontSize: "22px",
    fontWeight: 700,
    color: "var(--color-text-primary)",
    margin: 0,
  },
  buttonRow: {
    display: "flex",
    gap: "var(--spacing-sm)",
    flexWrap: "wrap",
  },
  newWorkoutButton: {
    padding: "var(--spacing-sm) var(--spacing-lg)",
    borderRadius: "var(--radius-md)",
    border: "none",
    background: "var(--color-accent)",
    color: "var(--color-fab-text)",
    fontSize: "14px",
    fontWeight: 600,
    cursor: "pointer",
    minHeight: "var(--tap-target-min)",
    transition: "background 0.15s ease",
    whiteSpace: "nowrap",
  },
  logActivityButton: {
    padding: "var(--spacing-sm) var(--spacing-lg)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-accent)",
    background: "transparent",
    color: "var(--color-accent)",
    fontSize: "14px",
    fontWeight: 600,
    cursor: "pointer",
    minHeight: "var(--tap-target-min)",
    transition: "background 0.15s ease, border-color 0.15s ease",
    whiteSpace: "nowrap",
  },
  leftColumn: {
    display: "flex",
    flexDirection: "column",
    minHeight: 0,
    height: "100%",
  },
  rightColumn: {
    display: "flex",
    flexDirection: "column",
    minHeight: 0,
    height: "100%",
  },
  actionSheetOverlay: {
    position: "fixed",
    inset: 0,
    background: "rgba(0, 0, 0, 0.6)",
    display: "flex",
    alignItems: "flex-end",
    justifyContent: "center",
    zIndex: 1001,
  },
  actionSheet: {
    background: "var(--color-bg-surface)",
    borderRadius: "var(--radius-lg) var(--radius-lg) 0 0",
    padding: "var(--spacing-lg)",
    width: "100%",
    maxWidth: "500px",
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-sm)",
  },
  actionSheetTitle: {
    fontSize: "16px",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    margin: "0 0 var(--spacing-sm) 0",
    textAlign: "center",
  },
  actionSheetItem: {
    width: "100%",
    padding: "var(--spacing-md) var(--spacing-lg)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-border)",
    background: "var(--color-bg-card)",
    color: "var(--color-text-primary)",
    fontSize: "16px",
    fontWeight: 500,
    cursor: "pointer",
    textAlign: "left",
    minHeight: "var(--tap-target-preferred)",
    transition: "background 0.15s ease",
  },
  actionSheetCancel: {
    width: "100%",
    padding: "var(--spacing-md) var(--spacing-lg)",
    borderRadius: "var(--radius-md)",
    border: "none",
    background: "transparent",
    color: "var(--color-text-secondary)",
    fontSize: "16px",
    fontWeight: 500,
    cursor: "pointer",
    textAlign: "center",
    minHeight: "var(--tap-target-preferred)",
    marginTop: "var(--spacing-sm)",
  },
  sectionTitle: {
    fontSize: "16px",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    margin: 0,
  },
};

function useIsWideViewport(breakpoint = 900): boolean {
  const [isWide, setIsWide] = useState(() =>
    typeof window !== "undefined" ? window.innerWidth >= breakpoint : false
  );

  useEffect(() => {
    const mql = window.matchMedia(`(min-width: ${breakpoint}px)`);
    const handler = (e: MediaQueryListEvent) => setIsWide(e.matches);
    setIsWide(mql.matches);
    mql.addEventListener("change", handler);
    return () => mql.removeEventListener("change", handler);
  }, [breakpoint]);

  return isWide;
}

export const LandingPage: React.FC = () => {
  const navigate = useNavigate();
  const isWide = useIsWideViewport(900);
  const [activeSession, setActiveSession] = useState<ActiveSession | null>(null);
  const [actionSheetOpen, setActionSheetOpen] = useState(false);

  const { data: weeklyStats } = useWeeklyStats();
  const { topExercises, monthlyFrequency } = usePerformanceData();

  useEffect(() => {
    let cancelled = false;

    const fetchActiveSession = async () => {
      try {
        const res = await apiClient.get<{ id: string; status: string; workoutSnapshot?: { name?: string } }>("/sessions/active");
        if (!cancelled && res.data) {
          const data = res.data;
          setActiveSession({
            sessionId: data.id,
            workoutName: (data.workoutSnapshot as { name?: string })?.name ?? "Workout",
            status: data.status === "PAUSED" ? "paused" : "active",
            progress: 0,
          });
        }
      } catch {
        if (!cancelled) {
          setActiveSession(null);
        }
      }
    };

    const fetchActiveEnrollment = async () => {
      try {
        await apiClient.get("/enrollments/active");
      } catch {
        // no-op
      }
    };

    fetchActiveSession();
    fetchActiveEnrollment();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleResume = (sessionId: string) => {
    navigate(`/workout/session/${sessionId}`);
  };

  const handleFabClick = () => {
    setActionSheetOpen(true);
  };

  const handleActionSheetClose = () => {
    setActionSheetOpen(false);
  };

  const handleActionSheetOption = (route: string) => {
    setActionSheetOpen(false);
    navigate(route);
  };

  // --- Wide (tablet/laptop) layout: 2-column grid, no scroll ---
  if (isWide) {
    return (
      <main style={styles.page} data-testid="landing-page">
        {/* Top bar: heading + action buttons + resume card */}
        <div style={styles.topBar}>
          <h1 style={styles.heading}>HybridStrength</h1>
          <div style={styles.buttonRow}>
            {activeSession && (
              <button
                type="button"
                style={{ ...styles.newWorkoutButton, background: "var(--color-success)" }}
                onClick={() => handleResume(activeSession.sessionId)}
                data-testid="resume-workout-button"
                aria-label="Resume Workout"
              >
                ▶ Resume Workout
              </button>
            )}
            <button
              type="button"
              style={styles.newWorkoutButton}
              onClick={() => navigate("/vault/search")}
              data-testid="new-workout-button"
              aria-label="Start Workout"
            >
              Start Workout
            </button>
            <button
              type="button"
              style={styles.logActivityButton}
              onClick={() => navigate("/log-activity")}
              data-testid="log-activity-button"
              aria-label="Log Activity"
            >
              Log Activity
            </button>
          </div>
        </div>

        {/* Left column: Weekly stats */}
        <div style={styles.leftColumn}>
          <WeeklyStatsChart
            completedCount={weeklyStats?.completedCount ?? null}
            goalCount={weeklyStats?.goalCount ?? null}
            dailyData={weeklyStats?.dailyData ?? null}
          />
        </div>

        {/* Right column: Quick Actions */}
        <div style={styles.rightColumn}>
          <QuickActionsGrid />
        </div>

        {/* Bottom row: Performance dashboard (spans both columns) */}
        <div style={{ gridColumn: "1 / -1", minHeight: 0, display: "flex", flex: 1 }}>
          <PerformanceDashboard
            topExercises={topExercises}
            monthlyFrequency={monthlyFrequency}
          />
        </div>

        {/* FAB */}
        <FABButton onClick={handleFabClick} ariaLabel="Add new item" />

        {/* Action Sheet */}
        {actionSheetOpen && (
          <div
            style={styles.actionSheetOverlay}
            onClick={handleActionSheetClose}
            role="dialog"
            aria-modal="true"
            aria-label="Create options"
            data-testid="fab-action-sheet"
          >
            <div style={styles.actionSheet} onClick={(e) => e.stopPropagation()}>
              <h3 style={styles.actionSheetTitle}>Create</h3>
              <button type="button" style={styles.actionSheetItem} onClick={() => handleActionSheetOption("/programs/create")} data-testid="action-create-program">
                Create Program
              </button>
              <button type="button" style={styles.actionSheetItem} onClick={() => handleActionSheetOption("/manual-input")} data-testid="action-manual-json">
                Manual JSON Input
              </button>
              <button type="button" style={styles.actionSheetItem} onClick={() => handleActionSheetOption("/vault/search")} data-testid="action-start-from-vault">
                Start Workout from Vault
              </button>
              <button type="button" style={styles.actionSheetCancel} onClick={handleActionSheetClose} data-testid="action-sheet-cancel">
                Cancel
              </button>
            </div>
          </div>
        )}
      </main>
    );
  }

  // --- Narrow (mobile) layout: stacked, scrollable ---
  return (
    <main style={styles.pageMobile} data-testid="landing-page">
      <h1 style={styles.heading}>HybridStrength</h1>

      {activeSession && (
        <ResumeWorkoutCard
          sessionId={activeSession.sessionId}
          workoutName={activeSession.workoutName}
          status={activeSession.status}
          progress={activeSession.progress}
          onResume={handleResume}
        />
      )}

      <div style={styles.buttonRow}>
        <button
          type="button"
          style={styles.newWorkoutButton}
          onClick={() => navigate("/vault/search")}
          data-testid="new-workout-button"
          aria-label="Start Workout"
        >
          Start Workout
        </button>
        <button
          type="button"
          style={styles.logActivityButton}
          onClick={() => navigate("/log-activity")}
          data-testid="log-activity-button"
          aria-label="Log Activity"
        >
          Log Activity
        </button>
      </div>

      <WeeklyStatsChart
        completedCount={weeklyStats?.completedCount ?? null}
        goalCount={weeklyStats?.goalCount ?? null}
        dailyData={weeklyStats?.dailyData ?? null}
      />

      <section>
        <h2 style={styles.sectionTitle}>Quick Actions</h2>
        <div style={{ marginTop: "var(--spacing-sm)" }}>
          <QuickActionsGrid />
        </div>
      </section>

      <PerformanceDashboard
        topExercises={topExercises}
        monthlyFrequency={monthlyFrequency}
      />

      <FABButton onClick={handleFabClick} ariaLabel="Add new item" />

      {actionSheetOpen && (
        <div
          style={styles.actionSheetOverlay}
          onClick={handleActionSheetClose}
          role="dialog"
          aria-modal="true"
          aria-label="Create options"
          data-testid="fab-action-sheet"
        >
          <div style={styles.actionSheet} onClick={(e) => e.stopPropagation()}>
            <h3 style={styles.actionSheetTitle}>Create</h3>
            <button type="button" style={styles.actionSheetItem} onClick={() => handleActionSheetOption("/programs/create")} data-testid="action-create-program">
              Create Program
            </button>
            <button type="button" style={styles.actionSheetItem} onClick={() => handleActionSheetOption("/manual-input")} data-testid="action-manual-json">
              Manual JSON Input
            </button>
            <button type="button" style={styles.actionSheetItem} onClick={() => handleActionSheetOption("/vault/search")} data-testid="action-start-from-vault">
              Start Workout from Vault
            </button>
            <button type="button" style={styles.actionSheetCancel} onClick={handleActionSheetClose} data-testid="action-sheet-cancel">
              Cancel
            </button>
          </div>
        </div>
      )}
    </main>
  );
};

export default LandingPage;
