import { useCallback, useEffect, useRef, useState } from "react";

interface RestTimerOverlayProps {
  /** Initial rest duration in seconds */
  durationSeconds: number;
  /** Called when the timer expires or is skipped */
  onDismiss: () => void;
}

const overlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  background: "rgba(0, 0, 0, 0.85)",
  zIndex: 1000,
  gap: "1.5rem",
};

const timeStyle: React.CSSProperties = {
  fontSize: "4rem",
  fontWeight: 700,
  fontFamily: "monospace",
  color: "#fff",
};

const labelStyle: React.CSSProperties = {
  fontSize: "1rem",
  color: "#bbb",
  textTransform: "uppercase",
  letterSpacing: "0.15em",
};

const buttonStyle: React.CSSProperties = {
  padding: "0.75rem 2rem",
  border: "2px solid #fff",
  borderRadius: 8,
  background: "transparent",
  color: "#fff",
  fontSize: "1rem",
  fontWeight: 600,
  cursor: "pointer",
};

const adjustContainerStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "1rem",
};

const adjustButtonStyle: React.CSSProperties = {
  width: 36,
  height: 36,
  border: "1px solid #888",
  borderRadius: "50%",
  background: "transparent",
  color: "#fff",
  fontSize: "1.25rem",
  cursor: "pointer",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
};

function formatTime(totalSeconds: number): string {
  const mins = Math.floor(totalSeconds / 60);
  const secs = totalSeconds % 60;
  return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
}

export function RestTimerOverlay({
  durationSeconds,
  onDismiss,
}: RestTimerOverlayProps) {
  const [remaining, setRemaining] = useState(durationSeconds);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Play notification sound on expiry
  const playNotification = useCallback(() => {
    try {
      // Use Web Audio API for a simple beep
      const ctx = new AudioContext();
      const oscillator = ctx.createOscillator();
      const gain = ctx.createGain();
      oscillator.connect(gain);
      gain.connect(ctx.destination);
      oscillator.frequency.value = 880;
      oscillator.type = "sine";
      gain.gain.value = 0.3;
      oscillator.start();
      oscillator.stop(ctx.currentTime + 0.5);
    } catch {
      // Audio not available — visual notification only
    }
  }, []);

  useEffect(() => {
    if (remaining <= 0) {
      if (intervalRef.current) clearInterval(intervalRef.current);
      playNotification();
      // Auto-dismiss after a brief delay so user sees "00:00"
      const timeout = setTimeout(onDismiss, 1500);
      return () => clearTimeout(timeout);
    }

    intervalRef.current = setInterval(() => {
      setRemaining((prev) => prev - 1);
    }, 1000);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [remaining, onDismiss, playNotification]);

  // Allow user to adjust duration (local-only, current rest period only)
  const adjustDuration = (delta: number) => {
    setRemaining((prev) => Math.max(0, prev + delta));
  };

  const isExpired = remaining <= 0;

  return (
    <div
      style={overlayStyle}
      role="dialog"
      aria-modal="true"
      aria-label="Rest timer"
    >
      <span style={labelStyle}>Rest</span>

      <span
        style={{
          ...timeStyle,
          color: isExpired ? "#66bb6a" : remaining <= 5 ? "#ff9800" : "#fff",
        }}
        role="timer"
        aria-label={`${formatTime(remaining)} rest remaining`}
      >
        {formatTime(remaining)}
      </span>

      {!isExpired && (
        <>
          <div style={adjustContainerStyle}>
            <button
              type="button"
              style={adjustButtonStyle}
              onClick={() => adjustDuration(-15)}
              aria-label="Decrease rest by 15 seconds"
            >
              −
            </button>
            <span style={{ color: "#bbb", fontSize: "0.8rem" }}>±15s</span>
            <button
              type="button"
              style={adjustButtonStyle}
              onClick={() => adjustDuration(15)}
              aria-label="Increase rest by 15 seconds"
            >
              +
            </button>
          </div>

          <button
            type="button"
            style={buttonStyle}
            onClick={onDismiss}
            aria-label="Skip rest timer"
          >
            Skip Rest
          </button>
        </>
      )}

      {isExpired && (
        <span style={{ color: "#66bb6a", fontWeight: 600, fontSize: "1.125rem" }}>
          Rest complete — get ready!
        </span>
      )}

      {/* Hidden audio element for fallback */}
      <audio ref={audioRef} />
    </div>
  );
}
