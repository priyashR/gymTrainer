import type { ExerciseLog } from "../../types/session";

interface ExerciseDefinition {
  name: string;
  sets?: number;
  reps?: number | string;
  restSeconds?: number;
}

interface ExerciseChecklistProps {
  exerciseLogs: ExerciseLog[];
  /** Exercise definitions from the workout snapshot for sets/reps info */
  exerciseDefinitions: ExerciseDefinition[];
  sectionIndex: number;
  onCompleteExercise: (sectionIndex: number, exerciseIndex: number) => void;
  /** Called when an exercise is checked off, passing the rest duration */
  onRestTimerStart: (restSeconds: number) => void;
}

const listStyle: React.CSSProperties = {
  listStyle: "none",
  padding: 0,
  margin: 0,
  display: "flex",
  flexDirection: "column",
  gap: "0.5rem",
};

const itemStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "0.75rem",
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

const detailStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  color: "#666",
};

const DEFAULT_REST_SECONDS = 60;

export function ExerciseChecklist({
  exerciseLogs,
  exerciseDefinitions,
  sectionIndex,
  onCompleteExercise,
  onRestTimerStart,
}: ExerciseChecklistProps) {
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
        const setsReps = def
          ? [def.sets && `${def.sets} sets`, def.reps && `${def.reps} reps`]
              .filter(Boolean)
              .join(" × ")
          : "";

        return (
          <li
            key={log.exerciseIndex}
            style={log.completed ? completedItemStyle : itemStyle}
          >
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
            {setsReps && <span style={detailStyle}>{setsReps}</span>}
          </li>
        );
      })}
    </ul>
  );
}
