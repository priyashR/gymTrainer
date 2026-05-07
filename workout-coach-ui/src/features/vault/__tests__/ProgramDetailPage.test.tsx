import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { ProgramDetailPage } from "../ProgramDetailPage";
import type { ProgramState } from "../useProgram";
import type { VaultProgramDetail } from "../../../types/vault";

// Requirements: 9.1, 9.2, 9.3, 9.5, 9.6, 9.7, 9.11

const mockOnUpdate = vi.fn().mockResolvedValue({ success: true });
const mockOnDelete = vi.fn().mockResolvedValue(undefined);
const mockOnCopy = vi.fn().mockResolvedValue(undefined);
const mockReload = vi.fn();

let mockState: ProgramState;
let mockProgram: VaultProgramDetail | null;

vi.mock("../useProgram", () => ({
  useProgram: () => ({
    state: mockState,
    program: mockProgram,
    reload: mockReload,
    onUpdate: mockOnUpdate,
    onDelete: mockOnDelete,
    onCopy: mockOnCopy,
  }),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useParams: () => ({ id: "prog-123" }),
  };
});

const sampleProgram: VaultProgramDetail = {
  id: "prog-123",
  name: "Hybrid Strength 4-Week",
  goal: "Build strength and conditioning",
  durationWeeks: 4,
  equipmentProfile: ["Barbell", "Pull-up Bar"],
  contentSource: "UPLOADED",
  createdAt: "2025-01-15T10:30:00Z",
  updatedAt: "2025-01-15T10:30:00Z",
  weeks: [
    {
      weekNumber: 1,
      days: [
        {
          dayNumber: 1,
          label: "Push Day",
          focusArea: "Push",
          modality: "HYPERTROPHY",
          warmUp: [{ movement: "Arm Circles", instruction: "30 seconds each direction" }],
          sections: [
            {
              name: "Tier 1: Compound",
              sectionType: "STRENGTH",
              format: "Sets/Reps",
              exercises: [
                { name: "Bench Press", sets: 4, reps: "6-8", weight: "80% 1RM", restSeconds: 120, notes: "Control the eccentric" },
              ],
            },
          ],
          coolDown: [{ movement: "Chest Stretch", instruction: "30 seconds each side" }],
        },
      ],
    },
  ],
};

function renderPage() {
  return render(
    <MemoryRouter>
      <ProgramDetailPage />
    </MemoryRouter>
  );
}

beforeEach(() => {
  mockState = { status: "loaded", program: sampleProgram };
  mockProgram = sampleProgram;
  vi.clearAllMocks();
});

describe("ProgramDetailPage", () => {
  it("displays loading indicator when state.status is 'loading'", () => {
    mockState = { status: "loading" };
    mockProgram = null;
    renderPage();

    const loading = screen.getByText(/loading/i);
    expect(loading).toBeInTheDocument();
    expect(loading).toHaveAttribute("aria-busy", "true");
  });

  it("displays forbidden message with link back to search when state.status is 'forbidden'", () => {
    mockState = { status: "forbidden" };
    mockProgram = null;
    renderPage();

    expect(screen.getByRole("alert")).toBeInTheDocument();
    expect(screen.getByText(/access denied/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /back to search/i })).toBeInTheDocument();
  });

  it("displays error message when state.status is 'error'", () => {
    mockState = { status: "error", message: "Failed to load program." };
    mockProgram = null;
    renderPage();

    const alert = screen.getByRole("alert");
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent("Failed to load program.");
  });

  it("renders program metadata (name, goal, duration, equipment, source) when loaded", () => {
    renderPage();

    expect(screen.getByRole("heading", { name: "Hybrid Strength 4-Week" })).toBeInTheDocument();
    expect(screen.getByText("Build strength and conditioning")).toBeInTheDocument();
    expect(screen.getByText(/4 weeks/i)).toBeInTheDocument();
    expect(screen.getByText(/Barbell, Pull-up Bar/i)).toBeInTheDocument();
    expect(screen.getByText(/Uploaded/i)).toBeInTheDocument();
  });

  it("renders action buttons (Edit JSON, Copy, Delete) when loaded", () => {
    renderPage();

    expect(screen.getByRole("button", { name: /edit json/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /copy/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /delete/i })).toBeInTheDocument();
  });

  it("shows delete confirmation dialog when Delete button is clicked", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: /delete/i }));

    expect(screen.getByRole("dialog", { name: /confirm deletion/i })).toBeInTheDocument();
    expect(screen.getByText(/are you sure you want to delete/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /yes, delete/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /cancel/i })).toBeInTheDocument();
  });

  it("calls onDelete when 'Yes, Delete' is confirmed", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: /delete/i }));
    await user.click(screen.getByRole("button", { name: /yes, delete/i }));

    expect(mockOnDelete).toHaveBeenCalledOnce();
  });

  it("hides confirmation dialog when Cancel is clicked", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: /delete/i }));
    expect(screen.getByRole("dialog", { name: /confirm deletion/i })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /cancel/i }));
    expect(screen.queryByRole("dialog", { name: /confirm deletion/i })).not.toBeInTheDocument();
  });

  it("calls onCopy when Copy button is clicked", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: /copy/i }));

    expect(mockOnCopy).toHaveBeenCalledOnce();
  });

  it("toggles JSON editor when Edit JSON button is clicked", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(screen.queryByLabelText(/program json editor/i)).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /edit json/i }));
    expect(screen.getByLabelText(/program json editor/i)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /close editor/i }));
    expect(screen.queryByLabelText(/program json editor/i)).not.toBeInTheDocument();
  });

  it("renders collapsible week buttons when loaded", () => {
    renderPage();

    const weekButton = screen.getByRole("button", { name: /week 1/i });
    expect(weekButton).toBeInTheDocument();
    expect(weekButton).toHaveAttribute("aria-expanded", "false");
  });

  it("expands week to show days when week button is clicked", async () => {
    const user = userEvent.setup();
    renderPage();

    const weekButton = screen.getByRole("button", { name: /week 1/i });
    await user.click(weekButton);

    expect(weekButton).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("button", { name: /day 1: push day/i })).toBeInTheDocument();
  });

  it("expands day to show focus area, modality, and sections when day button is clicked", async () => {
    const user = userEvent.setup();
    renderPage();

    // Expand week first
    await user.click(screen.getByRole("button", { name: /week 1/i }));

    // Expand day
    const dayButton = screen.getByRole("button", { name: /day 1: push day/i });
    await user.click(dayButton);

    expect(dayButton).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText(/Focus Area:/)).toBeInTheDocument();
    expect(screen.getByText(/Modality:/)).toBeInTheDocument();
    expect(screen.getByText("HYPERTROPHY")).toBeInTheDocument();
    expect(screen.getByText("Tier 1: Compound")).toBeInTheDocument();
    expect(screen.getByText("Bench Press")).toBeInTheDocument();
  });
});
