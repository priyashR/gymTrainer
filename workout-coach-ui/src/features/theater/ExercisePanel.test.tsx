import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ExercisePanel, ExercisePanelExercise } from "./ExercisePanel";

const mockExercises: ExercisePanelExercise[] = [
  {
    id: "ex-1",
    name: "Back Squat",
    recommendation: "4 × 6 @ 120kg · RPE 8",
    isCompleted: false,
  },
  {
    id: "ex-2",
    name: "Romanian Deadlift",
    recommendation: "3 × 10 @ 80kg · RPE 7",
    isCompleted: false,
  },
  {
    id: "ex-3",
    name: "Hip Thrust",
    recommendation: "3 × 12 @ 100kg",
    isCompleted: true,
  },
];

const baseProps = {
  exercises: mockExercises,
  currentExerciseIndex: 0,
  sectionType: "Strength",
  onExerciseCheck: vi.fn(),
  onExerciseMove: vi.fn(),
};

describe("ExercisePanel", () => {
  it("renders a scrollable list container with role='list'", () => {
    render(<ExercisePanel {...baseProps} />);
    const panel = screen.getByRole("list");
    expect(panel).toBeInTheDocument();
  });

  it("renders all exercises as ExerciseRow components", () => {
    render(<ExercisePanel {...baseProps} />);
    expect(screen.getByText("Back Squat")).toBeInTheDocument();
    expect(screen.getByText("Romanian Deadlift")).toBeInTheDocument();
    expect(screen.getByText("Hip Thrust")).toBeInTheDocument();
  });

  it("renders prescription text for each exercise", () => {
    render(<ExercisePanel {...baseProps} />);
    expect(screen.getByText("4 × 6 @ 120kg · RPE 8")).toBeInTheDocument();
    expect(screen.getByText("3 × 10 @ 80kg · RPE 7")).toBeInTheDocument();
    expect(screen.getByText("3 × 12 @ 100kg")).toBeInTheDocument();
  });

  it("marks the exercise at currentExerciseIndex as active", () => {
    const { container } = render(
      <ExercisePanel {...baseProps} currentExerciseIndex={1} />
    );
    const activeRow = container.querySelector(
      '[data-testid="exercise-row-ex-2"]'
    );
    expect(activeRow).toHaveStyle({ border: "1px solid var(--color-accent)" });
  });

  it("calls onExerciseCheck when a checkbox is clicked", async () => {
    const onExerciseCheck = vi.fn();
    render(
      <ExercisePanel {...baseProps} onExerciseCheck={onExerciseCheck} />
    );
    const checkboxes = screen.getAllByRole("checkbox");
    await userEvent.click(checkboxes[1]);
    expect(onExerciseCheck).toHaveBeenCalledWith("ex-2");
  });

  it("calls onExerciseMove when a Move button is clicked", async () => {
    const onExerciseMove = vi.fn();
    render(
      <ExercisePanel {...baseProps} onExerciseMove={onExerciseMove} />
    );
    const moveBtn = screen.getByRole("button", {
      name: /move to romanian deadlift/i,
    });
    await userEvent.click(moveBtn);
    expect(onExerciseMove).toHaveBeenCalledWith("ex-2");
  });

  it("does NOT render AMRAPInfoCard for Strength section type", () => {
    render(<ExercisePanel {...baseProps} sectionType="Strength" />);
    expect(screen.queryByTestId("amrap-info-card")).not.toBeInTheDocument();
  });

  it("renders AMRAPInfoCard for AMRAP section type with timerConfig", () => {
    render(
      <ExercisePanel
        {...baseProps}
        sectionType="AMRAP"
        timerConfig={{ timeCap: "12:00", description: "12 min time cap" }}
      />
    );
    const card = screen.getByTestId("amrap-info-card");
    expect(card).toBeInTheDocument();
    expect(screen.getByText("AMRAP — 12:00")).toBeInTheDocument();
    expect(screen.getByText("12 min time cap")).toBeInTheDocument();
  });

  it("renders AMRAPInfoCard for ForTime section type with timerConfig", () => {
    render(
      <ExercisePanel
        {...baseProps}
        sectionType="ForTime"
        timerConfig={{ timeCap: "20:00", description: "Complete as fast as possible" }}
      />
    );
    expect(screen.getByTestId("amrap-info-card")).toBeInTheDocument();
    expect(screen.getByText("ForTime — 20:00")).toBeInTheDocument();
  });

  it("renders AMRAPInfoCard for EMOM section type with timerConfig", () => {
    render(
      <ExercisePanel
        {...baseProps}
        sectionType="EMOM"
        timerConfig={{ timeCap: "10:00", description: "Every minute on the minute" }}
      />
    );
    expect(screen.getByTestId("amrap-info-card")).toBeInTheDocument();
    expect(screen.getByText("EMOM — 10:00")).toBeInTheDocument();
  });

  it("does NOT render AMRAPInfoCard for CrossFit section without timerConfig", () => {
    render(
      <ExercisePanel {...baseProps} sectionType="AMRAP" timerConfig={undefined} />
    );
    expect(screen.queryByTestId("amrap-info-card")).not.toBeInTheDocument();
  });

  it("applies overflow-y auto for scrollable content", () => {
    render(<ExercisePanel {...baseProps} />);
    const panel = screen.getByTestId("exercise-panel");
    expect(panel).toHaveStyle({ overflowY: "auto" });
  });
});
