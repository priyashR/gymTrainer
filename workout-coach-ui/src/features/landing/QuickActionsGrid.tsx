import React from "react";
import { useNavigate } from "react-router-dom";

interface QuickAction {
  id: string;
  icon: string;
  label: string;
  route: string;
  state?: Record<string, unknown>;
}

const QUICK_ACTIONS: QuickAction[] = [
  { id: "upload-json", icon: "📄", label: "Upload JSON", route: "/upload" },
  { id: "ai-gen", icon: "🤖", label: "AI Gen", route: "/new-workout" },
  { id: "manage-programs", icon: "📋", label: "Manage Programs", route: "/vault/search" },
  { id: "upload-pic", icon: "📷", label: "Upload Pic", route: "/coming-soon/upload-pic", state: { title: "Upload Pic" } },
];

const styles: Record<string, React.CSSProperties> = {
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(2, 1fr)",
    gap: "var(--spacing-md)",
  },
  item: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    gap: "var(--spacing-sm)",
    padding: "var(--spacing-lg)",
    background: "var(--color-bg-card)",
    borderRadius: "var(--radius-md)",
    border: "1px solid var(--color-border)",
    cursor: "pointer",
    minHeight: "var(--tap-target-preferred)",
    transition: "background 0.15s ease, border-color 0.15s ease",
  },
  icon: {
    fontSize: "28px",
    lineHeight: 1,
  },
  label: {
    fontSize: "14px",
    fontWeight: 500,
    color: "var(--color-text-primary)",
    textAlign: "center",
    margin: 0,
  },
};

export const QuickActionsGrid: React.FC = () => {
  const navigate = useNavigate();

  const handleAction = (action: QuickAction) => {
    navigate(action.route, { state: action.state });
  };

  return (
    <nav aria-label="Quick actions" style={styles.grid} data-testid="quick-actions-grid">
      {QUICK_ACTIONS.map((action) => (
        <button
          key={action.id}
          type="button"
          style={styles.item}
          onClick={() => handleAction(action)}
          aria-label={action.label}
          data-testid={`quick-action-${action.id}`}
        >
          <span style={styles.icon} aria-hidden="true">
            {action.icon}
          </span>
          <span style={styles.label}>{action.label}</span>
        </button>
      ))}
    </nav>
  );
};

export default QuickActionsGrid;
