import type { SectionProgress } from "../../types/session";

interface NextUpIndicatorProps {
  currentSectionIndex: number;
  sectionProgresses: SectionProgress[];
}

const containerStyle: React.CSSProperties = {
  padding: "0.5rem 1rem",
  background: "#e3f2fd",
  borderRadius: 6,
  fontSize: "0.875rem",
  color: "#1565c0",
  display: "flex",
  alignItems: "center",
  gap: "0.5rem",
};

/**
 * Computes the "next up" value:
 * - First uncompleted exercise in the current section (if any remain)
 * - Next section name (if current section is fully complete and more sections exist)
 * - Empty (if all sections are complete)
 */
function computeNextUp(
  currentSectionIndex: number,
  sectionProgresses: SectionProgress[]
): string | null {
  const currentSection = sectionProgresses[currentSectionIndex];
  if (!currentSection) return null;

  // Find first uncompleted exercise in current section
  const nextExercise = currentSection.exerciseLogs.find(
    (log) => !log.completed
  );
  if (nextExercise) {
    return nextExercise.exerciseName;
  }

  // Current section is fully complete — check for next section
  const nextSectionIndex = currentSectionIndex + 1;
  if (nextSectionIndex < sectionProgresses.length) {
    return `Next section: ${sectionProgresses[nextSectionIndex].sectionName}`;
  }

  // All sections complete
  return null;
}

export function NextUpIndicator({
  currentSectionIndex,
  sectionProgresses,
}: NextUpIndicatorProps) {
  const nextUp = computeNextUp(currentSectionIndex, sectionProgresses);

  if (!nextUp) return null;

  return (
    <div style={containerStyle} aria-label="Next up indicator">
      <span style={{ fontWeight: 600 }}>Next up:</span>
      <span>{nextUp}</span>
    </div>
  );
}
