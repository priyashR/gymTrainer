import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { SectionNavigator } from "../SectionNavigator";
import type { SectionProgress } from "../../../types/session";

function makeSectionProgresses(count: number): SectionProgress[] {
  return Array.from({ length: count }, (_, i) => ({
    sectionIndex: i,
    sectionName: `Section ${i + 1}`,
    sectionType: "STRENGTH" as const,
    exerciseLogs: [
      { exerciseIndex: 0, exerciseName: "Exercise A", completed: false, completedAt: null },
    ],
    completed: false,
  }));
}

describe("SectionNavigator", () => {
  it("disables Previous button on the first section", () => {
    const onAdvance = vi.fn();
    render(
      <SectionNavigator
        currentSectionIndex={0}
        sectionProgresses={makeSectionProgresses(3)}
        onAdvanceSection={onAdvance}
      />
    );

    const prevButton = screen.getByRole("button", { name: /previous section/i });
    expect(prevButton).toBeDisabled();
  });

  it("disables Next button on the last section", () => {
    const onAdvance = vi.fn();
    render(
      <SectionNavigator
        currentSectionIndex={2}
        sectionProgresses={makeSectionProgresses(3)}
        onAdvanceSection={onAdvance}
      />
    );

    const nextButton = screen.getByRole("button", { name: /next section/i });
    expect(nextButton).toBeDisabled();
  });

  it("enables both buttons when in a middle section", () => {
    const onAdvance = vi.fn();
    render(
      <SectionNavigator
        currentSectionIndex={1}
        sectionProgresses={makeSectionProgresses(3)}
        onAdvanceSection={onAdvance}
      />
    );

    expect(screen.getByRole("button", { name: /previous section/i })).not.toBeDisabled();
    expect(screen.getByRole("button", { name: /next section/i })).not.toBeDisabled();
  });

  it("calls onAdvanceSection with index - 1 when Previous is clicked", async () => {
    const user = userEvent.setup();
    const onAdvance = vi.fn().mockResolvedValue(undefined);
    render(
      <SectionNavigator
        currentSectionIndex={2}
        sectionProgresses={makeSectionProgresses(3)}
        onAdvanceSection={onAdvance}
      />
    );

    await user.click(screen.getByRole("button", { name: /previous section/i }));
    expect(onAdvance).toHaveBeenCalledWith(1);
  });

  it("calls onAdvanceSection with index + 1 when Next is clicked", async () => {
    const user = userEvent.setup();
    const onAdvance = vi.fn().mockResolvedValue(undefined);
    render(
      <SectionNavigator
        currentSectionIndex={0}
        sectionProgresses={makeSectionProgresses(3)}
        onAdvanceSection={onAdvance}
      />
    );

    await user.click(screen.getByRole("button", { name: /next section/i }));
    expect(onAdvance).toHaveBeenCalledWith(1);
  });

  it("displays the current section name and progress indicator", () => {
    const onAdvance = vi.fn();
    render(
      <SectionNavigator
        currentSectionIndex={1}
        sectionProgresses={makeSectionProgresses(4)}
        onAdvanceSection={onAdvance}
      />
    );

    expect(screen.getByText("Section 2")).toBeInTheDocument();
    expect(screen.getByText("Section 2 of 4")).toBeInTheDocument();
  });

  it("disables both buttons when there is only one section", () => {
    const onAdvance = vi.fn();
    render(
      <SectionNavigator
        currentSectionIndex={0}
        sectionProgresses={makeSectionProgresses(1)}
        onAdvanceSection={onAdvance}
      />
    );

    expect(screen.getByRole("button", { name: /previous section/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /next section/i })).toBeDisabled();
  });
});
