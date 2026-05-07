import { useState } from 'react';
import type { FieldError } from '../../types/auth';

interface ProgramJsonEditorProps {
  /** Initial JSON content (Upload_Schema format). */
  initialJson: string;
  /** Called when user clicks "Save Changes". */
  onSave: (json: string) => Promise<{ success: boolean; errors?: FieldError[] | string }>;
  /** Called when user clicks "Cancel" to close the editor. */
  onCancel: () => void;
}

/**
 * Inline JSON editor pre-populated with program content in Upload_Schema format.
 * Provides Preview (client-side parse) and Save Changes (PUT request) actions.
 *
 * Requirements: 9.7, 9.8, 9.9, 9.10
 */
export function ProgramJsonEditor({ initialJson, onSave, onCancel }: ProgramJsonEditorProps) {
  const [json, setJson] = useState(initialJson);
  const [parseError, setParseError] = useState<string | null>(null);
  const [preview, setPreview] = useState<object | null>(null);
  const [saving, setSaving] = useState(false);
  const [validationErrors, setValidationErrors] = useState<FieldError[] | string | null>(null);

  // -------------------------------------------------------------------------
  // Preview — parse JSON client-side
  // -------------------------------------------------------------------------
  const handlePreview = () => {
    setValidationErrors(null);
    try {
      const parsed = JSON.parse(json);
      setParseError(null);
      setPreview(parsed);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Invalid JSON';
      setParseError(msg);
      // Do NOT clear editor content (Requirement 9.8)
    }
  };

  // -------------------------------------------------------------------------
  // Save Changes — submit PUT request
  // -------------------------------------------------------------------------
  const handleSave = async () => {
    setValidationErrors(null);
    setParseError(null);

    // Validate JSON client-side first
    try {
      JSON.parse(json);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Invalid JSON';
      setParseError(msg);
      return;
    }

    setSaving(true);
    const result = await onSave(json);
    setSaving(false);

    if (!result.success && result.errors) {
      setValidationErrors(result.errors);
    }
  };

  return (
    <div style={{ marginTop: '1rem' }}>
      <h3 style={{ margin: '0 0 0.75rem' }}>Edit Program JSON</h3>

      {/* JSON textarea */}
      <textarea
        value={json}
        onChange={(e) => {
          setJson(e.target.value);
          setParseError(null);
          setPreview(null);
        }}
        aria-label="Program JSON editor"
        style={{
          width: '100%',
          minHeight: 300,
          fontFamily: 'monospace',
          fontSize: '0.85rem',
          padding: '0.75rem',
          border: '1px solid #ccc',
          borderRadius: 4,
          resize: 'vertical',
        }}
      />

      {/* Parse error */}
      {parseError && (
        <div role="alert" style={{ color: '#c62828', marginTop: '0.5rem', fontSize: '0.9rem' }}>
          <strong>JSON Parse Error:</strong> {parseError}
        </div>
      )}

      {/* Validation errors from server (400) */}
      {validationErrors && (
        <div role="alert" style={{ color: '#c62828', marginTop: '0.5rem', fontSize: '0.9rem' }}>
          <strong>Validation Errors:</strong>
          {typeof validationErrors === 'string' ? (
            <p style={{ margin: '0.25rem 0 0' }}>{validationErrors}</p>
          ) : (
            <ul style={{ margin: '0.25rem 0 0', paddingLeft: '1.25rem' }}>
              {validationErrors.map((err, i) => (
                <li key={i}>
                  <code>{err.field}</code>: {err.message}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* Action buttons */}
      <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.75rem' }}>
        <button
          type="button"
          onClick={handlePreview}
          style={{ padding: '0.5rem 1rem', cursor: 'pointer', borderRadius: 4 }}
        >
          Preview
        </button>
        <button
          type="button"
          onClick={handleSave}
          disabled={saving}
          style={{
            padding: '0.5rem 1rem',
            cursor: saving ? 'not-allowed' : 'pointer',
            borderRadius: 4,
            background: '#2e7d32',
            color: '#fff',
            border: 'none',
          }}
        >
          {saving ? 'Saving…' : 'Save Changes'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          style={{ padding: '0.5rem 1rem', cursor: 'pointer', borderRadius: 4 }}
        >
          Cancel
        </button>
      </div>

      {/* Structured preview */}
      {preview && !parseError && (
        <div
          style={{
            marginTop: '1rem',
            padding: '1rem',
            background: '#f5f5f5',
            borderRadius: 4,
            border: '1px solid #e0e0e0',
          }}
        >
          <h4 style={{ margin: '0 0 0.5rem' }}>Preview</h4>
          <pre style={{ margin: 0, fontSize: '0.8rem', whiteSpace: 'pre-wrap', overflowX: 'auto' }}>
            {JSON.stringify(preview, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
