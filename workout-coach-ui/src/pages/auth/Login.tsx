import { type FormEvent, useState } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../features/auth/useAuth";
import { extractApiError } from "../../lib/authApi";
import type { FieldError } from "../../types/auth";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function Login() {
  const { login, isAuthenticated, isLoading } = useAuth();
  const location = useLocation();
  const justRegistered = (location.state as { registered?: boolean })?.registered;

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isLoading) return <div aria-busy="true">Loading…</div>;
  if (isAuthenticated) return <Navigate to="/" replace />;

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {};
    if (!email.trim()) {
      errors.email = "Email is required";
    } else if (!EMAIL_RE.test(email)) {
      errors.email = "Must be a valid email address";
    }
    if (!password) {
      errors.password = "Password is required";
    }
    return errors;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setServerError(null);

    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      await login(email, password);
      // AuthContext handles redirect via navigate on success
    } catch (err: unknown) {
      const parsed = extractApiError(err);
      if (parsed.fieldErrors) {
        const mapped: Record<string, string> = {};
        parsed.fieldErrors.forEach((fe: FieldError) => {
          mapped[fe.field] = fe.message;
        });
        setFieldErrors(mapped);
      }
      setServerError(parsed.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={{
      maxWidth: 400,
      margin: "2rem auto",
      padding: "var(--spacing-lg)",
      background: "var(--color-bg-surface)",
      borderRadius: "var(--radius-md)",
      border: "1px solid var(--color-border)",
      fontFamily: "var(--font-sans)",
    }}>
      <h1 style={{ color: "var(--color-text-primary)", marginBottom: "var(--spacing-lg)" }}>Log in</h1>

      {justRegistered && (
        <div role="status" style={{ color: "var(--color-success)", marginBottom: "var(--spacing-md)" }}>
          Registration successful. Please log in.
        </div>
      )}

      {serverError && (
        <div role="alert" style={{ color: "var(--color-error)", marginBottom: "var(--spacing-md)" }}>
          {serverError}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div style={{ marginBottom: "var(--spacing-md)" }}>
          <label htmlFor="email" style={{ color: "var(--color-text-primary)", display: "block", marginBottom: "var(--spacing-xs)" }}>Email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              setFieldErrors((prev) => ({ ...prev, email: "" }));
            }}
            aria-invalid={!!fieldErrors.email || undefined}
            aria-describedby={fieldErrors.email ? "email-error" : undefined}
            style={{
              display: "block",
              width: "100%",
              padding: "var(--spacing-sm) var(--spacing-md)",
              background: "var(--color-bg-card)",
              color: "var(--color-text-primary)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-sm)",
              fontSize: "1rem",
            }}
          />
          {fieldErrors.email && (
            <span id="email-error" style={{ color: "var(--color-error)", fontSize: "0.875rem" }}>
              {fieldErrors.email}
            </span>
          )}
        </div>

        <div style={{ marginBottom: "var(--spacing-md)" }}>
          <label htmlFor="password" style={{ color: "var(--color-text-primary)", display: "block", marginBottom: "var(--spacing-xs)" }}>Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              setFieldErrors((prev) => ({ ...prev, password: "" }));
            }}
            aria-invalid={!!fieldErrors.password || undefined}
            aria-describedby={fieldErrors.password ? "password-error" : undefined}
            style={{
              display: "block",
              width: "100%",
              padding: "var(--spacing-sm) var(--spacing-md)",
              background: "var(--color-bg-card)",
              color: "var(--color-text-primary)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-sm)",
              fontSize: "1rem",
            }}
          />
          {fieldErrors.password && (
            <span id="password-error" style={{ color: "var(--color-error)", fontSize: "0.875rem" }}>
              {fieldErrors.password}
            </span>
          )}
        </div>

        <button
          type="submit"
          disabled={submitting}
          style={{
            width: "100%",
            padding: "var(--spacing-sm) var(--spacing-md)",
            background: "var(--color-accent)",
            color: "var(--color-bg-primary)",
            border: "none",
            borderRadius: "var(--radius-sm)",
            fontSize: "1rem",
            fontWeight: 600,
            cursor: submitting ? "default" : "pointer",
            opacity: submitting ? 0.7 : 1,
            minHeight: "var(--tap-target-min)",
          }}
        >
          {submitting ? "Logging in…" : "Log in"}
        </button>
      </form>

      <p style={{ marginTop: "var(--spacing-md)", textAlign: "center", color: "var(--color-text-secondary)" }}>
        Don&apos;t have an account?{" "}
        <Link to="/register" style={{ color: "var(--color-accent)" }}>Register</Link>
      </p>
    </main>
  );
}
