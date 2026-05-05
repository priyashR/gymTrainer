import { type ChangeEvent, useId, useRef } from 'react';

interface FilePickerProps {
  /** Called with the selected File when the user picks a valid .json file. */
  onFileSelected: (file: File) => void;
  /** Whether the control should be disabled (e.g. while an upload is in progress). */
  disabled?: boolean;
}

const MAX_SIZE_LABEL = '1 MB';
const ACCEPTED_EXTENSION = '.json';

/**
 * File picker that accepts only `.json` files.
 *
 * Non-.json files are rejected client-side before any request is made.
 * A size hint (1 MB maximum) is displayed adjacent to the control.
 *
 * Requirements: 7.1, 7.2
 */
export function FilePicker({ onFileSelected, disabled = false }: FilePickerProps) {
  const inputId = useId();
  const hintId = useId();
  const inputRef = useRef<HTMLInputElement>(null);

  function handleChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    // Reject non-.json files before any request is made (Requirement 7.1)
    if (!file.name.toLowerCase().endsWith(ACCEPTED_EXTENSION)) {
      alert(`Only ${ACCEPTED_EXTENSION} files are accepted.`);
      // Reset the input so the same file can be re-selected after correction
      if (inputRef.current) inputRef.current.value = '';
      return;
    }

    onFileSelected(file);

    // Reset so the same file can be re-selected if the user edits and re-uploads
    if (inputRef.current) inputRef.current.value = '';
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
      <label htmlFor={inputId} style={{ fontWeight: 500 }}>
        Upload Program JSON
      </label>

      <input
        ref={inputRef}
        id={inputId}
        type="file"
        accept={ACCEPTED_EXTENSION}
        onChange={handleChange}
        disabled={disabled}
        aria-describedby={hintId}
        style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
      />

      {/* Size hint — Requirement 7.2 */}
      <span
        id={hintId}
        style={{ fontSize: '0.875rem', color: '#666' }}
        aria-label={`Maximum file size: ${MAX_SIZE_LABEL}`}
      >
        Max {MAX_SIZE_LABEL}
      </span>
    </div>
  );
}
