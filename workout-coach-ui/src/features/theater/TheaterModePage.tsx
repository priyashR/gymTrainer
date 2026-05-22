import { useCallback, useState } from "react";
import { useParams } from "react-router-dom";
import { useSession } from "../../hooks/useSession";
import { SectionNavigator } from "./SectionNavigator";
import { TimerDisplay } from "./TimerDisplay";
import { ExerciseChecklist } from "./ExerciseChecklist";
import { RestTimerOverlay } from "./RestTimerOverlay";
import { NextUpIndicator } from "./NextUpIndicator";
import { SessionControls } from "./SessionControls";
import { FinishWorkoutPrompt } from "./FinishWorkoutPrompt";
import type { SectionProgress } from "../../types/session";

const pageStyle: React.CSSProperties = {
  maxWidth: 700,
  margin: "0 auto",
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column",
  background: "#fff",
};

const mainContentStyle: React.CSSProperties = {
  flex: 1,
  display: "flex",
  flexDirection: "column",
  gap: "1rem",
  padding: "1rem",
};

const loadingStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  minHeight: "100vh",
  fontSize: "1.125rem",
  color: "#666",
};

const errorStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  minHeight: "100vh",
  gap: "1rem",
  padding: "2rem",
  textAlign: "center",
};

/**
 * Extracts exercise definitions from the workout snapshot for the given section.
 * The snapshot structure is expected to follow the Workout Creator Service format.
 */
function getExerciseDefinitions(
  workoutSnapshot: unknown,
  sectionIndex: number
): Array<{ name: string; sets?: number; reps?: number | string; restSeconds?: number }> {
  try {
    const snapshot = workoutSnapshot as {
      sections?: Array<{
        exercises?: Array<{
          name?: string;
          sets?: number;
          reps?: number | string;
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
          workSeconds?: number;
          restSeconds?: number;
          rounds?: number;
        };
      }>;
    };
    return snapshot?.sections?.[sectionIndex]?.timerConfig ?? {};
  } catch {
    return {};
  }
}

/**
 * Checks if all exercises in all sections are complete.
 */
function isAllComplete(sectionProgresses: SectionProgress[]): boolean {
  return sectionProgresses.every((sp) =>
    sp.exerciseLogs.every((log) => log.completed)
  );
}

export function TheaterModePage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const {
    session,
    loading,
    error,
    completeExercise,
    advanceSection,
    pauseSession,
    resumeSession,
    endSession,
  } = useSession(sessionId ?? "");

  const [restTimerDuration, setRestTimerDuration] = useState<number | null>(null);

  const handleRestTimerStart = useCallback((restSeconds: number) => {
    setRestTimerDuration(restSeconds);
  }, []);

  const handleRestTimerDismiss = useCallback(() => {
    setRestTimerDuration(null);
  }, []);

  // Loading state
  if (loading) {
    return (
      <div style={loadingStyle} aria-busy="true">
        Loading session…
      </div>
    );
  }

  // Error state
  if (error || !session) {
    return (
      <div style={errorStyle} role="alert">
        <h2 style={{ margin: 0, color: "#c62828" }}>Unable to load session</h2>
        <p style={{ color: "#666" }}>{error ?? "Session not found"}</p>
      </div>
    );
  }

  const { currentSectionIndex, sectionProgresses, workoutSnapshot, status } =
    session;
  const currentSection = sectionProgresses[currentSectionIndex];
  const isPaused = status === "PAUSED";
  const isCompleted = status === "COMPLETED";
  const allComplete = isAllComplete(sectionProgresses);

  const exerciseDefinitions = getExerciseDefinitions(
    workoutSnapshot,
    currentSectionIndex
  );
  const timerConfig = getTimerConfig(workoutSnapshot, currentSectionIndex);

  return (
    <div style={pageStyle}>
      {/* Section Navigator */}
      <SectionNavigator
        currentSectionIndex={currentSectionIndex}
        sectionProgresses={sectionProgresses}
        onAdvanceSection={advanceSection}
      />

      <main style={mainContentStyle}>
        {/* Timer Display */}
        {currentSection && !isCompleted && (
          <TimerDisplay
            sectionType={currentSection.sectionType}
            durationSeconds={timerConfig.durationSeconds}
            workSeconds={timerConfig.workSeconds}
            restSeconds={timerConfig.restSeconds}
            rounds={timerConfig.rounds}
            isPaused={isPaused}
          />
        )}

        {/* Next Up Indicator */}
        <NextUpIndicator
          currentSectionIndex={currentSectionIndex}
          sectionProgresses={sectionProgresses}
        />

        {/* Exercise Checklist */}
        {currentSection && (
          <ExerciseChecklist
            exerciseLogs={currentSection.exerciseLogs}
            exerciseDefinitions={exerciseDefinitions}
            sectionIndex={currentSectionIndex}
            onCompleteExercise={completeExercise}
            onRestTimerStart={handleRestTimerStart}
          />
        )}

        {/* Finish Workout Prompt — shown when all exercises are complete */}
        {allComplete && !isCompleted && (
          <FinishWorkoutPrompt onFinish={endSession} />
        )}
      </main>

      {/* Session Controls */}
      {!isCompleted && (
        <SessionControls
          isPaused={isPaused}
          onPause={pauseSession}
          onResume={resumeSession}
          onEnd={endSession}
        />
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
