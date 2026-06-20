import { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useSession } from "../../hooks/useSession";
import { useRecommendations } from "../../hooks/useRecommendations";
import { useWakeLock } from "../../hooks/useWakeLock";
import { TheaterHeader } from "./TheaterHeader";
import { ExercisePanel } from "./ExercisePanel";
import { LoggingPanel } from "./LoggingPanel";
import { RestTimerOverlay } from "./RestTimerOverlay";
import { FinishWorkoutPrompt } from "./FinishWorkoutPrompt";
import type { LoggedSet, CrossFitScoreData } from "./LoggingPanel";
import type { ExercisePanelExercise } from "./ExercisePanel";
import type { SectionType } from "../../types/session";

// --- Responsive layout hook ---

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

// --- Elapsed time formatting ---

function formatElapsedTime(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  const mm = minutes.toString().padStart(2, "0");
  const ss = seconds.toString().padStart(2, "0");

  if (hours > 0) {
    return `${hours}:${mm}:${ss}`;
  }
  return `${mm}:${ss}`;
}

// --- Helpers ---

/**
 * Extracts exercise definitions from the workout snapshot for the given section.
 */
function getExerciseDefinitions(
  workoutSnapshot: unknown,
  sectionIndex: number
): Array<{
  name: string;
  sets?: number;
  reps?: number | string;
  weight?: number | string;
  notes?: string;
  restSeconds?: number;
}> {
  try {
    const snapshot = workoutSnapshot as {
      sections?: Array<{
        exercises?: Array<{
          name?: string;
          sets?: number;
          reps?: number | string;
          weight?: number | string;
          notes?: string;
          restSeconds?: number;
        }>;
      }>;
    };
    const section = snapshot?.sections?.[sectionIndex];
    if (!section?.exercises) return [];
    return section.exercises.map((ex) => ({
      name: ex.name ?? "Exercise",
      sets: ex.sets,
      reps: ex.reps,
      weight: ex.weight,
      notes: ex.notes,
      restSeconds: ex.restSeconds,
    }));
  } catch {
    return [];
  }
}

/**
 * Extracts timer config from the workout snapshot for the given section.
 */
function getTimerConfig(workoutSnapshot: unknown, sectionIndex: number) {
  try {
    const snapshot = workoutSnapshot as {
      sections?: Array<{
        timerConfig?: {
          durationSeconds?: number;
        };
        timeCapSeconds?: number;
      }>;
    };
    const section = snapshot?.sections?.[sectionIndex];
    const timeCap = section?.timeCapSeconds ?? section?.timerConfig?.durationSeconds;
    return timeCap
      ? {
          timeCap: `${Math.floor(timeCap / 60)}:${(timeCap % 60).toString().padStart(2, "0")}`,
          description: `Time cap: ${Math.floor(timeCap / 60)} minutes`,
        }
      : undefined;
  } catch {
    return undefined;
  }
}

/**
 * Builds a prescription string from exercise definition.
 */
function buildPrescription(exerciseDef: {
  sets?: number;
  reps?: number | string;
  weight?: number | string;
}): string {
  const parts: string[] = [];
  if (exerciseDef.sets) parts.push(`${exerciseDef.sets}`);
  if (exerciseDef.reps) parts.push(`× ${exerciseDef.reps}`);
  if (exerciseDef.weight) parts.push(`@ ${exerciseDef.weight}kg`);
  return parts.length > 0 ? parts.join(" ") : "Complete as prescribed";
}

/**
 * Checks if all exercises in the current tier are complete.
 */
function isCurrentTierComplete(
  sectionProgresses: Array<{ exerciseLogs: Array<{ completed: boolean }> }>,
  tierIndex: number
): boolean {
  const tier = sectionProgresses[tierIndex];
  if (!tier) return false;
  return tier.exerciseLogs.every((log) => log.completed);
}

/**
 * Checks if all exercises across all tiers are complete.
 */
function isAllComplete(
  sectionProgresses: Array<{ exerciseLogs: Array<{ completed: boolean }> }>
): boolean {
  return sectionProgresses.every((sp) =>
    sp.exerciseLogs.every((log) => log.completed)
  );
}

/**
 * Determines the section type label for display.
 */
function getSectionTypeLabel(sectionType: SectionType): string {
  switch (sectionType) {
    case "STRENGTH":
      return "Strength";
    case "AMRAP":
      return "AMRAP";
    case "EMOM":
      return "EMOM";
    case "FOR_TIME":
      return "For Time";
    case "TABATA":
      return "Tabata";
    default:
      return sectionType;
  }
}

