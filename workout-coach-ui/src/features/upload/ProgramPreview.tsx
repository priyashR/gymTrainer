import { useState } from 'react';
import type { Block, Day, ParsedProgram, Week } from '../../types/upload';

interface ProgramPreviewProps {
  program: ParsedProgram;
  /** Called when the user clicks "Save to Vault". */
  onSave: () => void;
  /** Called when the user clicks "Edit JSON". */
  onEditJson: () => void;
  /** True while an upload request is in flight — disables Save and shows a spinner. */
  isUploading: boolean;
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

function MovementRow({ movement }: { movement: Block['movements'][number] }) {
  return (
    <tr>
      <td style={{ padding: '0.25rem 0.5rem' }}>{movement.exercise_name}</td>
      <td style={{ padding: '0.25rem 0.5rem', textAlign: 'center' }}>{movement.prescribed_sets}</td>
      <td style={{ padding: '0.25rem 0.5rem', textAlign: 'center' }}>{movement.prescribed_reps}</td>
      <td style={{ padding: '0.25rem 0.5rem', textAlign: 'center' }}>
        {movement.prescribed_weight ?? '—'}
      </td>
      {movement.modality_type && (
        <td style={{ padding: '0.25rem 0.5rem', color: '#555', fontSize: '0.8rem' }}>
          {movement.modality_type}
        </td>
      )}
    </tr>
  );
}

function BlockSection({ block }: { block: Block }) {
  const [open, setOpen] = useState(true);

  return (
    <div style={{ marginBottom: '0.75rem', paddingLeft: '1rem', borderLeft: '2px solid #ddd' }}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          fontWeight: 600,
          fontSize: '0.9rem',
          padding: '0.25rem 0',
          textAlign: 'left',
        }}
      >
        {open ? '▾' : '▸'} {block.block_type} — {block.format}
        {block.time_cap_minutes != null && ` (${block.time_cap_minutes} min cap)`}
      </button>

      {open && (
        <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '0.25rem', fontSize: '0.875rem' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #eee' }}>
              <th style={{ padding: '0.25rem 0.5rem', textAlign: 'left' }}>Exercise</th>
              <th style={{ padding: '0.25rem 0.5rem' }}>Sets</th>
              <th style={{ padding: '0.25rem 0.5rem' }}>Reps</th>
              <th style={{ padding: '0.25rem 0.5rem' }}>Weight</th>
            </tr>
          </thead>
          <tbody>
            {block.movements.map((m, i) => (
              <MovementRow key={i} movement={m} />
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function DaySection({ day }: { day: Day }) {
  const [open, setOpen] = useState(true);

  return (
    <div style={{ marginBottom: '0.75rem', paddingLeft: '1rem', borderLeft: '3px solid #bbb' }}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          fontWeight: 600,
          padding: '0.25rem 0',
          textAlign: 'left',
        }}
      >
        {open ? '▾' : '▸'} Day {day.day_number}: {day.day_label} — {day.focus_area}
        <span style={{ fontWeight: 400, color: '#666', marginLeft: '0.5rem', fontSize: '0.85rem' }}>
          ({day.modality})
        </span>
      </button>

      {open && (
        <div style={{ marginTop: '0.5rem' }}>
          {day.blocks.map((block, i) => (
            <BlockSection key={i} block={block} />
          ))}
        </div>
      )}
    </div>
  );
}

function WeekSection({ week }: { week: Week }) {
  const [open, setOpen] = useState(true);

  return (
    <div style={{ marginBottom: '1rem', border: '1px solid #ddd', borderRadius: 6, padding: '0.75rem' }}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          fontWeight: 700,
          fontSize: '1rem',
          padding: 0,
          textAlign: 'left',
          width: '100%',
        }}
      >
        {open ? '▾' : '▸'} Week {week.week_number}
      </button>

      {open && (
        <div style={{ marginTop: '0.5rem' }}>
          {week.days.map((day, i) => (
            <DaySection key={i} day={day} />
          ))}
        </div>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Main component
// ---------------------------------------------------------------------------

/**
 * Displays a structured preview of a parsed program before it is saved.
 *
 * Shows program metadata (name, goal, duration, equipment) and a collapsible
 * breakdown of each week → day → block → movement.
 *
 * Requirements: 7.3, 7.4, 7.8
 */
export function ProgramPreview({ program, onSave, onEditJson, isUploading }: ProgramPreviewProps) {
  const { program_metadata, program_structure } = program;

  return (
    <section aria-label="Program preview">
      {/* Metadata summary — Requirement 7.3 */}
      <div
        style={{
          background: '#f9f9f9',
          border: '1px solid #e0e0e0',
          borderRadius: 8,
          padding: '1rem',
          marginBottom: '1.25rem',
        }}
      >
        <h2 style={{ margin: '0 0 0.5rem' }}>{program_metadata.program_name}</h2>
        <dl style={{ display: 'grid', gridTemplateColumns: 'auto 1fr', gap: '0.25rem 1rem', margin: 0 }}>
          <dt style={{ fontWeight: 600 }}>Goal</dt>
          <dd style={{ margin: 0 }}>{program_metadata.goal}</dd>

          <dt style={{ fontWeight: 600 }}>Duration</dt>
          <dd style={{ margin: 0 }}>
            {program_metadata.duration_weeks} {program_metadata.duration_weeks === 1 ? 'week' : 'weeks'}
          </dd>

          <dt style={{ fontWeight: 600 }}>Equipment</dt>
          <dd style={{ margin: 0 }}>{program_metadata.equipment_profile.join(', ')}</dd>
        </dl>
      </div>

      {/* Collapsible week/day/block breakdown — Requirement 7.3 */}
      <div style={{ marginBottom: '1.25rem' }}>
        {program_structure.map((week, i) => (
          <WeekSection key={i} week={week} />
        ))}
      </div>

      {/* Action buttons — Requirement 7.4 */}
      <div style={{ display: 'flex', gap: '0.75rem' }}>
        <button
          type="button"
          onClick={onSave}
          disabled={isUploading}
          aria-busy={isUploading}
          aria-label={isUploading ? 'Saving to Vault…' : 'Save to Vault'}
          style={{
            padding: '0.6rem 1.25rem',
            fontWeight: 600,
            cursor: isUploading ? 'not-allowed' : 'pointer',
            opacity: isUploading ? 0.7 : 1,
          }}
        >
          {/* Loading indicator while upload is in progress — Requirement 7.8 */}
          {isUploading ? 'Saving…' : 'Save to Vault'}
        </button>

        <button
          type="button"
          onClick={onEditJson}
          disabled={isUploading}
          style={{
            padding: '0.6rem 1.25rem',
            cursor: isUploading ? 'not-allowed' : 'pointer',
            opacity: isUploading ? 0.7 : 1,
          }}
        >
          Edit JSON
        </button>
      </div>
    </section>
  );
}
