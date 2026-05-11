import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useProgram } from './useProgram';
import { ProgramJsonEditor } from './ProgramJsonEditor';
import type { VaultDay, VaultProgramDetail, VaultSection, VaultWeek } from '../../types/vault';

/**
 * Program detail page displaying metadata, collapsible week/day breakdown,
 * and actions (delete, edit JSON, copy).
 *
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.9, 9.10, 9.11
 */
export function ProgramDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { state, program, onUpdate, onDelete, onCopy } = useProgram(id!);
  const [showEditor, setShowEditor] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [expandedWeeks, setExpandedWeeks] = useState<Set<number>>(new Set());
  const [expandedDays, setExpandedDays] = useState<Set<string>>(new Set());

  // -------------------------------------------------------------------------
  // Loading state
  // -------------------------------------------------------------------------
  if (state.status === 'loading' || state.status === 'updating' || state.status === 'deleting' || state.status === 'copying') {
    const message =
      state.status === 'updating' ? 'Saving changes…' :
      state.status === 'deleting' ? 'Deleting…' :
      state.status === 'copying' ? 'Copying…' : 'Loading…';
    return (
      <main style={{ maxWidth: 900, margin: '0 auto', padding: '2rem 1rem' }}>
        <p aria-busy="true">{message}</p>
      </main>
    );
  }

  // -------------------------------------------------------------------------
  // 403 Forbidden
  // -------------------------------------------------------------------------
  if (state.status === 'forbidden') {
    return (
      <main style={{ maxWidth: 900, margin: '0 auto', padding: '2rem 1rem' }}>
        <div role="alert" style={{ color: '#c62828' }}>
          <p>Program not found or access denied.</p>
          <Link to="/vault/search">← Back to search</Link>
        </div>
      </main>
    );
  }

  // -------------------------------------------------------------------------
  // Error state
  // -------------------------------------------------------------------------
  if (state.status === 'error') {
    return (
      <main style={{ maxWidth: 900, margin: '0 auto', padding: '2rem 1rem' }}>
        <div role="alert" style={{ color: '#c62828' }}>
          <p>{state.message}</p>
          <Link to="/vault/search">← Back to search</Link>
        </div>
      </main>
    );
  }

  // -------------------------------------------------------------------------
  // Loaded — render program detail
  // -------------------------------------------------------------------------
  const prog: VaultProgramDetail = program!;

  const toggleWeek = (weekNum: number) => {
    setExpandedWeeks((prev) => {
      const next = new Set(prev);
      if (next.has(weekNum)) next.delete(weekNum);
      else next.add(weekNum);
      return next;
    });
  };

  const toggleDay = (key: string) => {
    setExpandedDays((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const handleDelete = async () => {
    setConfirmDelete(false);
    await onDelete();
  };

  const handleSave = async (json: string) => {
    const result = await onUpdate(json);
    if (result.success) {
      setShowEditor(false);
    }
    return result;
  };

  return (
    <main style={{ maxWidth: 900, margin: '0 auto', padding: '2rem 1rem' }}>
      {/* Header */}
      <header style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
        <Link to="/vault/search" style={{ fontSize: '0.9rem' }}>← Back to search</Link>
      </header>

      {/* Program metadata */}
      <section style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ margin: '0 0 0.5rem' }}>{prog.name}</h1>
        {prog.goal && <p style={{ margin: '0 0 0.75rem', color: '#444' }}>{prog.goal}</p>}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1.5rem', fontSize: '0.9rem', color: '#555' }}>
          <span><strong>Duration:</strong> {prog.durationWeeks} {prog.durationWeeks === 1 ? 'week' : 'weeks'}</span>
          <span><strong>Equipment:</strong> {prog.equipmentProfile.length > 0 ? prog.equipmentProfile.join(', ') : 'None'}</span>
          <span><strong>Source:</strong> {formatSource(prog.contentSource)}</span>
        </div>
      </section>

      {/* Action buttons */}
      <section style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        <button
          type="button"
          onClick={() => setShowEditor(!showEditor)}
          style={{ padding: '0.5rem 1rem', cursor: 'pointer', borderRadius: 4 }}
        >
          {showEditor ? 'Close Editor' : 'Edit JSON'}
        </button>
        <button
          type="button"
          onClick={() => onCopy()}
          style={{ padding: '0.5rem 1rem', cursor: 'pointer', borderRadius: 4 }}
        >
          Copy
        </button>
        <button
          type="button"
          onClick={() => setConfirmDelete(true)}
          style={{ padding: '0.5rem 1rem', cursor: 'pointer', borderRadius: 4, color: '#c62828', borderColor: '#c62828' }}
        >
          Delete
        </button>
      </section>

      {/* Delete confirmation dialog */}
      {confirmDelete && (
        <div
          role="dialog"
          aria-label="Confirm deletion"
          style={{
            padding: '1rem',
            marginBottom: '1rem',
            background: '#fbe9e7',
            border: '1px solid #ef9a9a',
            borderRadius: 8,
          }}
        >
          <p style={{ margin: '0 0 0.75rem' }}>
            Are you sure you want to delete <strong>{prog.name}</strong>? This cannot be undone.
          </p>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button
              type="button"
              onClick={handleDelete}
              style={{ padding: '0.5rem 1rem', cursor: 'pointer', background: '#c62828', color: '#fff', border: 'none', borderRadius: 4 }}
            >
              Yes, Delete
            </button>
            <button
              type="button"
              onClick={() => setConfirmDelete(false)}
              style={{ padding: '0.5rem 1rem', cursor: 'pointer', borderRadius: 4 }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* JSON Editor */}
      {showEditor && (
        <ProgramJsonEditor
          initialJson={JSON.stringify(programToUploadSchema(prog), null, 2)}
          onSave={handleSave}
          onCancel={() => setShowEditor(false)}
        />
      )}

      {/* Week breakdown */}
      <section>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>Program Structure</h2>
        {prog.weeks.map((week) => (
          <WeekSection
            key={week.weekNumber}
            week={week}
            expanded={expandedWeeks.has(week.weekNumber)}
            onToggle={() => toggleWeek(week.weekNumber)}
            expandedDays={expandedDays}
            onToggleDay={toggleDay}
          />
        ))}
      </section>
    </main>
  );
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

function WeekSection({
  week,
  expanded,
  onToggle,
  expandedDays,
  onToggleDay,
}: {
  week: VaultWeek;
  expanded: boolean;
  onToggle: () => void;
  expandedDays: Set<string>;
  onToggleDay: (key: string) => void;
}) {
  return (
    <div style={{ marginBottom: '0.5rem' }}>
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={expanded}
        style={{
          width: '100%',
          textAlign: 'left',
          padding: '0.75rem 1rem',
          fontSize: '1rem',
          fontWeight: 600,
          background: '#f5f5f5',
          border: '1px solid #e0e0e0',
          borderRadius: 4,
          cursor: 'pointer',
        }}
      >
        Week {week.weekNumber} {expanded ? '▲' : '▼'}
      </button>
      {expanded && (
        <div style={{ paddingLeft: '1rem', marginTop: '0.5rem' }}>
          {week.days.map((day) => {
            const dayKey = `${week.weekNumber}-${day.dayNumber}`;
            return (
              <DaySection
                key={dayKey}
                day={day}
                expanded={expandedDays.has(dayKey)}
                onToggle={() => onToggleDay(dayKey)}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}

function DaySection({
  day,
  expanded,
  onToggle,
}: {
  day: VaultDay;
  expanded: boolean;
  onToggle: () => void;
}) {
  return (
    <div style={{ marginBottom: '0.5rem' }}>
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={expanded}
        style={{
          width: '100%',
          textAlign: 'left',
          padding: '0.5rem 0.75rem',
          fontSize: '0.95rem',
          background: '#fafafa',
          border: '1px solid #eee',
          borderRadius: 4,
          cursor: 'pointer',
        }}
      >
        Day {day.dayNumber}: {day.label} {expanded ? '▲' : '▼'}
      </button>
      {expanded && (
        <div style={{ paddingLeft: '1rem', marginTop: '0.5rem', fontSize: '0.9rem' }}>
          <p style={{ margin: '0 0 0.25rem' }}><strong>Focus Area:</strong> {day.focusArea}</p>
          <p style={{ margin: '0 0 0.5rem' }}><strong>Modality:</strong> {day.modality}</p>

          {/* Warm-up */}
          {day.warmUp.length > 0 && (
            <div style={{ marginBottom: '0.5rem' }}>
              <strong>Warm-Up:</strong>
              <ul style={{ margin: '0.25rem 0', paddingLeft: '1.25rem' }}>
                {day.warmUp.map((entry, i) => (
                  <li key={i}>{entry.movement} — {entry.instruction}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Sections */}
          {day.sections.map((section, i) => (
            <SectionBlock key={i} section={section} />
          ))}

          {/* Cool-down */}
          {day.coolDown.length > 0 && (
            <div style={{ marginTop: '0.5rem' }}>
              <strong>Cool-Down:</strong>
              <ul style={{ margin: '0.25rem 0', paddingLeft: '1.25rem' }}>
                {day.coolDown.map((entry, i) => (
                  <li key={i}>{entry.movement} — {entry.instruction}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function SectionBlock({ section }: { section: VaultSection }) {
  return (
    <div style={{ marginBottom: '0.75rem', paddingLeft: '0.5rem', borderLeft: '3px solid #e0e0e0' }}>
      <p style={{ margin: '0 0 0.25rem', fontWeight: 600 }}>
        {section.name}
        {section.format && <span style={{ fontWeight: 400, color: '#666' }}> ({section.format})</span>}
        {section.timeCap && <span style={{ fontWeight: 400, color: '#666' }}> — {section.timeCap} min cap</span>}
      </p>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid #ddd', textAlign: 'left' }}>
            <th style={{ padding: '0.25rem 0.5rem' }}>Exercise</th>
            <th style={{ padding: '0.25rem 0.5rem' }}>Sets</th>
            <th style={{ padding: '0.25rem 0.5rem' }}>Reps</th>
            <th style={{ padding: '0.25rem 0.5rem' }}>Weight</th>
            <th style={{ padding: '0.25rem 0.5rem' }}>Rest</th>
            <th style={{ padding: '0.25rem 0.5rem' }}>Notes</th>
          </tr>
        </thead>
        <tbody>
          {section.exercises.map((ex, i) => (
            <tr key={i} style={{ borderBottom: '1px solid #f0f0f0' }}>
              <td style={{ padding: '0.25rem 0.5rem' }}>{ex.name}</td>
              <td style={{ padding: '0.25rem 0.5rem' }}>{ex.sets}</td>
              <td style={{ padding: '0.25rem 0.5rem' }}>{ex.reps}</td>
              <td style={{ padding: '0.25rem 0.5rem' }}>{ex.weight ?? '—'}</td>
              <td style={{ padding: '0.25rem 0.5rem' }}>{ex.restSeconds ? `${ex.restSeconds}s` : '—'}</td>
              <td style={{ padding: '0.25rem 0.5rem' }}>{ex.notes ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatSource(source: string): string {
  switch (source) {
    case 'AI_GENERATED': return 'AI Generated';
    case 'UPLOADED': return 'Uploaded';
    case 'MANUAL': return 'Manual';
    default: return source;
  }
}

/**
 * Converts a VaultProgramDetail back to the Upload_Schema format for editing.
 */
function programToUploadSchema(prog: VaultProgramDetail) {
  return {
    name: prog.name,
    goal: prog.goal,
    durationWeeks: prog.durationWeeks,
    equipmentProfile: prog.equipmentProfile,
    weeks: prog.weeks.map((week) => ({
      weekNumber: week.weekNumber,
      days: week.days.map((day) => ({
        dayNumber: day.dayNumber,
        label: day.label,
        focusArea: day.focusArea,
        modality: day.modality,
        methodologySource: day.methodologySource ?? undefined,
        warmUp: day.warmUp,
        sections: day.sections.map((section) => ({
          name: section.name,
          sectionType: section.sectionType,
          format: section.format ?? undefined,
          timeCap: section.timeCap ?? undefined,
          exercises: section.exercises.map((ex) => ({
            name: ex.name,
            modalityType: ex.modalityType ?? undefined,
            sets: ex.sets,
            reps: ex.reps,
            weight: ex.weight ?? undefined,
            restSeconds: ex.restSeconds ?? undefined,
            notes: ex.notes ?? undefined,
          })),
        })),
        coolDown: day.coolDown,
      })),
    })),
  };
}
