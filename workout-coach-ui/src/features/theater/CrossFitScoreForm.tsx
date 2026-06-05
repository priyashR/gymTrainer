import { useCallback, useState } from "react";
import type {
  LogCrossFitScoreRequest,
  SectionType,
  SessionStatus,
} from "../../types/session";

interface CrossFitScoreFormProps {
  sectionIndex: number;
  sectionType: SectionType;
  sessionStatus: SessionStatus;
  onLogCrossFitScore: (request: LogCrossFitScoreRequest) => Promise<void>;
}

const formStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "0.5rem",
  alignItems: "flex-end",
  padding: "0.75rem",
  border: "1px solid #e0e0e0",
  borderRadius: 8,
  background: "#fafafa",
};

const fieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "0.25rem",
};

const labelStyle: React.CSSProperties = {
  fontSize: "0.7rem",
  fontWeight: 600,
  color: "#555",
  textTransform: "uppercase",
  letterSpacing: "0.03em",
};

const inputStyle: React.CSSProperties = {
  width: 80,
  padding: "0.4rem 0.5rem",
  border: "1px solid #ccc",
  borderRadius: 6,
  fontSize: "0.875rem",
};

const submitButtonStyle: React.CSSProperties = {
  padding: "0.4rem 0.75rem",
  border: "1px solid #388e3c",
  borderRadius: 6,
  background: "#388e3c",
  color: "#fff",
  fontSize: "0.8rem",
  fontWeight: 600,
  cursor: "pointer",
};

const disabledButtonStyle: React.CSSProperties = {
  ...submitButtonStyle,
  background: "#bdbdbd",
  borderColor: "#bdbdbd",
  cursor: "not-allowed",
};

const errorStyle: React.CSSProperties = {
  fontSize: "0.75rem",
  color: "#c62828",
  width: "100%",
};

const titleStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  fontWeight: 600,
  color: "#333",
  width: "100%",
  margin: 0,
};

export function CrossFitScoreForm({
  sectionIndex,
  sectionType,
  sessionStatus,
  onLogCrossFitScore,
}: CrossFitScoreFormProps) {
  const [rounds, setRounds] = useState("");
  const [additionalReps, setAdditionalReps] = useState("");
  const [totalTimeSeconds, setTotalTimeSeconds] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const isDisabled =
    sessionStatus === "PAUSED" || sessionStatus === "COMPLETED" || submitting;
  const showTimeField = sectionType === "FOR_TIME";

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);

      const roundsNum = parseInt(rounds, 10);
      const additionalRepsNum = parseInt(additionalReps, 10);

      if (isNaN(roundsNum) || roundsNum < 0) {
        setError("Rounds must be 0 or greater");
        return;
      }
      if (isNaN(additionalRepsNum) || additionalRepsNum < 0) {
        setError("Additional reps must be 0 or greater");
        return;
      }

      let timeNum: number | null = null;
      if (showTimeField) {
        timeNum = parseInt(totalTimeSeconds, 10);
        if (isNaN(timeNum) || timeNum <= 0) {
          setError("Total time must be greater than 0");
          return;
        }
      }

      const request: LogCrossFitScoreRequest = {
        sectionIndex,
        rounds: roundsNum,
        additionalReps: additionalRepsNum,
        totalTimeSeconds: timeNum,
      };

      setSubmitting(true);
      try {
        await onLogCrossFitScore(request);
        setRounds("");
        setAdditionalReps("");
        setTotalTimeSeconds("");
      } catch {
        setError("Failed to log score. Please try again.");
      } finally {
        setSubmitting(false);
      }
    },
    [rounds, additionalReps, totalTimeSeconds, sectionIndex, showTimeField, onLogCrossFitScore]
  );

  const sectionLabel =
    sectionType === "AMRAP"
      ? "AMRAP"
      : sectionType === "EMOM"
        ? "EMOM"
        : "For Time";

  return (
    <form onSubmit={handleSubmit} style={formStyle} aria-label={`Log ${sectionLabel} score`}>
      <p style={titleStyle}>Log {sectionLabel} Score</p>

      <div style={fieldStyle}>
        <label style={labelStyle} htmlFor={`rounds-${sectionIndex}`}>
          Rounds
        </label>
        <input
          id={`rounds-${sectionIndex}`}
          type="number"
          step="1"
          min="0"
          style={inputStyle}
          value={rounds}
          onChange={(e) => setRounds(e.target.value)}
          disabled={isDisabled}
          placeholder="0"
        />
      </div>

      <div style={fieldStyle}>
        <label style={labelStyle} htmlFor={`additional-reps-${sectionIndex}`}>
          + Reps
        </label>
        <input
          id={`additional-reps-${sectionIndex}`}
          type="number"
          step="1"
          min="0"
          style={inputStyle}
          value={additionalReps}
          onChange={(e) => setAdditionalReps(e.target.value)}
          disabled={isDisabled}
          placeholder="0"
        />
      </div>

      {showTimeField && (
        <div style={fieldStyle}>
          <label style={labelStyle} htmlFor={`time-${sectionIndex}`}>
            Time (sec)
          </label>
          <input
            id={`time-${sectionIndex}`}
            type="number"
            step="1"
            min="1"
            style={inputStyle}
            value={totalTimeSeconds}
            onChange={(e) => setTotalTimeSeconds(e.target.value)}
            disabled={isDisabled}
            placeholder="0"
          />
        </div>
      )}

      <button
        type="submit"
        style={isDisabled ? disabledButtonStyle : submitButtonStyle}
        disabled={isDisabled}
      >
        {submitting ? "…" : "Save Score"}
      </button>

      {error && (
        <span style={errorStyle} role="alert">
          {error}
        </span>
      )}
    </form>
  );
}
