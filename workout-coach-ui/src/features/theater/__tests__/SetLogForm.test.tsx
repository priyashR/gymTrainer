import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { SetLogForm } from "../SetLogForm";

describe("SetLogForm", () => {
  const defaultProps = {
    sectionIndex: 0,
    exerciseIndex: 1,
    sessionStatus: "IN_PROGRESS" as const,
    onLogSet: vi.fn().mockResolvedValue(undefined),
  };

  it("shows validation error when weight is empty", async () => {
    const user = userEvent.setup();
    const onLogSet = vi.fn().mockResolvedValue(undefined);
    render(<SetLogForm {...defaultProps} onLogSet={onLogSet} />);

    const repsInput = screen.getByLabelText(/reps/i);

    // Leave weight empty, fill reps
    await user.type(repsInput, "5");
    await user.click(screen.getByRole("button", { name: /set/i }));

    expect(screen.getByRole("alert")).toHaveTextContent(/weight must be greater than 0/i);
    expect(onLogSet).not.toHaveBeenCalled();
  });

  it("shows validation error when reps is empty", async () => {
    const user = userEvent.setup();
    const onLogSet = vi.fn().mockResolvedValue(undefined);
    render(<SetLogForm {...defaultProps} onLogSet={onLogSet} />);

    const weightInput = screen.getByLabelText(/weight/i);

    // Fill weight, leave reps empty
    await user.type(weightInput, "100");
    await user.click(screen.getByRole("button", { name: /set/i }));

    expect(screen.getByRole("alert")).toHaveTextContent(/reps must be at least 1/i);
    expect(onLogSet).not.toHaveBeenCalled();
  });

  it("calls onLogSet with correct data on valid submission (no RPE)", async () => {
    const user = userEvent.setup();
    const onLogSet = vi.fn().mockResolvedValue(undefined);
    render(<SetLogForm {...defaultProps} onLogSet={onLogSet} />);

    await user.type(screen.getByLabelText(/weight/i), "100");
    await user.type(screen.getByLabelText(/reps/i), "8");
    await user.click(screen.getByRole("button", { name: /set/i }));

    expect(onLogSet).toHaveBeenCalledWith({
      sectionIndex: 0,
      exerciseIndex: 1,
      weight: 100,
      repetitions: 8,
      rpe: null,
    });
  });

  it("calls onLogSet with RPE when RPE is selected", async () => {
    const user = userEvent.setup();
    const onLogSet = vi.fn().mockResolvedValue(undefined);
    render(<SetLogForm {...defaultProps} onLogSet={onLogSet} />);

    await user.type(screen.getByLabelText(/weight/i), "60");
    await user.type(screen.getByLabelText(/reps/i), "12");
    await user.selectOptions(screen.getByLabelText(/rpe/i), "7.5");
    await user.click(screen.getByRole("button", { name: /set/i }));

    expect(onLogSet).toHaveBeenCalledWith({
      sectionIndex: 0,
      exerciseIndex: 1,
      weight: 60,
      repetitions: 12,
      rpe: 7.5,
    });
  });

  it("resets form fields after successful submission", async () => {
    const user = userEvent.setup();
    const onLogSet = vi.fn().mockResolvedValue(undefined);
    render(<SetLogForm {...defaultProps} onLogSet={onLogSet} />);

    const weightInput = screen.getByLabelText(/weight/i) as HTMLInputElement;
    const repsInput = screen.getByLabelText(/reps/i) as HTMLInputElement;

    await user.type(weightInput, "80");
    await user.type(repsInput, "5");
    await user.click(screen.getByRole("button", { name: /set/i }));

    await waitFor(() => {
      expect(weightInput).toHaveValue(null);
      expect(repsInput).toHaveValue(null);
    });
  });

  it("shows error message when onLogSet rejects", async () => {
    const user = userEvent.setup();
    const onLogSet = vi.fn().mockRejectedValue(new Error("Network error"));
    render(<SetLogForm {...defaultProps} onLogSet={onLogSet} />);

    await user.type(screen.getByLabelText(/weight/i), "100");
    await user.type(screen.getByLabelText(/reps/i), "5");
    await user.click(screen.getByRole("button", { name: /set/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/failed to log set/i);
  });

  it("disables form inputs when session is PAUSED", () => {
    render(<SetLogForm {...defaultProps} sessionStatus="PAUSED" />);

    expect(screen.getByLabelText(/weight/i)).toBeDisabled();
    expect(screen.getByLabelText(/reps/i)).toBeDisabled();
    expect(screen.getByLabelText(/rpe/i)).toBeDisabled();
    expect(screen.getByRole("button", { name: /set/i })).toBeDisabled();
  });

  it("disables form inputs when session is COMPLETED", () => {
    render(<SetLogForm {...defaultProps} sessionStatus="COMPLETED" />);

    expect(screen.getByLabelText(/weight/i)).toBeDisabled();
    expect(screen.getByLabelText(/reps/i)).toBeDisabled();
    expect(screen.getByLabelText(/rpe/i)).toBeDisabled();
    expect(screen.getByRole("button", { name: /set/i })).toBeDisabled();
  });

  it("enables form inputs when session is IN_PROGRESS", () => {
    render(<SetLogForm {...defaultProps} sessionStatus="IN_PROGRESS" />);

    expect(screen.getByLabelText(/weight/i)).not.toBeDisabled();
    expect(screen.getByLabelText(/reps/i)).not.toBeDisabled();
    expect(screen.getByLabelText(/rpe/i)).not.toBeDisabled();
    expect(screen.getByRole("button", { name: /set/i })).not.toBeDisabled();
  });
});
