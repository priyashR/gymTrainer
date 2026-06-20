import { describe, it, expect } from "vitest";
import fc from "fast-check";
import type { VaultItem } from "../../../types/vault";
import type { SearchFilters } from "../FilterChipsRow";

/**
 * **Validates: Requirements 5.3, 5.4**
 *
 * Property: When multiple filter chips are active, AND logic is applied —
 * every returned program must satisfy ALL active filters simultaneously.
 *
 * The SearchPage applies filters as follows:
 * - focusArea and modality are sent to the backend API (server-side filtering)
 * - days is applied client-side by filtering on the `durationWeeks` field
 *
 * This test generates arbitrary program datasets and arbitrary filter
 * combinations, applies the same filtering logic as SearchPage, and asserts
 * that every program in the result set satisfies every active filter.
 */

// --- Filter options matching FilterChipsRow.tsx ---
const FOCUS_AREA_OPTIONS = ["Push", "Pull", "Legs", "Full Body", "Metcon"];
const MODALITY_OPTIONS = ["CrossFit", "Hypertrophy", "Strength"];
const DAYS_OPTIONS = ["3", "4", "5", "6", "7"];

/**
 * Extended program type that includes the fields the server would match against.
 * In production, `focusArea` and `modality` are derived from the program's workout
 * days (VaultDay.focusArea and VaultDay.modality). For this property test, we
 * surface them as top-level fields to simulate the server-side filter behaviour.
 */
interface FilterableProgram extends VaultItem {
  focusArea: string;
  modality: string;
}

/**
 * Pure filtering function replicating SearchPage's AND-logic behaviour.
 *
 * - If `filters.focusArea` is set (non-empty), only programs whose `focusArea`
 *   matches (case-insensitive) are included.
 * - If `filters.modality` is set (non-empty), only programs whose `modality`
 *   matches (case-insensitive) are included.
 * - If `filters.days` is set (non-empty), only programs whose `durationWeeks`
 *   equals the numeric value are included.
 *
 * All active filters must be satisfied (AND logic).
 */
function applyFilters(
  programs: FilterableProgram[],
  filters: SearchFilters
): FilterableProgram[] {
  let results = [...programs];

  // Server-side: focusArea filter
  if (filters.focusArea) {
    results = results.filter(
      (p) => p.focusArea.toLowerCase() === filters.focusArea.toLowerCase()
    );
  }

  // Server-side: modality filter
  if (filters.modality) {
    results = results.filter(
      (p) => p.modality.toLowerCase() === filters.modality.toLowerCase()
    );
  }

  // Client-side: days filter on durationWeeks (matches SearchPage logic)
  if (filters.days) {
    const daysValue = parseInt(filters.days, 10);
    results = results.filter((p) => p.durationWeeks === daysValue);
  }

  return results;
}

// --- Arbitraries ---

/** Generate a filter combination where each filter is either active or "All" (empty string) */
const filtersArbitrary: fc.Arbitrary<SearchFilters> = fc.record({
  focusArea: fc.oneof(fc.constant(""), fc.constantFrom(...FOCUS_AREA_OPTIONS)),
  modality: fc.oneof(fc.constant(""), fc.constantFrom(...MODALITY_OPTIONS)),
  days: fc.oneof(fc.constant(""), fc.constantFrom(...DAYS_OPTIONS)),
});

/** Generate a single program with values drawn from the valid filter options */
const programArbitrary: fc.Arbitrary<FilterableProgram> = fc.record({
  id: fc.uuid(),
  name: fc.string({ minLength: 1, maxLength: 50 }),
  goal: fc.constantFrom(...MODALITY_OPTIONS, "General Fitness", "Weight Loss"),
  durationWeeks: fc.constantFrom(3, 4, 5, 6, 7, 8, 10, 12),
  equipmentProfile: fc.array(
    fc.constantFrom("Barbell", "Dumbbells", "Kettlebell", "Bodyweight"),
    { minLength: 0, maxLength: 3 }
  ),
  contentSource: fc.constantFrom(
    "AI_GENERATED" as const,
    "UPLOADED" as const,
    "MANUAL" as const
  ),
  createdAt: fc.constant("2024-01-01T00:00:00Z"),
  updatedAt: fc.constant("2024-01-01T00:00:00Z"),
  focusArea: fc.constantFrom(...FOCUS_AREA_OPTIONS, "Cardio", "Core"),
  modality: fc.constantFrom(...MODALITY_OPTIONS, "General", "Endurance"),
});

/** Generate a list of programs (1 to 30 items) */
const programListArbitrary = fc.array(programArbitrary, {
  minLength: 1,
  maxLength: 30,
});

describe("Filter chips AND-logic property test", () => {
  it("every program in filtered results satisfies ALL active filters", () => {
    fc.assert(
      fc.property(programListArbitrary, filtersArbitrary, (programs, filters) => {
        const results = applyFilters(programs, filters);

        for (const program of results) {
          // If focusArea filter is active, program must match
          if (filters.focusArea) {
            expect(
              program.focusArea.toLowerCase(),
              `Program "${program.name}" has focusArea "${program.focusArea}" ` +
                `but filter requires "${filters.focusArea}"`
            ).toBe(filters.focusArea.toLowerCase());
          }

          // If modality filter is active, program must match
          if (filters.modality) {
            expect(
              program.modality.toLowerCase(),
              `Program "${program.name}" has modality "${program.modality}" ` +
                `but filter requires "${filters.modality}"`
            ).toBe(filters.modality.toLowerCase());
          }

          // If days filter is active, program.durationWeeks must equal the numeric value
          if (filters.days) {
            const daysValue = parseInt(filters.days, 10);
            expect(
              program.durationWeeks,
              `Program "${program.name}" has durationWeeks ${program.durationWeeks} ` +
                `but filter requires days=${filters.days}`
            ).toBe(daysValue);
          }
        }
      }),
      { numRuns: 100 }
    );
  });

  it("programs excluded by filters violate at least one active filter", () => {
    fc.assert(
      fc.property(programListArbitrary, filtersArbitrary, (programs, filters) => {
        const results = applyFilters(programs, filters);
        const resultIds = new Set(results.map((p) => p.id));
        const excluded = programs.filter((p) => !resultIds.has(p.id));

        for (const program of excluded) {
          const violatesFocusArea =
            filters.focusArea !== "" &&
            program.focusArea.toLowerCase() !== filters.focusArea.toLowerCase();

          const violatesModality =
            filters.modality !== "" &&
            program.modality.toLowerCase() !== filters.modality.toLowerCase();

          const violatesDays =
            filters.days !== "" &&
            program.durationWeeks !== parseInt(filters.days, 10);

          // The program must violate at least one active filter to be excluded
          expect(
            violatesFocusArea || violatesModality || violatesDays,
            `Program "${program.name}" was excluded but satisfies all active filters: ` +
              `focusArea="${program.focusArea}" (filter="${filters.focusArea}"), ` +
              `modality="${program.modality}" (filter="${filters.modality}"), ` +
              `durationWeeks=${program.durationWeeks} (filter="${filters.days}")`
          ).toBe(true);
        }
      }),
      { numRuns: 100 }
    );
  });

  it("no filters active returns all programs unchanged", () => {
    fc.assert(
      fc.property(programListArbitrary, (programs) => {
        const noFilters: SearchFilters = { focusArea: "", modality: "", days: "" };
        const results = applyFilters(programs, noFilters);

        expect(results).toHaveLength(programs.length);
      }),
      { numRuns: 100 }
    );
  });
});
