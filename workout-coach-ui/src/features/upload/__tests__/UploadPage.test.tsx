import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { UploadPage } from '../UploadPage';
import type { UploadState } from '../useUpload';

// Requirements: 10.1, 10.2

const mockOnFileSelected = vi.fn();
const mockOnEditJson = vi.fn();
const mockOnPreview = vi.fn();
const mockOnSave = vi.fn();
const mockOnReset = vi.fn();

let mockState: UploadState;

vi.mock('../useUpload', () => ({
  useUpload: () => ({
    state: mockState,
    onFileSelected: mockOnFileSelected,
    onEditJson: mockOnEditJson,
    onPreview: mockOnPreview,
    onSave: mockOnSave,
    onReset: mockOnReset,
  }),
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <UploadPage />
    </MemoryRouter>
  );
}

beforeEach(() => {
  mockState = { status: 'idle' };
  vi.clearAllMocks();
});

describe('UploadPage — success state "View in Vault" link', () => {
  beforeEach(() => {
    mockState = {
      status: 'success',
      programName: 'Push Pull Legs',
      programId: 'prog-abc-123',
    };
  });

  it('renders "View in Vault" link with correct href when upload is successful (Requirement 10.1)', () => {
    renderPage();

    const link = screen.getByRole('link', { name: /view in vault/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/vault/programs/prog-abc-123');
  });

  it('renders the program name in the success message (Requirement 10.2)', () => {
    renderPage();

    expect(screen.getByText(/push pull legs/i)).toBeInTheDocument();
    expect(screen.getByText(/has been saved to your vault/i)).toBeInTheDocument();
  });

  it('"View in Vault" link points to /vault/programs/{programId} using the id from the upload response', () => {
    mockState = {
      status: 'success',
      programName: 'Full Body Blast',
      programId: 'unique-id-456',
    };
    renderPage();

    const link = screen.getByRole('link', { name: /view in vault/i });
    expect(link).toHaveAttribute('href', '/vault/programs/unique-id-456');
  });

  it('"Upload Another" button is present alongside the "View in Vault" link', async () => {
    const user = userEvent.setup();
    renderPage();

    const link = screen.getByRole('link', { name: /view in vault/i });
    const button = screen.getByRole('button', { name: /upload another/i });

    expect(link).toBeInTheDocument();
    expect(button).toBeInTheDocument();

    await user.click(button);
    expect(mockOnReset).toHaveBeenCalledOnce();
  });
});
