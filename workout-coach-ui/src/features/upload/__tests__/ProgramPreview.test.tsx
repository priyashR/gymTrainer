import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProgramPreview } from '../ProgramPreview';
import type { ParsedProgram } from '../../../types/upload';

// Requirements: 7.3, 7.4

// Helper: create a full valid program for testing
function createTestProgram(overrides?: Partial<ParsedProgram>): ParsedProgram {
  return {
    program_metadata: {
      program_name: 'Hypertrophy Block A',
      duration_weeks: 4,
      goal: 'Muscle Growth',
      equipment_profile: ['Barbell', 'Dumbbells', 'Cable Machine'],
      version: '1.0',
    },
    program_structure: [
      {
        week_number: 1,
        days: [
          {
            day_number: 1,
            day_label: 'Monday',
            focus_area: 'Push',
            modality: 'Hypertrophy',
            warm_up: [{ movement: 'Arm circles', instruction: '30 seconds each direction' }],
            blocks: [
              {
                block_type: 'Tier 1: Compound',
                format: 'Sets/Reps',
                movements: [
                  {
                    exercise_name: 'Bench Press',
                    prescribed_sets: 4,
                    prescribed_reps: '8-10',
                    prescribed_weight: '75% 1RM',
                  },
                  {
                    exercise_name: 'Overhead Press',
                    prescribed_sets: 3,
                    prescribed_reps: '10-12',
                    prescribed_weight: '60% 1RM',
                  },
                ],
              },
              {
                block_type: 'Tier 3: Finisher',
                format: 'AMRAP',
                time_cap_minutes: 8,
                movements: [
                  {
                    exercise_name: 'Push-ups',
                    prescribed_sets: 1,
                    prescribed_reps: 'Max',
                  },
                ],
              },
            ],
            cool_down: [{ movement: 'Stretching', instruction: '5 minutes' }],
          },
          {
            day_number: 3,
            day_label: 'Wednesday',
            focus_area: 'Pull',
            modality: 'Hypertrophy',
            warm_up: [{ movement: 'Band pull-aparts', instruction: '2x15' }],
            blocks: [
              {
                block_type: 'Tier 1: Compound',
                format: 'Sets/Reps',
                movements: [
                  {
                    exercise_name: 'Barbell Row',
                    prescribed_sets: 4,
                    prescribed_reps: '8-10',
                    prescribed_weight: '70% 1RM',
                  },
                ],
              },
            ],
            cool_down: [{ movement: 'Foam rolling', instruction: '5 minutes' }],
          },
        ],
      },
      {
        week_number: 2,
        days: [
          {
            day_number: 1,
            day_label: 'Monday',
            focus_area: 'Full Body',
            modality: 'CrossFit',
            warm_up: [{ movement: 'Rowing', instruction: '500m easy' }],
            blocks: [
              {
                block_type: 'Metcon',
                format: 'AMRAP',
                time_cap_minutes: 12,
                movements: [
                  {
                    exercise_name: 'Thrusters',
                    modality_type: 'Weightlifting',
                    prescribed_sets: 1,
                    prescribed_reps: '15',
                    prescribed_weight: '43kg',
                  },
                  {
                    exercise_name: 'Pull-ups',
                    modality_type: 'Gymnastics',
                    prescribed_sets: 1,
                    prescribed_reps: '10',
                  },
                ],
              },
            ],
            cool_down: [{ movement: 'Walk', instruction: '3 minutes' }],
          },
        ],
      },
    ],
    ...overrides,
  };
}

