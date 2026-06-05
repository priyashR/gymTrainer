import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ExerciseChecklist } from "../ExerciseChecklist";
import type { ExerciseLog } from "../../../types/session";

const defaultProps = {
  sectionType: "STRENGTH" as const,
  sessionStatus: "IN_PROGRESS" as const,
  onLogSet: vi.fn(),
};

const exerciseDefinitions = [
  { name: "Back Squat", sets: 4, reps: 6, restSeconds: 90 },
  { name: "Bench Press", sets: 3, reps: 8, restSeconds: 60 },
  { name: "Deadlift", sets: 5, reps: 3, restSeconds: 120 },
];

function makeExerciseLogs(completedIndices: number[] = []): ExerciseLog[] {
  return exerciseDefinitions.map((def, i) => ({
    exerciseIndex: i,
    exerciseName: def.name,
    completed: completedIndices.includes(i),
    completedAt: completedIndices.includes(i) ? "2026-01-15T10:30:00Z" : null,
    setLogs: [],
  }));
}

describe("ExerciseChecklist", () => {
  it("renders all exercises with checkboxes", () => {
    render(
      <ExerciseChecklist
        exerciseLogs={makeExerciseLogs()}
        exerciseDefinitions={exerciseDefinitions}
        sectionIndex={0}
        onCompleteExercise={vi.fn()}
        onRestTimerStart={vi.fn()}
        {...defaultProps}
      />
    );

    expect(screen.getByLabelText(/mark back squat as complete/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/mark bench press as complete/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/mark deadlift as complete/i)).toBeInTheDocument();
  });

  it("calls onCompleteExercise and onRestTimerStart when an exercise is checked", async () => {
    const user = userEvent.setup();
    const onComplete = vi.fn();
    const onRestStart = vi.fn();

    render(
      <ExerciseChecklist
        exerciseLogs={makeExerciseLogs()}
        exerciseDefinitions={exerciseDefinitions}
        sectionIndex={2}
        onCompleteExercise={onComplete}
        onRestTimerStart={onRestStart}
        {...defaultProps}
      />
    );

    await user.click(screen.getByLabelText(/mark bench press as complete/i));

    expect(onComplete).toHaveBeenCalledWith(2, 1);
    expect(onRestStart).toHaveBeenCalledWith(60);
  });

  it("disables checkbox for already completed exercises", () => {
    render(
      <ExerciseChecklist
        exerciseLogs={makeExerciseLogs([0, 1])}
        exerciseDefinitions={exerciseDefinitions}
        sectionIndex={0}
        onCompleteExercise={vi.fn()}
        onRestTimerStart={vi.fn()}
        {...defaultProps}
      />
    );

    const squat = screen.getByLabelText(/mark back squat as complete/i);
    const bench = screen.getByLabelText(/mark bench press as complete/i);
    const deadlift = screen.getByLabelText(/mark deadlift as complete/i);

    expect(squat).toBeDisabled();
    expect(bench).toBeDisabled();
    expect(deadlift).not.toBeDisabled();
  });

  it("shows visual completion state with checkmark for completed exercises", () => {
    render(
      <ExerciseChecklist
        exerciseLogs={makeExerciseLogs([0])}
        exerciseDefinitions={exerciseDefinitions}
        sectionIndex={0}
        onCompleteExercise={vi.fn()}
        onRestTimerStart={vi.fn()}
        {...defaultProps}
      />
    );

    expect(screen.getByText(/Back Squat ✓/)).toBeInTheDocument();
  });

  it("displays sets and reps information via ExercisePrescription for STRENGTH sections", () => {
    render(
      <ExerciseChecklist
        exerciseLogs={makeExerciseLogs()}
        exerciseDefinitions={exerciseDefinitions}
        sectionIndex={0}
        onCompleteExercise={vi.fn()}
        onRestTimerStart={vi.fn()}
        {...defaultProps}
      />
    );

    // ExercisePrescription renders sets and reps as separate badges
    expect(screen.getAllByText("4 sets").length).toBeGreaterThan(0);
    expect(screen.getAllByText("6 reps").length).toBeGreaterThan(0);
    expect(screen.getAllByText("3 sets").length).toBeGreaterThan(0);
    expect(screen.getAllByText("8 reps").length).toBeGreaterThan(0);
    expect(screen.getAllByText("5 sets").length).toBeGreaterThan(0);
    expect(screen.getAllByText("3 reps").length).toBeGreaterThan(0);
  });

  it("does not call handlers when clicking an already completed exercise", async () => {
    const user = userEvent.setup();
    const onComplete = vi.fn();
    const onRestStart = vi.fn();

    render(
      <ExerciseChecklist
        exerciseLogs={makeExerciseLogs([0])}
        exerciseDefinitions={exerciseDefinitions}
        sectionIndex={0}
        onCompleteExercise={onComplete}
        onRestTimerStart={onRestStart}
        {...defaultProps}
      />
    );

    // The checkbox is disabled, so clicking it should not trigger handlers
    await user.click(screen.getByLabelText(/mark back squat as complete/i));

    expect(onComplete).not.toHaveBeenCalled();
    expect(onRestStart).not.toHaveBeenCalled();
  });
});
