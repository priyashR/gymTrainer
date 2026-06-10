import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { RoundCounter } from "../RoundCounter";

describe("RoundCounter", () => {
  it("displays the current round count", () => {
    render(<RoundCounter roundCount={5} onRoundCountChange={vi.fn()} />);

    expect(screen.getByText("5")).toBeInTheDocument();
  });

  it("increments round count on tap", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<RoundCounter roundCount={3} onRoundCountChange={onChange} />);

    const incrementButton = screen.getByRole("button", { name: /increment round count/i });
    await user.click(incrementButton);

    expect(onChange).toHaveBeenCalledWith(4);
  });

  it("decrements round count on decrement button click", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<RoundCounter roundCount={2} onRoundCountChange={onChange} />);

    const decrementButton = screen.getByRole("button", { name: /decrement round count/i });
    await user.click(decrementButton);

    expect(onChange).toHaveBeenCalledWith(1);
  });

  it("disables decrement button when count is 0 (floor at zero)", () => {
    render(<RoundCounter roundCount={0} onRoundCountChange={vi.fn()} />);

    const decrementButton = screen.getByRole("button", { name: /decrement round count/i });
    expect(decrementButton).toBeDisabled();
  });

  it("does not call onRoundCountChange when decrementing at zero", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<RoundCounter roundCount={0} onRoundCountChange={onChange} />);

    const decrementButton = screen.getByRole("button", { name: /decrement round count/i });
    await user.click(decrementButton);

    expect(onChange).not.toHaveBeenCalled();
  });

  it("displays the correct count after multiple increments", () => {
    const { rerender } = render(
      <RoundCounter roundCount={0} onRoundCountChange={vi.fn()} />
    );

    expect(screen.getByText("0")).toBeInTheDocument();

    rerender(<RoundCounter roundCount={7} onRoundCountChange={vi.fn()} />);

    expect(screen.getByText("7")).toBeInTheDocument();
  });

  it("disables both buttons when disabled prop is true", () => {
    render(<RoundCounter roundCount={3} onRoundCountChange={vi.fn()} disabled={true} />);

    const incrementButton = screen.getByRole("button", { name: /increment round count/i });
    const decrementButton = screen.getByRole("button", { name: /decrement round count/i });

    expect(incrementButton).toBeDisabled();
    expect(decrementButton).toBeDisabled();
  });
});
