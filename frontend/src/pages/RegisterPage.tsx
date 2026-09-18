import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router";
import * as authApi from "../api/authApi";
import type { Role } from "../api/authApi";
import { ApiRequestError } from "../api/client";
import { PasswordField } from "../components/PasswordField";

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{12,128}$/;

export default function RegisterPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("PATIENT");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  function validate(): boolean {
    const errors: Record<string, string> = {};
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = "Enter a valid email address.";
    }
    if (!PASSWORD_PATTERN.test(password)) {
      errors.password = "At least 12 characters, with an uppercase letter, a lowercase letter, and a digit.";
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!validate()) {
      return;
    }
    setSubmitting(true);
    try {
      await authApi.register({ email, password, role });
      navigate("/login?registered=1", { replace: true });
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message);
      } else {
        setError("Couldn't reach the server. Check your connection and try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-layout">
      <div className="auth-card">
        <h1>Create your account</h1>
        <p className="subtitle">Set up your HELIOS health record.</p>

        {error && <div className="form-error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            {fieldErrors.email && <div className="field-error">{fieldErrors.email}</div>}
          </div>

          <div className="field">
            <label htmlFor="password">Password</label>
            <PasswordField
              id="password"
              value={password}
              onChange={setPassword}
              autoComplete="new-password"
            />
            {fieldErrors.password ? (
              <div className="field-error">{fieldErrors.password}</div>
            ) : (
              <div className="field-hint">At least 12 characters, with an uppercase letter, a lowercase letter, and a digit.</div>
            )}
          </div>

          <div className="field">
            <label htmlFor="role">I am a</label>
            <select id="role" value={role} onChange={(e) => setRole(e.target.value as Role)}>
              <option value="PATIENT">Patient</option>
              <option value="PROVIDER">Provider</option>
            </select>
          </div>

          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? "Creating account…" : "Create account"}
          </button>
        </form>

        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
