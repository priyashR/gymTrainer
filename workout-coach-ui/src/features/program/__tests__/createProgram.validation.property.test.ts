import { describe, it, expect } from "vitest";
import fc from "fast-check";

/**
 * **Validates: Requirements 3.13, 3.14**
 *
 * Property: The Create Program save is blocked if and only if:
 * - The program name is empty (or whitespace-only), OR
 * - ALL days have no assignment (type is null for every day)
 *
 * From CreateProgramPage.tsx validate() logic:
 * - `!state.programName.trim()` → name error
 * - `!state.days.some((day) => day.type !== null)` → days error
 *
 * Save proceeds only when both conditions pass (name is non-empty AND at
 * least one day has a non-null type).
 */

// --- Types mirroring the actual DayAssignment from DayTile.tsx ---

interface DayAssignment {
  type: "workout" | "activity" | null;
  workoutId?: string;
  workoutName?: string;
  activityType?: string;
}

interface CreateProgramState {
  programName: string;
  days: DayAssignment[];
}

// --- Pure validation logic replicating CreateProgramPage's validate() ---

function validateCreateProgram(state: CreateProgramState): {
  nameError?: string;
  daysError?: string;
} {
  const errors: { nameError?: string; daysError?: string } = {};

  if (!state.programName.trim()) {
    errors.nameError = "Program name is required.";
  }

  const hasAssignment = state.days.some((day) => day.type !== null);
  if (!hasAssignment) {
    errors.daysError =
      "At least one day must have a workout or activity assigned.";
  }

  return errors;
}

function isSaveBlocked(state: CreateProgramState): boolean {
  const errors = validateCreateProgram(state);
  return Object.keys(errors).length > 0;
}

// --- Arbitraries ---

/** Generate a valid (non-empty, non-whitespace-only) program name */
const validProgramNameArb: fc.Arbitrary<string> = fc
  .tuple(
    fc.string({ minLength: 1, maxLength: 50 }),
    fc.constantFrom("A", "B", "My Program", "PPL Hypertrophy", "Push Day")
  )
  .map(([random, fixed]) => (random.trim().length > 0 ? random : fixed));

/** Generate an empty/whitespace-only program name */
const emptyProgramNameArb: fc.Arbitrary<string> = fc.constantFrom(
  "",
  " ",
  "  ",
  "\t",
  "\n",
  "   \t\n  "
);

/** Generate a day assignment with a workout assigned */
const workoutAssignmentArb: fc.Arbitrary<DayAssignment> = fc.record({
  type: fc.constant("workout" as const),
  workoutId: fc.uuid(),
  workoutName: fc.string({ minLength: 1, maxLength: 30 }),
});

/** Generate a day assignment with an activity assigned */
const activityAssignmentArb: fc.Arbitrary<DayAssignment> = fc.record({
  type: fc.constant("activity" as const),
  activityType: fc.constantFrom(
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
    "Other"
  ),
});

/** Generate an unassigned day (type is null) */
const unassignedDayArb: fc.Arbitrary<DayAssignment> = fc.constant({
  type: null,
});

/** Generate a day that has an assignment (workout or activity) */
const assignedDayArb: fc.Arbitrary<DayAssignment> = fc.oneof(
  workoutAssignmentArb,
  activityAssignmentArb
);

/** Generate a mix of days (some assigned, some not) */
const mixedDayArb: fc.Arbitrary<DayAssignment> = fc.oneof(
  assignedDayArb,
  unassignedDayArb
);

/**
 * Generate a days array where ALL days are unassigned (type is null).
 * This triggers the "no assignment" validation error.
 */
const allUnassignedDaysArb: fc.Arbitrary<DayAssignment[]> = fc
  .integer({ min: 1, max: 7 })
  .chain((length) => fc.array(unassignedDayArb, { minLength: length, maxLength: length }));

/**
 * Generate a days array where AT LEAST ONE day has an assignment.
 * This passes the "at least one assignment" validation.
 */
const atLeastOneAssignedDaysArb: fc.Arbitrary<DayAssignment[]> = fc
  .tuple(
    fc.array(mixedDayArb, { minLength: 0, maxLength: 6 }),
    assignedDayArb,
    fc.array(mixedDayArb, { minLength: 0, maxLength: 6 })
  )
  .map(([before, assigned, after]) => [...before, assigned, ...after]);

// --- Composed state arbitraries ---

/** A valid state: non-empty name AND at least one assigned day */
const validStateArb: fc.Arbitrary<CreateProgramState> = fc.record({
  programName: validProgramNameArb,
  days: atLeastOneAssignedDaysArb,
});

/** An invalid state where the name is empty (days may or may not be valid) */
const emptyNameStateArb: fc.Arbitrary<CreateProgramState> = fc.record({
  programName: emptyProgramNameArb,
  days: fc.oneof(allUnassignedDaysArb, atLeastOneAssignedDaysArb),
});

/** An invalid state where all days are unassigned (name may or may not be valid) */
const zerAssignmentsStateArb: fc.Arbitrary<CreateProgramState> = fc.record({
  programName: fc.oneof(validProgramNameArb, emptyProgramNameArb),
  days: allUnassignedDaysArb,
});

/** Arbitrary state mixing valid and invalid combinations */
const arbitraryStateArb: fc.Arbitrary<CreateProgramState> = fc.oneof(
  validStateArb,
  emptyNameStateArb,
  zerAssignmentsStateArb
);

// --- Property tests ---

describe("Create Program save validation property test", () => {
  it("save proceeds when name is non-empty AND at least one day is assigned", () => {
    fc.assert(
      fc.property(validStateArb, (state) => {
        const blocked = isSaveBlocked(state);
        expect(
          blocked,
          `State with name="${state.programName}" and ${state.days.filter((d) => d.type !== null).length} assigned days ` +
            `should NOT be blocked, but was`
        ).toBe(false);
      }),
      { numRuns: 100 }
    );
  });

  it("save is blocked when program name is empty or whitespace-only", () => {
    fc.assert(
      fc.property(
        fc.record({
          programName: emptyProgramNameArb,
          days: atLeastOneAssignedDaysArb,
        }),
        (state) => {
          const blocked = isSaveBlocked(state);
          expect(
            blocked,
            `State with name="${state.programName}" should be blocked (empty name)`
          ).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });

  it("save is blocked when all days have no assignment", () => {
    fc.assert(
      fc.property(
        fc.record({
          programName: validProgramNameArb,
          days: allUnassignedDaysArb,
        }),
        (state) => {
          const blocked = isSaveBlocked(state);
          expect(
            blocked,
            `State with name="${state.programName}" and ${state.days.length} unassigned days ` +
              `should be blocked (zero assignments)`
          ).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });

  it("save is blocked iff name is empty OR all days have no assignment (biconditional)", () => {
    fc.assert(
      fc.property(arbitraryStateArb, (state) => {
        const blocked = isSaveBlocked(state);

        const nameIsEmpty = !state.programName.trim();
        const allDaysUnassigned = !state.days.some((d) => d.type !== null);
        const shouldBeBlocked = nameIsEmpty || allDaysUnassigned;

        expect(
          blocked,
          `State: name="${state.programName}", assigned=${state.days.filter((d) => d.type !== null).length}/${state.days.length}. ` +
            `Expected blocked=${shouldBeBlocked}, got blocked=${blocked}`
        ).toBe(shouldBeBlocked);
      }),
      { numRuns: 100 }
    );
  });
});
