import React from "react";

interface FABButtonProps {
  onClick: () => void;
  icon?: string;
  ariaLabel: string;
}

const styles: Record<string, React.CSSProperties> = {
  button: {
    position: "fixed",
    bottom: "var(--spacing-lg)",
    right: "var(--spacing-lg)",
    width: "var(--fab-size)",
    height: "var(--fab-size)",
    borderRadius: "var(--radius-full)",
    background: "var(--color-fab-bg)",
    color: "var(--color-fab-text)",
    border: "none",
    fontSize: "24px",
    fontWeight: 700,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    cursor: "pointer",
    boxShadow: "0 4px 12px rgba(0, 0, 0, 0.4)",
    transition: "transform 0.15s ease, box-shadow 0.15s ease",
    zIndex: 1000,
    minWidth: "var(--fab-size)",
    minHeight: "var(--fab-size)",
  },
};

export const FABButton: React.FC<FABButtonProps> = ({
  onClick,
  icon = "+",
  ariaLabel,
}) => {
  return (
    <button
      style={styles.button}
      onClick={onClick}
      aria-label={ariaLabel}
      type="button"
    >
      {icon}
    </button>
  );
};

export default FABButton;
