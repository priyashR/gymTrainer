import type { ExerciseLog, LogSetRequest, SessionStatus, SectionType } from "../../types/session";
import type { ExerciseRecommendationDto } from "../../types/recommendation";
import { ExercisePrescription } from "./ExercisePrescription";
import { RecommendationBadge } from "./RecommendationBadge";
import { SetLogForm } from "./SetLogForm";
import { SetLogList } from "./SetLogList";

interface ExerciseDefinition {
  name: string;
  sets?: number;
  reps?: number | string;
  weight?: number | string;
  notes?: string;
  restSeconds?: number;
}

interface ExerciseChecklistProps {
  exerciseLogs: ExerciseLog[];
  /** Exercise definitions from the workout snapshot for sets/reps info */
  exerciseDefinitions: ExerciseDefinition[];
  sectionIndex: number;
  sectionType: SectionType;
  sessionStatus: SessionStatus;
  onCompleteExercise: (sectionIndex: number, exerciseIndex: number) => void;
  /** Called when an exercise is checked off, passing the rest duration */
  onRestTimerStart: (restSeconds: number) => void;
  /** Called to log a strength set */
  onLogSet: (request: LogSetRequest) => Promise<void>;
  /** Recommendations for exercises in the current section */
  recommendations?: ExerciseRecommendationDto[];
  /** Whether recommendations are currently being loaded */
  recommendationsLoading?: boolean;
  /** Whether recommendations failed to load (hides badge area) */
  recommendationsError?: boolean;
}

const listStyle: React.CSSProperties = {
  listStyle: "none",
  padding: 0,
  margin: 0,
  display: "flex",
  flexDirection: "column",
  gap: "0.75rem",
};

const itemStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "0.25rem",
  padding: "0.75rem 1rem",
  border: "1px solid #e0e0e0",
  borderRadius: 8,
  background: "#fff",
};

const completedItemStyle: React.CSSProperties = {
  ...itemStyle,
  background: "#f1f8e9",
  borderColor: "#c5e1a5",
};

const checkRowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "0.75rem",
};

const checkboxStyle: React.CSSProperties = {
  width: 22,
  height: 22,
  cursor: "pointer",
  accentColor: "#388e3c",
};

const nameStyle: React.CSSProperties = {
  flex: 1,
  fontSize: "1rem",
  fontWeight: 500,
};

const completedNameStyle: React.CSSProperties = {
  ...nameStyle,
  textDecoration: "line-through",
  color: "#888",
};

const DEFAULT_REST_SECONDS = 60;

export function ExerciseChecklist({
  exerciseLogs,
  exerciseDefinitions,
  sectionIndex,
  sectionType,
  sessionStatus,
  onCompleteExercise,
  onRestTimerStart,
  onLogSet,
  recommendations = [],
  recommendationsLoading = false,
  recommendationsError = false,
}: ExerciseChecklistProps) {
  const isStrength = sectionType === "STRENGTH";

  const handleCheck = (exerciseIndex: number) => {
    const log = exerciseLogs[exerciseIndex];
    if (log?.completed) return; // Already completed — idempotent

    onCompleteExercise(sectionIndex, exerciseIndex);

    // Trigger rest timer with the exercise's configured rest duration
    const def = exerciseDefinitions[exerciseIndex];
    const restSeconds = def?.restSeconds ?? DEFAULT_REST_SECONDS;
    onRestTimerStart(restSeconds);
  };

  return (
    <ul style={listStyle} aria-label="Exercise checklist">
      {exerciseLogs.map((log, index) => {
        const def = exerciseDefinitions[index];

        return (
          <li
            key={log.exerciseIndex}
            style={log.completed ? completedItemStyle : itemStyle}
          >
            {/* Recommendation Badge — hidden on error, positioned above set-logging */}
            {!recommendationsError && (() => {
              const rec = recommendations.find(
                (r) => r.exerciseIndex === index
              );
              return (
                <RecommendationBadge
                  prescribedWeight={rec?.prescribedWeight ?? null}
                  prescribedReps={rec?.prescribedReps ?? null}
                  prescribedSets={rec?.prescribedSets ?? null}
                  isLoading={recommendationsLoading}
                />
              );
            })()}

            {/* Prescription display for STRENGTH sections */}
            {isStrength && def && (
              <ExercisePrescription
                name={def.name}
                sets={def.sets}
                reps={def.reps}
                weight={def.weight}
                notes={def.notes}
              />
            )}

            {/* Checkbox row */}
            <div style={checkRowStyle}>
              <input
                type="checkbox"
                style={checkboxStyle}
                checked={log.completed}
                onChange={() => handleCheck(index)}
                disabled={log.completed}
                aria-label={`Mark ${log.exerciseName} as complete`}
              />
              <span style={log.completed ? completedNameStyle : nameStyle}>
                {log.exerciseName}
                {log.completed && " ✓"}
              </span>
            </div>

            {/* Set logging for STRENGTH sections */}
            {isStrength && (
              <>
                <SetLogList setLogs={log.setLogs} />
                <SetLogForm
                  sectionIndex={sectionIndex}
                  exerciseIndex={log.exerciseIndex}
                  sessionStatus={sessionStatus}
                  onLogSet={onLogSet}
                />
              </>
            )}
          </li>
        );
      })}
    </ul>
  );
}
