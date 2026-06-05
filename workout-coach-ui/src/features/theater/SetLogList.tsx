import type { SetLog } from "../../types/session";

interface SetLogListProps {
  setLogs: SetLog[];
}

const listStyle: React.CSSProperties = {
  listStyle: "none",
  padding: 0,
  margin: "0.25rem 0 0 0",
  display: "flex",
  flexDirection: "column",
  gap: "0.25rem",
};

const itemStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.5rem",
  alignItems: "center",
  fontSize: "0.8rem",
  color: "#444",
  padding: "0.25rem 0.5rem",
  background: "#f5f5f5",
  borderRadius: 4,
};

const setNumberStyle: React.CSSProperties = {
  fontWeight: 700,
  color: "#1976d2",
  minWidth: 28,
};

const valueStyle: React.CSSProperties = {
  color: "#333",
};

const rpeStyle: React.CSSProperties = {
  color: "#888",
  fontSize: "0.75rem",
};

const emptyStyle: React.CSSProperties = {
  fontSize: "0.75rem",
  color: "#999",
  fontStyle: "italic",
  padding: "0.25rem 0",
};

export function SetLogList({ setLogs }: SetLogListProps) {
  if (setLogs.length === 0) {
    return <p style={emptyStyle}>No sets logged yet</p>;
  }

  return (
    <ul style={listStyle} aria-label="Logged sets">
      {setLogs.map((log) => (
        <li key={log.setNumber} style={itemStyle}>
          <span style={setNumberStyle}>#{log.setNumber}</span>
          <span style={valueStyle}>
            {log.weight}kg × {log.repetitions}
          </span>
          {log.rpe !== null && (
            <span style={rpeStyle}>RPE {log.rpe}</span>
          )}
        </li>
      ))}
    </ul>
  );
}
