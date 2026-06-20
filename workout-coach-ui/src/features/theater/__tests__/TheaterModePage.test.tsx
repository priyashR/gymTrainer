import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

// --- Mock hooks and router ---

const mockAcquire = vi.fn();
const mockRelease = vi.fn();

vi.mock("../../../hooks/useWakeLock", () => ({
  useWakeLock: () => ({ acquire: mockAcquire, release: mockRelease }),
}));

const mockCompleteExercise = vi.fn();
const mockAdvanceSection = vi.fn();
const mockPauseSession = vi.fn();
const mockResumeSession = vi.fn();
const mockEndSession = vi.fn();
const mockLogSet = vi.fn();
const mockLogCrossFitScore = vi.fn();

let mockSessionReturn: ReturnType<typeof buildMockSessionReturn>;

function buildMockSessionReturn(overrides: Record<string, unknown> = {}) {
  return {
    session: buildStrengthSession(),
    loading: false,
    error: null,
    completeExercise: mockCompleteExercise,
    advanceSection: mockAdvanceSection,
    pauseSession: mockPauseSession,
    resumeSession: mockResumeSession,
    endSession: mockEndSession,
    logSet: mockLogSet,
    logCrossFitScore: mockLogCrossFitScore,
    ...overrides,
  };
}

vi.mock("../../../hooks/useSession", () => ({
  useSession: () => mockSessionReturn,
}));

vi.mock("../../../hooks/useRecommendations", () => ({
  useRecommendations: () => ({
    recommendations: [],
    isLoading: false,
    error: null,
  }),
}));

const mockNavigate = vi.fn();

vi.mock("react-router-dom", () => ({
  useParams: () => ({ sessionId: "test-session-123" }),
  useNavigate: () => mockNavigate,
}));

// --- Import component AFTER mocks are defined ---

import { TheaterModePage } from "../TheaterModePage";

// --- Test data builders ---

function buildStrengthSession(overrides: Record<string, unknown> = {}) {
  return {
    id: "test-session-123",
    status: "IN_PROGRESS" as const,
    currentSectionIndex: 0,
    sectionProgresses: [
      {
        sectionIndex: 0,
        sectionName: "Compound",
        sectionType: "STRENGTH" as const,
        exerciseLogs: [
          {
            exerciseIndex: 0,
            exerciseName: "Back Squat",
            completed: false,
            completedAt: null,
            setLogs: [],
          },
          {
            exerciseIndex: 1,
            exerciseName: "Romanian Deadlift",
            completed: false,
            completedAt: null,
            setLogs: [],
          },
        ],
        completed: false,
        crossFitScore: null,
        roundCount: 0,
      },
      {
        sectionIndex: 1,
        sectionName: "Accessory",
        sectionType: "STRENGTH" as const,
        exerciseLogs: [
          {
            exerciseIndex: 0,
            exerciseName: "Leg Curl",
            completed: false,
            completedAt: null,
            setLogs: [],
          },
        ],
        completed: false,
        crossFitScore: null,
        roundCount: 0,
      },
    ],
    workoutSnapshot: {
      sections: [
        {
          exercises: [
            { name: "Back Squat", sets: 4, reps: 6, weight: 120, restSeconds: 180 },
            { name: "Romanian Deadlift", sets: 3, reps: 10, weight: 80 },
          ],
        },
        {
          exercises: [
            { name: "Leg Curl", sets: 3, reps: 12, weight: 40 },
          ],
        },
      ],
    },
    startedAt: new Date(Date.now() - 600000).toISOString(), // 10 min ago
    pausedAt: null,
    completedAt: null,
    durationSeconds: null,
    ...overrides,
  };
}

