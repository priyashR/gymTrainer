import { describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
import { within } from '@testing-library/react';
import fc from 'fast-check';
import { ProgramPreview } from '../ProgramPreview';
import type { ParsedProgram, ProgramMetadata } from '../../../types/upload';

// Feature: workout-creator-service-upload, Property 9: Client-side preview displays required metadata fields
// Validates: Requirements 7.3

/**
 * Arbitrary for generating valid ProgramMetadata objects.
 * Generates non-empty strings for program_name, goal, and equipment_profile
 * with duration_weeks constrained to 1 or 4.
 *
 * We use alphanumeric strings to avoid edge cases with special characters
 * in text matching, while still exercising the property across many inputs.
 */
const alphanumChar = fc.constantFrom(
  ...'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'.split('')
);

const programMetadataArb: fc.Arbitrary<ProgramMetadata> = fc.record({
  program_name: fc.stringOf(alphanumChar, { minLength: 1, maxLength: 50 }),
  duration_weeks: fc.constantFrom(1 as const, 4 as const),
  goal: fc.stringOf(alphanumChar, { minLength: 1, maxLength: 50 }),
  equipment_profile: fc.array(
    fc.stringOf(alphanumChar, { minLength: 1, maxLength: 30 }),
    { minLength: 1, maxLength: 5 }
  ),
  version: fc.constant('1.0' as const),
});

/**
 * Builds a minimal valid ParsedProgram from a given ProgramMetadata.
 * The program_structure contains the minimum required weeks/days/blocks/movements
 * to satisfy the component's rendering without focusing on structure content.
 */
function buildProgramFromMetadata(metadata: ProgramMetadata): ParsedProgram {
  const weeks = Array.from({ length: metadata.duration_weeks }, (_, i) => ({
    week_number: i + 1,
    days: [
      {
        day_number: 1,
        day_label: 'Monday',
        focus_area: 'Full Body',
        modality: 'Hypertrophy' as const,
        warm_up: [{ movement: 'Jog', instruction: '5 min' }],
        blocks: [
          {
            block_type: 'Tier 1: Compound',
            format: 'Sets/Reps',
            movements: [
              {
                exercise_name: 'Squat',
                prescribed_sets: 3,
                prescribed_reps: '8-10',
              },
            ],
          },
        ],
        cool_down: [{ movement: 'Stretch', instruction: '5 min' }],
      },
    ],
  }));

  return {
    program_metadata: metadata,
    program_structure: weeks,
  };
}

describe('ProgramPreview property test — metadata display', () => {
  const defaultProps = {
    onSave: vi.fn(),
    onEditJson: vi.fn(),
    isUploading: false,
  };

  it('should always display program_name, goal, duration_weeks, and equipment_profile for any valid ProgramMetadata', () => {
    fc.assert(
      fc.property(programMetadataArb, (metadata) => {
        const program = buildProgramFromMetadata(metadata);
        const { container, unmount } = render(<ProgramPreview program={program} {...defaultProps} />);
        const section = within(container);

        // program_name is rendered as the h2 heading
        const heading = section.getByRole('heading', { level: 2 });
        expect(heading).toHaveTextContent(metadata.program_name);

        // goal is rendered in a dd element following the "Goal" dt
        const dts = section.getAllByRole('term');
        const dds = section.getAllByRole('definition');

        const goalIndex = dts.findIndex((dt) => dt.textContent === 'Goal');
        expect(goalIndex).toBeGreaterThanOrEqual(0);
        expect(dds[goalIndex]).toHaveTextContent(metadata.goal);

        // duration_weeks is rendered in the "Duration" dd
        const durationIndex = dts.findIndex((dt) => dt.textContent === 'Duration');
        expect(durationIndex).toBeGreaterThanOrEqual(0);
        const expectedDuration =
          metadata.duration_weeks === 1
            ? '1 week'
            : `${metadata.duration_weeks} weeks`;
        expect(dds[durationIndex]).toHaveTextContent(expectedDuration);

        // equipment_profile is rendered as comma-separated in the "Equipment" dd
        const equipmentIndex = dts.findIndex((dt) => dt.textContent === 'Equipment');
        expect(equipmentIndex).toBeGreaterThanOrEqual(0);
        const expectedEquipment = metadata.equipment_profile.join(', ');
        expect(dds[equipmentIndex]).toHaveTextContent(expectedEquipment);

        unmount();
      }),
      { numRuns: 100 }
    );
  });
});
