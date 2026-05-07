import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../features/auth/useAuth";

const cardStyle = {
  display: "block",
  padding: "1.5rem",
  border: "1px solid #ccc",
  borderRadius: 8,
  textDecoration: "none",
  color: "inherit",
  textAlign: "center" as const,
  fontSize: "1.125rem",
};

const subItemStyle = {
  ...cardStyle,
  padding: "1rem 1.5rem",
  fontSize: "1rem",
  border: "1px solid #e0e0e0",
  borderRadius: 6,
};

export default function Home() {
  const { logout } = useAuth();
  const [newWorkoutOpen, setNewWorkoutOpen] = useState(false);
  const [workoutOpen, setWorkoutOpen] = useState(false);

  return (
    <main style={{ maxWidth: 600, margin: "2rem auto", padding: "0 1rem" }}>
      <header
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "2rem",
        }}
      >
        <h1>HybridStrength</h1>
        <button type="button" onClick={logout}>
          Log out
        </button>
      </header>

      <nav aria-label="Primary actions" style={{ display: "grid", gap: "1rem" }}>
        {/* New Workout — expandable with sub-options */}
        <div>
          <button
            type="button"
            onClick={() => setNewWorkoutOpen((prev) => !prev)}
            style={{
              ...cardStyle,
              width: "100%",
              background: "none",
              cursor: "pointer",
            }}
            aria-expanded={newWorkoutOpen}
          >
            New Workout {newWorkoutOpen ? "▲" : "▼"}
          </button>

          {newWorkoutOpen && (
            <div
              style={{
                display: "grid",
                gap: "0.5rem",
                marginTop: "0.5rem",
                paddingLeft: "1rem",
              }}
            >
              <Link to="/new-workout" style={subItemStyle}>
                Ask Gemini
              </Link>
              <Link to="/upload" style={subItemStyle}>
                Upload Program
              </Link>
            </div>
          )}
        </div>

        {/* Other top-level actions */}
        <Link to="/my-performance" style={cardStyle}>
          My Performance
        </Link>
        {/* Workout — expandable with sub-options */}
        <div>
          <button
            type="button"
            onClick={() => setWorkoutOpen((prev) => !prev)}
            style={{
              ...cardStyle,
              width: "100%",
              background: "none",
              cursor: "pointer",
            }}
            aria-expanded={workoutOpen}
          >
            Workout {workoutOpen ? "▲" : "▼"}
          </button>

          {workoutOpen && (
            <div
              style={{
                display: "grid",
                gap: "0.5rem",
                marginTop: "0.5rem",
                paddingLeft: "1rem",
              }}
            >
              <Link to="/workout/continue" style={subItemStyle}>
                Continue with Program
              </Link>
              <Link to="/vault/search" style={subItemStyle}>
                Search for a workout or program
              </Link>
            </div>
          )}
        </div>
      </nav>
    </main>
  );
}
