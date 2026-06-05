import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { EmptySessionGuard, hasPerformanceData } from "../EmptySessionGuard";
import type { SectionProgress } from "../../../types/session";

function makeSectionProgress(overrides: Partial<SectionProgress> = {}): SectionProgress {
  return {
    sectionIndex: 0,
    sectionName: "Strength",
    sectionType: "STRENGTH",
    exerciseLogs: [
      {
        exerciseIndex: 0,
        exerciseName: "Squat",
        completed: false,
        completedAt: null,
        setLogs: [],
      },
    ],
    completed: false,
    crossFitScore: null,
    roundCount: 0,
    ...overrides,
  };
}

describe("EmptySessionGuard", () => {
  const defaultProps = {
    onConfirmEnd: vi.fn().mockResolvedValue(undefined),
    onCancel: vi.fn(),
  };

  it("shows prompt when no performance data exists", () => {
    const sectionProgresses = [makeSectionProgress()];

    render(<EmptySessionGuard sectionProgresses={sectionProgresses} {...defaultProps} />);

    expect(screen.getByRole("dialog", { name: /no performance data recorded/i })).toBeInTheDocument();
    expect(screen.getByText(/go back/i)).toBeInTheDocument();
    expect(screen.getByText(/end anyway/i)).toBeInTheDocument();
  });

  it("does not show prompt when performance data exists (setLogs)", () => {
    const sectionProgresses = [
      makeSectionProgress({
        exerciseLogs: [
          {
            exerciseIndex: 0,
            exerciseName: "Squat",
            completed: false,
            completedAt: null,
            setLogs: [
              { setNumber: 1, weight: 100, repetitions: 5, rpe: null, loggedAt: "2026-01-15T10:00:00Z" },
            ],
          },
        ],
      }),
    ];

    const { container } = render(
      <EmptySessionGuard sectionProgresses={sectionProgresses} {...defaultProps} />
    );

    expect(container.innerHTML).toBe("");
  });

  it("does not show prompt when performance data exists (crossFitScore)", () => {
    const sectionProgresses = [
      makeSectionProgress({
        sectionType: "AMRAP",
        crossFitScore: {
          rounds: 5,
          additionalReps: 3,
          totalTimeSeconds: null,
          loggedAt: "2026-01-15T10:00:00Z",
        },
      }),
    ];

    const { container } = render(
      <EmptySessionGuard sectionProgresses={sectionProgresses} {...defaultProps} />
    );

    expect(container.innerHTML).toBe("");
  });

  it("calls onCancel when Go Back is clicked", async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    const sectionProgresses = [makeSectionProgress()];

    render(
      <EmptySessionGuard
        sectionProgresses={sectionProgresses}
        onConfirmEnd={vi.fn().mockResolvedValue(undefined)}
        onCancel={onCancel}
      />
    );

    await user.click(screen.getByText(/go back/i));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("calls onConfirmEnd when End Anyway is clicked", async () => {
    const user = userEvent.setup();
    const onConfirmEnd = vi.fn().mockResolvedValue(undefined);
    const sectionProgresses = [makeSectionProgress()];

    render(
      <EmptySessionGuard
        sectionProgresses={sectionProgresses}
        onConfirmEnd={onConfirmEnd}
        onCancel={vi.fn()}
      />
    );

    await user.click(screen.getByText(/end anyway/i));

    expect(onConfirmEnd).toHaveBeenCalledTimes(1);
  });
});

describe("hasPerformanceData", () => {
  it("returns false when no sets and no scores exist", () => {
    const sections: SectionProgress[] = [
      {
        sectionIndex: 0,
        sectionName: "Strength",
        sectionType: "STRENGTH",
        exerciseLogs: [
          { exerciseIndex: 0, exerciseName: "Squat", completed: false, completedAt: null, setLogs: [] },
        ],
        completed: false,
        crossFitScore: null,
        roundCount: 0,
      },
    ];

    expect(hasPerformanceData(sections)).toBe(false);
  });

  it("returns true when at least one set log exists", () => {
    const sections: SectionProgress[] = [
      {
        sectionIndex: 0,
        sectionName: "Strength",
        sectionType: "STRENGTH",
        exerciseLogs: [
          {
            exerciseIndex: 0,
            exerciseName: "Squat",
            completed: false,
            completedAt: null,
            setLogs: [{ setNumber: 1, weight: 100, repetitions: 5, rpe: null, loggedAt: "2026-01-15T10:00:00Z" }],
          },
        ],
        completed: false,
        crossFitScore: null,
        roundCount: 0,
      },
    ];

    expect(hasPerformanceData(sections)).toBe(true);
  });

  it("returns true when at least one crossFitScore exists", () => {
    const sections: SectionProgress[] = [
      {
        sectionIndex: 0,
        sectionName: "AMRAP",
        sectionType: "AMRAP",
        exerciseLogs: [],
        completed: false,
        crossFitScore: { rounds: 5, additionalReps: 3, totalTimeSeconds: null, loggedAt: "2026-01-15T10:00:00Z" },
        roundCount: 5,
      },
    ];

    expect(hasPerformanceData(sections)).toBe(true);
  });
});
