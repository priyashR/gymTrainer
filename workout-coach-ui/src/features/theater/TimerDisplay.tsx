import { useCallback, useEffect, useRef, useState } from "react";
import type { SectionType } from "../../types/session";

interface TimerDisplayProps {
  sectionType: SectionType;
  /** Total duration in seconds for AMRAP countdown */
  durationSeconds?: number;
  /** Work interval in seconds for Tabata/EMOM */
  workSeconds?: number;
  /** Rest interval in seconds for Tabata/EMOM */
  restSeconds?: number;
  /** Number of rounds for Tabata/EMOM */
  rounds?: number;
  /** Whether the session is paused */
  isPaused: boolean;
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  padding: "1rem",
  gap: "0.5rem",
};

const timeStyle: React.CSSProperties = {
  fontSize: "2.5rem",
  fontWeight: 700,
  fontFamily: "monospace",
  letterSpacing: "0.05em",
};

const labelStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  color: "#666",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

function formatTime(totalSeconds: number): string {
  const mins = Math.floor(Math.abs(totalSeconds) / 60);
  const secs = Math.abs(totalSeconds) % 60;
  return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
}

/**
 * Countdown timer for AMRAP sections.
 */
function CountdownTimer({
  durationSeconds,
  isPaused,
}: {
  durationSeconds: number;
  isPaused: boolean;
}) {
  const [remaining, setRemaining] = useState(durationSeconds);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (isPaused || remaining <= 0) {
      if (intervalRef.current) clearInterval(intervalRef.current);
      return;
    }

    intervalRef.current = setInterval(() => {
      setRemaining((prev) => {
        if (prev <= 1) {
          if (intervalRef.current) clearInterval(intervalRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [isPaused, remaining]);

  const isExpired = remaining <= 0;

  return (
    <div style={containerStyle}>
      <span style={labelStyle}>AMRAP — Countdown</span>
      <span
        style={{
          ...timeStyle,
          color: isExpired ? "#d32f2f" : remaining <= 10 ? "#f57c00" : "#333",
        }}
        role="timer"
        aria-label={`${formatTime(remaining)} remaining`}
      >
        {formatTime(remaining)}
      </span>
      {isExpired && (
        <span style={{ color: "#d32f2f", fontWeight: 600 }}>Time's up!</span>
      )}
    </div>
  );
}

/**
 * Stopwatch timer for Strength sections (counts up).
 */
function StopwatchTimer({ isPaused }: { isPaused: boolean }) {
  const [elapsed, setElapsed] = useState(0);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (isPaused) {
      if (intervalRef.current) clearInterval(intervalRef.current);
      return;
    }

    intervalRef.current = setInterval(() => {
      setElapsed((prev) => prev + 1);
    }, 1000);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [isPaused]);

  return (
    <div style={containerStyle}>
      <span style={labelStyle}>Strength — Stopwatch</span>
      <span style={timeStyle} role="timer" aria-label={`${formatTime(elapsed)} elapsed`}>
        {formatTime(elapsed)}
      </span>
    </div>
  );
}

/**
 * Interval timer for Tabata and EMOM sections (work/rest cycles).
 */
function IntervalTimer({
  sectionType,
  workSeconds = 20,
  restSeconds = 10,
  rounds = 8,
  isPaused,
}: {
  sectionType: "TABATA" | "EMOM";
  workSeconds: number;
  restSeconds: number;
  rounds: number;
  isPaused: boolean;
}) {
  const [currentRound, setCurrentRound] = useState(1);
  const [phase, setPhase] = useState<"work" | "rest">("work");
  const [remaining, setRemaining] = useState(workSeconds);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const isComplete = currentRound > rounds;

  const advancePhase = useCallback(() => {
    if (phase === "work") {
      setPhase("rest");
      setRemaining(restSeconds);
    } else {
      // End of rest — move to next round
      const nextRound = currentRound + 1;
      if (nextRound > rounds) {
        setCurrentRound(nextRound);
      } else {
        setCurrentRound(nextRound);
        setPhase("work");
        setRemaining(workSeconds);
      }
    }
  }, [phase, currentRound, rounds, workSeconds, restSeconds]);

  useEffect(() => {
    if (isPaused || isComplete) {
      if (intervalRef.current) clearInterval(intervalRef.current);
      return;
    }

    intervalRef.current = setInterval(() => {
      setRemaining((prev) => {
        if (prev <= 1) {
          advancePhase();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [isPaused, isComplete, advancePhase]);

  if (isComplete) {
    return (
      <div style={containerStyle}>
        <span style={labelStyle}>{sectionType} — Complete</span>
        <span style={{ ...timeStyle, color: "#388e3c" }}>Done!</span>
        <span style={{ fontSize: "0.875rem", color: "#666" }}>
          {rounds} rounds completed
        </span>
      </div>
    );
  }

  return (
    <div style={containerStyle}>
      <span style={labelStyle}>
        {sectionType} — Round {currentRound} of {rounds}
      </span>
      <span
        style={{
          ...timeStyle,
          color: phase === "work" ? "#1565c0" : "#388e3c",
        }}
        role="timer"
        aria-label={`${formatTime(remaining)} ${phase}`}
      >
        {formatTime(remaining)}
      </span>
      <span
        style={{
          fontSize: "0.875rem",
          fontWeight: 600,
          color: phase === "work" ? "#1565c0" : "#388e3c",
          textTransform: "uppercase",
        }}
      >
        {phase === "work" ? "Work" : "Rest"}
      </span>
    </div>
  );
}

export function TimerDisplay({
  sectionType,
  durationSeconds,
  workSeconds,
  restSeconds,
  rounds,
  isPaused,
}: TimerDisplayProps) {
  switch (sectionType) {
    case "AMRAP":
      return (
        <CountdownTimer
          durationSeconds={durationSeconds ?? 600}
          isPaused={isPaused}
        />
      );
    case "STRENGTH":
      return <StopwatchTimer isPaused={isPaused} />;
    case "TABATA":
      return (
        <IntervalTimer
          sectionType="TABATA"
          workSeconds={workSeconds ?? 20}
          restSeconds={restSeconds ?? 10}
          rounds={rounds ?? 8}
          isPaused={isPaused}
        />
      );
    case "EMOM":
      return (
        <IntervalTimer
          sectionType="EMOM"
          workSeconds={workSeconds ?? 60}
          restSeconds={restSeconds ?? 0}
          rounds={rounds ?? 10}
          isPaused={isPaused}
        />
      );
    default:
      return null;
  }
}
