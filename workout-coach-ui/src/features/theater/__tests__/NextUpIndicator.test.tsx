import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { NextUpIndicator } from "../NextUpIndicator";
import type { SectionProgress } from "../../../types/session";

function makeSectionProgresses(
  sections: Array<{ name: string; exerciseNames: string[]; completedIndices?: number[] }>
): SectionProgress[] {
  return sections.map((sec, i) => ({
    sectionIndex: i,
    sectionName: sec.name,
    sectionType: "STRENGTH" as const,
    exerciseLogs: sec.exerciseNames.map((name, j) => ({
      exerciseIndex: j,
      exerciseName: name,
      completed: sec.completedIndices?.includes(j) ?? false,
      completedAt: sec.completedIndices?.includes(j) ? "2026-01-15T10:30:00Z" : null,
      setLogs: [],
    })),
    completed: sec.completedIndices?.length === sec.exerciseNames.length,
    crossFitScore: null,
    roundCount: 0,
  }));
}

describe("NextUpIndicator", () => {
  it("shows the first uncompleted exercise in the current section", () => {
    const progresses = makeSectionProgresses([
      { name: "Strength", exerciseNames: ["Squat", "Bench", "Row"], completedIndices: [0] },
      { name: "Cardio", exerciseNames: ["Run"] },
    ]);

    render(
      <NextUpIndicator currentSectionIndex={0} sectionProgresses={progresses} />
    );

    expect(screen.getByText("Bench")).toBeInTheDocument();
  });

  it("shows next section name when current section is fully complete", () => {
    const progresses = makeSectionProgresses([
      { name: "Strength", exerciseNames: ["Squat", "Bench"], completedIndices: [0, 1] },
      { name: "Cardio", exerciseNames: ["Run"] },
    ]);

    render(
      <NextUpIndicator currentSectionIndex={0} sectionProgresses={progresses} />
    );

    expect(screen.getByText("Next section: Cardio")).toBeInTheDocument();
  });

  it("renders nothing when all sections are complete", () => {
    const progresses = makeSectionProgresses([
      { name: "Strength", exerciseNames: ["Squat"], completedIndices: [0] },
      { name: "Cardio", exerciseNames: ["Run"], completedIndices: [0] },
    ]);

    const { container } = render(
      <NextUpIndicator currentSectionIndex={1} sectionProgresses={progresses} />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it("shows the first exercise when nothing is completed yet", () => {
    const progresses = makeSectionProgresses([
      { name: "Strength", exerciseNames: ["Squat", "Bench", "Row"] },
    ]);

    render(
      <NextUpIndicator currentSectionIndex={0} sectionProgresses={progresses} />
    );

    expect(screen.getByText("Squat")).toBeInTheDocument();
  });

  it("renders nothing when on the last section and it is fully complete", () => {
    const progresses = makeSectionProgresses([
      { name: "Strength", exerciseNames: ["Squat"], completedIndices: [0] },
    ]);

    const { container } = render(
      <NextUpIndicator currentSectionIndex={0} sectionProgresses={progresses} />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it("displays the 'Next up:' label", () => {
    const progresses = makeSectionProgresses([
      { name: "Strength", exerciseNames: ["Squat"] },
    ]);

    render(
      <NextUpIndicator currentSectionIndex={0} sectionProgresses={progresses} />
    );

    expect(screen.getByText("Next up:")).toBeInTheDocument();
  });
});
