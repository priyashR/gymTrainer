import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ActivityTypeGrid, ACTIVITY_TYPES } from "./ActivityTypeGrid";

function renderComponent(
  selectedType: string | null = null,
  onSelect = vi.fn()
) {
  return {
    onSelect,
    ...render(
      <ActivityTypeGrid selectedType={selectedType} onSelect={onSelect} />
    ),
  };
}

describe("ActivityTypeGrid", () => {
  it("renders a 3-column grid with all activity tiles", () => {
    renderComponent();
    const grid = screen.getByRole("group", { name: /activity types/i });
    expect(grid).toBeInTheDocument();
    expect(grid.style.gridTemplateColumns).toBe("repeat(3, 1fr)");

    const buttons = screen.getAllByRole("button");
    expect(buttons).toHaveLength(ACTIVITY_TYPES.length);
  });

  it("renders emoji and name for each activity type", () => {
    renderComponent();
    for (const activity of ACTIVITY_TYPES) {
      expect(screen.getByText(activity.name)).toBeInTheDocument();
    }
    // Padel and Tennis share the 🎾 emoji, so use getAllByText for that one
    const uniqueEmojis = [...new Set(ACTIVITY_TYPES.map((a) => a.emoji))];
    for (const emoji of uniqueEmojis) {
      const matches = screen.getAllByText(emoji);
      expect(matches.length).toBeGreaterThan(0);
    }
  });

  it("calls onSelect with the activity id when a tile is clicked", async () => {
    const user = userEvent.setup();
    const { onSelect } = renderComponent();

    await user.click(screen.getByRole("button", { name: "Soccer" }));
    expect(onSelect).toHaveBeenCalledWith("soccer");
  });

  it("marks the selected tile with aria-pressed=true", () => {
    renderComponent("running");
    const runningTile = screen.getByRole("button", { name: "Running" });
    expect(runningTile).toHaveAttribute("aria-pressed", "true");

    const soccerTile = screen.getByRole("button", { name: "Soccer" });
    expect(soccerTile).toHaveAttribute("aria-pressed", "false");
  });

  it("shows a text input when Other is selected", () => {
    renderComponent("other");
    const input = screen.getByTestId("custom-activity-input");
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute("placeholder", "Enter activity name");
  });

  it("does not show a text input when a non-Other activity is selected", () => {
    renderComponent("soccer");
    expect(screen.queryByTestId("custom-activity-input")).not.toBeInTheDocument();
  });

  it("calls onSelect with custom text when typing in the Other input", async () => {
    const user = userEvent.setup();
    const { onSelect } = renderComponent("other");

    const input = screen.getByTestId("custom-activity-input");
    await user.type(input, "Yoga");

    // onSelect is called on each keystroke with the trimmed value
    expect(onSelect).toHaveBeenLastCalledWith("Yoga");
  });

  it("applies selected styles to the chosen tile", () => {
    renderComponent("golf");
    const golfTile = screen.getByTestId("activity-tile-golf");
    expect(golfTile.style.borderColor).toBe("var(--color-accent)");
  });

  it("renders data-testid attributes for each tile", () => {
    renderComponent();
    for (const activity of ACTIVITY_TYPES) {
      expect(
        screen.getByTestId(`activity-tile-${activity.id}`)
      ).toBeInTheDocument();
    }
  });

  it("ensures tiles have minimum tap target size", () => {
    renderComponent();
    const firstTile = screen.getByTestId("activity-tile-soccer");
    expect(firstTile.style.minHeight).toBe("var(--tap-target-min)");
    expect(firstTile.style.minWidth).toBe("var(--tap-target-min)");
  });
});
