import type { SectionProgress } from "../../types/session";

interface SectionNavigatorProps {
  currentSectionIndex: number;
  sectionProgresses: SectionProgress[];
  onAdvanceSection: (targetIndex: number) => Promise<void>;
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  padding: "0.75rem 1rem",
  borderBottom: "1px solid #e0e0e0",
  background: "#fafafa",
};

const buttonStyle: React.CSSProperties = {
  padding: "0.5rem 1rem",
  border: "1px solid #ccc",
  borderRadius: 6,
  background: "#fff",
  cursor: "pointer",
  fontSize: "0.875rem",
};

const disabledButtonStyle: React.CSSProperties = {
  ...buttonStyle,
  opacity: 0.4,
  cursor: "not-allowed",
};

const infoStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: "0.25rem",
};

export function SectionNavigator({
  currentSectionIndex,
  sectionProgresses,
  onAdvanceSection,
}: SectionNavigatorProps) {
  const totalSections = sectionProgresses.length;
  const currentSection = sectionProgresses[currentSectionIndex];
  const isFirst = currentSectionIndex === 0;
  const isLast = currentSectionIndex === totalSections - 1;

  return (
    <nav aria-label="Section navigation" style={containerStyle}>
      <button
        type="button"
        style={isFirst ? disabledButtonStyle : buttonStyle}
        disabled={isFirst}
        aria-label="Previous section"
        onClick={() => onAdvanceSection(currentSectionIndex - 1)}
      >
        ← Previous
      </button>

      <div style={infoStyle}>
        <span style={{ fontWeight: 600, fontSize: "1rem" }}>
          {currentSection?.sectionName ?? "Section"}
        </span>
        <span style={{ fontSize: "0.8rem", color: "#666" }}>
          Section {currentSectionIndex + 1} of {totalSections}
        </span>
      </div>

      <button
        type="button"
        style={isLast ? disabledButtonStyle : buttonStyle}
        disabled={isLast}
        aria-label="Next section"
        onClick={() => onAdvanceSection(currentSectionIndex + 1)}
      >
        Next →
      </button>
    </nav>
  );
}
