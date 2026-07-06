import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { ProgramDetailPage } from "../ProgramDetailPage";
import type { ProgramState } from "../useProgram";
import type { VaultProgramDetail } from "../../../types/vault";

// Requirements: 12.1, 12.2, 12.3

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
    useParams: () => ({ id: "manual-prog-1" }),
  };
});

/**
 * A manual program with a copied_day assignment containing a full snapshot
 * structure: warm-up, sections with exercises, and cool-down.
 */
const programWithCopiedDay: VaultProgramDetail = {
  id: "manual-prog-1",
  name: "My Custom Plan",
  goal: "Hypertrophy focus",
  durationWeeks: 4,
  equipmentProfile: ["Barbell", "Dumbbells"],
  contentSource: "MANUAL",
  createdAt: "2025-06-01T10:00:00Z",
  updatedAt: "2025-06-01T10:00:00Z",
  weeks: [],
  dayAssignments: [
    {
      dayNumber: 1,
      type: "copied_day",
      sourceProgramId: "source-prog-123",
      sourceProgramName: "PPL Power Program",
      sourceWeekNumber: 2,
      sourceDayNumber: 3,
      snapshotData: {
        dayNumber: 3,
        label: "Push Day",
        focusArea: "Push",
        modality: "HYPERTROPHY",
        warmUp: [
          { movement: "Arm Circles", instruction: "20 each direction" },
          { movement: "Band Pull-aparts", instruction: "15 reps" },
        ],
        sections: [
          {
            name: "Tier 1: Compound",
            sectionType: "STRENGTH",
            format: "Sets/Reps",
            timeCap: null,
            exercises: [
              {
                name: "Bench Press",
                modalityType: null,
                sets: 4,
                reps: "8-10",
                weight: "80kg",
                restSeconds: 120,
                notes: "Pause at bottom",
              },
              {
                name: "Overhead Press",
                modalityType: null,
                sets: 3,
                reps: "10-12",
                weight: "40kg",
                restSeconds: 90,
                notes: null,
              },
            ],
          },
          {
            name: "Tier 2: Isolation",
            sectionType: "HYPERTROPHY",
            format: "Sets/Reps",
            timeCap: 15,
            exercises: [
              {
                name: "Lateral Raises",
                modalityType: null,
                sets: 3,
                reps: "15",
                weight: "10kg",
                restSeconds: 60,
                notes: "Slow eccentric",
              },
            ],
          },
        ],
        coolDown: [
          { movement: "Chest Stretch", instruction: "30 seconds each side" },
          { movement: "Shoulder Stretch", instruction: "20 seconds each" },
        ],
      },
    },
  ],
};

/**
 * A manual program with a copied_day where the source program has been deleted
 * (sourceProgramName is absent).
 */
