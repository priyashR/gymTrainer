interface RecommendationBadgeProps {
  prescribedWeight: string | null;
  prescribedReps: string | null;
  prescribedSets: number | null;
  isLoading: boolean;
}

const badgeContainerStyle: React.CSSProperties = {
  display: "inline-flex",
  alignItems: "center",
  padding: "0.2rem 0.5rem",
  background: "#e8f5e9",
  borderRadius: 4,
  fontSize: "0.75rem",
  fontWeight: 500,
  color: "#2e7d32",
  marginBottom: "0.25rem",
  pointerEvents: "none",
  userSelect: "none",
};

const skeletonStyle: React.CSSProperties = {
  display: "inline-block",
  width: 80,
  height: 16,
  borderRadius: 4,
  background: "linear-gradient(90deg, #e0e0e0 25%, #f5f5f5 50%, #e0e0e0 75%)",
  backgroundSize: "200% 100%",
  animation: "shimmer 1.5s infinite",
  marginBottom: "0.25rem",
};

export function RecommendationBadge({
  prescribedWeight,
  prescribedReps,
  prescribedSets,
  isLoading,
}: RecommendationBadgeProps) {
  if (isLoading) {
    return (
      <span
        style={skeletonStyle}
        role="status"
        aria-label="Loading recommendation"
      />
    );
  }

  const allNull =
    prescribedWeight === null &&
    prescribedReps === null &&
    prescribedSets === null;

  if (allNull) {
    return null;
  }

  const parts: string[] = [];
  if (prescribedWeight !== null) {
    parts.push(prescribedWeight);
  }
  if (prescribedReps !== null) {
    parts.push(prescribedReps);
  }
  if (prescribedSets !== null) {
    parts.push(String(prescribedSets));
  }

  const displayText = parts.join(" · ");

  return (
    <span
      style={badgeContainerStyle}
      aria-label="Recommendation"
      data-testid="recommendation-badge"
    >
      {displayText}
    </span>
  );
}
