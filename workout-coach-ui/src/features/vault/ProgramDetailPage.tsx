import { useCallback, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useProgram } from './useProgram';
import { ProgramJsonEditor } from './ProgramJsonEditor';
import { enrollProgram, getActiveEnrollment, startSession } from '../../lib/sessionApi';
import type {
  DayAssignmentDetail,
  SnapshotSection,
  VaultDay,
  VaultProgramDetail,
  VaultSection,
  VaultWeek,
} from '../../types/vault';

/**
 * Program detail page displaying metadata, collapsible week/day breakdown,
 * and actions (delete, edit JSON, copy).
 *
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.9, 9.10, 9.11
 */
export function ProgramDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { state, program, onUpdate, onDelete, onCopy } = useProgram(id!);
  const [showEditor, setShowEditor] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [expandedWeeks, setExpandedWeeks] = useState<Set<number>>(new Set());
  const [expandedDays, setExpandedDays] = useState<Set<string>>(new Set());

  // Vault-initiated workout action state
  const [showProgramActions, setShowProgramActions] = useState(false);
  const [confirmReplace, setConfirmReplace] = useState(false);
  const [startingWorkout, setStartingWorkout] = useState(false);
  const [workoutError, setWorkoutError] = useState<string | null>(null);

  // Start a standalone session for a specific day
  const handleStartStandalone = useCallback(async (weekNumber: number, dayNumber: number) => {
    if (!id) return;
    setStartingWorkout(true);
    setWorkoutError(null);
    try {
      const session = await startSession({
        programId: id,
        weekNumber,
        dayNumber,
        standalone: true,
      });
      navigate(`/workout/session/${session.id}`);
    } catch {
      setWorkoutError("Failed to start standalone session. Please try again.");
      setStartingWorkout(false);
    }
  }, [id, navigate]);

  // Start a new program enrollment (replaces active if exists)
  const handleStartNewProgram = useCallback(async () => {
    if (!id || !program) return;
    setStartingWorkout(true);
    setWorkoutError(null);
    setConfirmReplace(false);
    setShowProgramActions(false);
    try {
      // For manual programs (durationWeeks = 0), treat as 1 week with N days
      const isManual = program.contentSource === 'MANUAL';
      const totalWeeks = isManual ? 1 : program.durationWeeks;
      const totalDaysPerWeek = isManual
        ? Math.max(program.dayAssignments?.length ?? 1, 1)
        : Math.max(...(program.weeks?.map(w => w.days?.length ?? 0) ?? [1]), 1);

      await enrollProgram({
        programId: id,
        programName: program.name,
        totalWeeks,
        totalDaysPerWeek,
      });
      // Start the first day of the program
      const session = await startSession({
        programId: id,
        weekNumber: 1,
        dayNumber: 1,
        standalone: false,
      });
      navigate(`/workout/session/${session.id}`);
    } catch {
      setWorkoutError("Failed to start program. Please try again.");
      setStartingWorkout(false);
    }
  }, [id, program, navigate]);

  // Check if there's an active enrollment before starting a new program
  const handleStartNewProgramClick = useCallback(async () => {
    setWorkoutError(null);
    try {
      const activeEnrollment = await getActiveEnrollment();
      if (activeEnrollment) {
        // Show confirmation prompt
        setConfirmReplace(true);
      } else {
        // No active program — start directly
        await handleStartNewProgram();
      }
    } catch {
      // If we can't check, proceed with start (server will handle replacement)
      await handleStartNewProgram();
    }
  }, [handleStartNewProgram]);

  // -------------------------------------------------------------------------
  // Loading state
  // -------------------------------------------------------------------------
  if (state.status === 'loading' || state.status === 'updating' || state.status === 'deleting' || state.status === 'copying') {
    const message =
      state.status === 'updating' ? 'Saving changes…' :
      state.status === 'deleting' ? 'Deleting…' :
      state.status === 'copying' ? 'Copying…' : 'Loading…';
    return (
      <main style={{ maxWidth: 900, margin: '0 auto', padding: '2rem 1rem', color: 'var(--color-text-primary)' }}>
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
        <div role="alert" style={{ color: 'var(--color-error)' }}>
          <p>Program not found or access denied.</p>
          <Link to="/vault/search" style={{ color: 'var(--color-accent)' }}>← Back to search</Link>
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
        <div role="alert" style={{ color: 'var(--color-error)' }}>
          <p>{state.message}</p>
          <Link to="/vault/search" style={{ color: 'var(--color-accent)' }}>← Back to search</Link>
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
    <main style={{ maxWidth: 900, margin: '0 auto', padding: '2rem 1rem', color: 'var(--color-text-primary)' }}>
      {/* Header */}
      <header style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
        <Link to="/vault/search" style={{ fontSize: '0.9rem', color: 'var(--color-accent)' }}>← Back to search</Link>
      </header>

      {/* Program metadata */}
      <section style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ margin: '0 0 0.5rem', color: 'var(--color-text-primary)' }}>{prog.name}</h1>
        {prog.goal && <p style={{ margin: '0 0 0.75rem', color: 'var(--color-text-secondary)' }}>{prog.goal}</p>}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1.5rem', fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
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
          style={{
            padding: '0.5rem 1rem',
            cursor: 'pointer',
            borderRadius: 'var(--radius-sm)',
            background: 'var(--color-bg-card)',
            color: 'var(--color-text-primary)',
            border: '1px solid var(--color-border)',
            minHeight: 'var(--tap-target-min)',
          }}
        >
          {showEditor ? 'Close Editor' : 'Edit JSON'}
        </button>
        <button
          type="button"
          onClick={() => onCopy()}
          style={{
            padding: '0.5rem 1rem',
            cursor: 'pointer',
            borderRadius: 'var(--radius-sm)',
            background: 'var(--color-bg-card)',
            color: 'var(--color-text-primary)',
            border: '1px solid var(--color-border)',
            minHeight: 'var(--tap-target-min)',
          }}
        >
          Copy
        </button>
        <button
          type="button"
          onClick={() => setConfirmDelete(true)}
          style={{
            padding: '0.5rem 1rem',
            cursor: 'pointer',
            borderRadius: 'var(--radius-sm)',
            background: 'var(--color-bg-card)',
            color: 'var(--color-error)',
            border: '1px solid var(--color-error)',
            minHeight: 'var(--tap-target-min)',
          }}
        >
          Delete
        </button>
      </section>

      {/* Workout actions */}
      <section style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        <button
          type="button"
          onClick={() => setShowProgramActions(!showProgramActions)}
          style={{
            padding: '0.5rem 1rem',
            cursor: 'pointer',
            borderRadius: 'var(--radius-sm)',
            background: 'var(--color-accent)',
            color: 'var(--color-bg-primary)',
            border: 'none',
            fontWeight: 600,
            minHeight: 'var(--tap-target-min)',
          }}
        >
          Start Workout {showProgramActions ? '▲' : '▼'}
        </button>
      </section>

      {showProgramActions && (
        <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem', flexWrap: 'wrap', paddingLeft: '1rem' }}>
          <button
            type="button"
            onClick={handleStartNewProgramClick}
            disabled={startingWorkout}
            style={{
              padding: '0.5rem 1rem',
              cursor: startingWorkout ? 'default' : 'pointer',
              borderRadius: 'var(--radius-sm)',
              background: 'var(--color-success)',
              color: 'var(--color-bg-primary)',
              border: 'none',
              minHeight: 'var(--tap-target-min)',
              opacity: startingWorkout ? 0.7 : 1,
            }}
          >
            {startingWorkout ? 'Starting…' : 'Start New Program'}
          </button>
        </div>
      )}

      {/* Workout error message */}
      {workoutError && (
        <div role="alert" style={{
          padding: '0.75rem 1rem',
          marginBottom: '1rem',
          background: 'var(--color-bg-card)',
          border: '1px solid var(--color-error)',
          borderRadius: 'var(--radius-sm)',
          color: 'var(--color-error)',
        }}>
          {workoutError}
        </div>
      )}

      {/* Confirm replace active program dialog */}
      {confirmReplace && (
        <div
          role="dialog"
          aria-label="Confirm program replacement"
          style={{
            padding: '1rem',
            marginBottom: '1rem',
            background: 'var(--color-bg-card)',
            border: '1px solid var(--color-warning)',
            borderRadius: 'var(--radius-sm)',
            color: 'var(--color-text-primary)',
          }}
        >
          <p style={{ margin: '0 0 0.75rem' }}>
            You have an active program. Starting <strong>{prog.name}</strong> will end your current program and replace it. Continue?
          </p>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button
              type="button"
              onClick={handleStartNewProgram}
              disabled={startingWorkout}
              style={{
                padding: '0.5rem 1rem',
                cursor: startingWorkout ? 'default' : 'pointer',
                background: 'var(--color-warning)',
                color: 'var(--color-bg-primary)',
                border: 'none',
                borderRadius: 'var(--radius-sm)',
                minHeight: 'var(--tap-target-min)',
              }}
            >
              {startingWorkout ? 'Starting…' : 'Yes, Replace Program'}
            </button>
            <button
              type="button"
              onClick={() => setConfirmReplace(false)}
              style={{
                padding: '0.5rem 1rem',
                cursor: 'pointer',
                borderRadius: 'var(--radius-sm)',
                background: 'var(--color-bg-surface)',
                color: 'var(--color-text-primary)',
                border: '1px solid var(--color-border)',
                minHeight: 'var(--tap-target-min)',
              }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Delete confirmation dialog */}
      {confirmDelete && (
        <div
          role="dialog"
          aria-label="Confirm deletion"
          style={{
            padding: '1rem',
            marginBottom: '1rem',
            background: 'var(--color-bg-card)',
            border: '1px solid var(--color-error)',
            borderRadius: 'var(--radius-sm)',
            color: 'var(--color-text-primary)',
          }}
        >
          <p style={{ margin: '0 0 0.75rem' }}>
            Are you sure you want to delete <strong>{prog.name}</strong>? This cannot be undone.
          </p>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button
              type="button"
              onClick={handleDelete}
              style={{
                padding: '0.5rem 1rem',
                cursor: 'pointer',
                background: 'var(--color-error)',
                color: 'var(--color-text-primary)',
                border: 'none',
                borderRadius: 'var(--radius-sm)',
                minHeight: 'var(--tap-target-min)',
              }}
            >
              Yes, Delete
            </button>
            <button
              type="button"
              onClick={() => setConfirmDelete(false)}
              style={{
                padding: '0.5rem 1rem',
                cursor: 'pointer',
                borderRadius: 'var(--radius-sm)',
                background: 'var(--color-bg-surface)',
                color: 'var(--color-text-primary)',
                border: '1px solid var(--color-border)',
                minHeight: 'var(--tap-target-min)',
              }}
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
        <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem', color: 'var(--color-text-primary)' }}>Program Structure</h2>
        {prog.weeks.map((week) => (
          <WeekSection
            key={week.weekNumber}
            week={week}
            expanded={expandedWeeks.has(week.weekNumber)}
            onToggle={() => toggleWeek(week.weekNumber)}
            expandedDays={expandedDays}
            onToggleDay={toggleDay}
            onStartStandalone={handleStartStandalone}
            startingWorkout={startingWorkout}
          />
        ))}
      </section>

      {/* Day assignments for manual programs with copied days */}
      {prog.dayAssignments && prog.dayAssignments.length > 0 && (
        <section style={{ marginTop: '1.5rem' }}>
          <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem', color: 'var(--color-text-primary)' }}>Day Assignments</h2>
          {prog.dayAssignments.map((assignment) => (
            <DayAssignmentBlock key={assignment.dayNumber} assignment={assignment} />
          ))}
        </section>
      )}
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
  onStartStandalone,
  startingWorkout,
}: {
  week: VaultWeek;
  expanded: boolean;
  onToggle: () => void;
  expandedDays: Set<string>;
  onToggleDay: (key: string) => void;
  onStartStandalone: (weekNumber: number, dayNumber: number) => void;
  startingWorkout: boolean;
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
          background: 'var(--color-bg-card)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-sm)',
          cursor: 'pointer',
          color: 'var(--color-text-primary)',
          minHeight: 'var(--tap-target-min)',
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
                weekNumber={week.weekNumber}
                expanded={expandedDays.has(dayKey)}
                onToggle={() => onToggleDay(dayKey)}
                onStartStandalone={onStartStandalone}
                startingWorkout={startingWorkout}
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
  weekNumber,
  expanded,
  onToggle,
  onStartStandalone,
  startingWorkout,
}: {
  day: VaultDay;
  weekNumber: number;
  expanded: boolean;
  onToggle: () => void;
  onStartStandalone: (weekNumber: number, dayNumber: number) => void;
  startingWorkout: boolean;
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
          background: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-sm)',
          cursor: 'pointer',
          color: 'var(--color-text-primary)',
          minHeight: 'var(--tap-target-min)',
        }}
      >
        Day {day.dayNumber}: {day.label} {expanded ? '▲' : '▼'}
      </button>
      {expanded && (
        <div style={{ paddingLeft: '1rem', marginTop: '0.5rem', fontSize: '0.9rem', color: 'var(--color-text-primary)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <div>
              <p style={{ margin: '0 0 0.25rem' }}><strong>Focus Area:</strong> {day.focusArea}</p>
              <p style={{ margin: '0' }}><strong>Modality:</strong> {day.modality}</p>
            </div>
            <button
              type="button"
              onClick={() => onStartStandalone(weekNumber, day.dayNumber)}
              disabled={startingWorkout}
              style={{
                padding: '0.4rem 0.75rem',
                background: 'var(--color-accent)',
                color: 'var(--color-bg-primary)',
                border: 'none',
                borderRadius: 'var(--radius-sm)',
                cursor: startingWorkout ? 'default' : 'pointer',
                fontSize: '0.8rem',
                fontWeight: 600,
                opacity: startingWorkout ? 0.7 : 1,
                whiteSpace: 'nowrap',
                minHeight: 'var(--tap-target-min)',
              }}
            >
              Start Standalone
            </button>
          </div>

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
    <div style={{ marginBottom: '0.75rem', paddingLeft: '0.5rem', borderLeft: '3px solid var(--color-border)' }}>
      <p style={{ margin: '0 0 0.25rem', fontWeight: 600, color: 'var(--color-text-primary)' }}>
        {section.name}
        {section.format && <span style={{ fontWeight: 400, color: 'var(--color-text-secondary)' }}> ({section.format})</span>}
        {section.timeCap && <span style={{ fontWeight: 400, color: 'var(--color-text-secondary)' }}> — {section.timeCap} min cap</span>}
      </p>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--color-border)', textAlign: 'left' }}>
            <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Exercise</th>
            <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Sets</th>
            <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Reps</th>
            <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Weight</th>
            <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Rest</th>
            <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Notes</th>
          </tr>
        </thead>
        <tbody>
          {section.exercises.map((ex, i) => (
            <tr key={i} style={{ borderBottom: '1px solid var(--color-border)' }}>
              <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.name}</td>
              <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.sets}</td>
              <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.reps}</td>
              <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.weight ?? '—'}</td>
              <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.restSeconds ? `${ex.restSeconds}s` : '—'}</td>
              <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.notes ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function DayAssignmentBlock({ assignment }: { assignment: DayAssignmentDetail }) {
  const [expanded, setExpanded] = useState(false);

  if (assignment.type === 'copied_day') {
    return (
      <CopiedDayBlock
        assignment={assignment}
        expanded={expanded}
        onToggle={() => setExpanded(!expanded)}
      />
    );
  }

  // Activity-type assignment — simple display
  if (assignment.type === 'activity') {
    return (
      <div style={{ marginBottom: '0.5rem' }}>
        <div
          style={{
            padding: '0.75rem 1rem',
            background: 'var(--color-bg-surface)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-sm)',
            color: 'var(--color-text-primary)',
          }}
        >
          <strong>Day {assignment.dayNumber}:</strong> 🏃 {assignment.activityType ?? 'Activity'}
        </div>
      </div>
    );
  }

  return null;
}

function CopiedDayBlock({
  assignment,
  expanded,
  onToggle,
}: {
  assignment: DayAssignmentDetail;
  expanded: boolean;
  onToggle: () => void;
}) {
  const snapshot = assignment.snapshotData;
  const provenanceText = getProvenanceText(assignment);

  return (
    <div style={{ marginBottom: '0.5rem' }} data-testid={`copied-day-${assignment.dayNumber}`}>
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={expanded}
        style={{
          width: '100%',
          textAlign: 'left',
          padding: '0.75rem 1rem',
          fontSize: '0.95rem',
          background: 'var(--color-bg-surface)',
          border: '1px solid var(--color-accent, #3b82f6)',
          borderLeft: '3px solid var(--color-accent, #3b82f6)',
          borderRadius: 'var(--radius-sm)',
          cursor: 'pointer',
          color: 'var(--color-text-primary)',
          minHeight: 'var(--tap-target-min)',
        }}
      >
        <span style={{ fontWeight: 600 }}>Day {assignment.dayNumber}:</span>{' '}
        📋 {snapshot?.label ?? `Copied Day`}
        {snapshot?.focusArea && (
          <span style={{ color: 'var(--color-text-secondary)', marginLeft: '0.5rem' }}>
            ({snapshot.focusArea})
          </span>
        )}
        <span style={{ float: 'right' }}>{expanded ? '▲' : '▼'}</span>
      </button>

      {expanded && (
        <div style={{ paddingLeft: '1rem', marginTop: '0.5rem', fontSize: '0.9rem', color: 'var(--color-text-primary)' }}>
          {/* Provenance */}
          <p
            style={{
              margin: '0 0 0.75rem',
              fontSize: '0.85rem',
              color: 'var(--color-text-secondary)',
              fontStyle: 'italic',
            }}
            data-testid={`copied-day-${assignment.dayNumber}-provenance`}
          >
            {provenanceText}
          </p>

          {/* Day metadata */}
          {snapshot && (
            <>
              <div style={{ marginBottom: '0.5rem' }}>
                {snapshot.focusArea && <p style={{ margin: '0 0 0.25rem' }}><strong>Focus Area:</strong> {snapshot.focusArea}</p>}
                {snapshot.modality && <p style={{ margin: '0' }}><strong>Modality:</strong> {snapshot.modality}</p>}
              </div>

              {/* Warm-up */}
              {snapshot.warmUp && snapshot.warmUp.length > 0 && (
                <div style={{ marginBottom: '0.5rem' }}>
                  <strong>Warm-Up:</strong>
                  <ul style={{ margin: '0.25rem 0', paddingLeft: '1.25rem' }}>
                    {snapshot.warmUp.map((entry, i) => (
                      <li key={i}>{entry.movement} — {entry.instruction}</li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Sections */}
              {snapshot.sections && snapshot.sections.map((section, i) => (
                <SnapshotSectionBlock key={i} section={section} />
              ))}

              {/* Cool-down */}
              {snapshot.coolDown && snapshot.coolDown.length > 0 && (
                <div style={{ marginTop: '0.5rem' }}>
                  <strong>Cool-Down:</strong>
                  <ul style={{ margin: '0.25rem 0', paddingLeft: '1.25rem' }}>
                    {snapshot.coolDown.map((entry, i) => (
                      <li key={i}>{entry.movement} — {entry.instruction}</li>
                    ))}
                  </ul>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}

function SnapshotSectionBlock({ section }: { section: SnapshotSection }) {
  return (
    <div style={{ marginBottom: '0.75rem', paddingLeft: '0.5rem', borderLeft: '3px solid var(--color-border)' }}>
      <p style={{ margin: '0 0 0.25rem', fontWeight: 600, color: 'var(--color-text-primary)' }}>
        {section.name ?? 'Section'}
        {section.format && <span style={{ fontWeight: 400, color: 'var(--color-text-secondary)' }}> ({section.format})</span>}
        {section.timeCap && <span style={{ fontWeight: 400, color: 'var(--color-text-secondary)' }}> — {section.timeCap} min cap</span>}
      </p>
      {section.exercises && section.exercises.length > 0 && (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--color-border)', textAlign: 'left' }}>
              <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Exercise</th>
              <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Sets</th>
              <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Reps</th>
              <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Weight</th>
              <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Rest</th>
              <th style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-secondary)' }}>Notes</th>
            </tr>
          </thead>
          <tbody>
            {section.exercises.map((ex, i) => (
              <tr key={i} style={{ borderBottom: '1px solid var(--color-border)' }}>
                <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.name}</td>
                <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.sets ?? '—'}</td>
                <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.reps ?? '—'}</td>
                <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.weight ?? '—'}</td>
                <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.restSeconds ? `${ex.restSeconds}s` : '—'}</td>
                <td style={{ padding: '0.25rem 0.5rem', color: 'var(--color-text-primary)' }}>{ex.notes ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

/**
 * Generates the provenance text for a copied day assignment.
 * Shows "Copied from [Program Name] — Week X, Day Y" or "Deleted Program" if source is gone.
 */
function getProvenanceText(assignment: DayAssignmentDetail): string {
  const programName = assignment.sourceProgramName || 'Deleted Program';
  const weekNum = assignment.sourceWeekNumber;
  const dayNum = assignment.sourceDayNumber;

  if (weekNum != null && dayNum != null) {
    return `Copied from ${programName} — Week ${weekNum}, Day ${dayNum}`;
  }
  return `Copied from ${programName}`;
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
 * The backend UploadParser expects: { program_metadata: {...}, program_structure: [...] }
 */
function programToUploadSchema(prog: VaultProgramDetail) {
  return {
    program_metadata: {
      program_name: prog.name,
      duration_weeks: prog.durationWeeks,
      goal: prog.goal,
      equipment_profile: prog.equipmentProfile,
      version: "1.0",
    },
    program_structure: prog.weeks.map((week) => ({
      week_number: week.weekNumber,
      days: week.days.map((day) => ({
        day_number: day.dayNumber,
        day_label: day.label,
        focus_area: day.focusArea,
        modality: mapModalityToSchema(day.modality),
        methodology_source: day.methodologySource ?? undefined,
        warm_up: day.warmUp.map((entry) => ({
          movement: entry.movement,
          instruction: entry.instruction,
        })),
        blocks: day.sections.map((section) => ({
          block_type: section.name,
          format: section.format ?? "Sets/Reps",
          time_cap_minutes: section.timeCap ?? undefined,
          movements: section.exercises.map((ex) => ({
            exercise_name: ex.name,
            modality_type: mapModalityTypeToSchema(ex.modalityType),
            prescribed_sets: ex.sets,
            prescribed_reps: ex.reps,
            prescribed_weight: ex.weight ?? undefined,
            rest_interval_seconds: ex.restSeconds ?? undefined,
            notes: ex.notes ?? undefined,
          })),
        })),
        cool_down: day.coolDown.map((entry) => ({
          movement: entry.movement,
          instruction: entry.instruction,
        })),
      })),
    })),
  };
}

/** Maps the enum-style modality from the API response to the Upload_Schema string. */
function mapModalityToSchema(modality: string): string {
  switch (modality) {
    case 'CROSSFIT': return 'CrossFit';
    case 'HYPERTROPHY': return 'Hypertrophy';
    default: return modality;
  }
}

/** Maps the enum-style modality type from the API response to the Upload_Schema string. */
function mapModalityTypeToSchema(modalityType: string | undefined): string | undefined {
  if (!modalityType) return undefined;
  switch (modalityType) {
    case 'ENGINE': return 'Engine';
    case 'GYMNASTICS': return 'Gymnastics';
    case 'WEIGHTLIFTING': return 'Weightlifting';
    default: return modalityType;
  }
}
