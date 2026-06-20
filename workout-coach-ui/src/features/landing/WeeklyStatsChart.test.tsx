import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { WeeklyStatsChart } from "./WeeklyStatsChart";

describe("WeeklyStatsChart", () => {
  const defaultProps = {
    completedCount: 3,
    goalCount: 7,
    dailyData: [true, true, true, false, false, false, false] as (
      | boolean
      | number
    )[],
  };

  it("renders the chart container with data-testid", () => {
    render(<WeeklyStatsChart {...defaultProps} />);
    expect(screen.getByTestId("weekly-stats-chart")).toBeInTheDocument();
  });

  it("displays the title 'This Week'", () => {
    render(<WeeklyStatsChart {...defaultProps} />);
    expect(screen.getByText("This Week")).toBeInTheDocument();
  });

  it("displays the completed count relative to goal", () => {
    render(<WeeklyStatsChart {...defaultProps} />);
    expect(screen.getByText("3 of 7")).toBeInTheDocument();
  });

  it("renders 7 bars for each day of the week", () => {
    render(<WeeklyStatsChart {...defaultProps} />);
    for (let i = 0; i < 7; i++) {
      expect(screen.getByTestId(`bar-${i}`)).toBeInTheDocument();
    }
  });

  it("renders day labels M T W T F S S", () => {
    render(<WeeklyStatsChart {...defaultProps} />);
    const labels = ["M", "T", "W", "T", "F", "S", "S"];
    labels.forEach((label) => {
      expect(screen.getAllByText(label).length).toBeGreaterThan(0);
    });
  });

  it("marks completed days with 'completed' aria-label", () => {
    render(<WeeklyStatsChart {...defaultProps} />);
    expect(screen.getByTestId("bar-0")).toHaveAttribute(
      "aria-label",
      "M: completed"
    );
    expect(screen.getByTestId("bar-1")).toHaveAttribute(
      "aria-label",
      "T: completed"
    );
    expect(screen.getByTestId("bar-2")).toHaveAttribute(
      "aria-label",
      "W: completed"
    );
  });

  it("marks remaining days with 'remaining' aria-label", () => {
    render(<WeeklyStatsChart {...defaultProps} />);
    expect(screen.getByTestId("bar-3")).toHaveAttribute(
      "aria-label",
      "T: remaining"
    );
    expect(screen.getByTestId("bar-4")).toHaveAttribute(
      "aria-label",
      "F: remaining"
    );
  });

  it("provides an accessible chart area with progress description", () => {
    render(<WeeklyStatsChart {...defaultProps} />);
    expect(
      screen.getByRole("img", {
        name: "Weekly progress: 3 of 7 workouts completed",
      })
    ).toBeInTheDocument();
  });

  describe("EmptyState rendering", () => {
    it("renders EmptyState when dailyData is null", () => {
      render(
        <WeeklyStatsChart
          completedCount={3}
          goalCount={7}
          dailyData={null}
        />
      );
      expect(
        screen.getByText("Weekly stats not available")
      ).toBeInTheDocument();
    });

    it("renders EmptyState when completedCount is null", () => {
      render(
        <WeeklyStatsChart
          completedCount={null}
          goalCount={7}
          dailyData={[true, false, false, false, false, false, false]}
        />
      );
      expect(
        screen.getByText("Weekly stats not available")
      ).toBeInTheDocument();
    });

    it("renders EmptyState when goalCount is null", () => {
      render(
        <WeeklyStatsChart
          completedCount={3}
          goalCount={null}
          dailyData={[true, false, false, false, false, false, false]}
        />
      );
      expect(
        screen.getByText("Weekly stats not available")
      ).toBeInTheDocument();
    });

    it("renders EmptyState subtitle hint", () => {
      render(
        <WeeklyStatsChart
          completedCount={null}
          goalCount={null}
          dailyData={null}
        />
      );
      expect(
        screen.getByText("Data will appear once tracking is active")
      ).toBeInTheDocument();
    });

    it("does not render bars when data is unavailable", () => {
      render(
        <WeeklyStatsChart
          completedCount={null}
          goalCount={null}
          dailyData={null}
        />
      );
      expect(screen.queryByTestId("bar-0")).not.toBeInTheDocument();
    });
  });

  describe("numeric dailyData values", () => {
    it("treats truthy numeric values as completed", () => {
      const numericData = [1, 1, 0, 0, 1, 0, 0] as (boolean | number)[];
      render(
        <WeeklyStatsChart
          completedCount={3}
          goalCount={7}
          dailyData={numericData}
        />
      );
      expect(screen.getByTestId("bar-0")).toHaveAttribute(
        "aria-label",
        "M: completed"
      );
      expect(screen.getByTestId("bar-2")).toHaveAttribute(
        "aria-label",
        "W: remaining"
      );
      expect(screen.getByTestId("bar-4")).toHaveAttribute(
        "aria-label",
        "F: completed"
      );
    });
  });
});
