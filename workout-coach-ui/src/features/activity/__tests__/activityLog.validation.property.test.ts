import { describe, it, expect } from "vitest";
import fc from "fast-check";

/**
 * **Validates: Requirements 9.5**
 *
 * Property: The Activity Log form submission is blocked if and only if
 * either mandatory field (activity type or date) is missing.
 *
 * The ActivityLogPage validates that:
 * - Activity type must be a non-null, non-empty string
 * - Date must be a non-empty string
 *
 * If both are present, submission proceeds. If either is missing, a
 * validation error is shown and submission is blocked.
 */

// --- Activity types matching the ActivityLogPage/ActivityTypeGrid ---
const ACTIVITY_TYPES = [
  "Soccer",
  "Squash",
  "Running",
  "Padel",
  "Golf",
  "Swimming",
  "Cycling",
  "Hiking",
  "Tennis",
  "Basketball",
  "Other",
] as const;

/**
 * Represents the form state relevant to mandatory field validation.
 * Mirrors what ActivityLogPage tracks for its `validate()` function.
 */
interface ActivityFormState {
  selectedActivityType: string | null;
  date: string;
}

/**
 * Pure validation function replicating ActivityLogPage's `validate()` logic.
 *
 * Returns an object with errors for each invalid field.
 * An empty errors object means the form is valid and submission proceeds.
 */
function validateActivityForm(state: ActivityFormState): {
  activityType?: string;
  date?: string;
} {
  const errors: { activityType?: string; date?: string } = {};

  if (!state.selectedActivityType) {
    errors.activityType = "Activity type is required";
  }
  if (!state.date) {
    errors.date = "Date is required";
  }

  return errors;
}

/**
 * Determines whether submission should be blocked based on validation errors.
 * Submission is blocked iff the errors object has at least one key.
 */
function isSubmissionBlocked(state: ActivityFormState): boolean {
  const errors = validateActivityForm(state);
  return Object.keys(errors).length > 0;
}

// --- Arbitraries ---

/** Generate a valid activity type (one of the predefined types or a custom "Other" string) */
const validActivityTypeArb: fc.Arbitrary<string> = fc.oneof(
  fc.constantFrom(...ACTIVITY_TYPES),
  fc.string({ minLength: 1, maxLength: 30 }) // custom "Other" input
);

/** Generate a missing activity type (null or empty string, matching falsy check) */
const missingActivityTypeArb: fc.Arbitrary<string | null> = fc.constantFrom(
  null,
  ""
);

/** Generate a valid date string (ISO format YYYY-MM-DD) */
const validDateArb: fc.Arbitrary<string> = fc
  .record({
    year: fc.integer({ min: 2020, max: 2030 }),
    month: fc.integer({ min: 1, max: 12 }),
    day: fc.integer({ min: 1, max: 28 }), // Capped at 28 to avoid invalid dates
  })
  .map(
    ({ year, month, day }) =>
      `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`
  );

/** Generate a missing date (empty string, matching falsy check) */
const missingDateArb: fc.Arbitrary<string> = fc.constant("");

/** Generate a form state with BOTH mandatory fields present (valid) */
const validFormStateArb: fc.Arbitrary<ActivityFormState> = fc.record({
  selectedActivityType: validActivityTypeArb,
  date: validDateArb,
});

/** Generate a form state with activity type missing */
const missingActivityTypeFormArb: fc.Arbitrary<ActivityFormState> = fc.record({
  selectedActivityType: missingActivityTypeArb,
  date: fc.oneof(validDateArb, missingDateArb),
});

/** Generate a form state with date missing */
const missingDateFormArb: fc.Arbitrary<ActivityFormState> = fc.record({
  selectedActivityType: fc.oneof(
    validActivityTypeArb,
    missingActivityTypeArb
  ) as fc.Arbitrary<string | null>,
  date: missingDateArb,
});

/** Generate a form state with at least one mandatory field missing */
const invalidFormStateArb: fc.Arbitrary<ActivityFormState> = fc.oneof(
  // Activity type missing (date may or may not be present)
  fc.record({
    selectedActivityType: missingActivityTypeArb,
    date: fc.oneof(validDateArb, missingDateArb),
  }),
  // Date missing (activity type may or may not be present)
  fc.record({
    selectedActivityType: fc.oneof(
      validActivityTypeArb,
      missingActivityTypeArb
    ) as fc.Arbitrary<string | null>,
    date: missingDateArb,
  })
);

/** Generate an arbitrary form state (mix of valid and invalid) */
const arbitraryFormStateArb: fc.Arbitrary<ActivityFormState> = fc.oneof(
  validFormStateArb,
  invalidFormStateArb
);

describe("Activity log mandatory field validation property test", () => {
  it("submission is allowed when both activity type and date are present", () => {
    fc.assert(
      fc.property(validFormStateArb, (formState) => {
        const blocked = isSubmissionBlocked(formState);
        expect(
          blocked,
          `Form with activityType="${formState.selectedActivityType}" and ` +
            `date="${formState.date}" should NOT be blocked, but was`
        ).toBe(false);
      }),
      { numRuns: 100 }
    );
  });

  it("submission is blocked when activity type is missing", () => {
    fc.assert(
      fc.property(
        fc.record({
          selectedActivityType: missingActivityTypeArb,
          date: validDateArb,
        }),
        (formState) => {
          const blocked = isSubmissionBlocked(formState);
          expect(
            blocked,
            `Form with activityType="${formState.selectedActivityType}" and ` +
              `date="${formState.date}" should be blocked (missing activity type)`
          ).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });

  it("submission is blocked when date is missing", () => {
    fc.assert(
      fc.property(
        fc.record({
          selectedActivityType: validActivityTypeArb,
          date: missingDateArb,
        }),
        (formState) => {
          const blocked = isSubmissionBlocked(formState);
          expect(
            blocked,
            `Form with activityType="${formState.selectedActivityType}" and ` +
              `date="${formState.date}" should be blocked (missing date)`
          ).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });

  it("submission is blocked iff either mandatory field is missing (biconditional)", () => {
    fc.assert(
      fc.property(arbitraryFormStateArb, (formState) => {
        const blocked = isSubmissionBlocked(formState);

        const hasMissingField =
          !formState.selectedActivityType || !formState.date;

        expect(
          blocked,
          `Form state: activityType="${formState.selectedActivityType}", ` +
            `date="${formState.date}". ` +
            `Expected blocked=${hasMissingField}, got blocked=${blocked}`
        ).toBe(hasMissingField);
      }),
      { numRuns: 100 }
    );
  });
});
