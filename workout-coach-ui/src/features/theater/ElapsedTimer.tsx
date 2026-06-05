import { useEffect, useRef, useState } from "react";

interface ElapsedTimerProps {
  /** ISO-8601 timestamp when the session started */
  startedAt: string;
  /** Whether the session is currently paused */
  isPaused: boolean;
  /** ISO-8601 timestamp when the session was paused (null if not paused) */
  pausedAt: string | null;
  /** Total seconds already accumulated in paused state from previous pause/resume cycles */
  totalPausedSeconds?: number;
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "0.5rem",
  padding: "0.5rem 0.75rem",
};

const timerStyle: React.CSSProperties = {
  fontSize: "1.25rem",
  fontWeight: 600,
  fontVariantNumeric: "tabular-nums",
  color: "#333",
};

const pausedTimerStyle: React.CSSProperties = {
  ...timerStyle,
  color: "#e65100",
};

const labelStyle: React.CSSProperties = {
  fontSize: "0.7rem",
  fontWeight: 500,
  color: "#888",
  textTransform: "uppercase",
  letterSpacing: "0.04em",
};

function formatElapsed(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  const mm = String(minutes).padStart(2, "0");
  const ss = String(seconds).padStart(2, "0");

  if (hours > 0) {
    return `${hours}:${mm}:${ss}`;
  }
  return `${mm}:${ss}`;
}

export function ElapsedTimer({
  startedAt,
  isPaused,
  pausedAt,
  totalPausedSeconds = 0,
}: ElapsedTimerProps) {
  const [elapsed, setElapsed] = useState(0);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    const startTime = new Date(startedAt).getTime();

    const computeElapsed = () => {
      const now = Date.now();
      let currentPausedMs = totalPausedSeconds * 1000;

      // If currently paused, add the time from pausedAt to now
      if (isPaused && pausedAt) {
        currentPausedMs += now - new Date(pausedAt).getTime();
      }

      const elapsedMs = now - startTime - currentPausedMs;
      return Math.max(0, Math.floor(elapsedMs / 1000));
    };

    // Set initial value
    setElapsed(computeElapsed());

    if (!isPaused) {
      // Update every second when running
      intervalRef.current = setInterval(() => {
        setElapsed(computeElapsed());
      }, 1000);
    }

    return () => {
      if (intervalRef.current !== null) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [startedAt, isPaused, pausedAt, totalPausedSeconds]);

  return (
    <div style={containerStyle} aria-label="Elapsed workout time" role="timer">
      <span style={labelStyle}>Elapsed</span>
      <span style={isPaused ? pausedTimerStyle : timerStyle}>
        {formatElapsed(elapsed)}
        {isPaused && " ⏸"}
      </span>
    </div>
  );
}
