import { useCallback, useState } from "react";
import { useParams } from "react-router-dom";
import { useSession } from "../../hooks/useSession";
import { useRecommendations } from "../../hooks/useRecommendations";
import { SectionNavigator } from "./SectionNavigator";
import { SectionHeader } from "./SectionHeader";
import { ElapsedTimer } from "./ElapsedTimer";
import { TimerDisplay } from "./TimerDisplay";
import { RoundCounter } from "./RoundCounter";
import { ExerciseChecklist } from "./ExerciseChecklist";
import { CrossFitScoreForm } from "./CrossFitScoreForm";
import { RestTimerOverlay } from "./RestTimerOverlay";
import { NextUpIndicator } from "./NextUpIndicator";
import { SessionControls } from "./SessionControls";
import { FinishWorkoutPrompt } from "./FinishWorkoutPrompt";
import type { SectionProgress, LogSetRequest, LogCrossFitScoreRequest } from "../../types/session";

const pageStyle: React.CSSProperties = {
  maxWidth: 700,
  margin: "0 auto",
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column",
  background: "#fff",
};

const headerAreaStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  padding: "0 1rem",
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
): Array<{ name: string; sets?: number; reps?: number | string; weight?: number | string; notes?: string; restSeconds?: number }> {
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
 * Extracts section metadata from the workout snapshot.
 */
function getSectionMeta(workoutSnapshot: unknown, sectionIndex: number) {
  try {
    const snapshot = workoutSnapshot as {
      sections?: Array<{
        name?: string;
        formatDescriptor?: string;
        timeCapSeconds?: number;
        timerConfig?: {
          durationSeconds?: number;
        };
      }>;
    };
    const section = snapshot?.sections?.[sectionIndex];
    return {
      formatDescriptor: section?.formatDescriptor,
      timeCapSeconds: section?.timeCapSeconds ?? section?.timerConfig?.durationSeconds,
    };
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

/**
 * Determines if a section type supports CrossFit scoring.
 */
function isScoredSection(sectionType: string): boolean {
  return sectionType === "AMRAP" || sectionType === "EMOM" || sectionType === "FOR_TIME";
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
    logSet,
    logCrossFitScore,
  } = useSession(sessionId ?? "");

  const {
    recommendations,
    isLoading: recommendationsLoading,
    error: recommendationsError,
  } = useRecommendations(sessionId ?? "", session?.currentSectionIndex ?? 0);

  const [restTimerDuration, setRestTimerDuration] = useState<number | null>(null);

  const handleRestTimerStart = useCallback((restSeconds: number) => {
    setRestTimerDuration(restSeconds);
  }, []);

  const handleRestTimerDismiss = useCallback(() => {
    setRestTimerDuration(null);
  }, []);

  const handleLogSet = useCallback(
    async (request: LogSetRequest): Promise<void> => {
      await logSet(request);
    },
    [logSet]
  );

  const handleLogCrossFitScore = useCallback(
    async (request: LogCrossFitScoreRequest): Promise<void> => {
      await logCrossFitScore(request);
    },
    [logCrossFitScore]
  );

  const handleRoundCountChange = useCallback(
    async (newCount: number) => {
      // Round count is persisted via the logCrossFitScore or a dedicated endpoint.
      // For now, we update optimistically via the score form.
      // The RoundCounter state is managed by the section's roundCount field.
      // In a full implementation, this would call a dedicated API.
      // For the MVP, the round count is stored locally and submitted with the final score.
      // We'll use logCrossFitScore to persist the round count as part of the score.
      void newCount; // Round count changes are handled locally until score submission
    },
    []
  );

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

  const { currentSectionIndex, sectionProgresses, workoutSnapshot, status, startedAt, pausedAt } =
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
  const sectionMeta = getSectionMeta(workoutSnapshot, currentSectionIndex);

  const showRoundCounter =
    currentSection && currentSection.sectionType === "AMRAP" && !isCompleted;
  const showCrossFitScoreForm =
    currentSection &&
    isScoredSection(currentSection.sectionType) &&
    !isCompleted &&
    currentSection.completed;

  return (
    <div style={pageStyle}>
      {/* Section Navigator */}
      <SectionNavigator
        currentSectionIndex={currentSectionIndex}
        sectionProgresses={sectionProgresses}
        onAdvanceSection={advanceSection}
      />

      {/* Header area with Elapsed Timer */}
      {!isCompleted && (
        <div style={headerAreaStyle}>
          <ElapsedTimer
            startedAt={startedAt}
            isPaused={isPaused}
            pausedAt={pausedAt}
          />
        </div>
      )}

      {/* Section Header */}
      {currentSection && (
        <SectionHeader
          sectionName={currentSection.sectionName}
          sectionType={currentSection.sectionType}
          timeCapSeconds={sectionMeta.timeCapSeconds}
          formatDescriptor={sectionMeta.formatDescriptor}
        />
      )}

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

        {/* Round Counter for AMRAP sections */}
        {showRoundCounter && (
          <RoundCounter
            roundCount={currentSection.roundCount}
            onRoundCountChange={handleRoundCountChange}
            disabled={isPaused}
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
            sectionType={currentSection.sectionType}
            sessionStatus={status}
            onCompleteExercise={completeExercise}
            onRestTimerStart={handleRestTimerStart}
            onLogSet={handleLogSet}
            recommendations={recommendations}
            recommendationsLoading={recommendationsLoading}
            recommendationsError={!!recommendationsError}
          />
        )}

        {/* CrossFit Score Form — shown when section is scored and completed */}
        {showCrossFitScoreForm && (
          <CrossFitScoreForm
            sectionIndex={currentSectionIndex}
            sectionType={currentSection.sectionType}
            sessionStatus={status}
            onLogCrossFitScore={handleLogCrossFitScore}
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
          sectionProgresses={sectionProgresses}
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
