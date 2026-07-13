import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import SearchPage from "../SearchPage";
import type { PaginatedResponse, VaultItem } from "../../types/vault";

// Mock vaultApi
vi.mock("../../lib/vaultApi", () => ({
  searchPrograms: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

import { searchPrograms } from "../../lib/vaultApi";

const mockSearchPrograms = vi.mocked(searchPrograms);

function createMockProgram(overrides: Partial<VaultItem> = {}): VaultItem {
  return {
    id: "prog-1",
    name: "Push Day A",
    goal: "Hypertrophy",
    durationWeeks: 4,
    equipmentProfile: ["Barbell", "Dumbbells"],
    contentSource: "AI_GENERATED",
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T00:00:00Z",
    ...overrides,
  };
}

function createMockResponse(
  programs: VaultItem[] = []
): PaginatedResponse<VaultItem> {
  return {
    content: programs,
    page: 0,
    size: 20,
    totalElements: programs.length,
    totalPages: 1,
  };
}

function renderSearchPage() {
  return render(
    <MemoryRouter>
      <SearchPage />
    </MemoryRouter>
  );
}

describe("SearchPage", () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.clearAllMocks();
    mockSearchPrograms.mockResolvedValue(createMockResponse([]));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe("Search input debounce (Req 5.1)", () => {
    it("debounces search input and triggers API call after 300ms", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      const programs = [createMockProgram()];
      mockSearchPrograms.mockResolvedValue(createMockResponse(programs));

      renderSearchPage();

      // Wait for the initial debounced call from mount
      await vi.advanceTimersByTimeAsync(300);
      await waitFor(() => {
        expect(mockSearchPrograms).toHaveBeenCalledTimes(1);
      });

      mockSearchPrograms.mockClear();

      const searchInput = screen.getByTestId("search-input");
      await user.type(searchInput, "push");

      // Should not have called immediately
      expect(mockSearchPrograms).not.toHaveBeenCalled();

      // Advance timers past debounce threshold
      await vi.advanceTimersByTimeAsync(300);

      await waitFor(() => {
        expect(mockSearchPrograms).toHaveBeenCalledWith(
          "push",
          undefined,
          undefined
        );
      });
    });
  });

  describe("Filter chips update results (Req 5.3)", () => {
    it("triggers a new search with updated filter parameters", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      const programs = [createMockProgram({ goal: "Strength gains" })];
      mockSearchPrograms.mockResolvedValue(createMockResponse(programs));

      renderSearchPage();

      // Wait for initial call
      await vi.advanceTimersByTimeAsync(300);
      await waitFor(() => {
        expect(mockSearchPrograms).toHaveBeenCalledTimes(1);
      });

      mockSearchPrograms.mockClear();

      // Select a focus area filter
      const focusAreaSelect = screen.getByLabelText("Focus Area");
      await user.selectOptions(focusAreaSelect, "Pull");

      // Advance debounce
      await vi.advanceTimersByTimeAsync(300);

      await waitFor(() => {
        expect(mockSearchPrograms).toHaveBeenCalledWith(
          undefined,
          "Pull",
          undefined
        );
      });
    });
  });

  describe("Results grid renders ProgramCard (Req 5.5)", () => {
    it("displays program cards in the results grid when API returns results", async () => {
      const programs = [
        createMockProgram({ id: "prog-1", name: "Push Day A" }),
        createMockProgram({ id: "prog-2", name: "Pull Day B" }),
      ];
      mockSearchPrograms.mockResolvedValue(createMockResponse(programs));

      renderSearchPage();

      await vi.advanceTimersByTimeAsync(300);

      await waitFor(() => {
        expect(screen.getByText("Push Day A")).toBeInTheDocument();
      });
      expect(screen.getByText("Pull Day B")).toBeInTheDocument();
      expect(screen.getByLabelText("Search results")).toBeInTheDocument();
    });
  });

  describe("Empty state (Req 5.7)", () => {
    it("shows EmptyState when API returns no results after search", async () => {
      mockSearchPrograms.mockResolvedValue(createMockResponse([]));

      renderSearchPage();

      await vi.advanceTimersByTimeAsync(300);

      await waitFor(() => {
        expect(
          screen.getByText("No matching programs found")
        ).toBeInTheDocument();
      });
      expect(
        screen.getByText("Try adjusting your search or filters")
      ).toBeInTheDocument();
    });
  });

  describe("Create Program link (Req 5.6)", () => {
    it("navigates to /programs/create when Create Program link is clicked", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      renderSearchPage();

      const createLink = screen.getByTestId("create-program-link");
      await user.click(createLink);

      expect(mockNavigate).toHaveBeenCalledWith("/programs/create");
    });
  });

  describe("Program card navigation (Req 5.8)", () => {
    it("navigates to /vault/programs/:id when a program card is clicked", async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      const programs = [
        createMockProgram({ id: "prog-123", name: "My Program" }),
      ];
      mockSearchPrograms.mockResolvedValue(createMockResponse(programs));

      renderSearchPage();

      await vi.advanceTimersByTimeAsync(300);

      await waitFor(() => {
        expect(screen.getByText("My Program")).toBeInTheDocument();
      });

      const programCard = screen.getByLabelText("View program: My Program");
      await user.click(programCard);

      expect(mockNavigate).toHaveBeenCalledWith("/vault/programs/prog-123");
    });
  });
});
