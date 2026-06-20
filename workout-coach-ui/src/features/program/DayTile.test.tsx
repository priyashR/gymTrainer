import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { DayTile, DayAssignment } from "./DayTile";

describe("DayTile", () => {
  const defaultProps = {
    dayNumber: 1,
    assignment: null as DayAssignment | null,
    onClick: vi.fn(),
    onRemove: vi.fn(),
  };

  it("renders the day number label", () => {
    render(<DayTile {...defaultProps} />);
    expect(screen.getByText("Day 1")).toBeInTheDocument();
  });

  it("renders 'No assignment' when assignment is null", () => {
    render(<DayTile {...defaultProps} assignment={null} />);
    expect(screen.getByText("No assignment")).toBeInTheDocument();
  });

  it("renders 'No assignment' when assignment type is null", () => {
    render(<DayTile {...defaultProps} assignment={{ type: null }} />);
    expect(screen.getByText("No assignment")).toBeInTheDocument();
  });

  it("renders workout name when assignment is a workout", () => {
    const assignment: DayAssignment = {
      type: "workout",
      workoutId: "uuid-123",
      workoutName: "PPL Push Day",
    };
    render(<DayTile {...defaultProps} assignment={assignment} />);
    expect(screen.getByText("PPL Push Day")).toBeInTheDocument();
  });

  it("renders activity type when assignment is an activity", () => {
    const assignment: DayAssignment = {
      type: "activity",
      activityType: "⚽ Soccer",
    };
    render(<DayTile {...defaultProps} assignment={assignment} />);
    expect(screen.getByText("⚽ Soccer")).toBeInTheDocument();
  });

  it("shows Assign button when no assignment exists", () => {
    render(<DayTile {...defaultProps} assignment={null} />);
    expect(screen.getByRole("button", { name: /assign workout or activity to day 1/i })).toBeInTheDocument();
  });

  it("shows Change and Remove buttons when an assignment exists", () => {
    const assignment: DayAssignment = {
      type: "workout",
      workoutId: "uuid-123",
      workoutName: "Upper Body",
    };
    render(<DayTile {...defaultProps} assignment={assignment} />);
    expect(screen.getByRole("button", { name: /change assignment for day 1/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /remove assignment for day 1/i })).toBeInTheDocument();
  });

  it("calls onClick when Assign button is clicked", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(<DayTile {...defaultProps} onClick={onClick} assignment={null} />);

    await user.click(screen.getByRole("button", { name: /assign/i }));
    expect(onClick).toHaveBeenCalledOnce();
  });

  it("calls onClick when Change button is clicked", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    const assignment: DayAssignment = {
      type: "workout",
      workoutId: "uuid-123",
      workoutName: "Leg Day",
    };
    render(<DayTile {...defaultProps} onClick={onClick} assignment={assignment} />);

    await user.click(screen.getByRole("button", { name: /change/i }));
    expect(onClick).toHaveBeenCalledOnce();
  });

  it("calls onRemove when Remove button is clicked", async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    const assignment: DayAssignment = {
      type: "activity",
      activityType: "Running",
    };
    render(<DayTile {...defaultProps} onRemove={onRemove} assignment={assignment} />);

    await user.click(screen.getByRole("button", { name: /remove/i }));
    expect(onRemove).toHaveBeenCalledOnce();
  });

  it("has the correct data-testid", () => {
    render(<DayTile {...defaultProps} dayNumber={3} />);
    expect(screen.getByTestId("day-tile-3")).toBeInTheDocument();
  });
});
