import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { FABButton } from "./FABButton";

describe("FABButton", () => {
  it("renders with default + icon", () => {
    render(<FABButton onClick={() => {}} ariaLabel="Add new" />);
    const button = screen.getByRole("button", { name: "Add new" });
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("+");
  });

  it("renders with custom icon", () => {
    render(<FABButton onClick={() => {}} icon="✏️" ariaLabel="Edit" />);
    const button = screen.getByRole("button", { name: "Edit" });
    expect(button).toHaveTextContent("✏️");
  });

  it("calls onClick when clicked", async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();
    render(<FABButton onClick={handleClick} ariaLabel="Add new" />);

    await user.click(screen.getByRole("button", { name: "Add new" }));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it("has accessible aria-label", () => {
    render(<FABButton onClick={() => {}} ariaLabel="Create program" />);
    expect(
      screen.getByRole("button", { name: "Create program" })
    ).toBeInTheDocument();
  });
});
