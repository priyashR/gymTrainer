import React from "react";

interface EmptyStateProps {
  icon?: string;
  title: string;
  subtitle?: string;
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    padding: "var(--spacing-xl)",
    textAlign: "center",
    minHeight: "160px",
  },
  icon: {
    fontSize: "48px",
    marginBottom: "var(--spacing-md)",
  },
  title: {
    fontSize: "18px",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    margin: "0 0 var(--spacing-sm) 0",
  },
  subtitle: {
    fontSize: "14px",
    color: "var(--color-text-secondary)",
    margin: 0,
  },
};

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  subtitle,
}) => {
  return (
    <div style={styles.container} role="status" aria-label={title}>
      {icon && (
        <span style={styles.icon} aria-hidden="true">
          {icon}
        </span>
      )}
      <h3 style={styles.title}>{title}</h3>
      {subtitle && <p style={styles.subtitle}>{subtitle}</p>}
    </div>
  );
};

export default EmptyState;
