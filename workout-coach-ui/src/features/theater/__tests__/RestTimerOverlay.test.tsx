import { render, screen, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { RestTimerOverlay } from "../RestTimerOverlay";

describe("RestTimerOverlay", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("displays the initial countdown time formatted as MM:SS", () => {
    render(<RestTimerOverlay durationSeconds={90} onDismiss={vi.fn()} />);

    expect(screen.getByRole("timer")).toHaveTextContent("01:30");
  });

  it("counts down every second", () => {
    render(<RestTimerOverlay durationSeconds={5} onDismiss={vi.fn()} />);

    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(screen.getByRole("timer")).toHaveTextContent("00:04");

    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(screen.getByRole("timer")).toHaveTextContent("00:03");
  });

  it("renders a Skip Rest button", () => {
    render(<RestTimerOverlay durationSeconds={60} onDismiss={vi.fn()} />);

    expect(screen.getByRole("button", { name: /skip rest/i })).toBeInTheDocument();
  });

  it("calls onDismiss when Skip Rest is clicked", () => {
    const onDismiss = vi.fn();
    render(<RestTimerOverlay durationSeconds={60} onDismiss={onDismiss} />);

    const skipButton = screen.getByRole("button", { name: /skip rest/i });
    act(() => {
      skipButton.click();
    });

    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it("calls onDismiss after timer expires (with brief delay)", () => {
    const onDismiss = vi.fn();
    render(<RestTimerOverlay durationSeconds={3} onDismiss={onDismiss} />);

    // Advance through the countdown
    act(() => {
      vi.advanceTimersByTime(3000);
    });

    expect(screen.getByRole("timer")).toHaveTextContent("00:00");

    // Auto-dismiss after 1500ms delay
    act(() => {
      vi.advanceTimersByTime(1500);
    });

    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it("shows 'Rest complete' message when timer expires", () => {
    render(<RestTimerOverlay durationSeconds={2} onDismiss={vi.fn()} />);

    act(() => {
      vi.advanceTimersByTime(2000);
    });

    expect(screen.getByText(/rest complete/i)).toBeInTheDocument();
  });

  it("hides Skip Rest button when timer expires", () => {
    render(<RestTimerOverlay durationSeconds={1} onDismiss={vi.fn()} />);

    act(() => {
      vi.advanceTimersByTime(1000);
    });

    expect(screen.queryByRole("button", { name: /skip rest/i })).not.toBeInTheDocument();
  });

  it("renders as a dialog with proper accessibility attributes", () => {
    render(<RestTimerOverlay durationSeconds={60} onDismiss={vi.fn()} />);

    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(dialog).toHaveAttribute("aria-label", "Rest timer");
  });
});