// --- Styles ---

const pageStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column",
  background: "var(--color-bg-primary)",
  color: "var(--color-text-primary)",
};

const mainGridWideStyle: React.CSSProperties = {
  flex: 1,
  display: "grid",
  gridTemplateColumns: "1fr 1fr",
  gap: "var(--spacing-md)",
  padding: "var(--spacing-md)",
  overflow: "hidden",
};

const mainGridNarrowStyle: React.CSSProperties = {
  flex: 1,
  display: "grid",
  gridTemplateColumns: "1fr",
  gap: "var(--spacing-md)",
  padding: "var(--spacing-md)",
  overflow: "auto",
};

const loadingStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  minHeight: "100vh",
  fontSize: "1.125rem",
  color: "var(--color-text-secondary)",
  background: "var(--color-bg-primary)",
};

const errorStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  minHeight: "100vh",
  gap: "var(--spacing-md)",
  padding: "var(--spacing-xl)",
  textAlign: "center",
  background: "var(--color-bg-primary)",
  color: "var(--color-text-primary)",
};

const finishOverlayStyle: React.CSSProperties = {
  padding: "var(--spacing-lg)",
  display: "flex",
  justifyContent: "center",
};

/**
 * TheaterModePage — tier-based tablet layout for active workout execution.
 *
 * Composes TheaterHeader (stopwatch + tier navigation), ExercisePanel (left),
 * and LoggingPanel (right) in a responsive CSS Grid layout.
 *
 * - ≥900px: side-by-side panels (exercise left, logging right)
 * - <900px: stacked vertically
 *
 * Integrates useWakeLock to prevent device sleep during active workout.
 *
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.10, 4.11, 4.12, 4.13, 6.4
 */
