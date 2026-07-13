import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { DayPicker } from "./DayPicker";

vi.mock("../../lib/vaultApi", () => ({
  getProgramDays: vi.fn(),
}));

import { getProgramDays } from "../../lib/vaultApi";

const mockGetProgramDays = vi.mocked(getProgramDays);

describe("DayPicker", () => {
  const defaultProps = {
    programId: "program-123",
    onDaySelect: vi.fn(),
    onBack: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows loading indicator while fetching days", () => {
    mockGetProgramDays.mockReturnValue(new Promise(() => {})); // never resolves
    render(<DayPicker {...defaultProps} />);
    expect(screen.getByTestId("day-picker-loading")).toBeInTheDocument();
    expect(screen.getByText("Loading days…")).toBeInTheDocument();
  });

  it("shows error message when API call fails", async () => {
    mockGetProgramDays.mockRejectedValue(new Error("Network error"));
    render(<DayPicker {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("day-picker-error")).toBeInTheDocument();
    });
    expect(screen.getByText("Network error")).toBeInTheDocument();
  });

  it("shows empty state when no days are available", async () => {
    mockGetProgramDays.mockResolvedValue({ weeks: [] });
    render(<DayPicker {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("day-picker-empty")).toBeInTheDocument();
    });
    expect(screen.getByText("No days available in this program")).toBeInTheDocument();
  });

  it("displays days grouped by week with headings", async () => {
    mockGetProgramDays.mockResolvedValue({
      weeks: [
        {
          weekNumber: 1,
          days: [
            { dayNumber: 1, label: "Push Day", focusArea: "Push" },
            { dayNumber: 2, label: "Pull Day", focusArea: "Pull" },
          ],
        },
        {
          weekNumber: 2,
          days: [
            { dayNumber: 1, label: "Legs", focusArea: "Lower" },
          ],
        },
      ],
    });
    render(<DayPicker {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("day-picker-list")).toBeInTheDocument();
    });

    expect(screen.getByText("Week 1")).toBeInTheDocument();
    expect(screen.getByText("Week 2")).toBeInTheDocument();
    expect(screen.getByText("Day 1 — Push Day")).toBeInTheDocument();
    expect(screen.getByText("Day 2 — Pull Day")).toBeInTheDocument();
    expect(screen.getByText("Day 1 — Legs")).toBeInTheDocument();
    expect(screen.getByText("Push")).toBeInTheDocument();
    expect(screen.getByText("Pull")).toBeInTheDocument();
    expect(screen.getByText("Lower")).toBeInTheDocument();
  });

  it("calls onDaySelect with correct data when a day is clicked", async () => {
    const onDaySelect = vi.fn();
    mockGetProgramDays.mockResolvedValue({
      weeks: [
        {
          weekNumber: 2,
          days: [
            { dayNumber: 3, label: "Upper Body", focusArea: "Upper" },
          ],
        },
      ],
    });
    render(<DayPicker {...defaultProps} onDaySelect={onDaySelect} />);

    await waitFor(() => {
      expect(screen.getByTestId("day-picker-list")).toBeInTheDocument();
    });

    await userEvent.click(screen.getByTestId("day-picker-item-2-3"));
    expect(onDaySelect).toHaveBeenCalledWith({
      weekNumber: 2,
      dayNumber: 3,
      label: "Upper Body",
      focusArea: "Upper",
    });
  });

  it("calls onBack when back button is clicked", async () => {
    const onBack = vi.fn();
    mockGetProgramDays.mockResolvedValue({ weeks: [] });
    render(<DayPicker {...defaultProps} onBack={onBack} />);

    await waitFor(() => {
      expect(screen.getByTestId("day-picker-back-button")).toBeInTheDocument();
    });

    await userEvent.click(screen.getByTestId("day-picker-back-button"));
    expect(onBack).toHaveBeenCalledOnce();
  });

  it("does not render back button when onBack is not provided", async () => {
    mockGetProgramDays.mockResolvedValue({ weeks: [] });
    render(<DayPicker programId="program-123" onDaySelect={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId("day-picker-empty")).toBeInTheDocument();
    });

    expect(screen.queryByTestId("day-picker-back-button")).not.toBeInTheDocument();
  });

  it("re-fetches days when programId changes", async () => {
    mockGetProgramDays.mockResolvedValue({
      weeks: [{ weekNumber: 1, days: [{ dayNumber: 1, label: "Day A", focusArea: "A" }] }],
    });

    const { rerender } = render(<DayPicker {...defaultProps} programId="program-1" />);

    await waitFor(() => {
      expect(screen.getByText("Day 1 — Day A")).toBeInTheDocument();
    });

    mockGetProgramDays.mockResolvedValue({
      weeks: [{ weekNumber: 1, days: [{ dayNumber: 1, label: "Day B", focusArea: "B" }] }],
    });

    rerender(<DayPicker {...defaultProps} programId="program-2" />);

    await waitFor(() => {
      expect(screen.getByText("Day 1 — Day B")).toBeInTheDocument();
    });

    expect(mockGetProgramDays).toHaveBeenCalledWith("program-1");
    expect(mockGetProgramDays).toHaveBeenCalledWith("program-2");
  });

  it("fetches days using getProgramDays with the programId", () => {
    mockGetProgramDays.mockReturnValue(new Promise(() => {}));
    render(<DayPicker {...defaultProps} programId="my-program-id" />);
    expect(mockGetProgramDays).toHaveBeenCalledWith("my-program-id");
  });
});
