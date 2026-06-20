import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { ExerciseRow } from "./ExerciseRow";

const baseProps = {
  exercise: { id: "ex-1", name: "Back Squat" },
  recommendation: "4 × 6 @ 120kg · RPE 8",
  isActive: false,
  isCompleted: false,
  onCheck: vi.fn(),
  onMove: vi.fn(),
};

describe("ExerciseRow", () => {
  it("renders exercise name at correct hierarchy", () => {
    render(<ExerciseRow {...baseProps} />);
    expect(screen.getByText("Back Squat")).toBeInTheDocument();
  });

  it("renders prescription/recommendation text", () => {
    render(<ExerciseRow {...baseProps} />);
    expect(screen.getByText("4 × 6 @ 120kg · RPE 8")).toBeInTheDocument();
  });

  it("renders an unchecked checkbox when not completed", () => {
    render(<ExerciseRow {...baseProps} />);
    const checkbox = screen.getByRole("checkbox", {
      name: /mark back squat as complete/i,
    });
    expect(checkbox).toHaveAttribute("aria-checked", "false");
  });

  it("renders a checked checkbox when completed", () => {
    render(<ExerciseRow {...baseProps} isCompleted={true} />);
    const checkbox = screen.getByRole("checkbox", {
      name: /mark back squat as incomplete/i,
    });
    expect(checkbox).toHaveAttribute("aria-checked", "true");
  });

  it("calls onCheck with exercise id when checkbox is clicked", async () => {
    const onCheck = vi.fn();
    render(<ExerciseRow {...baseProps} onCheck={onCheck} />);
    const checkbox = screen.getByRole("checkbox");
    await userEvent.click(checkbox);
    expect(onCheck).toHaveBeenCalledWith("ex-1");
  });

  it("renders the Move button", () => {
    render(<ExerciseRow {...baseProps} />);
    expect(
      screen.getByRole("button", { name: /move to back squat/i })
    ).toBeInTheDocument();
  });

  it("calls onMove with exercise id when Move button is clicked", async () => {
    const onMove = vi.fn();
    render(<ExerciseRow {...baseProps} onMove={onMove} />);
    const moveBtn = screen.getByRole("button", { name: /move to back squat/i });
    await userEvent.click(moveBtn);
    expect(onMove).toHaveBeenCalledWith("ex-1");
  });

  it("applies dimmed styling when completed", () => {
    const { container } = render(
      <ExerciseRow {...baseProps} isCompleted={true} />
    );
    const row = container.querySelector('[data-testid="exercise-row-ex-1"]');
    expect(row).toHaveStyle({ opacity: "0.5" });
  });

  it("applies active/highlighted styling when active", () => {
    const { container } = render(
      <ExerciseRow {...baseProps} isActive={true} />
    );
    const row = container.querySelector('[data-testid="exercise-row-ex-1"]');
    expect(row).toHaveStyle({ border: "1px solid var(--color-accent)" });
  });

  it("checkbox has minimum 48px tap target", () => {
    render(<ExerciseRow {...baseProps} />);
    const checkbox = screen.getByRole("checkbox");
    expect(checkbox).toHaveStyle({
      minWidth: "var(--tap-target-preferred)",
      minHeight: "var(--tap-target-preferred)",
    });
  });

  it("Move button has minimum 48px tap target", () => {
    render(<ExerciseRow {...baseProps} />);
    const moveBtn = screen.getByRole("button", { name: /move to back squat/i });
    expect(moveBtn).toHaveStyle({
      minWidth: "var(--tap-target-preferred)",
      minHeight: "var(--tap-target-preferred)",
    });
  });
});
