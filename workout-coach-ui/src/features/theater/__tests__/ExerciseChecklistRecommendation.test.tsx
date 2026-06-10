import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ExerciseChecklist } from "../ExerciseChecklist";
import type { ExerciseLog } from "../../../types/session";
import type { ExerciseRecommendationDto } from "../../../types/recommendation";

const defaultProps = {
  sectionType: "STRENGTH" as const,
  sessionStatus: "IN_PROGRESS" as const,
  onLogSet: vi.fn().mockResolvedValue(undefined),
  onCompleteExercise: vi.fn(),
  onRestTimerStart: vi.fn(),
  sectionIndex: 0,
};

const exerciseDefinitions = [
  { name: "Back Squat", sets: 4, reps: "8-10", weight: "80kg", restSeconds: 90 },
  { name: "Bench Press", sets: 3, reps: 8, weight: "60kg", restSeconds: 60 },
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

function makeRecommendations(): ExerciseRecommendationDto[] {
  return [
    { exerciseIndex: 0, prescribedWeight: "80kg", prescribedReps: "8-10", prescribedSets: 4 },
    { exerciseIndex: 1, prescribedWeight: "60kg", prescribedReps: "8", prescribedSets: 3 },
  ];
}

describe("ExerciseChecklist – RecommendationBadge integration", () => {
  describe("Badge is read-only (Requirement 6.5)", () => {
    it("badge does not contain interactive elements (no buttons, no editable inputs)", () => {
      render(
        <ExerciseChecklist
          exerciseLogs={makeExerciseLogs()}
          exerciseDefinitions={exerciseDefinitions}
          recommendations={makeRecommendations()}
          recommendationsLoading={false}
          recommendationsError={false}
          {...defaultProps}
        />
      );

      const badges = screen.getAllByTestId("recommendation-badge");
      expect(badges.length).toBe(2);

      for (const badge of badges) {
        // Badge should not contain any interactive elements
        expect(within(badge).queryAllByRole("button")).toHaveLength(0);
        expect(within(badge).queryAllByRole("textbox")).toHaveLength(0);
        expect(within(badge).queryAllByRole("link")).toHaveLength(0);
      }
    });

    it("badge has pointerEvents none and userSelect none styles", () => {
      render(
        <ExerciseChecklist
          exerciseLogs={makeExerciseLogs()}
          exerciseDefinitions={exerciseDefinitions}
          recommendations={makeRecommendations()}
          recommendationsLoading={false}
          recommendationsError={false}
          {...defaultProps}
        />
      );

      const badges = screen.getAllByTestId("recommendation-badge");
      for (const badge of badges) {
        expect(badge).toHaveStyle({ pointerEvents: "none" });
        expect(badge).toHaveStyle({ userSelect: "none" });
      }
    });
  });

  describe("Badge does not overlap input controls (Requirement 6.5)", () => {
    it("set-logging form inputs remain accessible when recommendations are present", async () => {
      const user = userEvent.setup();
      const onLogSet = vi.fn().mockResolvedValue(undefined);

      render(
        <ExerciseChecklist
          exerciseLogs={makeExerciseLogs()}
          exerciseDefinitions={exerciseDefinitions}
          recommendations={makeRecommendations()}
          recommendationsLoading={false}
          recommendationsError={false}
          {...defaultProps}
          onLogSet={onLogSet}
        />
      );

      // Verify badge is rendered
      expect(screen.getAllByTestId("recommendation-badge").length).toBe(2);

      // Verify weight input is accessible and usable
      const weightInput = screen.getByLabelText("Weight (kg)", { selector: "#weight-0-0" });
      expect(weightInput).not.toBeDisabled();
      await user.type(weightInput, "80");
      expect(weightInput).toHaveValue(80);

      // Verify reps input is accessible and usable
      const repsInput = screen.getByLabelText("Reps", { selector: "#reps-0-0" });
      expect(repsInput).not.toBeDisabled();
      await user.type(repsInput, "8");
      expect(repsInput).toHaveValue(8);
    });

    it("exercise completion checkboxes remain usable when recommendations are present", async () => {
      const user = userEvent.setup();
      const onComplete = vi.fn();

      render(
        <ExerciseChecklist
          exerciseLogs={makeExerciseLogs()}
          exerciseDefinitions={exerciseDefinitions}
          recommendations={makeRecommendations()}
          recommendationsLoading={false}
          recommendationsError={false}
          {...defaultProps}
          onCompleteExercise={onComplete}
        />
      );

      // Verify badge is rendered
      expect(screen.getAllByTestId("recommendation-badge").length).toBe(2);

      // Verify checkbox is accessible and clickable
      const checkbox = screen.getByLabelText(/mark back squat as complete/i);
      expect(checkbox).not.toBeDisabled();
      await user.click(checkbox);
      expect(onComplete).toHaveBeenCalledWith(0, 0);
    });

    it("badge is positioned before checkbox row and set-logging controls in DOM order", () => {
      const { container } = render(
        <ExerciseChecklist
          exerciseLogs={makeExerciseLogs()}
          exerciseDefinitions={exerciseDefinitions}
          recommendations={makeRecommendations()}
          recommendationsLoading={false}
          recommendationsError={false}
          {...defaultProps}
        />
      );

      // In each list item, the badge should appear before the form
      const listItems = container.querySelectorAll("li");
      for (const li of listItems) {
        const badge = li.querySelector("[data-testid='recommendation-badge']");
        const form = li.querySelector("form");
        if (badge && form) {
          // badge should come before form in document order
          const comparison = badge.compareDocumentPosition(form);
          // Node.DOCUMENT_POSITION_FOLLOWING = 4
          expect(comparison & Node.DOCUMENT_POSITION_FOLLOWING).toBe(
            Node.DOCUMENT_POSITION_FOLLOWING
          );
        }
      }
    });
  });

  describe("Badge hidden on error (Requirement 6.7)", () => {
    it("does not render any recommendation badges when recommendationsError is true", () => {
      render(
        <ExerciseChecklist
          exerciseLogs={makeExerciseLogs()}
          exerciseDefinitions={exerciseDefinitions}
          recommendations={makeRecommendations()}
          recommendationsLoading={false}
          recommendationsError={true}
          {...defaultProps}
        />
      );

      expect(screen.queryAllByTestId("recommendation-badge")).toHaveLength(0);
      expect(screen.queryByLabelText("Recommendation")).not.toBeInTheDocument();
    });

    it("set-logging still works when recommendations are in error state", async () => {
      const user = userEvent.setup();
      const onLogSet = vi.fn().mockResolvedValue(undefined);

      render(
        <ExerciseChecklist
          exerciseLogs={makeExerciseLogs()}
          exerciseDefinitions={exerciseDefinitions}
          recommendations={makeRecommendations()}
          recommendationsLoading={false}
          recommendationsError={true}
          {...defaultProps}
          onLogSet={onLogSet}
        />
      );

      // No badges rendered
      expect(screen.queryAllByTestId("recommendation-badge")).toHaveLength(0);

      // Set-logging form still works
      const weightInput = screen.getByLabelText("Weight (kg)", { selector: "#weight-0-0" });
      const repsInput = screen.getByLabelText("Reps", { selector: "#reps-0-0" });
      await user.type(weightInput, "100");
      await user.type(repsInput, "5");

      const submitButtons = screen.getAllByRole("button", { name: /\+ set/i });
      await user.click(submitButtons[0]);

      expect(onLogSet).toHaveBeenCalledWith({
        sectionIndex: 0,
        exerciseIndex: 0,
        weight: 100,
        repetitions: 5,
        rpe: null,
      });
    });

    it("does not render loading indicators when recommendationsError is true", () => {
      render(
        <ExerciseChecklist
          exerciseLogs={makeExerciseLogs()}
          exerciseDefinitions={exerciseDefinitions}
          recommendations={[]}
          recommendationsLoading={true}
          recommendationsError={true}
          {...defaultProps}
        />
      );

      // The error state takes precedence — no loading indicators should appear
      expect(screen.queryByLabelText("Loading recommendation")).not.toBeInTheDocument();
    });
  });
});
