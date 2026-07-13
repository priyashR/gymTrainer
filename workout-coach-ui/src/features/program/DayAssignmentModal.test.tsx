import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { DayAssignmentModal } from "./DayAssignmentModal";

// Mock ActivitySelector and CopyDaySelector to avoid API calls
vi.mock("./ActivitySelector", () => ({
  ActivitySelector: ({ onSelect }: { onSelect: (type: string) => void }) => (
    <div data-testid="mock-activity-selector">
      <button onClick={() => onSelect("⚽ Soccer")}>
        Select Soccer
      </button>
    </div>
  ),
}));

vi.mock("./CopyDaySelector", () => ({
  CopyDaySelector: ({ onSelect }: { onSelect: (assignment: any) => void }) => (
    <div data-testid="mock-copy-day-selector">
      <button onClick={() => onSelect({
        type: "copied_day",
        sourceProgramId: "prog-1",
        sourceProgramName: "Push Pull Legs",
        sourceWeekNumber: 1,
        sourceDayNumber: 1,
        dayLabel: "Push Day",
        focusArea: "Push",
      })}>
        Select Push Day
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

  it("renders activity tab as active by default", () => {
    render(<DayAssignmentModal {...defaultProps} />);
    const activityTab = screen.getByTestId("tab-activity");
    expect(activityTab).toHaveAttribute("aria-selected", "true");
  });

  it("renders ActivitySelector when activity tab is active by default", () => {
    render(<DayAssignmentModal {...defaultProps} />);
    expect(screen.getByTestId("mock-activity-selector")).toBeInTheDocument();
  });

  it("does not render a Workout tab", () => {
    render(<DayAssignmentModal {...defaultProps} />);
    expect(screen.queryByTestId("tab-workout")).not.toBeInTheDocument();
  });

  it("renders Copy Day tab", () => {
    render(<DayAssignmentModal {...defaultProps} />);
    expect(screen.getByTestId("tab-copy-day")).toBeInTheDocument();
    expect(screen.getByText("📋 Copy Day")).toBeInTheDocument();
  });

  it("switches to Copy Day tab and renders CopyDaySelector", async () => {
    const user = userEvent.setup();
    render(<DayAssignmentModal {...defaultProps} />);

    await user.click(screen.getByTestId("tab-copy-day"));
    expect(screen.getByTestId("mock-copy-day-selector")).toBeInTheDocument();
    expect(screen.queryByTestId("mock-activity-selector")).not.toBeInTheDocument();
  });

  it("calls onAssign with activity type when an activity is selected", async () => {
    const onAssign = vi.fn();
    render(<DayAssignmentModal {...defaultProps} onAssign={onAssign} />);

    await userEvent.click(screen.getByText("Select Soccer"));
    expect(onAssign).toHaveBeenCalledWith({
      type: "activity",
      activityType: "⚽ Soccer",
    });
  });

  it("calls onAssign with copied_day assignment when a day is selected", async () => {
    const user = userEvent.setup();
    const onAssign = vi.fn();
    render(<DayAssignmentModal {...defaultProps} onAssign={onAssign} />);

    await user.click(screen.getByTestId("tab-copy-day"));
    await user.click(screen.getByText("Select Push Day"));
    expect(onAssign).toHaveBeenCalledWith({
      type: "copied_day",
      sourceProgramId: "prog-1",
      sourceProgramName: "Push Pull Legs",
      sourceWeekNumber: 1,
      sourceDayNumber: 1,
      dayLabel: "Push Day",
      focusArea: "Push",
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
