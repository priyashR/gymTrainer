import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { CrossFitScoreForm } from "../CrossFitScoreForm";

describe("CrossFitScoreForm", () => {
  const defaultProps = {
    sectionIndex: 0,
    sectionType: "AMRAP" as const,
    sessionStatus: "IN_PROGRESS" as const,
    onLogCrossFitScore: vi.fn().mockResolvedValue(undefined),
  };

  describe("appropriate fields per section type", () => {
    it("shows rounds and additional reps fields for AMRAP without time field", () => {
      render(<CrossFitScoreForm {...defaultProps} sectionType="AMRAP" />);

      expect(screen.getByLabelText(/rounds/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/\+ reps/i)).toBeInTheDocument();
      expect(screen.queryByLabelText(/time \(sec\)/i)).not.toBeInTheDocument();
    });

    it("shows rounds and additional reps fields for EMOM without time field", () => {
      render(<CrossFitScoreForm {...defaultProps} sectionType="EMOM" />);

      expect(screen.getByLabelText(/rounds/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/\+ reps/i)).toBeInTheDocument();
      expect(screen.queryByLabelText(/time \(sec\)/i)).not.toBeInTheDocument();
    });

    it("shows rounds, additional reps, and total time fields for FOR_TIME", () => {
      render(<CrossFitScoreForm {...defaultProps} sectionType="FOR_TIME" />);

      expect(screen.getByLabelText(/rounds/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/\+ reps/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/time \(sec\)/i)).toBeInTheDocument();
    });
  });

  describe("validation", () => {
    it("shows error when rounds field is empty on submit", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockResolvedValue(undefined);
      render(
        <CrossFitScoreForm {...defaultProps} onLogCrossFitScore={onLogCrossFitScore} />
      );

      // Leave rounds empty, fill additional reps
      await user.type(screen.getByLabelText(/\+ reps/i), "5");
      await user.click(screen.getByRole("button", { name: /save score/i }));

      expect(screen.getByRole("alert")).toHaveTextContent(/rounds must be 0 or greater/i);
      expect(onLogCrossFitScore).not.toHaveBeenCalled();
    });

    it("shows error when additional reps field is empty on submit", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockResolvedValue(undefined);
      render(
        <CrossFitScoreForm {...defaultProps} onLogCrossFitScore={onLogCrossFitScore} />
      );

      // Fill rounds, leave additional reps empty
      await user.type(screen.getByLabelText(/rounds/i), "3");
      await user.click(screen.getByRole("button", { name: /save score/i }));

      expect(screen.getByRole("alert")).toHaveTextContent(/additional reps must be 0 or greater/i);
      expect(onLogCrossFitScore).not.toHaveBeenCalled();
    });

    it("shows error when total time is empty for FOR_TIME", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockResolvedValue(undefined);
      render(
        <CrossFitScoreForm
          {...defaultProps}
          sectionType="FOR_TIME"
          onLogCrossFitScore={onLogCrossFitScore}
        />
      );

      await user.type(screen.getByLabelText(/rounds/i), "3");
      await user.type(screen.getByLabelText(/\+ reps/i), "5");
      // Leave time empty
      await user.click(screen.getByRole("button", { name: /save score/i }));

      expect(screen.getByRole("alert")).toHaveTextContent(/total time must be greater than 0/i);
      expect(onLogCrossFitScore).not.toHaveBeenCalled();
    });

    it("allows zero rounds (non-negative validation)", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockResolvedValue(undefined);
      render(
        <CrossFitScoreForm {...defaultProps} onLogCrossFitScore={onLogCrossFitScore} />
      );

      await user.type(screen.getByLabelText(/rounds/i), "0");
      await user.type(screen.getByLabelText(/\+ reps/i), "5");
      await user.click(screen.getByRole("button", { name: /save score/i }));

      expect(onLogCrossFitScore).toHaveBeenCalledWith({
        sectionIndex: 0,
        rounds: 0,
        additionalReps: 5,
        totalTimeSeconds: null,
      });
    });

    it("allows zero additional reps (non-negative validation)", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockResolvedValue(undefined);
      render(
        <CrossFitScoreForm {...defaultProps} onLogCrossFitScore={onLogCrossFitScore} />
      );

      await user.type(screen.getByLabelText(/rounds/i), "4");
      await user.type(screen.getByLabelText(/\+ reps/i), "0");
      await user.click(screen.getByRole("button", { name: /save score/i }));

      expect(onLogCrossFitScore).toHaveBeenCalledWith({
        sectionIndex: 0,
        rounds: 4,
        additionalReps: 0,
        totalTimeSeconds: null,
      });
    });

    it("does not submit when time is zero for FOR_TIME (native min constraint)", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockResolvedValue(undefined);
      render(
        <CrossFitScoreForm
          {...defaultProps}
          sectionType="FOR_TIME"
          onLogCrossFitScore={onLogCrossFitScore}
        />
      );

      await user.type(screen.getByLabelText(/rounds/i), "3");
      await user.type(screen.getByLabelText(/\+ reps/i), "5");
      await user.type(screen.getByLabelText(/time \(sec\)/i), "0");
      await user.click(screen.getByRole("button", { name: /save score/i }));

      // Native HTML5 min="1" validation on the time input prevents form submission
      expect(onLogCrossFitScore).not.toHaveBeenCalled();
    });
  });

  describe("submission", () => {
    it("calls onLogCrossFitScore with correct data for AMRAP", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockResolvedValue(undefined);
      render(
        <CrossFitScoreForm {...defaultProps} onLogCrossFitScore={onLogCrossFitScore} />
      );

      await user.type(screen.getByLabelText(/rounds/i), "5");
      await user.type(screen.getByLabelText(/\+ reps/i), "3");
      await user.click(screen.getByRole("button", { name: /save score/i }));

      expect(onLogCrossFitScore).toHaveBeenCalledWith({
        sectionIndex: 0,
        rounds: 5,
        additionalReps: 3,
        totalTimeSeconds: null,
      });
    });

    it("calls onLogCrossFitScore with correct data for FOR_TIME including time", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockResolvedValue(undefined);
      render(
        <CrossFitScoreForm
          {...defaultProps}
          sectionIndex={2}
          sectionType="FOR_TIME"
          onLogCrossFitScore={onLogCrossFitScore}
        />
      );

      await user.type(screen.getByLabelText(/rounds/i), "4");
      await user.type(screen.getByLabelText(/\+ reps/i), "7");
      await user.type(screen.getByLabelText(/time \(sec\)/i), "480");
      await user.click(screen.getByRole("button", { name: /save score/i }));

      expect(onLogCrossFitScore).toHaveBeenCalledWith({
        sectionIndex: 2,
        rounds: 4,
        additionalReps: 7,
        totalTimeSeconds: 480,
      });
    });

    it("resets form fields after successful submission", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockResolvedValue(undefined);
      render(
        <CrossFitScoreForm {...defaultProps} onLogCrossFitScore={onLogCrossFitScore} />
      );

      const roundsInput = screen.getByLabelText(/rounds/i) as HTMLInputElement;
      const repsInput = screen.getByLabelText(/\+ reps/i) as HTMLInputElement;

      await user.type(roundsInput, "5");
      await user.type(repsInput, "3");
      await user.click(screen.getByRole("button", { name: /save score/i }));

      await waitFor(() => {
        expect(roundsInput).toHaveValue(null);
        expect(repsInput).toHaveValue(null);
      });
    });

    it("shows error message when onLogCrossFitScore rejects", async () => {
      const user = userEvent.setup();
      const onLogCrossFitScore = vi.fn().mockRejectedValue(new Error("Network error"));
      render(
        <CrossFitScoreForm {...defaultProps} onLogCrossFitScore={onLogCrossFitScore} />
      );

      await user.type(screen.getByLabelText(/rounds/i), "3");
      await user.type(screen.getByLabelText(/\+ reps/i), "0");
      await user.click(screen.getByRole("button", { name: /save score/i }));

      expect(await screen.findByRole("alert")).toHaveTextContent(/failed to log score/i);
    });
  });
});
