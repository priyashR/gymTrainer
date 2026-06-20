import React from "react";

export interface Exercise {
  id: string;
  name: string;
}

export interface ExerciseRowProps {
  /** The exercise object containing at minimum an id and name */
  exercise: Exercise;
  /** Prescription text (e.g. "4 × 6 @ 120kg · RPE 8") */
  recommendation: string;
  /** Whether this exercise is the currently focused/active one */
  isActive: boolean;
  /** Whether this exercise has been marked as completed */
  isCompleted: boolean;
  /** Callback when the completion checkbox is toggled */
  onCheck: (exerciseId: string) => void;
  /** Callback when the Move button is pressed (advance focus to this exercise) */
  onMove: (exerciseId: string) => void;
}

const rowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "var(--spacing-sm)",
  padding: "var(--spacing-sm) var(--spacing-md)",
  borderRadius: "var(--radius-sm)",
  transition: "background 0.15s, opacity 0.15s",
  position: "relative",
};

const activeRowStyle: React.CSSProperties = {
  ...rowStyle,
  background: "rgba(79, 195, 247, 0.08)",
  border: "1px solid var(--color-accent)",
};

const completedRowStyle: React.CSSProperties = {
  ...rowStyle,
  opacity: 0.5,
};

const checkboxContainerStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  minWidth: "var(--tap-target-preferred)",
  minHeight: "var(--tap-target-preferred)",
  cursor: "pointer",
  flexShrink: 0,
};

const checkboxStyle: React.CSSProperties = {
  width: "28px",
  height: "28px",
  borderRadius: "var(--radius-sm)",
  border: "2px solid var(--color-border)",
  background: "transparent",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  cursor: "pointer",
  transition: "border-color 0.15s, background 0.15s",
};

const checkboxCheckedStyle: React.CSSProperties = {
  ...checkboxStyle,
  borderColor: "var(--color-success)",
  background: "var(--color-success)",
};

const contentStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "var(--spacing-xs)",
  flex: 1,
  minWidth: 0,
};

const exerciseNameStyle: React.CSSProperties = {
  fontSize: "22px",
  fontWeight: 600,
  color: "var(--color-text-primary)",
  lineHeight: 1.3,
};

const prescriptionStyle: React.CSSProperties = {
  fontSize: "18px",
  fontWeight: 400,
  color: "var(--color-text-secondary)",
  lineHeight: 1.3,
};

const moveButtonStyle: React.CSSProperties = {
  minWidth: "var(--tap-target-preferred)",
  minHeight: "var(--tap-target-preferred)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "var(--spacing-sm) var(--spacing-md)",
  border: "1px solid var(--color-accent)",
  borderRadius: "var(--radius-sm)",
  background: "transparent",
  color: "var(--color-accent)",
  fontSize: "0.875rem",
  fontWeight: 600,
  cursor: "pointer",
  transition: "background 0.15s, color 0.15s",
  flexShrink: 0,
};

/**
 * A single exercise row in the Theater Mode exercise list.
 * Displays a completion checkbox, exercise name, prescription text, and a Move button.
 *
 * Validates: Requirements 4.4, 4.5, 4.7, 4.11
 */
export function ExerciseRow({
  exercise,
  recommendation,
  isActive,
  isCompleted,
  onCheck,
  onMove,
}: ExerciseRowProps) {
  const computedRowStyle = isCompleted
    ? completedRowStyle
    : isActive
      ? activeRowStyle
      : rowStyle;

  return (
    <div
      style={computedRowStyle}
      role="listitem"
      aria-label={`Exercise: ${exercise.name}`}
      data-testid={`exercise-row-${exercise.id}`}
    >
      {/* Checkbox with 48px tap target */}
      <div
        style={checkboxContainerStyle}
        onClick={() => onCheck(exercise.id)}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            onCheck(exercise.id);
          }
        }}
        role="checkbox"
        aria-checked={isCompleted}
        aria-label={`Mark ${exercise.name} as ${isCompleted ? "incomplete" : "complete"}`}
        tabIndex={0}
      >
        <div style={isCompleted ? checkboxCheckedStyle : checkboxStyle}>
          {isCompleted && (
            <svg
              width="16"
              height="16"
              viewBox="0 0 16 16"
              fill="none"
              aria-hidden="true"
            >
              <path
                d="M3.5 8.5L6.5 11.5L12.5 5.5"
                stroke="white"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          )}
        </div>
      </div>

      {/* Exercise name and prescription text */}
      <div style={contentStyle}>
        <span style={exerciseNameStyle}>{exercise.name}</span>
        <span style={prescriptionStyle}>{recommendation}</span>
      </div>

      {/* Move button */}
      <button
        style={moveButtonStyle}
        onClick={() => onMove(exercise.id)}
        aria-label={`Move to ${exercise.name}`}
        type="button"
      >
        Move
      </button>
    </div>
  );
}
