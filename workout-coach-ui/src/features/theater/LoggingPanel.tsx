import React, { useCallback, useState } from "react";
import type { SectionType } from "../../types/session";

// --- Types ---

export interface CurrentExercise {
  name: string;
  recommendation: string;
}

export interface LoggedSet {
  reps: number;
  weight: number;
  rpe: number | null;
}

export interface CrossFitScoreData {
  rounds: number;
  reps: number;
}

export interface LoggingPanelProps {
  /** Section type determines which variant to render */
  sectionType: SectionType;
  /** Current exercise info (used in Strength variant) */
  currentExercise: CurrentExercise;
  /** Previously logged sets for strength variant */
  loggedSets: LoggedSet[];
  /** Current round count for CrossFit variant */
  roundsCompleted: number;
  /** Callback when a strength set is logged */
  onLogSet: (set: LoggedSet) => void;
  /** Callback when rounds are incremented/decremented */
  onUpdateRounds: (newCount: number) => void;
  /** Callback when CrossFit score is submitted */
  onSubmitScore: (score: CrossFitScoreData) => void;
}

// --- Styles ---

const panelStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "var(--spacing-md)",
  padding: "var(--spacing-lg)",
  background: "var(--color-bg-surface)",
  borderRadius: "var(--radius-md)",
  minHeight: "200px",
};

const exerciseHeaderStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "var(--spacing-xs)",
};

const exerciseNameStyle: React.CSSProperties = {
  fontSize: "22px",
  fontWeight: 700,
  color: "var(--color-text-primary)",
  margin: 0,
};

const prescriptionStyle: React.CSSProperties = {
  fontSize: "16px",
  fontWeight: 400,
  color: "var(--color-text-secondary)",
  margin: 0,
};

const formStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "var(--spacing-sm)",
  alignItems: "flex-end",
};

const fieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "var(--spacing-xs)",
};

const labelStyle: React.CSSProperties = {
  fontSize: "0.75rem",
  fontWeight: 600,
  color: "var(--color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.03em",
};

const inputStyle: React.CSSProperties = {
  width: "80px",
  minHeight: "var(--tap-target-preferred)",
  padding: "var(--spacing-sm) var(--spacing-md)",
  border: "1px solid var(--color-border)",
  borderRadius: "var(--radius-sm)",
  fontSize: "1rem",
  background: "var(--color-bg-card)",
  color: "var(--color-text-primary)",
};

const logButtonStyle: React.CSSProperties = {
  minWidth: "var(--tap-target-preferred)",
  minHeight: "var(--tap-target-preferred)",
  padding: "var(--spacing-sm) var(--spacing-lg)",
  border: "none",
  borderRadius: "var(--radius-sm)",
  background: "var(--color-accent)",
  color: "var(--color-bg-primary)",
  fontSize: "0.875rem",
  fontWeight: 700,
  cursor: "pointer",
  transition: "background 0.15s",
};

const dividerStyle: React.CSSProperties = {
  height: "1px",
  background: "var(--color-border)",
  border: "none",
  margin: "var(--spacing-sm) 0",
};

const setsHeaderStyle: React.CSSProperties = {
  fontSize: "0.875rem",
  fontWeight: 600,
  color: "var(--color-text-secondary)",
  margin: 0,
};

const setListStyle: React.CSSProperties = {
  listStyle: "none",
  padding: 0,
  margin: 0,
  display: "flex",
  flexDirection: "column",
  gap: "var(--spacing-xs)",
};

const setItemStyle: React.CSSProperties = {
  display: "flex",
  gap: "var(--spacing-sm)",
  alignItems: "center",
  fontSize: "0.875rem",
  color: "var(--color-text-primary)",
  padding: "var(--spacing-xs) var(--spacing-sm)",
  background: "var(--color-bg-card)",
  borderRadius: "var(--radius-sm)",
};

const setNumberStyle: React.CSSProperties = {
  fontWeight: 700,
  color: "var(--color-accent)",
  minWidth: "28px",
};

const rpeTagStyle: React.CSSProperties = {
  fontSize: "0.75rem",
  color: "var(--color-text-secondary)",
};

const emptySetStyle: React.CSSProperties = {
  fontSize: "0.875rem",
  color: "var(--color-text-secondary)",
  fontStyle: "italic",
};

// CrossFit styles
const roundCounterContainerStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: "var(--spacing-md)",
  padding: "var(--spacing-lg) 0",
};

const roundsLabelStyle: React.CSSProperties = {
  fontSize: "0.875rem",
  fontWeight: 600,
  color: "var(--color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.05em",
};

const roundsCountStyle: React.CSSProperties = {
  fontSize: "4rem",
  fontWeight: 700,
  color: "var(--color-crossfit)",
  lineHeight: 1,
  userSelect: "none",
};

const roundButtonsRowStyle: React.CSSProperties = {
  display: "flex",
  gap: "var(--spacing-md)",
  alignItems: "center",
};

