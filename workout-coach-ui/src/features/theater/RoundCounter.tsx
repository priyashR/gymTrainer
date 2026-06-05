import { useCallback } from "react";

interface RoundCounterProps {
  roundCount: number;
  onRoundCountChange: (newCount: number) => void;
  disabled?: boolean;
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: "0.75rem",
  padding: "1.5rem",
};

const labelStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  fontWeight: 600,
  color: "#555",
  textTransform: "uppercase",
  letterSpacing: "0.05em",
};

const countStyle: React.CSSProperties = {
  fontSize: "4rem",
  fontWeight: 700,
  color: "#1976d2",
  lineHeight: 1,
  userSelect: "none",
};

const tapTargetStyle: React.CSSProperties = {
  width: 160,
  height: 160,
  borderRadius: "50%",
  border: "3px solid #1976d2",
  background: "#e3f2fd",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  cursor: "pointer",
  transition: "background 0.1s, transform 0.1s",
};

const tapTargetDisabledStyle: React.CSSProperties = {
  ...tapTargetStyle,
  border: "3px solid #bdbdbd",
  background: "#f5f5f5",
  cursor: "not-allowed",
};

const controlsStyle: React.CSSProperties = {
  display: "flex",
  gap: "1rem",
  alignItems: "center",
};

const decrementButtonStyle: React.CSSProperties = {
  padding: "0.4rem 1rem",
  border: "1px solid #ef9a9a",
  borderRadius: 6,
  background: "#ffebee",
  color: "#c62828",
  fontSize: "0.875rem",
  fontWeight: 600,
  cursor: "pointer",
};

const decrementDisabledStyle: React.CSSProperties = {
  ...decrementButtonStyle,
  background: "#f5f5f5",
  borderColor: "#e0e0e0",
  color: "#bdbdbd",
  cursor: "not-allowed",
};

export function RoundCounter({
  roundCount,
  onRoundCountChange,
  disabled = false,
}: RoundCounterProps) {
  const handleIncrement = useCallback(() => {
    if (!disabled) {
      onRoundCountChange(roundCount + 1);
    }
  }, [disabled, roundCount, onRoundCountChange]);

  const handleDecrement = useCallback(() => {
    if (!disabled && roundCount > 0) {
      onRoundCountChange(roundCount - 1);
    }
  }, [disabled, roundCount, onRoundCountChange]);

  const canDecrement = !disabled && roundCount > 0;

  return (
    <div style={containerStyle} aria-label="Round counter">
      <span style={labelStyle}>Rounds</span>

      <button
        type="button"
        style={disabled ? tapTargetDisabledStyle : tapTargetStyle}
        onClick={handleIncrement}
        disabled={disabled}
        aria-label={`Increment round count. Current count: ${roundCount}`}
      >
        <span style={countStyle}>{roundCount}</span>
      </button>

      <div style={controlsStyle}>
        <button
          type="button"
          style={canDecrement ? decrementButtonStyle : decrementDisabledStyle}
          onClick={handleDecrement}
          disabled={!canDecrement}
          aria-label="Decrement round count"
        >
          − 1
        </button>
      </div>
    </div>
  );
}
