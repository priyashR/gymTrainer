import { useCallback, useState } from "react";
import type { LogSetRequest, SessionStatus } from "../../types/session";

interface SetLogFormProps {
  sectionIndex: number;
  exerciseIndex: number;
  sessionStatus: SessionStatus;
  onLogSet: (request: LogSetRequest) => Promise<void>;
}

const formStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "0.5rem",
  alignItems: "flex-end",
  padding: "0.5rem 0",
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
  width: 72,
  padding: "0.4rem 0.5rem",
  border: "1px solid #ccc",
  borderRadius: 6,
  fontSize: "0.875rem",
};

const selectStyle: React.CSSProperties = {
  width: 72,
  padding: "0.4rem 0.25rem",
  border: "1px solid #ccc",
  borderRadius: 6,
  fontSize: "0.875rem",
  background: "#fff",
};

const submitButtonStyle: React.CSSProperties = {
  padding: "0.4rem 0.75rem",
  border: "1px solid #1976d2",
  borderRadius: 6,
  background: "#1976d2",
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

const RPE_OPTIONS = [
  { value: "", label: "—" },
  ...Array.from({ length: 19 }, (_, i) => {
    const val = (i + 2) * 0.5; // 1.0 to 10.0
    return { value: String(val), label: String(val) };
  }),
];

export function SetLogForm({
  sectionIndex,
  exerciseIndex,
  sessionStatus,
  onLogSet,
}: SetLogFormProps) {
  const [weight, setWeight] = useState("");
  const [repetitions, setRepetitions] = useState("");
  const [rpe, setRpe] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const isDisabled =
    sessionStatus === "PAUSED" || sessionStatus === "COMPLETED" || submitting;

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);

      const weightNum = parseFloat(weight);
      const repsNum = parseInt(repetitions, 10);

      if (!weight || isNaN(weightNum) || weightNum <= 0) {
        setError("Weight must be greater than 0");
        return;
      }
      if (!repetitions || isNaN(repsNum) || repsNum <= 0) {
        setError("Reps must be at least 1");
        return;
      }

      const rpeNum = rpe ? parseFloat(rpe) : null;

      const request: LogSetRequest = {
        sectionIndex,
        exerciseIndex,
        weight: weightNum,
        repetitions: repsNum,
        rpe: rpeNum,
      };

      setSubmitting(true);
      try {
        await onLogSet(request);
        // Reset form on success
        setWeight("");
        setRepetitions("");
        setRpe("");
      } catch {
        setError("Failed to log set. Please try again.");
      } finally {
        setSubmitting(false);
      }
    },
    [weight, repetitions, rpe, sectionIndex, exerciseIndex, onLogSet]
  );

  return (
    <form onSubmit={handleSubmit} style={formStyle} aria-label="Log set">
      <div style={fieldStyle}>
        <label style={labelStyle} htmlFor={`weight-${sectionIndex}-${exerciseIndex}`}>
          Weight (kg)
        </label>
        <input
          id={`weight-${sectionIndex}-${exerciseIndex}`}
          type="number"
          step="0.01"
          min="0.01"
          style={inputStyle}
          value={weight}
          onChange={(e) => setWeight(e.target.value)}
          disabled={isDisabled}
          placeholder="0"
        />
      </div>

      <div style={fieldStyle}>
        <label style={labelStyle} htmlFor={`reps-${sectionIndex}-${exerciseIndex}`}>
          Reps
        </label>
        <input
          id={`reps-${sectionIndex}-${exerciseIndex}`}
          type="number"
          step="1"
          min="1"
          style={inputStyle}
          value={repetitions}
          onChange={(e) => setRepetitions(e.target.value)}
          disabled={isDisabled}
          placeholder="0"
        />
      </div>

      <div style={fieldStyle}>
        <label style={labelStyle} htmlFor={`rpe-${sectionIndex}-${exerciseIndex}`}>
          RPE
        </label>
        <select
          id={`rpe-${sectionIndex}-${exerciseIndex}`}
          style={selectStyle}
          value={rpe}
          onChange={(e) => setRpe(e.target.value)}
          disabled={isDisabled}
        >
          {RPE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      <button
        type="submit"
        style={isDisabled ? disabledButtonStyle : submitButtonStyle}
        disabled={isDisabled}
      >
        {submitting ? "…" : "+ Set"}
      </button>

      {error && (
        <span style={errorStyle} role="alert">
          {error}
        </span>
      )}
    </form>
  );
}
