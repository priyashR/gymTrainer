import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { LoggingPanel } from "../LoggingPanel";
import type { LoggingPanelProps } from "../LoggingPanel";

function renderPanel(overrides: Partial<LoggingPanelProps> = {}) {
  const defaultProps: LoggingPanelProps = {
    sectionType: "STRENGTH",
    currentExercise: { name: "Romanian Deadlift", recommendation: "3 × 10 @ 80kg" },
    loggedSets: [],
    roundsCompleted: 0,
    onLogSet: vi.fn(),
    onUpdateRounds: vi.fn(),
    onSubmitScore: vi.fn(),
  };
  return render(<LoggingPanel {...defaultProps} {...overrides} />);
}

describe("LoggingPanel", () => {
  it("renders with data-testid logging-panel", () => {
    renderPanel();
    expect(screen.getByTestId("logging-panel")).toBeInTheDocument();
  });

  describe("Strength variant", () => {
    it("shows current exercise name and prescription", () => {
      renderPanel({
        sectionType: "STRENGTH",
        currentExercise: { name: "Back Squat", recommendation: "4 × 6 @ 120kg" },
      });

      expect(screen.getByText("Back Squat")).toBeInTheDocument();
      expect(screen.getByText("Prescribed: 4 × 6 @ 120kg")).toBeInTheDocument();
    });

    it("renders reps, weight, and RPE inputs", () => {
      renderPanel({ sectionType: "STRENGTH" });

      expect(screen.getByLabelText("Reps completed")).toBeInTheDocument();
      expect(screen.getByLabelText("Weight used")).toBeInTheDocument();
      expect(screen.getByLabelText("Rate of perceived exertion")).toBeInTheDocument();
    });

    it("renders a Log Set button", () => {
      renderPanel({ sectionType: "STRENGTH" });

      expect(screen.getByRole("button", { name: /log set/i })).toBeInTheDocument();
    });

    it("calls onLogSet with reps, weight, and RPE values", async () => {
      const user = userEvent.setup();
      const onLogSet = vi.fn();
      renderPanel({ sectionType: "STRENGTH", onLogSet });

      const repsInput = screen.getByLabelText("Reps completed");
      const weightInput = screen.getByLabelText("Weight used");
      const rpeInput = screen.getByLabelText("Rate of perceived exertion");

      await user.clear(repsInput);
      await user.type(repsInput, "10");
      await user.clear(weightInput);
      await user.type(weightInput, "80");
      await user.clear(rpeInput);
      await user.type(rpeInput, "7");

      fireEvent.submit(screen.getByTestId("set-log-form"));

      expect(onLogSet).toHaveBeenCalledWith({ reps: 10, weight: 80, rpe: 7 });
    });

    it("calls onLogSet with null RPE when RPE is not entered", async () => {
      const user = userEvent.setup();
      const onLogSet = vi.fn();
      renderPanel({ sectionType: "STRENGTH", onLogSet });

      const repsInput = screen.getByLabelText("Reps completed");
      const weightInput = screen.getByLabelText("Weight used");

      await user.clear(repsInput);
      await user.type(repsInput, "8");
      await user.clear(weightInput);
      await user.type(weightInput, "100");

      fireEvent.submit(screen.getByTestId("set-log-form"));

      expect(onLogSet).toHaveBeenCalledWith({ reps: 8, weight: 100, rpe: null });
    });

    it("shows validation error when reps is missing", async () => {
      const user = userEvent.setup();
      const onLogSet = vi.fn();
      renderPanel({ sectionType: "STRENGTH", onLogSet });

      const weightInput = screen.getByLabelText("Weight used");
      await user.clear(weightInput);
      await user.type(weightInput, "80");

      fireEvent.submit(screen.getByTestId("set-log-form"));

      expect(screen.getByRole("alert")).toHaveTextContent("Reps must be at least 1");
      expect(onLogSet).not.toHaveBeenCalled();
    });

    it("shows validation error when weight is missing", async () => {
      const user = userEvent.setup();
      const onLogSet = vi.fn();
      renderPanel({ sectionType: "STRENGTH", onLogSet });

      const repsInput = screen.getByLabelText("Reps completed");
      await user.clear(repsInput);
      await user.type(repsInput, "10");

      fireEvent.submit(screen.getByTestId("set-log-form"));

      expect(screen.getByRole("alert")).toHaveTextContent("Weight must be greater than 0");
      expect(onLogSet).not.toHaveBeenCalled();
    });

    it("displays logged sets history", () => {
      renderPanel({
        sectionType: "STRENGTH",
        loggedSets: [
          { reps: 10, weight: 80, rpe: 7 },
          { reps: 10, weight: 80, rpe: 7.5 },
        ],
      });

      expect(screen.getByTestId("set-log-history")).toBeInTheDocument();
      const listItems = screen.getAllByRole("listitem");
      expect(listItems).toHaveLength(2);
      expect(listItems[0]).toHaveTextContent("#1");
      expect(listItems[0]).toHaveTextContent("10 × 80kg");
      expect(listItems[0]).toHaveTextContent("RPE 7");
      expect(listItems[1]).toHaveTextContent("#2");
      expect(listItems[1]).toHaveTextContent("RPE 7.5");
    });

    it("shows empty state when no sets are logged", () => {
      renderPanel({ sectionType: "STRENGTH", loggedSets: [] });

      expect(screen.getByTestId("no-sets-message")).toHaveTextContent("No sets logged yet");
    });
  });

  describe("CrossFit variant (AMRAP)", () => {
    it("shows round counter for AMRAP section type", () => {
      renderPanel({ sectionType: "AMRAP", roundsCompleted: 3 });

      expect(screen.getByTestId("round-counter")).toBeInTheDocument();
      expect(screen.getByText("3")).toBeInTheDocument();
      expect(screen.getByText("Rounds Completed")).toBeInTheDocument();
    });

    it("calls onUpdateRounds with incremented value on + button click", async () => {
      const user = userEvent.setup();
      const onUpdateRounds = vi.fn();
      renderPanel({ sectionType: "AMRAP", roundsCompleted: 3, onUpdateRounds });

      await user.click(screen.getByRole("button", { name: /increment rounds/i }));

      expect(onUpdateRounds).toHaveBeenCalledWith(4);
    });

    it("calls onUpdateRounds with decremented value on − button click", async () => {
      const user = userEvent.setup();
      const onUpdateRounds = vi.fn();
      renderPanel({ sectionType: "AMRAP", roundsCompleted: 3, onUpdateRounds });

      await user.click(screen.getByRole("button", { name: /decrement rounds/i }));

      expect(onUpdateRounds).toHaveBeenCalledWith(2);
    });

    it("disables decrement button when rounds is 0", () => {
      renderPanel({ sectionType: "AMRAP", roundsCompleted: 0 });

      expect(screen.getByRole("button", { name: /decrement rounds/i })).toBeDisabled();
    });

    it("renders CrossFit score form", () => {
      renderPanel({ sectionType: "AMRAP" });

      expect(screen.getByTestId("crossfit-score-form")).toBeInTheDocument();
      expect(screen.getByLabelText("Score rounds")).toBeInTheDocument();
      expect(screen.getByLabelText("Score additional reps")).toBeInTheDocument();
    });

    it("calls onSubmitScore with rounds and reps on form submission", async () => {
      const user = userEvent.setup();
      const onSubmitScore = vi.fn();
      renderPanel({ sectionType: "AMRAP", onSubmitScore });

      await user.type(screen.getByLabelText("Score rounds"), "3");
      await user.type(screen.getByLabelText("Score additional reps"), "15");
      await user.click(screen.getByRole("button", { name: /submit score/i }));

      expect(onSubmitScore).toHaveBeenCalledWith({ rounds: 3, reps: 15 });
    });
  });

  describe("CrossFit variant (FOR_TIME)", () => {
    it("shows round counter for FOR_TIME section type", () => {
      renderPanel({ sectionType: "FOR_TIME", roundsCompleted: 2 });

      expect(screen.getByTestId("round-counter")).toBeInTheDocument();
      expect(screen.getByText("2")).toBeInTheDocument();
    });
  });

  describe("CrossFit variant (EMOM)", () => {
    it("shows round counter for EMOM section type", () => {
      renderPanel({ sectionType: "EMOM", roundsCompleted: 5 });

      expect(screen.getByTestId("round-counter")).toBeInTheDocument();
      expect(screen.getByText("5")).toBeInTheDocument();
    });
  });
});
