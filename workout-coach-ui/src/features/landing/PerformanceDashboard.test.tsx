import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import {
  PerformanceDashboard,
  TopExerciseEntry,
  MonthlyFrequencyEntry,
} from "./PerformanceDashboard";

describe("PerformanceDashboard", () => {
  const sampleTopExercises: TopExerciseEntry[] = [
    { exerciseName: "Back Squat", maxWeight: 140, unit: "kg" },
    { exerciseName: "Bench Press", maxWeight: 100, unit: "kg" },
    { exerciseName: "Deadlift", maxWeight: 180, unit: "kg" },
  ];

  const sampleMonthlyFrequency: MonthlyFrequencyEntry[] = [
    { month: "Jan", count: 12 },
    { month: "Feb", count: 15 },
    { month: "Mar", count: 10 },
  ];

  it("renders the dashboard container with data-testid", () => {
    render(
      <PerformanceDashboard
        topExercises={sampleTopExercises}
        monthlyFrequency={sampleMonthlyFrequency}
      />
    );
    expect(screen.getByTestId("performance-dashboard")).toBeInTheDocument();
  });

  it("renders both chart sections", () => {
    render(
      <PerformanceDashboard
        topExercises={sampleTopExercises}
        monthlyFrequency={sampleMonthlyFrequency}
      />
    );
    expect(screen.getByTestId("top-exercises-section")).toBeInTheDocument();
    expect(screen.getByTestId("monthly-frequency-section")).toBeInTheDocument();
  });

  describe("Top Exercises section", () => {
    it("displays exercise names and weights when data is available", () => {
      render(
        <PerformanceDashboard
          topExercises={sampleTopExercises}
          monthlyFrequency={sampleMonthlyFrequency}
        />
      );
      expect(screen.getByText("Back Squat")).toBeInTheDocument();
      expect(screen.getByText("140 kg")).toBeInTheDocument();
      expect(screen.getByText("Bench Press")).toBeInTheDocument();
      expect(screen.getByText("100 kg")).toBeInTheDocument();
      expect(screen.getByText("Deadlift")).toBeInTheDocument();
      expect(screen.getByText("180 kg")).toBeInTheDocument();
    });

    it("renders EmptyState when topExercises is null", () => {
      render(
        <PerformanceDashboard
          topExercises={null}
          monthlyFrequency={sampleMonthlyFrequency}
        />
      );
      const section = screen.getByTestId("top-exercises-section");
      expect(section).toBeInTheDocument();
      expect(
        screen.getAllByText("Performance data coming soon").length
      ).toBeGreaterThanOrEqual(1);
      expect(
        screen.getByText(
          "Top exercise data will appear once the Progress Tracker is available"
        )
      ).toBeInTheDocument();
    });

    it("does not show exercise data when topExercises is null", () => {
      render(
        <PerformanceDashboard
          topExercises={null}
          monthlyFrequency={sampleMonthlyFrequency}
        />
      );
      expect(screen.queryByText("Back Squat")).not.toBeInTheDocument();
    });

    it("limits display to 10 exercises maximum", () => {
      const manyExercises: TopExerciseEntry[] = Array.from(
        { length: 15 },
        (_, i) => ({
          exerciseName: `Exercise ${i + 1}`,
          maxWeight: 100 - i * 5,
          unit: "kg",
        })
      );
      render(
        <PerformanceDashboard
          topExercises={manyExercises}
          monthlyFrequency={sampleMonthlyFrequency}
        />
      );
      expect(screen.getByText("Exercise 1")).toBeInTheDocument();
      expect(screen.getByText("Exercise 10")).toBeInTheDocument();
      expect(screen.queryByText("Exercise 11")).not.toBeInTheDocument();
    });
  });

  describe("Monthly Frequency section", () => {
    it("displays month labels and counts when data is available", () => {
      render(
        <PerformanceDashboard
          topExercises={sampleTopExercises}
          monthlyFrequency={sampleMonthlyFrequency}
        />
      );
      expect(screen.getByText("Jan")).toBeInTheDocument();
      expect(screen.getByText("Feb")).toBeInTheDocument();
      expect(screen.getByText("Mar")).toBeInTheDocument();
      expect(screen.getByText("12")).toBeInTheDocument();
      expect(screen.getByText("15")).toBeInTheDocument();
      expect(screen.getByText("10")).toBeInTheDocument();
    });

    it("renders EmptyState when monthlyFrequency is null", () => {
      render(
        <PerformanceDashboard
          topExercises={sampleTopExercises}
          monthlyFrequency={null}
        />
      );
      const section = screen.getByTestId("monthly-frequency-section");
      expect(section).toBeInTheDocument();
      expect(
        screen.getAllByText("Performance data coming soon").length
      ).toBeGreaterThanOrEqual(1);
      expect(
        screen.getByText(
          "Monthly workout data will appear once the Progress Tracker is available"
        )
      ).toBeInTheDocument();
    });

    it("does not show monthly data when monthlyFrequency is null", () => {
      render(
        <PerformanceDashboard
          topExercises={sampleTopExercises}
          monthlyFrequency={null}
        />
      );
      expect(screen.queryByText("Jan")).not.toBeInTheDocument();
    });
  });

  describe("Both sections unavailable", () => {
    it("renders EmptyState in both sections when both props are null", () => {
      render(
        <PerformanceDashboard topExercises={null} monthlyFrequency={null} />
      );
      expect(
        screen.getAllByText("Performance data coming soon")
      ).toHaveLength(2);
    });

    it("renders dashboard container even when all data is unavailable", () => {
      render(
        <PerformanceDashboard topExercises={null} monthlyFrequency={null} />
      );
      expect(screen.getByTestId("performance-dashboard")).toBeInTheDocument();
    });
  });

  describe("Accessibility", () => {
    it("provides an accessible list for top exercises", () => {
      render(
        <PerformanceDashboard
          topExercises={sampleTopExercises}
          monthlyFrequency={sampleMonthlyFrequency}
        />
      );
      expect(
        screen.getByRole("list", { name: "Top 10 exercises by weight" })
      ).toBeInTheDocument();
    });

    it("provides accessible chart label for monthly frequency", () => {
      render(
        <PerformanceDashboard
          topExercises={sampleTopExercises}
          monthlyFrequency={sampleMonthlyFrequency}
        />
      );
      expect(
        screen.getByRole("img", { name: "Monthly workout frequency chart" })
      ).toBeInTheDocument();
    });
  });
});
