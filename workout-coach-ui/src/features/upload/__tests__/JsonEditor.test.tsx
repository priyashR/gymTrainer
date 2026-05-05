import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { JsonEditor } from '../JsonEditor';

// Requirements: 7.5, 7.6

const validJson = JSON.stringify(
  {
    program_metadata: {
      program_name: 'Test Program',
      duration_weeks: 1,
      goal: 'Strength',
      equipment_profile: ['Barbell'],
      version: '1.0',
    },
    program_structure: [],
  },
  null,
  2
);

describe('JsonEditor', () => {
  const defaultProps = {
    initialJson: validJson,
    onPreview: vi.fn(),
  };

  describe('Requirement 7.5: pre-population with raw JSON', () => {
    it('should pre-populate textarea with initialJson content', () => {
      render(<JsonEditor {...defaultProps} />);

      const textarea = screen.getByRole('textbox', { name: /edit json/i });
      expect(textarea).toHaveValue(validJson);
    });

    it('should allow editing the textarea content', () => {
      render(<JsonEditor {...defaultProps} />);

      const textarea = screen.getByRole('textbox', { name: /edit json/i });
      fireEvent.change(textarea, { target: { value: '{"new": "content"}' } });

      expect(textarea).toHaveValue('{"new": "content"}');
    });

    it('should call onPreview with current editor content when Preview is clicked', async () => {
      const user = userEvent.setup();
      const onPreview = vi.fn();
      render(<JsonEditor {...defaultProps} onPreview={onPreview} />);

      await user.click(screen.getByRole('button', { name: 'Preview' }));

      expect(onPreview).toHaveBeenCalledOnce();
      expect(onPreview).toHaveBeenCalledWith(validJson);
    });

    it('should call onPreview with modified content after editing', async () => {
      const user = userEvent.setup();
      const onPreview = vi.fn();
      render(<JsonEditor {...defaultProps} onPreview={onPreview} />);

      const textarea = screen.getByRole('textbox', { name: /edit json/i });
      fireEvent.change(textarea, { target: { value: '{"edited": true}' } });
      await user.click(screen.getByRole('button', { name: 'Preview' }));

      expect(onPreview).toHaveBeenCalledWith('{"edited": true}');
    });
  });

  describe('Requirement 7.6: inline parse error without clearing content', () => {
    it('should display inline parse error when parseError prop is set', () => {
      render(<JsonEditor {...defaultProps} parseError="Unexpected token at position 5" />);

      expect(screen.getByRole('alert')).toHaveTextContent('Unexpected token at position 5');
    });

    it('should not clear editor content when parseError is displayed', () => {
      render(<JsonEditor {...defaultProps} parseError="Invalid JSON" />);

      const textarea = screen.getByRole('textbox', { name: /edit json/i });
      expect(textarea).toHaveValue(validJson);
    });

    it('should not display error when parseError is undefined', () => {
      render(<JsonEditor {...defaultProps} />);

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('should mark textarea as aria-invalid when parseError is present', () => {
      render(<JsonEditor {...defaultProps} parseError="Bad JSON" />);

      const textarea = screen.getByRole('textbox', { name: /edit json/i });
      expect(textarea).toHaveAttribute('aria-invalid', 'true');
    });

    it('should not mark textarea as aria-invalid when no parseError', () => {
      render(<JsonEditor {...defaultProps} />);

      const textarea = screen.getByRole('textbox', { name: /edit json/i });
      expect(textarea).not.toHaveAttribute('aria-invalid');
    });

    it('should preserve user edits when parseError appears', () => {
      const { rerender } = render(<JsonEditor {...defaultProps} />);

      const textarea = screen.getByRole('textbox', { name: /edit json/i });
      fireEvent.change(textarea, { target: { value: '{invalid json' } });

      // Simulate parent re-rendering with a parse error
      rerender(<JsonEditor {...defaultProps} parseError="Unexpected end of JSON input" />);

      // The textarea should still contain the user's edited content
      expect(textarea).toHaveValue('{invalid json');
      expect(screen.getByRole('alert')).toHaveTextContent('Unexpected end of JSON input');
    });
  });

  describe('disabled state', () => {
    it('should disable textarea when disabled prop is true', () => {
      render(<JsonEditor {...defaultProps} disabled={true} />);

      const textarea = screen.getByRole('textbox', { name: /edit json/i });
      expect(textarea).toBeDisabled();
    });

    it('should disable Preview button when disabled prop is true', () => {
      render(<JsonEditor {...defaultProps} disabled={true} />);

      expect(screen.getByRole('button', { name: 'Preview' })).toBeDisabled();
    });

    it('should not disable textarea when disabled prop is false', () => {
      render(<JsonEditor {...defaultProps} disabled={false} />);

      const textarea = screen.getByRole('textbox', { name: /edit json/i });
      expect(textarea).not.toBeDisabled();
    });
  });
});
