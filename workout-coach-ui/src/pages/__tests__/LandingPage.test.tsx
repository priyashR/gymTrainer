import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import LandingPage from "../LandingPage";

// Mock apiClient
vi.mock("../../lib/apiClient", () => ({
  default: {
    get: vi.fn(),
  },
}));

// Mock hooks
vi.mock("../../hooks/useWeeklyStats", () => ({
  useWeeklyStats: vi.fn(() => ({
    data: { completedCount: 3, goalCount: 7, dailyData: [true, true, true, false, false, false, false] },
    isLoading: false,
    error: null,
  })),
}));

vi.mock("../../hooks/usePerformanceData", () => ({
  usePerformanceData: () => ({
    topExercises: [{ exerciseName: "Bench Press", maxWeight: 100, unit: "kg" }],
    monthlyFrequency: [{ month: "Jan", count: 12 }],
    isLoading: false,
    error: null,
  }),
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

import apiClient from "../../lib/apiClient";

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

function renderLandingPage() {
  return render(
    <MemoryRouter>
      <LandingPage />
    </MemoryRouter>
  );
}

describe("LandingPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Default: narrow viewport (mobile) for tests
    mockMatchMedia(false);
    Object.defineProperty(window, "innerWidth", { writable: true, value: 600 });
    (apiClient.get as ReturnType<typeof vi.fn>).mockRejectedValue(new Error("not found"));
  });

  it("renders the page with heading and primary buttons", () => {
    renderLandingPage();

    expect(screen.getByTestId("landing-page")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /hybridstrength/i })).toBeInTheDocument();
    expect(screen.getByTestId("new-workout-button")).toBeInTheDocument();
    expect(screen.getByTestId("log-activity-button")).toBeInTheDocument();
  });

  it("navigates to /vault/search when New Workout is clicked", async () => {
    const user = userEvent.setup();
    renderLandingPage();

    await user.click(screen.getByTestId("new-workout-button"));
    expect(mockNavigate).toHaveBeenCalledWith("/vault/search");
  });

  it("navigates to /log-activity when Log Activity is clicked", async () => {
    const user = userEvent.setup();
    renderLandingPage();

    await user.click(screen.getByTestId("log-activity-button"));
    expect(mockNavigate).toHaveBeenCalledWith("/log-activity");
  });

  it("hides ResumeWorkoutCard when no active session exists (Req 2.2)", () => {
    renderLandingPage();

    expect(screen.queryByTestId("resume-workout-card")).not.toBeInTheDocument();
  });

  it("shows ResumeWorkoutCard when active session exists (Req 2.1)", async () => {
    (apiClient.get as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
      if (url === "/sessions/active") {
        return Promise.resolve({
          data: {
            sessionId: "session-1",
            workoutName: "Push Day A",
            status: "active",
            progress: 45,
          },
        });
      }
      return Promise.reject(new Error("not found"));
    });

    renderLandingPage();

    await waitFor(() => {
      expect(screen.getByTestId("resume-workout-card")).toBeInTheDocument();
    });
    expect(screen.getByText("Push Day A")).toBeInTheDocument();
  });

  it("renders WeeklyStatsChart section (Req 2.5)", () => {
    renderLandingPage();

    expect(screen.getByTestId("weekly-stats-chart")).toBeInTheDocument();
  });

  it("renders WeeklyStatsChart placeholder when data unavailable (Req 2.5, 2.7)", async () => {
    // Override the useWeeklyStats mock to return null data
    const { useWeeklyStats } = await import("../../hooks/useWeeklyStats");
    vi.mocked(useWeeklyStats).mockReturnValue({
      data: null,
      isLoading: false,
      error: "Service unavailable",
    });

    renderLandingPage();

    expect(screen.getByTestId("weekly-stats-chart")).toBeInTheDocument();
    expect(screen.getByText("Weekly stats not available")).toBeInTheDocument();
    expect(screen.getByText("Data will appear once tracking is active")).toBeInTheDocument();
  });

  it("renders QuickActionsGrid with 4 navigation items and correct links (Req 2.8)", async () => {
    const user = userEvent.setup();
    renderLandingPage();

    expect(screen.getByTestId("quick-actions-grid")).toBeInTheDocument();

    // Verify all 4 quick action items are rendered
    expect(screen.getByTestId("quick-action-upload-json")).toBeInTheDocument();
    expect(screen.getByTestId("quick-action-ai-gen")).toBeInTheDocument();
    expect(screen.getByTestId("quick-action-manage-programs")).toBeInTheDocument();
    expect(screen.getByTestId("quick-action-upload-pic")).toBeInTheDocument();

    // Verify labels
    expect(screen.getByLabelText("Upload JSON")).toBeInTheDocument();
    expect(screen.getByLabelText("AI Gen")).toBeInTheDocument();
    expect(screen.getByLabelText("Manage Programs")).toBeInTheDocument();
    expect(screen.getByLabelText("Upload Pic")).toBeInTheDocument();

    // Verify navigation on click (Req 2.9, 2.10, 2.11, 2.12)
    await user.click(screen.getByTestId("quick-action-upload-json"));
    expect(mockNavigate).toHaveBeenCalledWith("/upload", expect.anything());

    mockNavigate.mockClear();
    await user.click(screen.getByTestId("quick-action-ai-gen"));
    expect(mockNavigate).toHaveBeenCalledWith("/new-workout", expect.anything());

    mockNavigate.mockClear();
    await user.click(screen.getByTestId("quick-action-manage-programs"));
    expect(mockNavigate).toHaveBeenCalledWith("/vault/search", expect.anything());

    mockNavigate.mockClear();
    await user.click(screen.getByTestId("quick-action-upload-pic"));
    expect(mockNavigate).toHaveBeenCalledWith("/coming-soon/upload-pic", expect.anything());
  });

  it("renders PerformanceDashboard (Req 2.6)", () => {
    renderLandingPage();

    expect(screen.getByTestId("performance-dashboard")).toBeInTheDocument();
  });

  describe("FAB Action Sheet (Req 2.4)", () => {
    it("opens action sheet when FAB is clicked", async () => {
      const user = userEvent.setup();
      renderLandingPage();

      expect(screen.queryByTestId("fab-action-sheet")).not.toBeInTheDocument();

      await user.click(screen.getByLabelText("Add new item"));

      expect(screen.getByTestId("fab-action-sheet")).toBeInTheDocument();
      expect(screen.getByTestId("action-create-program")).toBeInTheDocument();
      expect(screen.getByTestId("action-manual-json")).toBeInTheDocument();
      expect(screen.getByTestId("action-start-from-vault")).toBeInTheDocument();
    });

    it("navigates to /programs/create on Create Program click", async () => {
      const user = userEvent.setup();
      renderLandingPage();

      await user.click(screen.getByLabelText("Add new item"));
      await user.click(screen.getByTestId("action-create-program"));

      expect(mockNavigate).toHaveBeenCalledWith("/programs/create");
    });

    it("navigates to /manual-input on Manual JSON Input click", async () => {
      const user = userEvent.setup();
      renderLandingPage();

      await user.click(screen.getByLabelText("Add new item"));
      await user.click(screen.getByTestId("action-manual-json"));

      expect(mockNavigate).toHaveBeenCalledWith("/manual-input");
    });

    it("navigates to /vault/search on Start from Vault click", async () => {
      const user = userEvent.setup();
      renderLandingPage();

      await user.click(screen.getByLabelText("Add new item"));
      await user.click(screen.getByTestId("action-start-from-vault"));

      expect(mockNavigate).toHaveBeenCalledWith("/vault/search");
    });

    it("closes action sheet on Cancel click", async () => {
      const user = userEvent.setup();
      renderLandingPage();

      await user.click(screen.getByLabelText("Add new item"));
      expect(screen.getByTestId("fab-action-sheet")).toBeInTheDocument();

      await user.click(screen.getByTestId("action-sheet-cancel"));
      expect(screen.queryByTestId("fab-action-sheet")).not.toBeInTheDocument();
    });

    it("closes action sheet when clicking overlay", async () => {
      const user = userEvent.setup();
      renderLandingPage();

      await user.click(screen.getByLabelText("Add new item"));
      const overlay = screen.getByTestId("fab-action-sheet");

      await user.click(overlay);
      expect(screen.queryByTestId("fab-action-sheet")).not.toBeInTheDocument();
    });
  });
});
