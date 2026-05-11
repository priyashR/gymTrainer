import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { VaultSearchPage } from "../VaultSearchPage";
import type { VaultSearchState } from "../useVaultSearch";

// Requirements: 8.1, 8.2, 8.3, 8.4, 8.7, 8.8, 8.9, 8.10

const mockSetQuery = vi.fn();
const mockSetFocusArea = vi.fn();
const mockSetModality = vi.fn();
const mockExecuteSearch = vi.fn();

vi.mock("../useVaultSearch", () => ({
  useVaultSearch: () => ({
    state: mockState,
    setQuery: mockSetQuery,
    setFocusArea: mockSetFocusArea,
    setModality: mockSetModality,
    executeSearch: mockExecuteSearch,
  }),
}));

let mockState: VaultSearchState;

function defaultState(overrides: Partial<VaultSearchState> = {}): VaultSearchState {
  return {
    query: "",
    focusArea: "",
    modality: "",
    results: [],
    totalElements: 0,
    loading: false,
    error: null,
    searched: false,
    ...overrides,
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <VaultSearchPage />
    </MemoryRouter>
  );
}

beforeEach(() => {
  mockState = defaultState();
  vi.clearAllMocks();
});

describe("VaultSearchPage", () => {
  it("renders search input, focus area dropdown, modality dropdown, and search button", () => {
    renderPage();

    expect(screen.getByLabelText(/search query/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/focus area filter/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/modality filter/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /search/i })).toBeInTheDocument();
  });

  it("displays loading indicator when state.loading is true", () => {
    mockState = defaultState({ loading: true });
    renderPage();

    expect(screen.getByText(/searching/i)).toBeInTheDocument();
    expect(screen.getByText(/searching/i)).toHaveAttribute("aria-busy", "true");
  });

  it("displays empty state message when searched=true, results=[], loading=false, no error", () => {
    mockState = defaultState({ searched: true, results: [], loading: false, error: null });
    renderPage();

    expect(screen.getByText(/no programs found matching your search/i)).toBeInTheDocument();
  });

  it("displays error message when state.error is set", () => {
    mockState = defaultState({ error: "Search failed. Please try again." });
    renderPage();

    const alert = screen.getByRole("alert");
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent("Search failed. Please try again.");
  });

  it("renders VaultItemCard for each result item", () => {
    mockState = defaultState({
      searched: true,
      results: [
        {
          id: "prog-1",
          name: "Push Day Program",
          goal: "Build upper body strength",
          durationWeeks: 4,
          equipmentProfile: ["Barbell", "Dumbbells"],
          contentSource: "AI_GENERATED",
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-02T00:00:00Z",
        },
        {
          id: "prog-2",
          name: "Pull Day Program",
          goal: "Improve back development",
          durationWeeks: 6,
          equipmentProfile: ["Cable Machine"],
          contentSource: "UPLOADED",
          createdAt: "2024-01-03T00:00:00Z",
          updatedAt: "2024-01-04T00:00:00Z",
        },
      ],
    });
    renderPage();

    expect(screen.getByText("Push Day Program")).toBeInTheDocument();
    expect(screen.getByText("Pull Day Program")).toBeInTheDocument();
    expect(screen.getByText("Build upper body strength")).toBeInTheDocument();
    expect(screen.getByText("Improve back development")).toBeInTheDocument();
  });

  it("calls setQuery when user types in search input", async () => {
    const user = userEvent.setup();
    renderPage();

    const input = screen.getByLabelText(/search query/i);
    await user.type(input, "a");

    expect(mockSetQuery).toHaveBeenCalledWith("a");
  });

  it("calls setFocusArea when user changes focus area dropdown", async () => {
    const user = userEvent.setup();
    renderPage();

    const select = screen.getByLabelText(/focus area filter/i);
    await user.selectOptions(select, "Push");

    expect(mockSetFocusArea).toHaveBeenCalledWith("Push");
  });

  it("calls setModality when user changes modality dropdown", async () => {
    const user = userEvent.setup();
    renderPage();

    const select = screen.getByLabelText(/modality filter/i);
    await user.selectOptions(select, "CrossFit");

    expect(mockSetModality).toHaveBeenCalledWith("CrossFit");
  });

  it("calls executeSearch when search button is clicked", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: /search/i }));

    expect(mockExecuteSearch).toHaveBeenCalledOnce();
  });

  it("does not show empty state when searched=false (initial state)", () => {
    mockState = defaultState({ searched: false, results: [], loading: false, error: null });
    renderPage();

    expect(screen.queryByText(/no programs found matching your search/i)).not.toBeInTheDocument();
  });
});
