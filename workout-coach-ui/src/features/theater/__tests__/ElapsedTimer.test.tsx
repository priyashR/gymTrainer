import { render, screen, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ElapsedTimer } from "../ElapsedTimer";

describe("ElapsedTimer", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("displays elapsed time excluding paused time", () => {
    // Session started 65 seconds ago, with 5 seconds of total paused time
    const now = new Date("2026-01-15T10:01:05Z").getTime();
    vi.setSystemTime(now);

    render(
      <ElapsedTimer
        startedAt="2026-01-15T10:00:00Z"
        isPaused={false}
        pausedAt={null}
        totalPausedSeconds={5}
      />
    );

    const timer = screen.getByRole("timer");
    // 65 seconds elapsed - 5 seconds paused = 60 seconds = 01:00
    expect(timer).toHaveTextContent("01:00");
  });

  it("pauses the timer when session is PAUSED", () => {
    // Session started 120 seconds ago, paused 10 seconds ago
    const now = new Date("2026-01-15T10:02:00Z").getTime();
    vi.setSystemTime(now);

    render(
      <ElapsedTimer
        startedAt="2026-01-15T10:00:00Z"
        isPaused={true}
        pausedAt="2026-01-15T10:01:50Z"
        totalPausedSeconds={0}
      />
    );

    const timer = screen.getByRole("timer");
    // 120s total - 10s currently paused = 110s = 01:50
    expect(timer).toHaveTextContent("01:50");

    // Advance time by 5 seconds — timer should NOT update since paused
    act(() => {
      vi.advanceTimersByTime(5000);
    });

    // Still shows 01:50 because the timer interval is not running
    expect(timer).toHaveTextContent("01:50");
  });

  it("shows paused indicator when session is paused", () => {
    const now = new Date("2026-01-15T10:01:00Z").getTime();
    vi.setSystemTime(now);

    render(
      <ElapsedTimer
        startedAt="2026-01-15T10:00:00Z"
        isPaused={true}
        pausedAt="2026-01-15T10:00:50Z"
        totalPausedSeconds={0}
      />
    );

    const timer = screen.getByRole("timer");
    expect(timer).toHaveTextContent("⏸");
  });

  it("updates elapsed time every second when running", () => {
    const now = new Date("2026-01-15T10:00:30Z").getTime();
    vi.setSystemTime(now);

    render(
      <ElapsedTimer
        startedAt="2026-01-15T10:00:00Z"
        isPaused={false}
        pausedAt={null}
        totalPausedSeconds={0}
      />
    );

    const timer = screen.getByRole("timer");
    expect(timer).toHaveTextContent("00:30");

    act(() => {
      vi.advanceTimersByTime(1000);
    });

    expect(timer).toHaveTextContent("00:31");
  });

  it("has correct aria-label", () => {
    const now = new Date("2026-01-15T10:00:10Z").getTime();
    vi.setSystemTime(now);

    render(
      <ElapsedTimer
        startedAt="2026-01-15T10:00:00Z"
        isPaused={false}
        pausedAt={null}
      />
    );

    expect(screen.getByLabelText("Elapsed workout time")).toBeInTheDocument();
  });
});
