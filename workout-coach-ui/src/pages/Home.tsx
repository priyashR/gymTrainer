import { Link } from "react-router-dom";
import { useAuth } from "../features/auth/useAuth";

const actions = [
  { label: "New Workout", to: "/new-workout" },
  { label: "My Performance", to: "/my-performance" },
  { label: "Workout", to: "/workout" },
] as const;

export default function Home() {
  const { logout } = useAuth();

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
        {actions.map(({ label, to }) => (
          <Link
            key={to}
            to={to}
            style={{
              display: "block",
              padding: "1.5rem",
              border: "1px solid #ccc",
              borderRadius: 8,
              textDecoration: "none",
              color: "inherit",
              textAlign: "center",
              fontSize: "1.125rem",
            }}
          >
            {label}
          </Link>
        ))}
      </nav>
    </main>
  );
}
