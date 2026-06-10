import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { EmptySessionGuard, hasPerformanceData } from "./EmptySessionGuard";
import type { SectionProgress } from "../../types/session";

interface SessionControlsProps {
  isPaused: boolean;
  sectionProgresses: SectionProgress[];
  onPause: () => Promise<void>;
  onResume: () => Promise<void>;
  onEnd: () => Promise<void>;
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.75rem",
  padding: "1rem",
  borderTop: "1px solid #e0e0e0",
  justifyContent: "center",
  flexWrap: "wrap",
};

const baseButtonStyle: React.CSSProperties = {
  padding: "0.625rem 1.25rem",
  border: "1px solid #ccc",
  borderRadius: 6,
  fontSize: "0.875rem",
  fontWeight: 600,
  cursor: "pointer",
};

const pauseButtonStyle: React.CSSProperties = {
  ...baseButtonStyle,
  background: "#fff3e0",
  borderColor: "#ffb74d",
  color: "#e65100",
};

const resumeButtonStyle: React.CSSProperties = {
  ...baseButtonStyle,
  background: "#e8f5e9",
  borderColor: "#81c784",
  color: "#2e7d32",
};

const endButtonStyle: React.CSSProperties = {
  ...baseButtonStyle,
  background: "#ffebee",
  borderColor: "#ef9a9a",
  color: "#c62828",
};

const leaveButtonStyle: React.CSSProperties = {
  ...baseButtonStyle,
  background: "#f5f5f5",
  borderColor: "#bdbdbd",
  color: "#424242",
};

const confirmOverlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "rgba(0, 0, 0, 0.5)",
  zIndex: 900,
};

const confirmDialogStyle: React.CSSProperties = {
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

const confirmButtonRowStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.75rem",
  justifyContent: "center",
};

type ConfirmAction = "end" | "leave" | null;

export function SessionControls({
  isPaused,
  sectionProgresses,
  onPause,
  onResume,
  onEnd,
}: SessionControlsProps) {
  const navigate = useNavigate();
  const [confirmAction, setConfirmAction] = useState<ConfirmAction>(null);
  const [showEmptyGuard, setShowEmptyGuard] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  const handlePauseResume = useCallback(async () => {
    setIsProcessing(true);
    try {
      if (isPaused) {
        await onResume();
      } else {
        await onPause();
      }
    } finally {
      setIsProcessing(false);
    }
  }, [isPaused, onPause, onResume]);

  const handleEndClick = useCallback(() => {
    // Check if session has performance data before showing end confirmation
    if (!hasPerformanceData(sectionProgresses)) {
      setShowEmptyGuard(true);
    } else {
      setConfirmAction("end");
    }
  }, [sectionProgresses]);

  const handleConfirmEnd = useCallback(async () => {
    setIsProcessing(true);
    try {
      await onEnd();
      setConfirmAction(null);
      navigate("/", { replace: true });
    } finally {
      setIsProcessing(false);
    }
  }, [onEnd, navigate]);

  const handleEmptyGuardConfirm = useCallback(async () => {
    setIsProcessing(true);
    try {
      await onEnd();
      setShowEmptyGuard(false);
      navigate("/", { replace: true });
    } finally {
      setIsProcessing(false);
    }
  }, [onEnd, navigate]);

  const handleEmptyGuardCancel = useCallback(() => {
    setShowEmptyGuard(false);
  }, []);

  const handleConfirmLeave = useCallback(async () => {
    // Pause persists state, then navigate away
    setIsProcessing(true);
    try {
      if (!isPaused) {
        await onPause();
      }
      setConfirmAction(null);
      navigate("/", { replace: true });
    } finally {
      setIsProcessing(false);
    }
  }, [isPaused, onPause, navigate]);

  return (
    <>
      <div style={containerStyle}>
        <button
          type="button"
          style={isPaused ? resumeButtonStyle : pauseButtonStyle}
          onClick={handlePauseResume}
          disabled={isProcessing}
        >
          {isPaused ? "Resume Workout" : "Pause Workout"}
        </button>

        <button
          type="button"
          style={endButtonStyle}
          onClick={handleEndClick}
          disabled={isProcessing}
        >
          End Workout
        </button>

        <button
          type="button"
          style={leaveButtonStyle}
          onClick={() => setConfirmAction("leave")}
          disabled={isProcessing}
        >
          Leave Workout
        </button>
      </div>

      {/* Empty session guard — shown when ending with no performance data */}
      {showEmptyGuard && (
        <EmptySessionGuard
          sectionProgresses={sectionProgresses}
          onConfirmEnd={handleEmptyGuardConfirm}
          onCancel={handleEmptyGuardCancel}
        />
      )}

      {/* Standard confirmation dialog for end/leave */}
      {confirmAction && (
        <div
          style={confirmOverlayStyle}
          role="dialog"
          aria-modal="true"
          aria-label={
            confirmAction === "end"
              ? "Confirm end workout"
              : "Confirm leave workout"
          }
        >
          <div style={confirmDialogStyle}>
            <h2 style={{ margin: 0, fontSize: "1.25rem" }}>
              {confirmAction === "end" ? "End Workout?" : "Leave Workout?"}
            </h2>
            <p style={{ margin: 0, color: "#666" }}>
              {confirmAction === "end"
                ? "Your session will be marked as complete with whatever progress has been logged so far."
                : "Your current progress will be saved. You can resume this session later."}
            </p>
            <div style={confirmButtonRowStyle}>
              <button
                type="button"
                style={{ ...baseButtonStyle, background: "#f5f5f5" }}
                onClick={() => setConfirmAction(null)}
                disabled={isProcessing}
              >
                Cancel
              </button>
              <button
                type="button"
                style={
                  confirmAction === "end" ? endButtonStyle : leaveButtonStyle
                }
                onClick={
                  confirmAction === "end"
                    ? handleConfirmEnd
                    : handleConfirmLeave
                }
                disabled={isProcessing}
              >
                {isProcessing
                  ? "Processing…"
                  : confirmAction === "end"
                    ? "End Workout"
                    : "Leave"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
