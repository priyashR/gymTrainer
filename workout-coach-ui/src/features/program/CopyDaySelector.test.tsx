import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { CopyDaySelector } from "./CopyDaySelector";
import type { VaultItem } from "../../types/vault";

vi.mock("../../lib/vaultApi", () => ({
  listPrograms: vi.fn(),
  getProgramDays: vi.fn(),
}));

import { listPrograms, getProgramDays } from "../../lib/vaultApi";

const mockListPrograms = vi.mocked(listPrograms);
const mockGetProgramDays = vi.mocked(getProgramDays);

const mockPrograms: VaultItem[] = [
  {
    id: "prog-1",
    name: "Push Pull Legs",
    goal: "Hypertrophy",
    durationWeeks: 8,
    equipmentProfile: ["barbell"],
    contentSource: "AI_GENERATED",
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T00:00:00Z",
  },
  {
    id: "prog-2",
    name: "CrossFit WODs",
    goal: "General Fitness",
    durationWeeks: 4,
    equipmentProfile: ["kettlebell"],
    contentSource: "UPLOADED",
    createdAt: "2024-02-01T00:00:00Z",
    updatedAt: "2024-02-01T00:00:00Z",
  },
  {
    id: "prog-current",
    name: "My Current Program",
    goal: "Strength",
    durationWeeks: 12,
    equipmentProfile: [],
    contentSource: "MANUAL",
    createdAt: "2024-03-01T00:00:00Z",
    updatedAt: "2024-03-01T00:00:00Z",
  },
];

describe("CopyDaySelector", () => {
  const defaultProps = {
    onSelect: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows loading state while fetching programs", () => {
    mockListPrograms.mockReturnValue(new Promise(() => {})); // never resolves
    render(<CopyDaySelector {...defaultProps} />);

    expect(screen.getByTestId("copy-day-loading")).toBeInTheDocument();
    expect(screen.getByText("Loading programs…")).toBeInTheDocument();
  });

  it("renders program list after loading", async () => {
    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });

    render(<CopyDaySelector {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-program-list")).toBeInTheDocument();
    });

    expect(screen.getByText("Push Pull Legs")).toBeInTheDocument();
    expect(screen.getByText("CrossFit WODs")).toBeInTheDocument();
    expect(screen.getByText("My Current Program")).toBeInTheDocument();
  });

  it("excludes current program from the list (Req 9.3)", async () => {
    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });

    render(<CopyDaySelector {...defaultProps} excludeProgramId="prog-current" />);

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-program-list")).toBeInTheDocument();
    });

    expect(screen.getByText("Push Pull Legs")).toBeInTheDocument();
    expect(screen.getByText("CrossFit WODs")).toBeInTheDocument();
    expect(screen.queryByText("My Current Program")).not.toBeInTheDocument();
  });

  it("transitions to day picker when a program is selected (Req 9.2)", async () => {
    const user = userEvent.setup();
    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });
    mockGetProgramDays.mockResolvedValue({
      weeks: [
        {
          weekNumber: 1,
          days: [
            { dayNumber: 1, label: "Push Day", focusArea: "Push" },
            { dayNumber: 2, label: "Pull Day", focusArea: "Pull" },
          ],
        },
      ],
    });

    render(<CopyDaySelector {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-program-list")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("program-item-prog-1"));

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-day-picker")).toBeInTheDocument();
    });

    // Should show the selected program name
    expect(screen.getByTestId("copy-day-selected-program-name")).toHaveTextContent("Push Pull Legs");
    // Should show days
    expect(screen.getByText("Day 1 — Push Day")).toBeInTheDocument();
    expect(screen.getByText("Day 2 — Pull Day")).toBeInTheDocument();
    // Program list should no longer be visible
    expect(screen.queryByTestId("copy-day-program-list")).not.toBeInTheDocument();
  });

  it("back button returns to program list (Req 10.5)", async () => {
    const user = userEvent.setup();
    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });
    mockGetProgramDays.mockResolvedValue({
      weeks: [
        {
          weekNumber: 1,
          days: [{ dayNumber: 1, label: "Legs", focusArea: "Lower" }],
        },
      ],
    });

    render(<CopyDaySelector {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-program-list")).toBeInTheDocument();
    });

    // Select a program to go to day picker
    await user.click(screen.getByTestId("program-item-prog-2"));

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-day-picker")).toBeInTheDocument();
    });

    // Click back button
    await user.click(screen.getByTestId("copy-day-back-button"));

    // Should return to program list
    await waitFor(() => {
      expect(screen.getByTestId("copy-day-program-list")).toBeInTheDocument();
    });
    expect(screen.queryByTestId("copy-day-day-picker")).not.toBeInTheDocument();
  });

  it("calls onSelect with correct copied_day assignment data (Req 10.4)", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });
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

    render(<CopyDaySelector onSelect={onSelect} />);

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-program-list")).toBeInTheDocument();
    });

    // Select program
    await user.click(screen.getByTestId("program-item-prog-1"));

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-day-picker")).toBeInTheDocument();
    });

    // Select a day
    await user.click(screen.getByTestId("day-item-2-3"));

    expect(onSelect).toHaveBeenCalledOnce();
    expect(onSelect).toHaveBeenCalledWith({
      type: "copied_day",
      sourceProgramId: "prog-1",
      sourceProgramName: "Push Pull Legs",
      sourceWeekNumber: 2,
      sourceDayNumber: 3,
      dayLabel: "Upper Body",
      focusArea: "Upper",
    });
  });

  it("shows loading state while fetching days after program selection", async () => {
    const user = userEvent.setup();
    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });
    mockGetProgramDays.mockReturnValue(new Promise(() => {})); // never resolves

    render(<CopyDaySelector {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-program-list")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("program-item-prog-1"));

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-loading")).toBeInTheDocument();
    });
    expect(screen.getByText("Loading days…")).toBeInTheDocument();
  });

  it("shows error when program list fetch fails", async () => {
    mockListPrograms.mockRejectedValue(new Error("Failed to load"));

    render(<CopyDaySelector {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-error")).toBeInTheDocument();
    });
    expect(screen.getByText("Failed to load")).toBeInTheDocument();
  });

  it("shows error when days fetch fails", async () => {
    const user = userEvent.setup();
    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });
    mockGetProgramDays.mockRejectedValue(new Error("Days fetch error"));

    render(<CopyDaySelector {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-program-list")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("program-item-prog-1"));

    await waitFor(() => {
      expect(screen.getByTestId("copy-day-error")).toBeInTheDocument();
    });
    expect(screen.getByText("Days fetch error")).toBeInTheDocument();
  });
});
