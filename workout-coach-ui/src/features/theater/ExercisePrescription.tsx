interface ExercisePrescriptionProps {
  name: string;
  sets?: number;
  reps?: number | string;
  weight?: number | string;
  notes?: string;
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "0.2rem",
  padding: "0.4rem 0",
};

const nameStyle: React.CSSProperties = {
  fontSize: "0.875rem",
  fontWeight: 600,
  color: "#333",
};

const prescriptionStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.5rem",
  flexWrap: "wrap",
  fontSize: "0.8rem",
  color: "#555",
};

const badgeStyle: React.CSSProperties = {
  padding: "0.15rem 0.4rem",
  background: "#e3f2fd",
  borderRadius: 4,
  fontSize: "0.75rem",
  fontWeight: 500,
  color: "#1565c0",
};

const notesStyle: React.CSSProperties = {
  fontSize: "0.75rem",
  color: "#777",
  fontStyle: "italic",
  marginTop: "0.1rem",
};

export function ExercisePrescription({
  name,
  sets,
  reps,
  weight,
  notes,
}: ExercisePrescriptionProps) {
  const hasPrescription = sets !== undefined || reps !== undefined || weight !== undefined;

  return (
    <div style={containerStyle}>
      <span style={nameStyle}>{name}</span>

      {hasPrescription && (
        <div style={prescriptionStyle}>
          {sets !== undefined && (
            <span style={badgeStyle}>{sets} sets</span>
          )}
          {reps !== undefined && (
            <span style={badgeStyle}>{reps} reps</span>
          )}
          {weight !== undefined && (
            <span style={badgeStyle}>{weight} kg</span>
          )}
        </div>
      )}

      {notes && <p style={notesStyle}>{notes}</p>}
    </div>
  );
}
