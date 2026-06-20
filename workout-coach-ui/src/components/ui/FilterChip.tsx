import React from "react";

interface FilterChipProps {
  label: string;
  options: string[];
  value: string;
  onChange: (value: string) => void;
  active: boolean;
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: "inline-flex",
    alignItems: "center",
    position: "relative",
  },
  select: {
    appearance: "none",
    background: "var(--color-bg-surface)",
    color: "var(--color-text-primary)",
    border: "1px solid var(--color-border)",
    borderRadius: "9999px",
    padding: "var(--spacing-sm) var(--spacing-md)",
    paddingRight: "var(--spacing-xl)",
    fontSize: "14px",
    fontFamily: "inherit",
    cursor: "pointer",
    minHeight: "var(--tap-target-min)",
    outline: "none",
    transition: "border-color 0.15s ease",
  },
  selectActive: {
    borderColor: "var(--color-accent)",
    background: "var(--color-bg-surface)",
  },
  arrow: {
    position: "absolute",
    right: "12px",
    top: "50%",
    transform: "translateY(-50%)",
    pointerEvents: "none",
    fontSize: "12px",
    color: "var(--color-text-secondary)",
  },
};

export const FilterChip: React.FC<FilterChipProps> = ({
  label,
  options,
  value,
  onChange,
  active,
}) => {
  return (
    <div style={styles.container}>
      <select
        style={{
          ...styles.select,
          ...(active ? styles.selectActive : {}),
        }}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={label}
      >
        <option value="">{label}</option>
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
      <span style={styles.arrow as React.CSSProperties} aria-hidden="true">
        ▾
      </span>
    </div>
  );
};

export default FilterChip;
