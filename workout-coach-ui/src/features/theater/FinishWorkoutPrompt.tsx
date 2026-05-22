import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";

interface FinishWorkoutPromptProps {
  onFinish: () => Promise<void>;
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: "1rem",
  padding: "1.5rem",
  background: "#e8f5e9",
  borderRadius: 12,
  border: "2px solid #81c784",
  textAlign: "center",
};

const buttonStyle: React.CSSProperties = {
  padding: "0.75rem 2rem",
  border: "none",
  borderRadius: 8,
  background: "#388e3c",
  color: "#fff",
  fontSize: "1rem",
  fontWeight: 700,
  cursor: "pointer",
};

export function FinishWorkoutPrompt({ onFinish }: FinishWorkoutPromptProps) {
  const navigate = useNavigate();
  const [isFinishing, setIsFinishing] = useState(false);

  const handleFinish = useCallback(async () => {
    setIsFinishing(true);
    try {
      await onFinish();
      navigate("/", { replace: true });
    } finally {
      setIsFinishing(false);
    }
  }, [onFinish, navigate]);

  return (
    <div style={containerStyle} role="status" aria-label="All exercises complete">
      <span style={{ fontSize: "2rem" }}>🎉</span>
      <h2 style={{ margin: 0, fontSize: "1.25rem", color: "#2e7d32" }}>
        All Exercises Complete!
      </h2>
      <p style={{ margin: 0, color: "#555", fontSize: "0.9rem" }}>
        Great work! Finish your workout to save your progress.
      </p>
      <button
        type="button"
        style={buttonStyle}
        onClick={handleFinish}
        disabled={isFinishing}
      >
        {isFinishing ? "Finishing…" : "Finish Workout"}
      </button>
    </div>
  );
}
