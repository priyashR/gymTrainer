import { type FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../../features/auth/useAuth";
import { extractApiError } from "../../lib/authApi";
import type { FieldError } from "../../types/auth";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 8;

export default function Register() {
  const { isAuthenticated, isLoading, register } = useAuth();
  const navigate = useNavigate();

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
    } else if (password.length < MIN_PASSWORD_LENGTH) {
      errors.password = `Password must be at least ${MIN_PASSWORD_LENGTH} characters`;
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
      await register(email, password);
      navigate("/login", { state: { registered: true } });
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
    <main style={{ maxWidth: 400, margin: "2rem auto", padding: "0 1rem" }}>
      <h1>Create an account</h1>

      {serverError && (
        <div role="alert" style={{ color: "crimson", marginBottom: "1rem" }}>
          {serverError}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div style={{ marginBottom: "1rem" }}>
          <label htmlFor="email">Email</label>
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
            style={{ display: "block", width: "100%" }}
          />
          {fieldErrors.email && (
            <span id="email-error" style={{ color: "crimson", fontSize: "0.875rem" }}>
              {fieldErrors.email}
            </span>
          )}
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <label htmlFor="password">Password</label>
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
            style={{ display: "block", width: "100%" }}
          />
          {fieldErrors.password && (
            <span id="password-error" style={{ color: "crimson", fontSize: "0.875rem" }}>
              {fieldErrors.password}
            </span>
          )}
        </div>

        <button type="submit" disabled={submitting} style={{ width: "100%" }}>
          {submitting ? "Creating account…" : "Register"}
        </button>
      </form>

      <p style={{ marginTop: "1rem", textAlign: "center" }}>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </main>
  );
}
