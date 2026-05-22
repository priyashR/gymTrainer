import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/useAuth";
import { getActiveEnrollment, getActiveSession, startSession } from "../lib/sessionApi";
import type { EnrollmentResponse, SessionResponse } from "../types/session";

const cardStyle: React.CSSProperties = {
  display: "block",
  padding: "1.5rem",
  border: "1px solid #ccc",
  borderRadius: 8,
  textDecoration: "none",
  color: "inherit",
  textAlign: "center",
  fontSize: "1.125rem",
};

const subItemStyle: React.CSSProperties = {
  ...cardStyle,
  padding: "1rem 1.5rem",
  fontSize: "1rem",
  border: "1px solid #e0e0e0",
  borderRadius: 6,
};

const nextStepStyle: React.CSSProperties = {
  padding: "1.25rem",
  border: "2px solid #1976d2",
  borderRadius: 8,
  background: "#e3f2fd",
};

const resumeSessionStyle: React.CSSProperties = {
  padding: "1.25rem",
  border: "2px solid #f57c00",
  borderRadius: 8,
  background: "#fff3e0",
};

const actionButtonStyle: React.CSSProperties = {
  padding: "0.5rem 1.25rem",
  background: "#1976d2",
  color: "#fff",
  border: "none",
  borderRadius: 4,
  cursor: "pointer",
  fontSize: "0.95rem",
  fontWeight: 600,
};

const resumeButtonStyle: React.CSSProperties = {
  ...actionButtonStyle,
  background: "#f57c00",
};

export default function Home() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [newWorkoutOpen, setNewWorkoutOpen] = useState(false);

  // Next Step — active enrollment state
  const [enrollment, setEnrollment] = useState<EnrollmentResponse | null>(null);
  const [enrollmentLoading, setEnrollmentLoading] = useState(true);
  const [startingSession, setStartingSession] = useState(false);

  // Resume Session — active session state
  const [activeSession, setActiveSession] = useState<SessionResponse | null>(null);
  const [sessionLoading, setSessionLoading] = useState(true);

  // Fetch active enrollment and active session on mount
  useEffect(() => {
    let cancelled = false;

    getActiveEnrollment()
      .then((data) => {
        if (!cancelled) setEnrollment(data);
      })
      .catch(() => {
        // Silently ignore — no enrollment to show
      })
      .finally(() => {
        if (!cancelled) setEnrollmentLoading(false);
      });

    getActiveSession()
      .then((data) => {
        if (!cancelled) setActiveSession(data);
      })
      .catch(() => {
        // Silently ignore — no active session
      })
      .finally(() => {
        if (!cancelled) setSessionLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  // Start session from the "Next Step" indicator
  const handleStartNextSession = useCallback(async () => {
    if (!enrollment || !enrollment.nextDay) return;
    setStartingSession(true);
    try {
      const session = await startSession({
        programId: enrollment.programId,
        weekNumber: enrollment.nextDay.weekNumber,
        dayNumber: enrollment.nextDay.dayNumber,
        standalone: false,
      });
      navigate(`/workout/session/${session.id}`);
    } catch {
      // If start fails, allow retry
      setStartingSession(false);
    }
  }, [enrollment, navigate]);

  // Resume active session
  const handleResumeSession = useCallback(() => {
    if (!activeSession) return;
    navigate(`/workout/session/${activeSession.id}`);
  }, [activeSession, navigate]);

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
        {/* Resume Session — shown when an incomplete session exists */}
        {!sessionLoading && activeSession && (
          <section aria-label="Resume session" style={resumeSessionStyle}>
            <h2 style={{ margin: "0 0 0.5rem", fontSize: "1.1rem" }}>
              Session In Progress
            </h2>
            <p style={{ margin: "0 0 0.75rem", fontSize: "0.9rem", color: "#555" }}>
              You have an incomplete workout session. Pick up where you left off.
            </p>
            <button
              type="button"
              onClick={handleResumeSession}
              style={resumeButtonStyle}
            >
              Resume Session
            </button>
          </section>
        )}

        {/* Next Step — shown when an active enrollment exists with a next day */}
        {!enrollmentLoading && enrollment && enrollment.nextDay && (
          <section aria-label="Next step" style={nextStepStyle}>
            <h2 style={{ margin: "0 0 0.5rem", fontSize: "1.1rem" }}>
              Next Step
            </h2>
            <p style={{ margin: "0 0 0.25rem", fontSize: "0.95rem", fontWeight: 600 }}>
              {enrollment.programName}
            </p>
            <p style={{ margin: "0 0 0.75rem", fontSize: "0.9rem", color: "#555" }}>
              Week {enrollment.nextDay.weekNumber}, Day {enrollment.nextDay.dayNumber}
              {enrollment.nextDay.dayLabel && ` — ${enrollment.nextDay.dayLabel}`}
            </p>
            <button
              type="button"
              onClick={handleStartNextSession}
              disabled={startingSession}
              style={{
                ...actionButtonStyle,
                opacity: startingSession ? 0.7 : 1,
              }}
            >
              {startingSession ? "Starting…" : "Start"}
            </button>
          </section>
        )}

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

        {/* Search — direct link to vault search */}
        <Link to="/vault/search" style={cardStyle}>
          Search
        </Link>
      </nav>
    </main>
  );
}
