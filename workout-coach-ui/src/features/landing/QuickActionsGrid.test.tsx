import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { QuickActionsGrid } from "./QuickActionsGrid";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

function renderComponent() {
  return render(
    <MemoryRouter>
      <QuickActionsGrid />
    </MemoryRouter>
  );
}

describe("QuickActionsGrid", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it("renders a navigation landmark with 4 action items", () => {
    renderComponent();
    const nav = screen.getByRole("navigation", { name: /quick actions/i });
    expect(nav).toBeInTheDocument();

    const buttons = screen.getAllByRole("button");
    expect(buttons).toHaveLength(4);
  });

  it("renders all four action labels", () => {
    renderComponent();
    expect(screen.getByText("Upload JSON")).toBeInTheDocument();
    expect(screen.getByText("AI Gen")).toBeInTheDocument();
    expect(screen.getByText("Manage Programs")).toBeInTheDocument();
    expect(screen.getByText("Upload Pic")).toBeInTheDocument();
  });

  it("navigates to /upload when Upload JSON is clicked", async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByRole("button", { name: "Upload JSON" }));
    expect(mockNavigate).toHaveBeenCalledWith("/upload", { state: undefined });
  });

  it("navigates to /new-workout when AI Gen is clicked", async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByRole("button", { name: "AI Gen" }));
    expect(mockNavigate).toHaveBeenCalledWith("/new-workout", { state: undefined });
  });

  it("navigates to /vault/search when Manage Programs is clicked", async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByRole("button", { name: "Manage Programs" }));
    expect(mockNavigate).toHaveBeenCalledWith("/vault/search", { state: undefined });
  });

  it("navigates to /coming-soon/upload-pic when Upload Pic is clicked", async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByRole("button", { name: "Upload Pic" }));
    expect(mockNavigate).toHaveBeenCalledWith("/coming-soon/upload-pic", { state: { title: "Upload Pic" } });
  });

  it("uses a 2-column grid layout", () => {
    renderComponent();
    const grid = screen.getByTestId("quick-actions-grid");
    expect(grid.style.gridTemplateColumns).toBe("repeat(2, 1fr)");
  });

  it("renders data-testid attributes for each action", () => {
    renderComponent();
    expect(screen.getByTestId("quick-action-upload-json")).toBeInTheDocument();
    expect(screen.getByTestId("quick-action-ai-gen")).toBeInTheDocument();
    expect(screen.getByTestId("quick-action-manage-programs")).toBeInTheDocument();
    expect(screen.getByTestId("quick-action-upload-pic")).toBeInTheDocument();
  });
});