describe('ProgramPreview', () => {
  const defaultProps = {
    onSave: vi.fn(),
    onEditJson: vi.fn(),
    isUploading: false,
  };

  describe('Requirement 7.3: metadata display', () => {
    it('should render program_name', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByText('Hypertrophy Block A')).toBeInTheDocument();
    });

    it('should render goal', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByText('Muscle Growth')).toBeInTheDocument();
    });

    it('should render duration_weeks', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByText('4 weeks')).toBeInTheDocument();
    });

    it('should render singular "week" when duration_weeks is 1', () => {
      const program = createTestProgram({
        program_metadata: {
          program_name: 'Single Week',
          duration_weeks: 1,
          goal: 'GPP',
          equipment_profile: ['Bodyweight'],
          version: '1.0',
        },
      });
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByText('1 week')).toBeInTheDocument();
    });

    it('should render equipment_profile as comma-separated list', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByText('Barbell, Dumbbells, Cable Machine')).toBeInTheDocument();
    });
  });

  describe('Requirement 7.3: week/day/block breakdown', () => {
    it('should render week sections', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByText(/Week 1/)).toBeInTheDocument();
      expect(screen.getByText(/Week 2/)).toBeInTheDocument();
    });

    it('should render day sections with day_number, day_label, and focus_area', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByText(/Day 1.*Monday.*Push/)).toBeInTheDocument();
      expect(screen.getByText(/Day 3.*Wednesday.*Pull/)).toBeInTheDocument();
    });

    it('should render day modality', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getAllByText('(Hypertrophy)').length).toBeGreaterThan(0);
      expect(screen.getByText('(CrossFit)')).toBeInTheDocument();
    });

    it('should render block types and formats', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      // "Tier 1: Compound — Sets/Reps" appears in multiple days
      expect(screen.getAllByText(/Tier 1: Compound.*Sets\/Reps/).length).toBeGreaterThan(0);
      expect(screen.getByText(/Tier 3: Finisher.*AMRAP.*8 min cap/)).toBeInTheDocument();
      expect(screen.getByText(/Metcon.*AMRAP.*12 min cap/)).toBeInTheDocument();
    });

    it('should render movement names, sets, reps, and weight', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByText('Bench Press')).toBeInTheDocument();
      expect(screen.getByText('Overhead Press')).toBeInTheDocument();
      expect(screen.getByText('Push-ups')).toBeInTheDocument();
      expect(screen.getByText('Barbell Row')).toBeInTheDocument();
      expect(screen.getByText('Thrusters')).toBeInTheDocument();
      expect(screen.getByText('Pull-ups')).toBeInTheDocument();

      // Sets
      expect(screen.getAllByText('4').length).toBeGreaterThan(0);
      expect(screen.getAllByText('3').length).toBeGreaterThan(0);

      // Reps
      expect(screen.getAllByText('8-10').length).toBeGreaterThan(0);
      expect(screen.getByText('10-12')).toBeInTheDocument();
      expect(screen.getByText('Max')).toBeInTheDocument();

      // Weight
      expect(screen.getByText('75% 1RM')).toBeInTheDocument();
      expect(screen.getByText('60% 1RM')).toBeInTheDocument();
      expect(screen.getByText('43kg')).toBeInTheDocument();
    });

    it('should display dash for movements without prescribed_weight', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      // Push-ups and Pull-ups have no weight — should show '—'
      expect(screen.getAllByText('—').length).toBeGreaterThan(0);
    });
  });

  describe('Requirement 7.4: action buttons', () => {
    it('should render "Save to Vault" button', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByRole('button', { name: 'Save to Vault' })).toBeInTheDocument();
    });

    it('should render "Edit JSON" button', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      expect(screen.getByRole('button', { name: 'Edit JSON' })).toBeInTheDocument();
    });

    it('should call onSave when "Save to Vault" is clicked', async () => {
      const user = userEvent.setup();
      const onSave = vi.fn();
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} onSave={onSave} />);

      await user.click(screen.getByRole('button', { name: 'Save to Vault' }));

      expect(onSave).toHaveBeenCalledOnce();
    });

    it('should call onEditJson when "Edit JSON" is clicked', async () => {
      const user = userEvent.setup();
      const onEditJson = vi.fn();
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} onEditJson={onEditJson} />);

      await user.click(screen.getByRole('button', { name: 'Edit JSON' }));

      expect(onEditJson).toHaveBeenCalledOnce();
    });

    it('should disable "Save to Vault" button when isUploading is true', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} isUploading={true} />);

      const saveButton = screen.getByRole('button', { name: /Saving to Vault/i });
      expect(saveButton).toBeDisabled();
    });

    it('should show "Saving…" text when isUploading is true', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} isUploading={true} />);

      expect(screen.getByText('Saving…')).toBeInTheDocument();
    });

    it('should disable "Edit JSON" button when isUploading is true', () => {
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} isUploading={true} />);

      expect(screen.getByRole('button', { name: 'Edit JSON' })).toBeDisabled();
    });
  });

  describe('collapsible sections', () => {
    it('should collapse week section when week header is clicked', async () => {
      const user = userEvent.setup();
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      // Week 1 should be expanded by default
      expect(screen.getByText('Bench Press')).toBeInTheDocument();

      // Click Week 1 header to collapse
      const week1Button = screen.getByRole('button', { name: /Week 1/ });
      await user.click(week1Button);

      // Bench Press should no longer be visible (it's in Week 1)
      expect(screen.queryByText('Bench Press')).not.toBeInTheDocument();
    });

    it('should collapse day section when day header is clicked', async () => {
      const user = userEvent.setup();
      const program = createTestProgram();
      render(<ProgramPreview program={program} {...defaultProps} />);

      // Day 1 content should be visible
      expect(screen.getByText('Bench Press')).toBeInTheDocument();

      // Click Day 1 header to collapse
      const day1Button = screen.getByRole('button', { name: /Day 1.*Monday.*Push/ });
      await user.click(day1Button);

      // Bench Press should no longer be visible
      expect(screen.queryByText('Bench Press')).not.toBeInTheDocument();
      // But Barbell Row (Day 3) should still be visible
      expect(screen.getByText('Barbell Row')).toBeInTheDocument();
    });
  });
});