const programWithDeletedSource: VaultProgramDetail = {
  id: "manual-prog-1",
  name: "My Custom Plan",
  goal: "Hypertrophy focus",
  durationWeeks: 4,
  equipmentProfile: ["Barbell"],
  contentSource: "MANUAL",
  createdAt: "2025-06-01T10:00:00Z",
  updatedAt: "2025-06-01T10:00:00Z",
  weeks: [],
  dayAssignments: [
    {
      dayNumber: 1,
      type: "copied_day",
      sourceProgramId: "deleted-prog-456",
      sourceProgramName: undefined,
      sourceWeekNumber: 1,
      sourceDayNumber: 2,
      snapshotData: {
        dayNumber: 2,
        label: "Pull Day",
        focusArea: "Pull",
        modality: "HYPERTROPHY",
        warmUp: [{ movement: "Cat-Cow", instruction: "10 reps" }],
        sections: [
          {
            name: "Main Work",
            sectionType: "STRENGTH",
            format: "Sets/Reps",
            timeCap: null,
            exercises: [
              {
                name: "Deadlift",
                modalityType: null,
                sets: 5,
                reps: "5",
                weight: "140kg",
                restSeconds: 180,
                notes: "Belt on for working sets",
              },
            ],
          },
        ],
        coolDown: [{ movement: "Hamstring Stretch", instruction: "30 seconds each" }],
      },
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
  mockState = { status: "loaded", program: programWithCopiedDay };
  mockProgram = programWithCopiedDay;
  vi.clearAllMocks();
});

describe("ProgramDetailPage — Copied Day Rendering", () => {
  describe("full snapshot structure rendering (Requirement 12.1)", () => {
    it("renders the day assignment section with copied day block", async () => {
      const user = userEvent.setup();
      renderPage();

      // The day assignments section should be present
      expect(screen.getByText("Day Assignments")).toBeInTheDocument();

      // The copied day button should show the label
      const copiedDayButton = screen.getByRole("button", { name: /day 1.*push day/i });
      expect(copiedDayButton).toBeInTheDocument();

      // Expand the copied day block
      await user.click(copiedDayButton);

      // Verify warm-up entries are rendered
      expect(screen.getByText(/Warm-Up/)).toBeInTheDocument();
      expect(screen.getByText(/Arm Circles/)).toBeInTheDocument();
      expect(screen.getByText(/20 each direction/)).toBeInTheDocument();
      expect(screen.getByText(/Band Pull-aparts/)).toBeInTheDocument();
      expect(screen.getByText(/15 reps/)).toBeInTheDocument();
    });

    it("renders sections with exercises in table format", async () => {
      const user = userEvent.setup();
      renderPage();

      // Expand the copied day block
      const copiedDayButton = screen.getByRole("button", { name: /day 1.*push day/i });
      await user.click(copiedDayButton);

      // Verify section names
      expect(screen.getByText("Tier 1: Compound")).toBeInTheDocument();
      expect(screen.getByText("Tier 2: Isolation")).toBeInTheDocument();

      // Verify section format display (both sections have Sets/Reps format)
      const formatElements = screen.getAllByText(/\(Sets\/Reps\)/);
      expect(formatElements.length).toBeGreaterThanOrEqual(1);

      // Verify time cap display for Tier 2
      expect(screen.getByText(/15 min cap/)).toBeInTheDocument();

      // Verify exercises are rendered
      expect(screen.getByText("Bench Press")).toBeInTheDocument();
      expect(screen.getByText("Overhead Press")).toBeInTheDocument();
      expect(screen.getByText("Lateral Raises")).toBeInTheDocument();

      // Verify exercise details (sets, reps, weight, rest, notes)
      expect(screen.getByText("80kg")).toBeInTheDocument();
      expect(screen.getByText("120s")).toBeInTheDocument();
      expect(screen.getByText("Pause at bottom")).toBeInTheDocument();
      expect(screen.getByText("Slow eccentric")).toBeInTheDocument();
    });

    it("renders cool-down entries", async () => {
      const user = userEvent.setup();
      renderPage();

      const copiedDayButton = screen.getByRole("button", { name: /day 1.*push day/i });
      await user.click(copiedDayButton);

      // Verify cool-down entries
      expect(screen.getByText(/Cool-Down/)).toBeInTheDocument();
      expect(screen.getByText(/Chest Stretch/)).toBeInTheDocument();
      expect(screen.getByText(/30 seconds each side/)).toBeInTheDocument();
      expect(screen.getByText(/Shoulder Stretch/)).toBeInTheDocument();
      expect(screen.getByText(/20 seconds each/)).toBeInTheDocument();
    });

    it("renders focus area and modality metadata", async () => {
      const user = userEvent.setup();
      renderPage();

      const copiedDayButton = screen.getByRole("button", { name: /day 1.*push day/i });
      await user.click(copiedDayButton);

      expect(screen.getByText(/Focus Area:/)).toBeInTheDocument();
      expect(screen.getByText("Push")).toBeInTheDocument();
      expect(screen.getByText(/Modality:/)).toBeInTheDocument();
      expect(screen.getByText("HYPERTROPHY")).toBeInTheDocument();
    });
  });

  describe("provenance display (Requirement 12.2)", () => {
    it("shows provenance with source program name when program exists", async () => {
      const user = userEvent.setup();
      renderPage();

      const copiedDayButton = screen.getByRole("button", { name: /day 1.*push day/i });
      await user.click(copiedDayButton);

      const provenance = screen.getByTestId("copied-day-1-provenance");
      expect(provenance).toHaveTextContent(
        "Copied from PPL Power Program — Week 2, Day 3"
      );
    });

    it("shows 'Deleted Program' in provenance when source program no longer exists", async () => {
      mockState = { status: "loaded", program: programWithDeletedSource };
      mockProgram = programWithDeletedSource;

      const user = userEvent.setup();
      renderPage();

      const copiedDayButton = screen.getByRole("button", { name: /day 1.*pull day/i });
      await user.click(copiedDayButton);

      const provenance = screen.getByTestId("copied-day-1-provenance");
      expect(provenance).toHaveTextContent(
        "Copied from Deleted Program — Week 1, Day 2"
      );
    });
  });

  describe("exercise display format consistency (Requirement 12.3)", () => {
    it("renders exercise table with same columns as AI-generated program display", async () => {
      const user = userEvent.setup();
      renderPage();

      const copiedDayButton = screen.getByRole("button", { name: /day 1.*push day/i });
      await user.click(copiedDayButton);

      // The table should have the same column headers as the SectionBlock component
      const headers = screen.getAllByRole("columnheader");
      const headerTexts = headers.map((h) => h.textContent);
      expect(headerTexts).toContain("Exercise");
      expect(headerTexts).toContain("Sets");
      expect(headerTexts).toContain("Reps");
      expect(headerTexts).toContain("Weight");
      expect(headerTexts).toContain("Rest");
      expect(headerTexts).toContain("Notes");
    });

    it("renders dash for null weight, rest, and notes", async () => {
      const user = userEvent.setup();
      renderPage();

      const copiedDayButton = screen.getByRole("button", { name: /day 1.*push day/i });
      await user.click(copiedDayButton);

      // Overhead Press has null notes - should show '—'
      // Find all cells with dash content - there should be at least one for the null notes
      const cells = screen.getAllByRole("cell");
      const dashCells = cells.filter((c) => c.textContent === "—");
      expect(dashCells.length).toBeGreaterThan(0);
    });
  });
});
