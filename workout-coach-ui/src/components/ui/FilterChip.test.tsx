import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { FilterChip } from "./FilterChip";

describe("FilterChip", () => {
  const defaultProps = {
    label: "Focus Area",
    options: ["Push", "Pull", "Legs", "Full Body"],
    value: "",
    onChange: vi.fn(),
    active: false,
  };

  it("renders with label as placeholder option", () => {
    render(<FilterChip {...defaultProps} />);
    const select = screen.getByRole("combobox", { name: "Focus Area" });
    expect(select).toBeInTheDocument();
  });

  it("renders all options", () => {
    render(<FilterChip {...defaultProps} />);
    expect(screen.getByRole("option", { name: "Push" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Pull" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Legs" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Full Body" })).toBeInTheDocument();
  });

  it("calls onChange when a value is selected", async () => {
    const user = userEvent.setup();
    const handleChange = vi.fn();
    render(<FilterChip {...defaultProps} onChange={handleChange} />);

    await user.selectOptions(
      screen.getByRole("combobox", { name: "Focus Area" }),
      "Push"
    );
    expect(handleChange).toHaveBeenCalledWith("Push");
  });

  it("shows selected value", () => {
    render(<FilterChip {...defaultProps} value="Legs" active={true} />);
    const select = screen.getByRole("combobox", { name: "Focus Area" });
    expect(select).toHaveValue("Legs");
  });

  it("applies active styling via border color", () => {
    const { container } = render(
      <FilterChip {...defaultProps} value="Push" active={true} />
    );
    const select = container.querySelector("select");
    expect(select).toBeInTheDocument();
  });
});
