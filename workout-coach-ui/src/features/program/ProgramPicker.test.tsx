import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ProgramPicker } from "./ProgramPicker";
import type { VaultItem } from "../../types/vault";

vi.mock("../../lib/vaultApi", () => ({
  listPrograms: vi.fn(),
}));

import { listPrograms } from "../../lib/vaultApi";

const mockListPrograms = vi.mocked(listPrograms);

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
    id: "prog-3",
    name: "Custom Plan",
    goal: "Strength",
    durationWeeks: 12,
    equipmentProfile: [],
    contentSource: "MANUAL",
    createdAt: "2024-03-01T00:00:00Z",
    updatedAt: "2024-03-01T00:00:00Z",
  },
];

describe("ProgramPicker", () => {
  const defaultProps = {
    onProgramSelect: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows a loading indicator while fetching programs", () => {
    mockListPrograms.mockReturnValue(new Promise(() => {})); // never resolves
    render(<ProgramPicker {...defaultProps} />);

    expect(screen.getByTestId("program-picker-loading")).toBeInTheDocument();
    expect(screen.getByText("Loading programs…")).toBeInTheDocument();
  });

  it("renders list of programs with name and content source", async () => {
    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });

    render(<ProgramPicker {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("program-picker-list")).toBeInTheDocument();
    });

    expect(screen.getByText("Push Pull Legs")).toBeInTheDocument();
    expect(screen.getByText("ai generated")).toBeInTheDocument();
    expect(screen.getByText("CrossFit WODs")).toBeInTheDocument();
    expect(screen.getByText("uploaded")).toBeInTheDocument();
    expect(screen.getByText("Custom Plan")).toBeInTheDocument();
    expect(screen.getByText("manual")).toBeInTheDocument();
  });

  it("excludes the current program by ID", async () => {
    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });

    render(<ProgramPicker {...defaultProps} excludeProgramId="prog-2" />);

    await waitFor(() => {
      expect(screen.getByTestId("program-picker-list")).toBeInTheDocument();
    });

    expect(screen.getByText("Push Pull Legs")).toBeInTheDocument();
    expect(screen.queryByText("CrossFit WODs")).not.toBeInTheDocument();
    expect(screen.getByText("Custom Plan")).toBeInTheDocument();
  });

  it("shows an error message when loading fails", async () => {
    mockListPrograms.mockRejectedValue(new Error("Network error"));

    render(<ProgramPicker {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("program-picker-error")).toBeInTheDocument();
    });

    expect(screen.getByText("Network error")).toBeInTheDocument();
  });

  it("shows empty state when no programs are available", async () => {
    mockListPrograms.mockResolvedValue({
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    });

    render(<ProgramPicker {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("program-picker-empty")).toBeInTheDocument();
    });

    expect(screen.getByText("No programs available to copy from")).toBeInTheDocument();
  });

  it("calls onProgramSelect when a program is clicked", async () => {
    const user = userEvent.setup();
    const onProgramSelect = vi.fn();

    mockListPrograms.mockResolvedValue({
      content: mockPrograms,
      page: 0,
      size: 100,
      totalElements: 3,
      totalPages: 1,
    });

    render(<ProgramPicker onProgramSelect={onProgramSelect} />);

    await waitFor(() => {
      expect(screen.getByTestId("program-picker-list")).toBeInTheDocument();
    });

    await user.click(screen.getByRole("listitem", { name: /select program push pull legs/i }));

    expect(onProgramSelect).toHaveBeenCalledOnce();
    expect(onProgramSelect).toHaveBeenCalledWith(mockPrograms[0]);
  });

  it("shows empty state when all programs are excluded", async () => {
    const singleProgram: VaultItem[] = [mockPrograms[0]];
    mockListPrograms.mockResolvedValue({
      content: singleProgram,
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
    });

    render(<ProgramPicker {...defaultProps} excludeProgramId="prog-1" />);

    await waitFor(() => {
      expect(screen.getByTestId("program-picker-empty")).toBeInTheDocument();
    });
  });
});