const roundButtonStyle: React.CSSProperties = {
  minWidth: "var(--tap-target-preferred)",
  minHeight: "var(--tap-target-preferred)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  border: "1px solid var(--color-crossfit)",
  borderRadius: "var(--radius-sm)",
  background: "transparent",
  color: "var(--color-crossfit)",
  fontSize: "1.5rem",
  fontWeight: 700,
  cursor: "pointer",
  transition: "background 0.15s",
};

const roundButtonDisabledStyle: React.CSSProperties = {
  ...roundButtonStyle,
  opacity: 0.4,
  cursor: "not-allowed",
};

const scoreFormStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "var(--spacing-sm)",
  alignItems: "flex-end",
  padding: "var(--spacing-md) 0",
};

const scoreFormLabelStyle: React.CSSProperties = {
  fontSize: "0.875rem",
  fontWeight: 600,
  color: "var(--color-text-secondary)",
  width: "100%",
  margin: 0,
};

const submitScoreButtonStyle: React.CSSProperties = {
  minWidth: "var(--tap-target-preferred)",
  minHeight: "var(--tap-target-preferred)",
  padding: "var(--spacing-sm) var(--spacing-lg)",
  border: "none",
  borderRadius: "var(--radius-sm)",
  background: "var(--color-crossfit)",
  color: "#fff",
  fontSize: "0.875rem",
  fontWeight: 700,
  cursor: "pointer",
  transition: "background 0.15s",
};

const errorStyle: React.CSSProperties = {
  fontSize: "0.75rem",
  color: "var(--color-error)",
  width: "100%",
};

// --- Sub-components ---

