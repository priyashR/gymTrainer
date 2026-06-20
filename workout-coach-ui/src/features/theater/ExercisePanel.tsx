import React from "react";
import { ExerciseRow } from "./ExerciseRow";

export interface ExercisePanelExercise {
  id: string;
  name: string;
  recommendation: string;
  isCompleted: boolean;
}

export interface TimerConfig {
  timeCap: string;
  description: string;
}

export interface ExercisePanelProps {
  /** Array of exercises to render in the panel */
  exercises: ExercisePanelExercise[];
  /** Index of the currently focused/active exercise */
  currentExerciseIndex: number;
  /** Section type determining layout variant (e.g. "Strength", "AMRAP", "ForTime", "EMOM") */
  sectionType: string;
  /** Optional timer configuration for CrossFit-style sections */
  timerConfig?: TimerConfig;
  /** Callback when an exercise completion checkbox is toggled */
  onExerciseCheck: (exerciseId: string) => void;
  /** Callback when the Move button is pressed on an exercise */
  onExerciseMove: (exerciseId: string) => void;
}

const CROSSFIT_SECTION_TYPES = ["AMRAP", "ForTime", "EMOM"];

const panelStyle: React.CSSProperties = {
  overflowY: "auto",
  flex: 1,
  display: "flex",
  flexDirection: "column",
  gap: "var(--spacing-sm)",
  padding: "var(--spacing-md)",
};

const amrapCardStyle: React.CSSProperties = {
  background: "rgba(255, 112, 67, 0.1)",
  border: "1px solid var(--color-crossfit)",
  borderRadius: "var(--radius-md)",
  padding: "var(--spacing-md)",
  marginBottom: "var(--spacing-sm)",
};

const amrapTitleStyle: React.CSSProperties = {
  fontSize: "18px",
  fontWeight: 700,
  color: "var(--color-crossfit)",
  marginBottom: "var(--spacing-xs)",
};

const amrapDescriptionStyle: React.CSSProperties = {
  fontSize: "14px",
  fontWeight: 400,
  color: "var(--color-text-secondary)",
};

/**
 * Scrollable exercise list panel for Theater Mode.
 * Renders ExerciseRow components for each exercise and conditionally
 * displays an AMRAP/CrossFit info card at the top for CrossFit section types.
 *
 * Validates: Requirements 4.4, 4.7
 */
export function ExercisePanel({
  exercises,
  currentExerciseIndex,
  sectionType,
  timerConfig,
  onExerciseCheck,
  onExerciseMove,
}: ExercisePanelProps) {
  const isCrossFitSection = CROSSFIT_SECTION_TYPES.includes(sectionType);

  return (
    <div style={panelStyle} role="list" data-testid="exercise-panel">
      {isCrossFitSection && timerConfig && (
        <div
          style={amrapCardStyle}
          role="region"
          aria-label={`${sectionType} information`}
          data-testid="amrap-info-card"
        >
          <div style={amrapTitleStyle}>
            {sectionType} — {timerConfig.timeCap}
          </div>
          <div style={amrapDescriptionStyle}>{timerConfig.description}</div>
        </div>
      )}

      {exercises.map((exercise, index) => (
        <ExerciseRow
          key={exercise.id}
          exercise={{ id: exercise.id, name: exercise.name }}
          recommendation={exercise.recommendation}
          isActive={index === currentExerciseIndex}
          isCompleted={exercise.isCompleted}
          onCheck={onExerciseCheck}
          onMove={onExerciseMove}
        />
      ))}
    </div>
  );
}
