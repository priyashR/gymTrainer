import { useId, useState } from 'react';

interface JsonEditorProps {
  /** The raw JSON string to pre-populate the editor with. */
  initialJson: string;
  /** Called when the user clicks "Preview" with the current editor content. */
  onPreview: (rawJson: string) => void;
  /**
   * An inline parse error to display below the textarea.
   * When set, the editor content is NOT cleared — the user can fix the JSON.
   */
  parseError?: string;
  /** Whether the editor should be disabled (e.g. while uploading). */
  disabled?: boolean;
}

/**
 * Inline JSON editor pre-populated with raw file content.
 *
 * Displays a "Preview" button that re-parses the edited JSON and refreshes
 * the structured preview. If the JSON is invalid, an inline error is shown
 * without clearing the editor content.
 *
 * Requirements: 7.5, 7.6
 */
export function JsonEditor({ initialJson, onPreview, parseError, disabled = false }: JsonEditorProps) {
  const [value, setValue] = useState(initialJson);
  const errorId = useId();
  const textareaId = useId();

  function handlePreview() {
    onPreview(value);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
      <label htmlFor={textareaId} style={{ fontWeight: 500 }}>
        Edit JSON
      </label>

      <textarea
        id={textareaId}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        disabled={disabled}
        rows={20}
        spellCheck={false}
        aria-invalid={parseError ? true : undefined}
        aria-describedby={parseError ? errorId : undefined}
        style={{
          fontFamily: 'monospace',
          fontSize: '0.875rem',
          padding: '0.75rem',
          border: parseError ? '1px solid crimson' : '1px solid #ccc',
          borderRadius: 4,
          resize: 'vertical',
          width: '100%',
          boxSizing: 'border-box',
          cursor: disabled ? 'not-allowed' : 'text',
        }}
      />

      {/* Inline parse error — shown without clearing editor content (Requirement 7.6) */}
      {parseError && (
        <span
          id={errorId}
          role="alert"
          style={{ color: 'crimson', fontSize: '0.875rem' }}
        >
          {parseError}
        </span>
      )}

      <button
        type="button"
        onClick={handlePreview}
        disabled={disabled}
        style={{
          alignSelf: 'flex-start',
          padding: '0.6rem 1.25rem',
          fontWeight: 600,
          cursor: disabled ? 'not-allowed' : 'pointer',
          opacity: disabled ? 0.7 : 1,
        }}
      >
        Preview
      </button>
    </div>
  );
}
