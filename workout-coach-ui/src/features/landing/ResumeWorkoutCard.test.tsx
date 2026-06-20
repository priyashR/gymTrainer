import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ResumeWorkoutCard } from "./ResumeWorkoutCard";

describe("ResumeWorkoutCard", () => {
  const defaultProps = {
    sessionId: "session-123",
    workoutName: "PPL Day 1 - Push",
    status: "active" as const,
    progress: 45,
    onResume: vi.fn(),
  };

  it("renders the workout name", () => {
    render(<ResumeWorkoutCard {...defaultProps} />);
    expect(screen.getByText("PPL Day 1 - Push")).toBeInTheDocument();
  });

  it("renders the session status badge", () => {
    render(<ResumeWorkoutCard {...defaultProps} status="paused" />);
    expect(screen.getByText("paused")).toBeInTheDocument();
  });

  it("renders progress percentage", () => {
    render(<ResumeWorkoutCard {...defaultProps} progress={72} />);
    expect(screen.getByText("72% complete")).toBeInTheDocument();
  });

  it("renders progress bar with correct aria attributes", () => {
    render(<ResumeWorkoutCard {...defaultProps} progress={45} />);
    const progressBar = screen.getByRole("progressbar");
    expect(progressBar).toHaveAttribute("aria-valuenow", "45");
    expect(progressBar).toHaveAttribute("aria-valuemin", "0");
    expect(progressBar).toHaveAttribute("aria-valuemax", "100");
  });

  it("clamps progress to 0-100 range", () => {
    render(<ResumeWorkoutCard {...defaultProps} progress={150} />);
    const progressBar = screen.getByRole("progressbar");
    expect(progressBar).toHaveAttribute("aria-valuenow", "100");
    expect(screen.getByText("100% complete")).toBeInTheDocument();
  });

  it("clamps negative progress to 0", () => {
    render(<ResumeWorkoutCard {...defaultProps} progress={-10} />);
    const progressBar = screen.getByRole("progressbar");
    expect(progressBar).toHaveAttribute("aria-valuenow", "0");
    expect(screen.getByText("0% complete")).toBeInTheDocument();
  });

  it("calls onResume with sessionId when resume button is clicked", async () => {
    const user = userEvent.setup();
    const onResume = vi.fn();
    render(<ResumeWorkoutCard {...defaultProps} onResume={onResume} />);

    await user.click(screen.getByRole("button", { name: /resume workout/i }));
    expect(onResume).toHaveBeenCalledOnce();
    expect(onResume).toHaveBeenCalledWith("session-123");
  });

  it("renders with active status styling", () => {
    render(<ResumeWorkoutCard {...defaultProps} status="active" />);
    expect(screen.getByLabelText("Status: active")).toBeInTheDocument();
  });

  it("renders with paused status styling", () => {
    render(<ResumeWorkoutCard {...defaultProps} status="paused" />);
    expect(screen.getByLabelText("Status: paused")).toBeInTheDocument();
  });

  it("has the data-testid for conditional rendering", () => {
    render(<ResumeWorkoutCard {...defaultProps} />);
    expect(screen.getByTestId("resume-workout-card")).toBeInTheDocument();
  });
});
