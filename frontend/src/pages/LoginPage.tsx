import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { useAuth } from "../auth/AuthContext";
import { ApiRequestError } from "../api/client";
import { PasswordField } from "../components/PasswordField";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const justRegistered = searchParams.get("registered") === "1";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      navigate("/dashboard", { replace: true });
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
        <h1>Sign in</h1>
        <p className="subtitle">Access your HELIOS health record.</p>

        {justRegistered && !error && (
          <div className="form-success">Account created. Sign in below.</div>
        )}
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
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <PasswordField
              id="password"
              value={password}
              onChange={setPassword}
              autoComplete="current-password"
            />
          </div>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? "Signing in…" : "Sign in"}
          </button>
        </form>

        <p className="auth-switch">
          New to HELIOS? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  );
}
