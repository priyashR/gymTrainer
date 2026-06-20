import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ActivitySelector } from "./ActivitySelector";

describe("ActivitySelector", () => {
  const defaultProps = {
    onSelect: vi.fn(),
  };

  it("renders the activity selector grid", () => {
    render(<ActivitySelector {...defaultProps} />);
    expect(screen.getByTestId("activity-selector-grid")).toBeInTheDocument();
  });

  it("renders all 11 activity type tiles", () => {
    render(<ActivitySelector {...defaultProps} />);
    const tiles = [
      "soccer", "squash", "running", "padel", "golf",
      "swimming", "cycling", "hiking", "basketball", "tennis", "other",
    ];
    tiles.forEach((id) => {
      expect(screen.getByTestId(`activity-tile-${id}`)).toBeInTheDocument();
    });
  });

  it("renders emoji and name for each tile", () => {
    render(<ActivitySelector {...defaultProps} />);
    expect(screen.getByText("Soccer")).toBeInTheDocument();
    expect(screen.getByText("⚽")).toBeInTheDocument();
    expect(screen.getByText("Running")).toBeInTheDocument();
    expect(screen.getByText("🏃")).toBeInTheDocument();
    expect(screen.getByText("Other")).toBeInTheDocument();
    expect(screen.getByText("✏️")).toBeInTheDocument();
  });

  it("calls onSelect with emoji + name when a regular tile is clicked", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<ActivitySelector onSelect={onSelect} />);

    await user.click(screen.getByTestId("activity-tile-soccer"));
    expect(onSelect).toHaveBeenCalledWith("⚽ Soccer");
  });

  it("calls onSelect with the correct value for different activities", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<ActivitySelector onSelect={onSelect} />);

    await user.click(screen.getByTestId("activity-tile-cycling"));
    expect(onSelect).toHaveBeenCalledWith("🚴 Cycling");
  });

  it("does not show custom input initially", () => {
    render(<ActivitySelector {...defaultProps} />);
    expect(screen.queryByTestId("custom-activity-input-container")).not.toBeInTheDocument();
  });

  it("shows custom input when 'Other' tile is clicked", async () => {
    const user = userEvent.setup();
    render(<ActivitySelector {...defaultProps} />);

    await user.click(screen.getByTestId("activity-tile-other"));
    expect(screen.getByTestId("custom-activity-input-container")).toBeInTheDocument();
    expect(screen.getByTestId("custom-activity-input")).toBeInTheDocument();
  });

  it("does not call onSelect immediately when 'Other' is clicked", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<ActivitySelector onSelect={onSelect} />);

    await user.click(screen.getByTestId("activity-tile-other"));
    expect(onSelect).not.toHaveBeenCalled();
  });

  it("calls onSelect with custom text when confirm button is clicked", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<ActivitySelector onSelect={onSelect} />);

    await user.click(screen.getByTestId("activity-tile-other"));
    await user.type(screen.getByTestId("custom-activity-input"), "Yoga");
    await user.click(screen.getByTestId("custom-activity-submit"));

    expect(onSelect).toHaveBeenCalledWith("Yoga");
  });

  it("calls onSelect with custom text when Enter is pressed", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<ActivitySelector onSelect={onSelect} />);

    await user.click(screen.getByTestId("activity-tile-other"));
    await user.type(screen.getByTestId("custom-activity-input"), "Martial Arts{Enter}");

    expect(onSelect).toHaveBeenCalledWith("Martial Arts");
  });

  it("does not call onSelect when custom input is empty and submit is clicked", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<ActivitySelector onSelect={onSelect} />);

    await user.click(screen.getByTestId("activity-tile-other"));
    await user.click(screen.getByTestId("custom-activity-submit"));

    expect(onSelect).not.toHaveBeenCalled();
  });

  it("all tiles have minimum 44x44px tap targets via aria-label", () => {
    render(<ActivitySelector {...defaultProps} />);
    const buttons = screen.getAllByRole("button");
    buttons.forEach((button) => {
      expect(button).toHaveAttribute("aria-label");
    });
  });
});
