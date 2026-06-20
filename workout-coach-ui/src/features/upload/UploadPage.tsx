import { Link } from 'react-router-dom';
import type { FieldError } from '../../types/auth';
import { FilePicker } from './FilePicker';
import { JsonEditor } from './JsonEditor';
import { ProgramPreview } from './ProgramPreview';
import { useUpload } from './useUpload';

/**
 * Route-level page component for the upload feature.
 *
 * Composes FilePicker, ProgramPreview, and JsonEditor, driven by the
 * useUpload state machine. Displays a confirmation message with the
 * program name and navigation to the Vault on success (201).
 *
 * Requirements: 7.3, 7.4, 7.5, 7.9
 */
export function UploadPage() {
  const { state, onFileSelected, onEditJson, onPreview, onSave, onReset } = useUpload();

  return (
    <main style={{ maxWidth: 900, margin: '0 auto', padding: '2rem 1rem', color: 'var(--color-text-primary)' }}>
      <h1 style={{ marginBottom: '1.5rem', color: 'var(--color-text-primary)' }}>Upload Program</h1>

      {/* Idle — show file picker */}
      {state.status === 'idle' && (
        <FilePicker onFileSelected={onFileSelected} />
      )}

      {/* File selected — brief loading state while reading */}
      {state.status === 'file_selected' && (
        <p aria-busy="true">Reading file…</p>
      )}

      {/* Previewing — show structured preview with actions */}
      {state.status === 'previewing' && (
        <>
          <FilePicker onFileSelected={onFileSelected} />
          <div style={{ marginTop: '1.5rem' }}>
            <ProgramPreview
              program={state.program}
              onSave={onSave}
              onEditJson={onEditJson}
              isUploading={false}
            />
          </div>
        </>
      )}

      {/* Editing — show JSON editor */}
      {state.status === 'editing' && (
        <>
          <FilePicker onFileSelected={onFileSelected} />
          <div style={{ marginTop: '1.5rem' }}>
            <JsonEditor
              initialJson={state.rawJson}
              onPreview={onPreview}
              parseError={state.parseError}
            />
          </div>
        </>
      )}

      {/* Uploading — show preview with disabled save and loading indicator */}
      {state.status === 'uploading' && (
        <>
          <div style={{ marginTop: '1.5rem' }}>
            <ProgramPreview
              program={JSON.parse(state.rawJson)}
              onSave={onSave}
              onEditJson={onEditJson}
              isUploading={true}
            />
          </div>
        </>
      )}

      {/* Success — confirmation with program name and Vault navigation */}
      {state.status === 'success' && (
        <div
          role="status"
          style={{
            background: 'var(--color-bg-card)',
            border: '1px solid var(--color-success)',
            borderRadius: 'var(--radius-sm)',
            padding: '1.5rem',
          }}
        >
          <h2 style={{ margin: '0 0 0.5rem', color: 'var(--color-success)' }}>Upload Successful</h2>
          <p style={{ margin: '0 0 1rem', color: 'var(--color-text-primary)' }}>
            <strong>{state.programName}</strong> has been saved to your Vault.
          </p>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <Link
              to={`/vault/programs/${state.programId}`}
              style={{
                padding: '0.6rem 1.25rem',
                fontWeight: 600,
                textDecoration: 'none',
                background: 'var(--color-success)',
                color: 'var(--color-bg-primary)',
                borderRadius: 'var(--radius-sm)',
                minHeight: 'var(--tap-target-min)',
                display: 'inline-flex',
                alignItems: 'center',
              }}
            >
              View in Vault
            </Link>
            <button
              type="button"
              onClick={onReset}
              style={{
                padding: '0.6rem 1.25rem',
                cursor: 'pointer',
                background: 'var(--color-bg-surface)',
                color: 'var(--color-text-primary)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-sm)',
                minHeight: 'var(--tap-target-min)',
              }}
            >
              Upload Another
            </button>
          </div>
        </div>
      )}

      {/* Error — display error details and allow retry */}
      {state.status === 'error' && (
        <div
          role="alert"
          style={{
            background: 'var(--color-bg-card)',
            border: '1px solid var(--color-error)',
            borderRadius: 'var(--radius-sm)',
            padding: '1.5rem',
          }}
        >
          <h2 style={{ margin: '0 0 0.5rem', color: 'var(--color-error)' }}>Upload Failed</h2>

          {typeof state.errors === 'string' ? (
            <p style={{ margin: '0 0 1rem', color: 'var(--color-text-primary)' }}>{state.errors}</p>
          ) : (
            <ul style={{ margin: '0 0 1rem', paddingLeft: '1.25rem', color: 'var(--color-text-primary)' }}>
              {(state.errors as FieldError[]).map((err, i) => (
                <li key={i} style={{ marginBottom: '0.25rem' }}>
                  <code style={{ color: 'var(--color-accent)' }}>{err.field}</code>: {err.message}
                </li>
              ))}
            </ul>
          )}

          <button
            type="button"
            onClick={onReset}
            style={{
              padding: '0.6rem 1.25rem',
              cursor: 'pointer',
              background: 'var(--color-bg-surface)',
              color: 'var(--color-text-primary)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-sm)',
              minHeight: 'var(--tap-target-min)',
            }}
          >
            Try Again
          </button>
        </div>
      )}
    </main>
  );
}
