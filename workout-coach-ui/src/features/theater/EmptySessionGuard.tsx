import { useCallback, useState } from "react";
import type { SectionProgress } from "../../types/session";

interface EmptySessionGuardProps {
  sectionProgresses: SectionProgress[];
  onConfirmEnd: () => Promise<void>;
  onCancel: () => void;
}

const overlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "rgba(0, 0, 0, 0.5)",
  zIndex: 950,
};

const dialogStyle: React.CSSProperties = {
  background: "#fff",
  borderRadius: 12,
  padding: "2rem",
  maxWidth: 400,
  width: "90%",
  display: "flex",
  flexDirection: "column",
  gap: "1rem",
  textAlign: "center",
};

const titleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: "1.25rem",
  fontWeight: 700,
  color: "#e65100",
};

const messageStyle: React.CSSProperties = {
  margin: 0,
  color: "#666",
  fontSize: "0.9rem",
  lineHeight: 1.5,
};

const buttonRowStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.75rem",
  justifyContent: "center",
};

const baseButtonStyle: React.CSSProperties = {
  padding: "0.625rem 1.25rem",
  border: "1px solid #ccc",
  borderRadius: 6,
  fontSize: "0.875rem",
  fontWeight: 600,
  cursor: "pointer",
};

const cancelButtonStyle: React.CSSProperties = {
  ...baseButtonStyle,
  background: "#f5f5f5",
  borderColor: "#bdbdbd",
  color: "#424242",
};

const confirmButtonStyle: React.CSSProperties = {
  ...baseButtonStyle,
  background: "#ffebee",
  borderColor: "#ef9a9a",
  color: "#c62828",
};

/**
 * Checks whether the session has any performance data logged.
 * Returns true if at least one SetLog or one CrossFitScore exists.
 */
export function hasPerformanceData(sectionProgresses: SectionProgress[]): boolean {
  for (const section of sectionProgresses) {
    if (section.crossFitScore !== null) {
      return true;
    }
    for (const exerciseLog of section.exerciseLogs) {
      if (exerciseLog.setLogs.length > 0) {
        return true;
      }
    }
  }
  return false;
}

export function EmptySessionGuard({
  sectionProgresses,
  onConfirmEnd,
  onCancel,
}: EmptySessionGuardProps) {
  const [processing, setProcessing] = useState(false);

  // If there is performance data, this guard should not be shown.
  // The parent component should check hasPerformanceData before rendering.
  const isEmpty = !hasPerformanceData(sectionProgresses);

  const handleConfirm = useCallback(async () => {
    setProcessing(true);
    try {
      await onConfirmEnd();
    } finally {
      setProcessing(false);
    }
  }, [onConfirmEnd]);

  if (!isEmpty) {
    return null;
  }

  return (
    <div
      style={overlayStyle}
      role="dialog"
      aria-modal="true"
      aria-label="No performance data recorded"
    >
      <div style={dialogStyle}>
        <h2 style={titleStyle}>No Data Recorded</h2>
        <p style={messageStyle}>
          You haven't logged any sets or scores in this session. Are you sure you
          want to end the workout without recording any performance data?
        </p>
        <div style={buttonRowStyle}>
          <button
            type="button"
            style={cancelButtonStyle}
            onClick={onCancel}
            disabled={processing}
          >
            Go Back
          </button>
          <button
            type="button"
            style={confirmButtonStyle}
            onClick={handleConfirm}
            disabled={processing}
          >
            {processing ? "Ending…" : "End Anyway"}
          </button>
        </div>
      </div>
    </div>
  );
}