function buildCrossFitSession() {
  return {
    id: "test-session-123",
    status: "IN_PROGRESS" as const,
    currentSectionIndex: 0,
    sectionProgresses: [
      {
        sectionIndex: 0,
        sectionName: "Fran",
        sectionType: "AMRAP" as const,
        exerciseLogs: [
          {
            exerciseIndex: 0,
            exerciseName: "Thrusters",
            completed: false,
            completedAt: null,
            setLogs: [],
          },
          {
            exerciseIndex: 1,
            exerciseName: "Pull-ups",
            completed: false,
            completedAt: null,
            setLogs: [],
          },
        ],
        completed: false,
        crossFitScore: null,
        roundCount: 3,
      },
    ],
    workoutSnapshot: {
      sections: [
        {
          timeCapSeconds: 720,
          exercises: [
            { name: "Thrusters", reps: 21, weight: 43 },
            { name: "Pull-ups", reps: 21 },
          ],
        },
      ],
    },
    startedAt: new Date(Date.now() - 300000).toISOString(),
    pausedAt: null,
    completedAt: null,
    durationSeconds: null,
  };
}

// --- matchMedia mock utility ---

function mockMatchMedia(matches: boolean) {
  const listeners: Array<(e: MediaQueryListEvent) => void> = [];
  const mql = {
    matches,
    media: "(min-width: 900px)",
    addEventListener: (_event: string, handler: (e: MediaQueryListEvent) => void) => {
      listeners.push(handler);
    },
    removeEventListener: (_event: string, handler: (e: MediaQueryListEvent) => void) => {
      const idx = listeners.indexOf(handler);
      if (idx >= 0) listeners.splice(idx, 1);
    },
    dispatchEvent: () => true,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
  };
  Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: vi.fn().mockReturnValue(mql),
  });
  return { mql, listeners };
}

// --- Test suite ---

