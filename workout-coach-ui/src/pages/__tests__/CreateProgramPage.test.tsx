import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import CreateProgramPage from "../CreateProgramPage";

// Mock apiClient
vi.mock("../../lib/apiClient", () => ({
  default: {
    post: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Mock the DayAssignmentModal to simplify testing
vi.mock("../../features/program/DayAssignmentModal", () => ({
  DayAssignmentModal: ({
    isOpen,
    dayNumber,
    onAssign,
    onClose,
  }: {
    isOpen: boolean;
    dayNumber: number;
    onAssign: (assignment: { type: string; workoutId?: string; workoutName?: string; activityType?: string }) => void;
    onClose: () => void;
  }) => {
    if (!isOpen) return null;
    return (
      <div data-testid="mock-assignment-modal">
        <span data-testid="modal-day-number">{dayNumber}</span>
        <button
          data-testid="mock-assign-workout"
          onClick={() =>
            onAssign({
              type: "workout",
              workoutId: "test-workout-id",
              workoutName: "Push Day A",
            })
          }
        >
          Assign Workout
        </button>
        <button
          data-testid="mock-assign-activity"
          onClick={() =>
            onAssign({
              type: "activity",
              activityType: "Soccer",
            })
          }
        >
          Assign Activity
        </button>
        <button data-testid="mock-close-modal" onClick={onClose}>
          Close
        </button>
      </div>
    );
  },
}));

import apiClient from "../../lib/apiClient";

function renderCreateProgramPage() {
  return render(
    <MemoryRouter>
      <CreateProgramPage />
    </MemoryRouter>
  );
}

describe("CreateProgramPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the page with heading, back link, name input, and save button", () => {
    renderCreateProgramPage();

    expect(screen.getByTestId("create-program-page")).toBeInTheDocument();
    expect(screen.getByTestId("back-link")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /create program/i })).toBeInTheDocument();
    expect(screen.getByTestId("program-name-input")).toBeInTheDocument();
    expect(screen.getByTestId("save-program-button")).toBeInTheDocument();
  });

  it("renders a back link that navigates to home", () => {
    renderCreateProgramPage();

    const backLink = screen.getByTestId("back-link");
    expect(backLink).toHaveAttribute("href", "/");
    expect(backLink).toHaveTextContent("← Home");
  });

  it("renders initial 3 day tiles via DayTilesGrid", () => {
    renderCreateProgramPage();

    expect(screen.getByTestId("day-tiles-grid")).toBeInTheDocument();
    expect(screen.getByTestId("day-tile-1")).toBeInTheDocument();
    expect(screen.getByTestId("day-tile-2")).toBeInTheDocument();
    expect(screen.getByTestId("day-tile-3")).toBeInTheDocument();
  });

  it("shows validation error when saving without program name (Req 3.13)", async () => {
    const user = userEvent.setup();
    renderCreateProgramPage();

    // Assign a workout to day 1 first so we only test name validation
    await user.click(screen.getByTestId("day-tile-1-assign"));
    await user.click(screen.getByTestId("mock-assign-workout"));

    await user.click(screen.getByTestId("save-program-button"));

    expect(screen.getByTestId("program-name-error")).toBeInTheDocument();
    expect(screen.getByTestId("program-name-error")).toHaveTextContent(
      "Program name is required."
    );
  });

  it("shows validation error when saving with zero assignments (Req 3.14)", async () => {
    const user = userEvent.setup();
    renderCreateProgramPage();

    // Enter a program name but don't assign anything
    await user.type(screen.getByTestId("program-name-input"), "My Program");
    await user.click(screen.getByTestId("save-program-button"));

    expect(screen.getByTestId("days-assignment-error")).toBeInTheDocument();
    expect(screen.getByTestId("days-assignment-error")).toHaveTextContent(
      "At least one day must have a workout or activity assigned."
    );
  });

  it("clears name validation error when user types in the input", async () => {
    const user = userEvent.setup();
    renderCreateProgramPage();

    // Trigger name error
    await user.click(screen.getByTestId("day-tile-1-assign"));
    await user.click(screen.getByTestId("mock-assign-workout"));
    await user.click(screen.getByTestId("save-program-button"));

    expect(screen.getByTestId("program-name-error")).toBeInTheDocument();

    // Typing should clear the error
    await user.type(screen.getByTestId("program-name-input"), "A");
    expect(screen.queryByTestId("program-name-error")).not.toBeInTheDocument();
  });

  it("opens DayAssignmentModal when a day tile is tapped", async () => {
    const user = userEvent.setup();
    renderCreateProgramPage();

    expect(screen.queryByTestId("mock-assignment-modal")).not.toBeInTheDocument();

    await user.click(screen.getByTestId("day-tile-1-assign"));

    expect(screen.getByTestId("mock-assignment-modal")).toBeInTheDocument();
    expect(screen.getByTestId("modal-day-number")).toHaveTextContent("1");
  });

  it("assigns a workout from Vault to a day (Req 3.8)", async () => {
    const user = userEvent.setup();
    renderCreateProgramPage();

    await user.click(screen.getByTestId("day-tile-1-assign"));
    await user.click(screen.getByTestId("mock-assign-workout"));

    // Modal should close and day tile should show assignment
    expect(screen.queryByTestId("mock-assignment-modal")).not.toBeInTheDocument();
    expect(screen.getByTestId("day-tile-1-name")).toHaveTextContent("Push Day A");
  });

  it("assigns an activity to a day (Req 3.9)", async () => {
    const user = userEvent.setup();
    renderCreateProgramPage();

    await user.click(screen.getByTestId("day-tile-2-assign"));
    await user.click(screen.getByTestId("mock-assign-activity"));

    // Modal should close and day tile should show the activity name
    expect(screen.queryByTestId("mock-assignment-modal")).not.toBeInTheDocument();
    expect(screen.getByTestId("day-tile-2-name")).toHaveTextContent("Soccer");
  });

  it("calls API and navigates on successful save (Req 3.12, 3.16)", async () => {
    const user = userEvent.setup();
    (apiClient.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: { id: "new-program-id-123" },
    });

    renderCreateProgramPage();

    // Fill program name
    await user.type(screen.getByTestId("program-name-input"), "PPL Hypertrophy");

    // Assign a workout to day 1
    await user.click(screen.getByTestId("day-tile-1-assign"));
    await user.click(screen.getByTestId("mock-assign-workout"));

    // Save
    await user.click(screen.getByTestId("save-program-button"));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith("/vault/programs", {
        programName: "PPL Hypertrophy",
        days: [
          { dayNumber: 1, type: "workout", workoutId: "test-workout-id" },
        ],
      });
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/vault/programs/new-program-id-123");
    });
  });

  it("shows error banner on save failure and preserves data (Req 3.15)", async () => {
    const user = userEvent.setup();
    (apiClient.post as ReturnType<typeof vi.fn>).mockRejectedValueOnce({
      response: { data: { message: "Internal server error" } },
    });

    renderCreateProgramPage();

    // Fill program name
    await user.type(screen.getByTestId("program-name-input"), "My Program");

    // Assign an activity to day 2
    await user.click(screen.getByTestId("day-tile-2-assign"));
    await user.click(screen.getByTestId("mock-assign-activity"));

    // Save
    await user.click(screen.getByTestId("save-program-button"));

    await waitFor(() => {
      expect(screen.getByTestId("save-error-banner")).toBeInTheDocument();
    });
    expect(screen.getByTestId("save-error-banner")).toHaveTextContent(
      "Internal server error"
    );

    // Data should still be preserved
    expect(screen.getByTestId("program-name-input")).toHaveValue("My Program");
    expect(screen.getByTestId("day-tile-2-name")).toHaveTextContent("Soccer");
  });

  it("disables save button and shows loading text while saving (Req 3.15)", async () => {
    const user = userEvent.setup();
    let resolvePost: (value: unknown) => void;
    (apiClient.post as ReturnType<typeof vi.fn>).mockReturnValueOnce(
      new Promise((resolve) => {
        resolvePost = resolve;
      })
    );

    renderCreateProgramPage();

    await user.type(screen.getByTestId("program-name-input"), "My Program");
    await user.click(screen.getByTestId("day-tile-1-assign"));
    await user.click(screen.getByTestId("mock-assign-workout"));

    await user.click(screen.getByTestId("save-program-button"));

    // Button should be disabled with saving text
    expect(screen.getByTestId("save-program-button")).toBeDisabled();
    expect(screen.getByTestId("save-program-button")).toHaveTextContent("Saving…");

    // Resolve the promise to finish saving
    resolvePost!({ data: { id: "abc" } });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalled();
    });
  });

  it("adds a new day when Add Day button is clicked", async () => {
    const user = userEvent.setup();
    renderCreateProgramPage();

    expect(screen.queryByTestId("day-tile-4")).not.toBeInTheDocument();

    await user.click(screen.getByTestId("add-day-tile"));

    expect(screen.getByTestId("day-tile-4")).toBeInTheDocument();
  });

  it("removes a day when remove button is clicked", async () => {
    const user = userEvent.setup();
    renderCreateProgramPage();

    // Assign something to day 1 so we get a remove button
    await user.click(screen.getByTestId("day-tile-1-assign"));
    await user.click(screen.getByTestId("mock-assign-workout"));

    // Now remove day 1
    await user.click(screen.getByTestId("day-tile-1-remove"));

    // Should only have 2 tiles now (what was day 2 and 3 become day 1 and 2)
    expect(screen.getByTestId("day-tile-1")).toBeInTheDocument();
    expect(screen.getByTestId("day-tile-2")).toBeInTheDocument();
    expect(screen.queryByTestId("day-tile-3")).not.toBeInTheDocument();
  });
});