function StrengthVariant({
  currentExercise,
  loggedSets,
  onLogSet,
}: {
  currentExercise: CurrentExercise;
  loggedSets: LoggedSet[];
  onLogSet: (set: LoggedSet) => void;
}) {
  const [reps, setReps] = useState("");
  const [weight, setWeight] = useState("");
  const [rpe, setRpe] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleLogSet = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);

      const repsNum = parseInt(reps, 10);
      const weightNum = parseFloat(weight);

      if (!reps || isNaN(repsNum) || repsNum <= 0) {
        setError("Reps must be at least 1");
        return;
      }
      if (!weight || isNaN(weightNum) || weightNum <= 0) {
        setError("Weight must be greater than 0");
        return;
      }

      const rpeNum = rpe ? parseFloat(rpe) : null;
      if (rpe && (isNaN(rpeNum!) || rpeNum! < 1 || rpeNum! > 10)) {
        setError("RPE must be between 1 and 10");
        return;
      }

      onLogSet({ reps: repsNum, weight: weightNum, rpe: rpeNum });
      setReps("");
      setWeight("");
      setRpe("");
    },
    [reps, weight, rpe, onLogSet]
  );

  return (
    <>
      {/* Current Exercise Header */}
      <div style={exerciseHeaderStyle}>
        <h2 style={exerciseNameStyle}>{currentExercise.name}</h2>
        <p style={prescriptionStyle}>Prescribed: {currentExercise.recommendation}</p>
      </div>

      {/* Set Log Form */}
      <form
        onSubmit={handleLogSet}
        style={formStyle}
        aria-label="Log set form"
        data-testid="set-log-form"
      >
        <div style={fieldStyle}>
          <label style={labelStyle} htmlFor="logging-panel-reps">
            Reps
          </label>
          <input
            id="logging-panel-reps"
            type="number"
            min="1"
            step="1"
            style={inputStyle}
            value={reps}
            onChange={(e) => setReps(e.target.value)}
            placeholder="0"
            aria-label="Reps completed"
          />
        </div>

        <div style={fieldStyle}>
          <label style={labelStyle} htmlFor="logging-panel-weight">
            Weight
          </label>
          <input
            id="logging-panel-weight"
            type="number"
            min="0.01"
            step="0.5"
            style={inputStyle}
            value={weight}
            onChange={(e) => setWeight(e.target.value)}
            placeholder="0"
            aria-label="Weight used"
          />
        </div>

        <div style={fieldStyle}>
          <label style={labelStyle} htmlFor="logging-panel-rpe">
            RPE
          </label>
          <input
            id="logging-panel-rpe"
            type="number"
            min="1"
            max="10"
            step="0.5"
            style={inputStyle}
            value={rpe}
            onChange={(e) => setRpe(e.target.value)}
            placeholder="—"
            aria-label="Rate of perceived exertion"
          />
        </div>

        <button
          type="submit"
          style={logButtonStyle}
          aria-label="Log set"
        >
          ✓ Log Set
        </button>

        {error && (
          <span style={errorStyle} role="alert">
            {error}
          </span>
        )}
      </form>

      {/* Set Log History */}
      <hr style={dividerStyle} />
      <p style={setsHeaderStyle}>Sets Logged:</p>
      {loggedSets.length === 0 ? (
        <p style={emptySetStyle} data-testid="no-sets-message">
          No sets logged yet
        </p>
      ) : (
        <ul style={setListStyle} aria-label="Logged sets" data-testid="set-log-history">
          {loggedSets.map((set, index) => (
            <li key={index} style={setItemStyle}>
              <span style={setNumberStyle}>#{index + 1}</span>
              <span>
                {set.reps} × {set.weight}kg
              </span>
              {set.rpe !== null && <span style={rpeTagStyle}>RPE {set.rpe}</span>}
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

function CrossFitVariant({
  roundsCompleted,
  onUpdateRounds,
  onSubmitScore,
}: {
  roundsCompleted: number;
  onUpdateRounds: (newCount: number) => void;
  onSubmitScore: (score: CrossFitScoreData) => void;
}) {
  const [scoreRounds, setScoreRounds] = useState("");
  const [scoreReps, setScoreReps] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleDecrement = useCallback(() => {
    if (roundsCompleted > 0) {
      onUpdateRounds(roundsCompleted - 1);
    }
  }, [roundsCompleted, onUpdateRounds]);

  const handleIncrement = useCallback(() => {
    onUpdateRounds(roundsCompleted + 1);
  }, [roundsCompleted, onUpdateRounds]);

  const handleSubmitScore = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);

      const roundsNum = parseInt(scoreRounds, 10);
      const repsNum = parseInt(scoreReps, 10);

      if (isNaN(roundsNum) || roundsNum < 0) {
        setError("Rounds must be 0 or greater");
        return;
      }
      if (isNaN(repsNum) || repsNum < 0) {
        setError("Reps must be 0 or greater");
        return;
      }

      onSubmitScore({ rounds: roundsNum, reps: repsNum });
      setScoreRounds("");
      setScoreReps("");
    },
    [scoreRounds, scoreReps, onSubmitScore]
  );

  const canDecrement = roundsCompleted > 0;

  return (
    <>
      {/* Round Counter */}
      <div
        style={roundCounterContainerStyle}
        aria-label="Round counter"
        data-testid="round-counter"
      >
        <span style={roundsLabelStyle}>Rounds Completed</span>
        <span style={roundsCountStyle} aria-live="polite">
          {roundsCompleted}
        </span>
        <div style={roundButtonsRowStyle}>
          <button
            type="button"
            style={canDecrement ? roundButtonStyle : roundButtonDisabledStyle}
            onClick={handleDecrement}
            disabled={!canDecrement}
            aria-label="Decrement rounds"
          >
            −
          </button>
          <button
            type="button"
            style={roundButtonStyle}
            onClick={handleIncrement}
            aria-label="Increment rounds"
          >
            ＋
          </button>
        </div>
      </div>

      {/* CrossFit Score Form */}
      <hr style={dividerStyle} />
      <form
        onSubmit={handleSubmitScore}
        style={scoreFormStyle}
        aria-label="Submit CrossFit score"
        data-testid="crossfit-score-form"
      >
        <p style={scoreFormLabelStyle}>Log Score</p>

        <div style={fieldStyle}>
          <label style={labelStyle} htmlFor="logging-panel-score-rounds">
            Rounds
          </label>
          <input
            id="logging-panel-score-rounds"
            type="number"
            min="0"
            step="1"
            style={inputStyle}
            value={scoreRounds}
            onChange={(e) => setScoreRounds(e.target.value)}
            placeholder="0"
            aria-label="Score rounds"
          />
        </div>

        <div style={fieldStyle}>
          <label style={labelStyle} htmlFor="logging-panel-score-reps">
            + Reps
          </label>
          <input
            id="logging-panel-score-reps"
            type="number"
            min="0"
            step="1"
            style={inputStyle}
            value={scoreReps}
            onChange={(e) => setScoreReps(e.target.value)}
            placeholder="0"
            aria-label="Score additional reps"
          />
        </div>

        <button
          type="submit"
          style={submitScoreButtonStyle}
          aria-label="Submit score"
        >
          Submit Score{scoreRounds || scoreReps ? `: ${scoreRounds || 0} + ${scoreReps || 0}` : ""}
        </button>

        {error && (
          <span style={errorStyle} role="alert">
            {error}
          </span>
        )}
      </form>
    </>
  );
}

// --- Main Component ---

/**
 * LoggingPanel — right-side panel in Theater Mode.
 *
 * Strength variant: displays current exercise header, set logging form (reps, weight, RPE),
 * and a history of logged sets.
 *
 * CrossFit variant (AMRAP, FOR_TIME, EMOM): displays a round counter with
 * increment/decrement buttons and a score submission form.
 *
 * Validates: Requirements 4.5, 4.6
 */
export function LoggingPanel({
  sectionType,
  currentExercise,
  loggedSets,
  roundsCompleted,
  onLogSet,
  onUpdateRounds,
  onSubmitScore,
}: LoggingPanelProps) {
  const isCrossFit =
    sectionType === "AMRAP" || sectionType === "FOR_TIME" || sectionType === "EMOM";

  return (
    <div style={panelStyle} data-testid="logging-panel">
      {isCrossFit ? (
        <CrossFitVariant
          roundsCompleted={roundsCompleted}
          onUpdateRounds={onUpdateRounds}
          onSubmitScore={onSubmitScore}
        />
      ) : (
        <StrengthVariant
          currentExercise={currentExercise}
          loggedSets={loggedSets}
          onLogSet={onLogSet}
        />
      )}
    </div>
  );
}
