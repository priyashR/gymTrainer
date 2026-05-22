import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { TimerDisplay } from "../TimerDisplay";

describe("TimerDisplay", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("renders a countdown timer for AMRAP section type", () => {
    render(
      <TimerDisplay
        sectionType="AMRAP"
        durationSeconds={300}
        isPaused={false}
      />
    );

    expect(screen.getByText(/amrap/i)).toBeInTheDocument();
    expect(screen.getByText(/countdown/i)).toBeInTheDocument();
    expect(screen.getByRole("timer")).toHaveTextContent("05:00");
  });

  it("renders a stopwatch timer for STRENGTH section type", () => {
    render(
      <TimerDisplay sectionType="STRENGTH" isPaused={false} />
    );

    expect(screen.getByText(/strength/i)).toBeInTheDocument();
    expect(screen.getByText(/stopwatch/i)).toBeInTheDocument();
    expect(screen.getByRole("timer")).toHaveTextContent("00:00");
  });

  it("renders an interval timer for TABATA section type", () => {
    render(
      <TimerDisplay
        sectionType="TABATA"
        workSeconds={20}
        restSeconds={10}
        rounds={8}
        isPaused={false}
      />
    );

    expect(screen.getByText(/tabata/i)).toBeInTheDocument();
    expect(screen.getByText(/round 1 of 8/i)).toBeInTheDocument();
    expect(screen.getByRole("timer")).toHaveTextContent("00:20");
  });

  it("renders an interval timer for EMOM section type", () => {
    render(
      <TimerDisplay
        sectionType="EMOM"
        workSeconds={60}
        restSeconds={0}
        rounds={10}
        isPaused={false}
      />
    );

    expect(screen.getByText(/emom/i)).toBeInTheDocument();
    expect(screen.getByText(/round 1 of 10/i)).toBeInTheDocument();
  });

  it("uses default duration of 600s for AMRAP when not specified", () => {
    render(
      <TimerDisplay sectionType="AMRAP" isPaused={false} />
    );

    expect(screen.getByRole("timer")).toHaveTextContent("10:00");
  });

  it("renders nothing for an unknown section type", () => {
    const { container } = render(
      // @ts-expect-error testing unknown type
      <TimerDisplay sectionType="UNKNOWN" isPaused={false} />
    );

    expect(container).toBeEmptyDOMElement();
  });
});
