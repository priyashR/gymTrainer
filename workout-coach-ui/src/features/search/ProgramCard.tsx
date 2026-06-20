import React from "react";
import type { VaultItem } from "../../types/vault";

interface ProgramCardProps {
  program: VaultItem;
  onClick?: () => void;
}

const styles: Record<string, React.CSSProperties> = {
  card: {
    background: "var(--color-bg-surface)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    padding: "var(--spacing-md)",
    cursor: "pointer",
    transition: "border-color 0.2s ease, transform 0.15s ease",
    display: "flex",
    flexDirection: "column",
    gap: "var(--spacing-sm)",
  },
  name: {
    fontSize: "1rem",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    margin: 0,
    lineHeight: 1.3,
  },
  goal: {
    fontSize: "0.875rem",
    color: "var(--color-text-secondary)",
    margin: 0,
    lineHeight: 1.4,
  },
  metadataRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: "var(--spacing-sm)",
    marginTop: "var(--spacing-xs)",
  },
  metadataBadge: {
    fontSize: "0.75rem",
    color: "var(--color-text-secondary)",
    background: "var(--color-bg-card)",
    borderRadius: "var(--radius-sm)",
    padding: "2px 8px",
  },
  tagsRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: "var(--spacing-xs)",
    marginTop: "var(--spacing-xs)",
  },
  tag: {
    fontSize: "0.7rem",
    color: "var(--color-accent)",
    border: "1px solid var(--color-accent)",
    borderRadius: "var(--radius-sm)",
    padding: "2px 6px",
    textTransform: "lowercase" as const,
  },
};

export const ProgramCard: React.FC<ProgramCardProps> = ({
  program,
  onClick,
}) => {
  const [hovered, setHovered] = React.useState(false);

  const cardStyle: React.CSSProperties = {
    ...styles.card,
    ...(hovered
      ? {
          borderColor: "var(--color-accent)",
          transform: "translateY(-2px)",
        }
      : {}),
  };

  return (
    <article
      role="button"
      tabIndex={0}
      aria-label={`View program: ${program.name}`}
      style={cardStyle}
      onClick={onClick}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onClick?.();
        }
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocus={() => setHovered(true)}
      onBlur={() => setHovered(false)}
    >
      <h3 style={styles.name}>{program.name}</h3>

      {program.goal && <p style={styles.goal}>{program.goal}</p>}

      <div style={styles.metadataRow}>
        <span style={styles.metadataBadge}>
          {program.durationWeeks} {program.durationWeeks === 1 ? "week" : "weeks"}
        </span>
        <span style={styles.metadataBadge}>{program.contentSource.replace("_", " ").toLowerCase()}</span>
      </div>

      {program.equipmentProfile.length > 0 && (
        <div style={styles.tagsRow}>
          {program.equipmentProfile.map((tag) => (
            <span key={tag} style={styles.tag}>
              {tag}
            </span>
          ))}
        </div>
      )}
    </article>
  );
};

export default ProgramCard;
