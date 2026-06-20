import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { DayAssignmentModal } from "./DayAssignmentModal";

// Mock WorkoutSelector and ActivitySelector to avoid API calls
vi.mock("./WorkoutSelector", () => ({
  WorkoutSelector: ({ onSelect }: { onSelect: (id: string, name: string) => void }) => (
    <div data-testid="mock-workout-selector">
      <button onClick={() => onSelect("workout-1", "Push Day")}>
        Assign Push Day
      </button>
    </div>
  ),
}));

vi.mock("./ActivitySelector", () => ({
  ActivitySelector: ({ onSelect }: { onSelect: (type: string) => void }) => (
    <div data-testid="mock-activity-selector">
      <button onClick={() => onSelect("⚽ Soccer")}>
        Select Soccer
      </button>
    </div>
  ),
}));

describe("DayAssignmentModal", () => {
  const defaultProps = {
    isOpen: true,
    dayNumber: 3,
    onAssign: vi.fn(),
    onClose: vi.fn(),
  };

  it("returns null when isOpen is false", () => {
    const { container } = render(
      <DayAssignmentModal {...defaultProps} isOpen={false} />
    );
    expect(container.firstChild).toBeNull();
  });

  it("renders modal with correct day number title", () => {
    render(<DayAssignmentModal {...defaultProps} />);
    expect(screen.getByText("Assign Day 3")).toBeInTheDocument();
  });

  it("renders workout tab as active by default", () => {
    render(<DayAssignmentModal {...defaultProps} />);
    const workoutTab = screen.getByTestId("tab-workout");
    expect(workoutTab).toHaveAttribute("aria-selected", "true");
  });

  it("renders WorkoutSelector when workout tab is active", () => {
    render(<DayAssignmentModal {...defaultProps} />);
    expect(screen.getByTestId("mock-workout-selector")).toBeInTheDocument();
  });

  it("switches to activity tab and renders ActivitySelector", async () => {
    const user = userEvent.setup();
    render(<DayAssignmentModal {...defaultProps} />);

    await user.click(screen.getByTestId("tab-activity"));
    expect(screen.getByTestId("mock-activity-selector")).toBeInTheDocument();
    expect(screen.queryByTestId("mock-workout-selector")).not.toBeInTheDocument();
  });

  it("calls onAssign with workout type when a workout is selected", async () => {
    const user = userEvent.setup();
    const onAssign = vi.fn();
    render(<DayAssignmentModal {...defaultProps} onAssign={onAssign} />);

    await user.click(screen.getByText("Assign Push Day"));
    expect(onAssign).toHaveBeenCalledWith({
      type: "workout",
      workoutId: "workout-1",
      workoutName: "Push Day",
    });
  });

  it("calls onAssign with activity type when an activity is selected", async () => {
    const user = userEvent.setup();
    const onAssign = vi.fn();
    render(<DayAssignmentModal {...defaultProps} onAssign={onAssign} />);

    await user.click(screen.getByTestId("tab-activity"));
    await user.click(screen.getByText("Select Soccer"));
    expect(onAssign).toHaveBeenCalledWith({
      type: "activity",
      activityType: "⚽ Soccer",
    });
  });

  it("calls onClose when close button is clicked", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<DayAssignmentModal {...defaultProps} onClose={onClose} />);

    await user.click(screen.getByTestId("day-assignment-modal-close"));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("calls onClose when backdrop is clicked", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<DayAssignmentModal {...defaultProps} onClose={onClose} />);

    await user.click(screen.getByTestId("day-assignment-modal-backdrop"));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("does not close when clicking inside the modal content", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<DayAssignmentModal {...defaultProps} onClose={onClose} />);

    await user.click(screen.getByTestId("day-assignment-modal"));
    expect(onClose).not.toHaveBeenCalled();
  });

  it("has correct aria attributes for accessibility", () => {
    render(<DayAssignmentModal {...defaultProps} />);
    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(dialog).toHaveAttribute("aria-label", "Assign Day 3");
  });
});
