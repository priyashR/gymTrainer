import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { TheaterHeader } from "../TheaterHeader";

describe("TheaterHeader", () => {
  const defaultProps = {
    elapsedTime: "12:45",
    tierLabel: "Tier 1: Compound",
    sectionType: "Strength",
    isFirstTier: false,
    isLastTier: false,
    onPrevTier: vi.fn(),
    onNextTier: vi.fn(),
  };

  it("displays the elapsed time with 96px font", () => {
    render(<TheaterHeader {...defaultProps} />);

    const timer = screen.getByRole("timer");
    expect(timer).toHaveTextContent("12:45");
    expect(timer).toHaveStyle({ fontSize: "96px" });
  });

  it("displays the tier label", () => {
    render(<TheaterHeader {...defaultProps} />);

    expect(screen.getByText("Tier 1: Compound")).toBeInTheDocument();
  });

  it("displays the section type", () => {
    render(<TheaterHeader {...defaultProps} />);

    expect(screen.getByText("Strength")).toBeInTheDocument();
  });

  it("disables the Prev button when on the first tier", () => {
    render(<TheaterHeader {...defaultProps} isFirstTier={true} />);

    const prevButton = screen.getByRole("button", { name: /previous tier/i });
    expect(prevButton).toBeDisabled();
  });

  it("disables the Next button when on the last tier", () => {
    render(<TheaterHeader {...defaultProps} isLastTier={true} />);

    const nextButton = screen.getByRole("button", { name: /next tier/i });
    expect(nextButton).toBeDisabled();
  });

  it("enables Prev button when not on first tier", () => {
    render(<TheaterHeader {...defaultProps} isFirstTier={false} />);

    const prevButton = screen.getByRole("button", { name: /previous tier/i });
    expect(prevButton).not.toBeDisabled();
  });

  it("enables Next button when not on last tier", () => {
    render(<TheaterHeader {...defaultProps} isLastTier={false} />);

    const nextButton = screen.getByRole("button", { name: /next tier/i });
    expect(nextButton).not.toBeDisabled();
  });

  it("calls onPrevTier when Prev button is clicked", async () => {
    const user = userEvent.setup();
    const onPrevTier = vi.fn();
    render(<TheaterHeader {...defaultProps} onPrevTier={onPrevTier} />);

    const prevButton = screen.getByRole("button", { name: /previous tier/i });
    await user.click(prevButton);

    expect(onPrevTier).toHaveBeenCalledTimes(1);
  });

  it("calls onNextTier when Next button is clicked", async () => {
    const user = userEvent.setup();
    const onNextTier = vi.fn();
    render(<TheaterHeader {...defaultProps} onNextTier={onNextTier} />);

    const nextButton = screen.getByRole("button", { name: /next tier/i });
    await user.click(nextButton);

    expect(onNextTier).toHaveBeenCalledTimes(1);
  });

  it("does not call onPrevTier when Prev is disabled and clicked", async () => {
    const user = userEvent.setup();
    const onPrevTier = vi.fn();
    render(<TheaterHeader {...defaultProps} isFirstTier={true} onPrevTier={onPrevTier} />);

    const prevButton = screen.getByRole("button", { name: /previous tier/i });
    await user.click(prevButton);

    expect(onPrevTier).not.toHaveBeenCalled();
  });

  it("does not call onNextTier when Next is disabled and clicked", async () => {
    const user = userEvent.setup();
    const onNextTier = vi.fn();
    render(<TheaterHeader {...defaultProps} isLastTier={true} onNextTier={onNextTier} />);

    const nextButton = screen.getByRole("button", { name: /next tier/i });
    await user.click(nextButton);

    expect(onNextTier).not.toHaveBeenCalled();
  });

  it("renders with HH:MM:SS format", () => {
    render(<TheaterHeader {...defaultProps} elapsedTime="1:02:30" />);

    const timer = screen.getByRole("timer");
    expect(timer).toHaveTextContent("1:02:30");
  });
});
