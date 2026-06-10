import type { SectionType } from "../../types/session";

interface SectionHeaderProps {
  sectionName: string;
  sectionType: SectionType;
  /** Time cap in seconds (for AMRAP/FOR_TIME sections) */
  timeCapSeconds?: number;
  /** Format descriptor (e.g., "5 rounds for time", "20 min AMRAP") */
  formatDescriptor?: string;
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "0.25rem",
  padding: "0.75rem 1rem",
  borderBottom: "1px solid #e0e0e0",
  background: "#fafafa",
  borderRadius: "8px 8px 0 0",
};

const nameStyle: React.CSSProperties = {
  fontSize: "1.1rem",
  fontWeight: 700,
  color: "#222",
  margin: 0,
};

const metaStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.75rem",
  flexWrap: "wrap",
  alignItems: "center",
};

const typeTagStyle: React.CSSProperties = {
  fontSize: "0.7rem",
  fontWeight: 600,
  textTransform: "uppercase",
  letterSpacing: "0.04em",
  padding: "0.2rem 0.5rem",
  borderRadius: 4,
  background: "#e8eaf6",
  color: "#3949ab",
};

const timeCapStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  fontWeight: 500,
  color: "#e65100",
};

const descriptorStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  color: "#666",
  fontStyle: "italic",
};

function formatTimeCap(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  if (remainingSeconds === 0) {
    return `${minutes} min`;
  }
  return `${minutes}:${String(remainingSeconds).padStart(2, "0")}`;
}

export function SectionHeader({
  sectionName,
  sectionType,
  timeCapSeconds,
  formatDescriptor,
}: SectionHeaderProps) {
  const showTimeCap =
    timeCapSeconds !== undefined &&
    (sectionType === "AMRAP" || sectionType === "FOR_TIME");

  return (
    <header style={containerStyle} aria-label={`Section: ${sectionName}`}>
      <h2 style={nameStyle}>{sectionName}</h2>
      <div style={metaStyle}>
        <span style={typeTagStyle}>{sectionType}</span>
        {showTimeCap && (
          <span style={timeCapStyle}>⏱ {formatTimeCap(timeCapSeconds)}</span>
        )}
        {formatDescriptor && (
          <span style={descriptorStyle}>{formatDescriptor}</span>
        )}
      </div>
    </header>
  );
}
