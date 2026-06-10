import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { RecommendationBadge } from "../RecommendationBadge";

describe("RecommendationBadge", () => {
  it("renders HYPERTROPHY full format with weight, reps, and sets joined by dot separator", () => {
    render(
      <RecommendationBadge
        prescribedWeight="80kg"
        prescribedReps="8-10"
        prescribedSets={4}
        isLoading={false}
      />
    );

    const badge = screen.getByTestId("recommendation-badge");
    expect(badge.textContent).toBe("80kg · 8-10 · 4");
  });

  it("renders CROSSFIT weight-only format when reps and sets are null", () => {
    render(
      <RecommendationBadge
        prescribedWeight="60kg"
        prescribedReps={null}
        prescribedSets={null}
        isLoading={false}
      />
    );

    const badge = screen.getByTestId("recommendation-badge");
    expect(badge.textContent).toBe("60kg");
  });

  it("renders nothing when all fields are null", () => {
    const { container } = render(
      <RecommendationBadge
        prescribedWeight={null}
        prescribedReps={null}
        prescribedSets={null}
        isLoading={false}
      />
    );

    expect(container).toBeEmptyDOMElement();
  });

  describe("partial nulls omit missing fields", () => {
    it("omits weight when null", () => {
      render(
        <RecommendationBadge
          prescribedWeight={null}
          prescribedReps="12"
          prescribedSets={3}
          isLoading={false}
        />
      );

      const badge = screen.getByTestId("recommendation-badge");
      expect(badge.textContent).toBe("12 · 3");
    });

    it("omits reps when null", () => {
      render(
        <RecommendationBadge
          prescribedWeight="50kg"
          prescribedReps={null}
          prescribedSets={4}
          isLoading={false}
        />
      );

      const badge = screen.getByTestId("recommendation-badge");
      expect(badge.textContent).toBe("50kg · 4");
    });

    it("omits sets when null", () => {
      render(
        <RecommendationBadge
          prescribedWeight="70kg"
          prescribedReps="6-8"
          prescribedSets={null}
          isLoading={false}
        />
      );

      const badge = screen.getByTestId("recommendation-badge");
      expect(badge.textContent).toBe("70kg · 6-8");
    });
  });

  it("shows loading indicator when isLoading is true", () => {
    render(
      <RecommendationBadge
        prescribedWeight="80kg"
        prescribedReps="8-10"
        prescribedSets={4}
        isLoading={true}
      />
    );

    const loadingIndicator = screen.getByRole("status");
    expect(loadingIndicator).toBeInTheDocument();
    expect(loadingIndicator).toHaveAttribute("aria-label", "Loading recommendation");
    expect(screen.queryByTestId("recommendation-badge")).not.toBeInTheDocument();
  });
});