describe("TheaterModePage", () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    mockAcquire.mockReset();
    mockRelease.mockReset();
    mockCompleteExercise.mockReset();
    mockAdvanceSection.mockReset();
    mockEndSession.mockReset();
    mockLogSet.mockReset();
    mockNavigate.mockReset();
    // Default: wide viewport
    mockMatchMedia(true);
    // Default session
    mockSessionReturn = buildMockSessionReturn();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  // --- Requirement 4.2, 4.9: Tier navigation ---

  describe("Tier navigation", () => {
    it("displays the tier label and section type in the header", () => {
      render(<TheaterModePage />);

      expect(screen.getByText("Tier 1: Compound")).toBeInTheDocument();
      expect(screen.getByText("Strength")).toBeInTheDocument();
    });

    it("disables the Previous tier button on the first tier", () => {
      render(<TheaterModePage />);

      const prevButton = screen.getByRole("button", { name: /previous tier/i });
      expect(prevButton).toBeDisabled();
    });

    it("enables the Next tier button when there are more tiers", () => {
      render(<TheaterModePage />);

      const nextButton = screen.getByRole("button", { name: /next tier/i });
      expect(nextButton).not.toBeDisabled();
    });

    it("advances to the next tier when Next tier is clicked", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      render(<TheaterModePage />);

      const nextButton = screen.getByRole("button", { name: /next tier/i });
      await user.click(nextButton);

      expect(mockAdvanceSection).toHaveBeenCalledWith(1);
    });

    it("does not navigate before first tier when Prev is disabled", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      render(<TheaterModePage />);

      const prevButton = screen.getByRole("button", { name: /previous tier/i });
      await user.click(prevButton);

      expect(mockAdvanceSection).not.toHaveBeenCalled();
    });
  });

  // --- Requirement 4.6: Exercise completion checkbox persists data ---

  describe("Exercise completion", () => {
    it("renders exercise checkboxes in the exercise panel", () => {
      render(<TheaterModePage />);

      const exercisePanel = screen.getByTestId("exercise-panel");
      expect(exercisePanel).toBeInTheDocument();
      // Exercises appear in both ExercisePanel and LoggingPanel — use getAllByText
      expect(screen.getAllByText("Back Squat").length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText("Romanian Deadlift").length).toBeGreaterThanOrEqual(1);
    });

    it("calls completeExercise when an exercise checkbox is toggled", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      render(<TheaterModePage />);

      // The ExerciseRow renders a checkbox for each exercise
      const checkboxes = screen.getAllByRole("checkbox");
      expect(checkboxes.length).toBeGreaterThanOrEqual(1);

      await user.click(checkboxes[0]);

      expect(mockCompleteExercise).toHaveBeenCalledWith(0, 0);
    });
  });

  // --- Requirement 4.12: Wake lock acquired on mount and released on unmount ---

  describe("Wake lock", () => {
    it("acquires wake lock on mount", () => {
      render(<TheaterModePage />);

      expect(mockAcquire).toHaveBeenCalled();
    });

    it("releases wake lock on unmount", () => {
      const { unmount } = render(<TheaterModePage />);

      mockRelease.mockReset();
      unmount();

      expect(mockRelease).toHaveBeenCalled();
    });
  });

  // --- Requirement 6.4: Layout switches based on viewport width ---

  describe("Responsive layout", () => {
    it("renders side-by-side layout at viewport ≥900px", () => {
      mockMatchMedia(true);
      Object.defineProperty(window, "innerWidth", { writable: true, value: 1024 });

      render(<TheaterModePage />);

      const page = screen.getByTestId("theater-mode-page");
      // The main element should have grid with 2 columns
      const main = page.querySelector("main");
      expect(main).not.toBeNull();
      expect(main!.style.gridTemplateColumns).toBe("1fr 1fr");
    });

    it("renders stacked layout at viewport <900px", () => {
      mockMatchMedia(false);
      Object.defineProperty(window, "innerWidth", { writable: true, value: 600 });

      render(<TheaterModePage />);

      const page = screen.getByTestId("theater-mode-page");
      const main = page.querySelector("main");
      expect(main).not.toBeNull();
      expect(main!.style.gridTemplateColumns).toBe("1fr");
    });
  });

  // --- Requirement 4.4: CrossFit variant shows RoundCounter and AMRAPInfoCard ---

  describe("CrossFit variant", () => {
    it("shows RoundCounter in logging panel for AMRAP section type", () => {
      mockSessionReturn = buildMockSessionReturn({
        session: buildCrossFitSession(),
      });

      render(<TheaterModePage />);

      expect(screen.getByTestId("round-counter")).toBeInTheDocument();
      expect(screen.getByText("Rounds Completed")).toBeInTheDocument();
      expect(screen.getByText("3")).toBeInTheDocument();
    });

    it("shows AMRAPInfoCard in exercise panel for CrossFit section type", () => {
      mockSessionReturn = buildMockSessionReturn({
        session: buildCrossFitSession(),
      });

      render(<TheaterModePage />);

      const amrapCard = screen.getByTestId("amrap-info-card");
      expect(amrapCard).toBeInTheDocument();
      // The card contains the AMRAP label and time cap
      expect(amrapCard).toHaveTextContent("AMRAP");
      expect(amrapCard).toHaveTextContent("12:00");
    });
  });

  // --- Loading and error states ---

  describe("Loading and error states", () => {
    it("shows loading state while session is being fetched", () => {
      mockSessionReturn = buildMockSessionReturn({
        session: null,
        loading: true,
      });

      render(<TheaterModePage />);

      expect(screen.getByText("Loading session…")).toBeInTheDocument();
    });

    it("shows error state when session fails to load", () => {
      mockSessionReturn = buildMockSessionReturn({
        session: null,
        loading: false,
        error: "Network error",
      });

      render(<TheaterModePage />);

      expect(screen.getByText("Unable to load session")).toBeInTheDocument();
      expect(screen.getByText("Network error")).toBeInTheDocument();
    });
  });

  // --- Component composition ---

  describe("Component composition", () => {
    it("renders TheaterHeader with elapsed time", () => {
      render(<TheaterModePage />);

      // The header renders a timer role element
      expect(screen.getByRole("timer")).toBeInTheDocument();
    });

    it("renders ExercisePanel with exercises from session data", () => {
      render(<TheaterModePage />);

      const exercisePanel = screen.getByTestId("exercise-panel");
      expect(exercisePanel).toBeInTheDocument();
      // Exercise names appear in ExercisePanel rows
      expect(screen.getByTestId("exercise-row-0")).toHaveTextContent("Back Squat");
      expect(screen.getByTestId("exercise-row-1")).toHaveTextContent("Romanian Deadlift");
    });

    it("renders LoggingPanel in the correct variant", () => {
      render(<TheaterModePage />);

      expect(screen.getByTestId("logging-panel")).toBeInTheDocument();
      // Strength variant shows set-log-form
      expect(screen.getByTestId("set-log-form")).toBeInTheDocument();
    });
  });
});
