import React from "react";
import type { VaultItem } from "../../types/vault";
import { ProgramCard } from "./ProgramCard";

interface ResultsGridProps {
  programs: VaultItem[];
  onCardClick: (programId: string) => void;
}

const styles: Record<string, React.CSSProperties> = {
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
    gap: "var(--spacing-md)",
    width: "100%",
  },
};

export const ResultsGrid: React.FC<ResultsGridProps> = ({
  programs,
  onCardClick,
}) => {
  return (
    <section aria-label="Search results" style={styles.grid}>
      {programs.map((program) => (
        <ProgramCard
          key={program.id}
          program={program}
          onClick={() => onCardClick(program.id)}
        />
      ))}
    </section>
  );
};

export default ResultsGrid;
