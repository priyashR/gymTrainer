import React from "react";

export interface AMRAPInfoCardProps {
  /** Time cap display value, e.g. "12:00" */
  timeCap: string;
  /** Description of the format, e.g. "12 min time cap" */
  description: string;
}

const cardStyle: React.CSSProperties = {
  background: "rgba(255, 112, 67, 0.1)",
  border: "1px solid var(--color-crossfit)",
  borderRadius: "var(--radius-md)",
  padding: "var(--spacing-md)",
};

const timeCapStyle: React.CSSProperties = {
  fontSize: "18px",
  fontWeight: 700,
  color: "var(--color-crossfit)",
  marginBottom: "var(--spacing-xs)",
};

const descriptionStyle: React.CSSProperties = {
  fontSize: "14px",
  fontWeight: 400,
  color: "var(--color-text-secondary)",
};

/**
 * Orange accent info card displayed at the top of the exercise list
 * for AMRAP/ForTime section types. Shows the time cap prominently
 * with a format description below.
 *
 * Validates: Requirements 4.4
 */
export function AMRAPInfoCard({ timeCap, description }: AMRAPInfoCardProps) {
  return (
    <div
      style={cardStyle}
      role="region"
      aria-label={`AMRAP information: ${timeCap}`}
      data-testid="amrap-info-card"
    >
      <div style={timeCapStyle}>{timeCap}</div>
      <div style={descriptionStyle}>{description}</div>
    </div>
  );
}