export function TheaterModePage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const isWide = useIsWideViewport(900);
  const { acquire, release } = useWakeLock();

  // --- Session and recommendations hooks (retained) ---
  const {
    session,
    loading,
    error,
    completeExercise,
    advanceSection,
    pauseSession,
    resumeSession,
    endSession,
    logSet,
  } = useSession(sessionId ?? "");

  // Expose pause/resume for session controls
  const isPaused = session?.status === "PAUSED";

  const handlePauseResume = useCallback(async () => {
    if (isPaused) {
      await resumeSession();
    } else {
      await pauseSession();
    }
  }, [isPaused, pauseSession, resumeSession]);

  const handleLeave = useCallback(() => {
    release();
    navigate("/");
  }, [release, navigate]);

  const currentSectionIndex = session?.currentSectionIndex ?? 0;

  const {
    recommendations,
  } = useRecommendations(sessionId ?? "", currentSectionIndex);

  // --- Tier navigation state ---
  const [currentTierIndex, setCurrentTierIndex] = useState(0);

  // Sync tier index with session's current section index
  useEffect(() => {
    if (session) {
      setCurrentTierIndex(session.currentSectionIndex);
    }
  }, [session?.currentSectionIndex]);

  // --- Current exercise index within the current tier ---
  const [currentExerciseIndex, setCurrentExerciseIndex] = useState(0);

  // Reset exercise index when tier changes
  useEffect(() => {
    setCurrentExerciseIndex(0);
  }, [currentTierIndex]);

  // --- Logged sets per exercise: Map<`${tierIndex}-${exerciseIndex}`, LoggedSet[]> ---
  const [loggedSetsMap, setLoggedSetsMap] = useState<Record<string, LoggedSet[]>>({});

  // --- Elapsed stopwatch ---
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (!session) return;

    const isPaused = session.status === "PAUSED";
    const isCompleted = session.status === "COMPLETED";

    if (isPaused || isCompleted) {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
      return;
    }

    // Calculate initial elapsed from startedAt
    const startedAt = new Date(session.startedAt).getTime();
    const now = Date.now();
    const initialElapsed = Math.floor((now - startedAt) / 1000);
    setElapsedSeconds(Math.max(0, initialElapsed));

    timerRef.current = setInterval(() => {
      const elapsed = Math.floor((Date.now() - startedAt) / 1000);
      setElapsedSeconds(Math.max(0, elapsed));
    }, 1000);

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [session?.startedAt, session?.status]);

  // --- Wake lock: acquire on mount, release on unmount/pause/finish ---
  useEffect(() => {
    acquire();
    return () => {
      release();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Release wake lock when session is paused or completed
  useEffect(() => {
    if (session?.status === "PAUSED" || session?.status === "COMPLETED") {
      release();
    } else if (session?.status === "IN_PROGRESS") {
      acquire();
    }
  }, [session?.status]); // eslint-disable-line react-hooks/exhaustive-deps

  // --- Rest timer state ---
  const [restTimerDuration, setRestTimerDuration] = useState<number | null>(null);

  // --- CrossFit rounds state ---
  const [roundsCompleted, setRoundsCompleted] = useState(0);

  // Sync rounds from session data
  useEffect(() => {
    if (session) {
      const currentSection = session.sectionProgresses[currentTierIndex];
      if (currentSection) {
        setRoundsCompleted(currentSection.roundCount);
      }
    }
  }, [session, currentTierIndex]);

  // --- Tier navigation handlers ---
  const totalTiers = session?.sectionProgresses.length ?? 0;
  const isFirstTier = currentTierIndex === 0;
  const isLastTier = currentTierIndex >= totalTiers - 1;

  const handlePrevTier = useCallback(() => {
    if (!isFirstTier) {
      const newIndex = currentTierIndex - 1;
      setCurrentTierIndex(newIndex);
      advanceSection(newIndex);
    }
  }, [isFirstTier, currentTierIndex, advanceSection]);

  const handleNextTier = useCallback(() => {
    if (!isLastTier) {
      const newIndex = currentTierIndex + 1;
      setCurrentTierIndex(newIndex);
      advanceSection(newIndex);
    }
  }, [isLastTier, currentTierIndex, advanceSection]);

  // --- Exercise check/move handlers ---
  const handleExerciseCheck = useCallback(
    (exerciseId: string) => {
      const exerciseIndex = parseInt(exerciseId, 10);
      if (!isNaN(exerciseIndex)) {
        completeExercise(currentTierIndex, exerciseIndex);
      }
    },
    [currentTierIndex, completeExercise]
  );

  const handleExerciseMove = useCallback(
    (exerciseId: string) => {
      const exerciseIndex = parseInt(exerciseId, 10);
      if (!isNaN(exerciseIndex)) {
        const currentSection = session?.sectionProgresses[currentTierIndex];
        if (currentSection) {
          const nextIndex = Math.min(
            exerciseIndex + 1,
            currentSection.exerciseLogs.length - 1
          );
          setCurrentExerciseIndex(nextIndex);

          // Start rest timer if the exercise definition has a restSeconds value
          const exerciseDefs = getExerciseDefinitions(
            session?.workoutSnapshot,
            currentTierIndex
          );
          const currentDef = exerciseDefs[exerciseIndex];
          if (currentDef?.restSeconds && currentDef.restSeconds > 0) {
            setRestTimerDuration(currentDef.restSeconds);
          }
        }
      }
    },
    [session, currentTierIndex]
  );

  // --- Logging handlers ---
  const handleLogSet = useCallback(
    (set: LoggedSet) => {
      const key = `${currentTierIndex}-${currentExerciseIndex}`;
      setLoggedSetsMap((prev) => ({
        ...prev,
        [key]: [...(prev[key] ?? []), set],
      }));

      // Persist to backend
      logSet({
        sectionIndex: currentTierIndex,
        exerciseIndex: currentExerciseIndex,
        weight: set.weight,
        repetitions: set.reps,
        rpe: set.rpe,
      });
    },
    [currentTierIndex, currentExerciseIndex, logSet]
  );

  const handleUpdateRounds = useCallback((newCount: number) => {
    setRoundsCompleted(newCount);
  }, []);

  const handleSubmitScore = useCallback(
    (_score: CrossFitScoreData) => {
      // Score submission handled by session hook
      void _score;
    },
    []
  );

  // --- Rest timer dismiss ---
  const handleRestTimerDismiss = useCallback(() => {
    setRestTimerDuration(null);
  }, []);

  // --- Finish workout handler ---
  const handleFinishWorkout = useCallback(async () => {
    await release();
    await endSession();
    navigate("/");
  }, [release, endSession, navigate]);

  // --- Loading state ---
  if (loading) {
    return (
      <div style={loadingStyle} aria-busy="true">
        Loading session…
      </div>
    );
  }

  // --- Error state ---
  if (error || !session) {
    return (
      <div style={errorStyle} role="alert">
        <h2 style={{ margin: 0, color: "var(--color-error)" }}>
          Unable to load session
        </h2>
        <p style={{ color: "var(--color-text-secondary)" }}>
          {error ?? "Session not found"}
        </p>
      </div>
    );
  }

  // --- Derive state from session ---
  const { sectionProgresses, workoutSnapshot, status } = session;
  const currentSection = sectionProgresses[currentTierIndex];
  const isCompleted = status === "COMPLETED";

  const exerciseDefinitions = getExerciseDefinitions(
    workoutSnapshot,
    currentTierIndex
  );
  const timerConfig = getTimerConfig(workoutSnapshot, currentTierIndex);

  const tierLabel = currentSection
    ? `Tier ${currentTierIndex + 1}: ${currentSection.sectionName}`
    : `Tier ${currentTierIndex + 1}`;
  const sectionType: SectionType = currentSection?.sectionType ?? "STRENGTH";
  const sectionTypeLabel = getSectionTypeLabel(sectionType);

  // Build exercises for ExercisePanel
  const exercises: ExercisePanelExercise[] = currentSection
    ? currentSection.exerciseLogs.map((log, index) => {
        const def = exerciseDefinitions[index];
        // Merge recommendations if available
        const rec = recommendations?.find(
          (r) => r.exerciseIndex === index
        );
        const recommendation =
          rec?.prescribedWeight && rec?.prescribedReps
            ? `${rec.prescribedSets ?? def?.sets ?? 3} × ${rec.prescribedReps} @ ${rec.prescribedWeight}kg`
            : def
              ? buildPrescription(def)
              : "Complete as prescribed";

        return {
          id: String(index),
          name: log.exerciseName,
          recommendation,
          isCompleted: log.completed,
        };
      })
    : [];

  // Current exercise info for LoggingPanel
  const currentExercise = exercises[currentExerciseIndex] ?? {
    id: "0",
    name: "Exercise",
    recommendation: "",
    isCompleted: false,
  };

  const currentLoggedSets =
    loggedSetsMap[`${currentTierIndex}-${currentExerciseIndex}`] ?? [];

  // Determine if finish prompt should show
  const allComplete = isAllComplete(sectionProgresses);
  const lastTierComplete =
    isLastTier && isCurrentTierComplete(sectionProgresses, currentTierIndex);
  const showFinishPrompt = (allComplete || lastTierComplete) && !isCompleted;

  return (
    <div style={pageStyle} data-testid="theater-mode-page">
      {/* Header with stopwatch and tier navigation */}
      {!isCompleted && (
        <TheaterHeader
          elapsedTime={formatElapsedTime(elapsedSeconds)}
          tierLabel={tierLabel}
          sectionType={sectionTypeLabel}
          isFirstTier={isFirstTier}
          isLastTier={isLastTier}
          isPaused={isPaused}
          onPrevTier={handlePrevTier}
          onNextTier={handleNextTier}
          onPauseResume={handlePauseResume}
          onEndWorkout={handleFinishWorkout}
          onLeave={handleLeave}
        />
      )}

      {/* Main content: CSS Grid layout */}
      <main style={isWide ? mainGridWideStyle : mainGridNarrowStyle}>
        {/* Left panel: Exercise list */}
        <ExercisePanel
          exercises={exercises}
          currentExerciseIndex={currentExerciseIndex}
          sectionType={sectionTypeLabel}
          timerConfig={timerConfig}
          onExerciseCheck={handleExerciseCheck}
          onExerciseMove={handleExerciseMove}
        />

        {/* Right panel: Logging inputs */}
        <LoggingPanel
          sectionType={sectionType}
          currentExercise={{
            name: currentExercise.name,
            recommendation: currentExercise.recommendation,
          }}
          loggedSets={currentLoggedSets}
          roundsCompleted={roundsCompleted}
          onLogSet={handleLogSet}
          onUpdateRounds={handleUpdateRounds}
          onSubmitScore={handleSubmitScore}
        />
      </main>

      {/* Finish workout prompt */}
      {showFinishPrompt && (
        <div style={finishOverlayStyle}>
          <FinishWorkoutPrompt onFinish={handleFinishWorkout} />
        </div>
      )}

      {/* Rest Timer Overlay */}
      {restTimerDuration !== null && (
        <RestTimerOverlay
          durationSeconds={restTimerDuration}
          onDismiss={handleRestTimerDismiss}
        />
      )}
    </div>
  );
}
