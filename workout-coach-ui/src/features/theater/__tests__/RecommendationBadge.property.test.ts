import { describe, it, expect } from "vitest";
import * as fc from "fast-check";
import { render } from "@testing-library/react";
import { createElement } from "react";
import { RecommendationBadge } from "../RecommendationBadge";

/**
 * Property 7: Recommendation badge format correctness
 *
 * For any non-empty ExerciseRecommendation (at least one non-null field),
 * the UI badge format function SHALL produce a string that contains exactly
 * the non-null field values joined by " · " in the order [weight, reps, sets],
 * omitting null fields entirely. For CROSSFIT recommendations (only weight
 * present), the string equals the weight value alone.
 *
 * Validates: Requirements 6.1, 6.2, 6.4
 */

// Arbitrary for a non-blank weight string (non-empty, max 50 chars)
const weightArb = fc.string({ minLength: 1, maxLength: 50 }).filter((s) => s.trim().length > 0);

// Arbitrary for a non-blank reps string (non-empty, max 50 chars)
const repsArb = fc.string({ minLength: 1, maxLength: 50 }).filter((s) => s.trim().length > 0);

// Arbitrary for a valid sets integer [1, 100]
const setsArb = fc.integer({ min: 1, max: 100 });

// Arbitrary for nullable fields - either a valid value or null
const nullableWeightArb = fc.oneof(weightArb, fc.constant(null as string | null));
const nullableRepsArb = fc.oneof(repsArb, fc.constant(null as string | null));
const nullableSetsArb = fc.oneof(setsArb, fc.constant(null as number | null));

// Arbitrary that generates at least one non-null field (filters out all-null case)
const nonEmptyRecommendationArb = fc
  .tuple(nullableWeightArb, nullableRepsArb, nullableSetsArb)
  .filter(([w, r, s]) => w !== null || r !== null || s !== null);

describe("RecommendationBadge - Property 7: Badge format correctness", () => {
  it("non-null fields are joined by ' · ' in order [weight, reps, sets]", () => {
    fc.assert(
      fc.property(nonEmptyRecommendationArb, ([weight, reps, sets]) => {
        const { container } = render(
          createElement(RecommendationBadge, {
            prescribedWeight: weight,
            prescribedReps: reps,
            prescribedSets: sets,
            isLoading: false,
          })
        );

        const badge = container.querySelector('[data-testid="recommendation-badge"]');
        expect(badge).not.toBeNull();

        const parts: string[] = [];
        if (weight !== null) parts.push(weight);
        if (reps !== null) parts.push(reps);
        if (sets !== null) parts.push(String(sets));

        const expected = parts.join(" · ");
        expect(badge!.textContent).toBe(expected);
      }),
      { numRuns: 100 }
    );
  });

  it("CROSSFIT format: weight-only recommendation equals the weight value alone", () => {
    fc.assert(
      fc.property(weightArb, (weight) => {
        const { container } = render(
          createElement(RecommendationBadge, {
            prescribedWeight: weight,
            prescribedReps: null,
            prescribedSets: null,
            isLoading: false,
          })
        );

        const badge = container.querySelector('[data-testid="recommendation-badge"]');
        expect(badge).not.toBeNull();
        expect(badge!.textContent).toBe(weight);
      }),
      { numRuns: 100 }
    );
  });

  it("all-null recommendation renders nothing", () => {
    fc.assert(
      fc.property(fc.constant(null), () => {
        const { container } = render(
          createElement(RecommendationBadge, {
            prescribedWeight: null,
            prescribedReps: null,
            prescribedSets: null,
            isLoading: false,
          })
        );

        const badge = container.querySelector('[data-testid="recommendation-badge"]');
        expect(badge).toBeNull();
      }),
      { numRuns: 100 }
    );
  });
});
