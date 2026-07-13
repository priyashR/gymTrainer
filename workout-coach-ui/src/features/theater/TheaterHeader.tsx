import React from "react";

export interface TheaterHeaderProps {
  /** Formatted elapsed time string (e.g. "12:45" or "1:02:30") */
  elapsedTime: string;
  /** Current tier label (e.g. "Tier 1: Compound") */
  tierLabel: string;
  /** Current section type (e.g. "Strength") */
  sectionType: string;
  /** Whether the current tier is the first tier */
  isFirstTier: boolean;
  /** Whether the current tier is the last tier */
  isLastTier: boolean;
  /** Whether the session is currently paused */
  isPaused?: boolean;
  /** Callback when "Previous tier" is pressed */
  onPrevTier: () => void;
  /** Callback when "Next tier" is pressed */
  onNextTier: () => void;
  /** Callback to pause or resume the session */
  onPauseResume?: () => void;
  /** Callback to end the workout */
  onEndWorkout?: () => void;
  /** Callback to leave (navigate away without ending) */
  onLeave?: () => void;
}

const headerStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  padding: "var(--spacing-md) var(--spacing-lg)",
  background: "var(--color-bg-surface)",
  borderBottom: "1px solid var(--color-border)",
  gap: "var(--spacing-md)",
  flexWrap: "wrap",
};

const leftSectionStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "var(--spacing-lg)",
  flexWrap: "wrap",
};

const stopwatchStyle: React.CSSProperties = {
  fontSize: "96px",
  fontFamily: "var(--font-mono)",
  fontWeight: 700,
  fontVariantNumeric: "tabular-nums",
  color: "var(--color-text-primary)",
  lineHeight: 1,
  textAlign: "left",
};

const sessionControlsStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "var(--spacing-sm)",
  flexWrap: "wrap",
};

const controlButtonStyle: React.CSSProperties = {
  minWidth: "var(--tap-target-min)",
  minHeight: "var(--tap-target-min)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "var(--spacing-sm) var(--spacing-md)",
  border: "1px solid var(--color-border)",
  borderRadius: "var(--radius-sm)",
  background: "var(--color-bg-card)",
  color: "var(--color-text-primary)",
  fontSize: "0.8125rem",
  fontWeight: 600,
  cursor: "pointer",
  transition: "background 0.15s, border-color 0.15s",
  whiteSpace: "nowrap",
};

const pauseResumeButtonStyle: React.CSSProperties = {
  ...controlButtonStyle,
  color: "var(--color-warning)",
  borderColor: "var(--color-warning)",
};

const endButtonStyle: React.CSSProperties = {
  ...controlButtonStyle,
  color: "var(--color-error)",
  borderColor: "var(--color-error)",
};

const leaveButtonStyle: React.CSSProperties = {
  ...controlButtonStyle,
  color: "var(--color-text-secondary)",
};

const navContainerStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "var(--spacing-sm)",
  flexShrink: 0,
};

const tierInfoStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: "var(--spacing-xs)",
};

const tierLabelStyle: React.CSSProperties = {
  fontSize: "1.125rem",
  fontWeight: 600,
  color: "var(--color-text-primary)",
  whiteSpace: "nowrap",
};

const sectionTypeStyle: React.CSSProperties = {
  fontSize: "0.875rem",
  fontWeight: 500,
  color: "var(--color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.04em",
};

const navButtonStyle: React.CSSProperties = {
  minWidth: "var(--tap-target-preferred)",
  minHeight: "var(--tap-target-preferred)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "var(--spacing-sm) var(--spacing-md)",
  border: "1px solid var(--color-border)",
  borderRadius: "var(--radius-sm)",
  background: "var(--color-bg-card)",
  color: "var(--color-accent)",
  fontSize: "0.875rem",
  fontWeight: 600,
  cursor: "pointer",
  transition: "background 0.15s, color 0.15s",
};

const navButtonDisabledStyle: React.CSSProperties = {
  ...navButtonStyle,
  color: "var(--color-text-secondary)",
  cursor: "not-allowed",
  opacity: 0.5,
};

/**
 * Theater Mode header displaying an elapsed stopwatch on the left
 * and tier navigation (label + section type + Prev/Next buttons) on the right.
 *
 * Validates: Requirements 4.2, 4.3, 4.9
 */
export function TheaterHeader({
  elapsedTime,
  tierLabel,
  sectionType,
  isFirstTier,
  isLastTier,
  isPaused = false,
  onPrevTier,
  onNextTier,
  onPauseResume,
  onEndWorkout,
  onLeave,
}: TheaterHeaderProps) {
  return (
    <header style={headerStyle} aria-label="Theater mode header">
      {/* Left: Stopwatch + Session controls */}
      <div style={leftSectionStyle}>
        <div
          style={stopwatchStyle}
          role="timer"
          aria-label={`Elapsed time: ${elapsedTime}`}
        >
          {elapsedTime}
        </div>

        {/* Session control buttons */}
        <div style={sessionControlsStyle}>
          {onPauseResume && (
            <button
              style={pauseResumeButtonStyle}
              onClick={onPauseResume}
              aria-label={isPaused ? "Resume workout" : "Pause workout"}
              data-testid="session-pause-resume"
            >
              {isPaused ? "▶ Resume" : "⏸ Pause"}
            </button>
          )}
          {onEndWorkout && (
            <button
              style={endButtonStyle}
              onClick={onEndWorkout}
              aria-label="End workout"
              data-testid="session-end"
            >
              ⏹ End
            </button>
          )}
          {onLeave && (
            <button
              style={leaveButtonStyle}
              onClick={onLeave}
              aria-label="Leave workout"
              data-testid="session-leave"
            >
              ← Leave
            </button>
          )}
        </div>
      </div>

      {/* Right: Tier navigation */}
      <nav style={navContainerStyle} aria-label="Tier navigation">
        <button
          style={isFirstTier ? navButtonDisabledStyle : navButtonStyle}
          onClick={onPrevTier}
          disabled={isFirstTier}
          aria-label="Previous tier"
        >
          ◀ Prev
        </button>

        <div style={tierInfoStyle}>
          <span style={tierLabelStyle}>{tierLabel}</span>
          <span style={sectionTypeStyle}>{sectionType}</span>
        </div>

        <button
          style={isLastTier ? navButtonDisabledStyle : navButtonStyle}
          onClick={onNextTier}
          disabled={isLastTier}
          aria-label="Next tier"
        >
          Next ▶
        </button>
      </nav>
    </header>
  );
}
