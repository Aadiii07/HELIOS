import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router";
import { useAuth } from "../auth/AuthContext";
import { ApiRequestError } from "../api/client";
import * as authApi from "../api/authApi";
import { PasswordField } from "../components/PasswordField";

// Mirrors the backend's PasswordPolicy (the backend is still the
// authority — this only gives faster feedback before a round trip).
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{12,128}$/;
const POLICY_HINT = "At least 12 characters, with an uppercase letter, a lowercase letter, and a digit.";

export default function ChangePasswordPage() {
  const { token } = useAuth();
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!token) return;
    setError(null);

    if (!PASSWORD_PATTERN.test(newPassword)) {
      setError(POLICY_HINT);
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("The new password and its confirmation don't match.");
      return;
    }

    setSaving(true);
    try {
      await authApi.changePassword(token, { currentPassword, newPassword });
      setSuccess(true);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setTimeout(() => navigate("/dashboard"), 1200);
    } catch (err) {
      if (err instanceof ApiRequestError) {
        if (err.code === "AUTHENTICATION_FAILED") {
          // The backend deliberately returns a generic "Invalid
          // credentials" here; on this form the only thing it can mean
          // is that the current password was wrong.
          setError("Your current password is incorrect.");
        } else {
          setError(err.details.length > 0 ? err.details.join(" ") : err.message);
        }
      } else {
        setError("Couldn't reach the server. Check your connection and try again.");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="auth-layout">
      <div className="auth-card">
        <h1>Change password</h1>
        <p className="subtitle">Enter your current password, then choose a new one.</p>

        {success && <div className="form-success">Password changed.</div>}
        {error && <div className="form-error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="currentPassword">Current password</label>
            <PasswordField
              id="currentPassword"
              value={currentPassword}
              onChange={setCurrentPassword}
              autoComplete="current-password"
            />
          </div>
          <div className="field">
            <label htmlFor="newPassword">New password</label>
            <PasswordField
              id="newPassword"
              value={newPassword}
              onChange={setNewPassword}
              autoComplete="new-password"
            />
            <div className="field-hint">{POLICY_HINT}</div>
          </div>
          <div className="field">
            <label htmlFor="confirmPassword">Confirm new password</label>
            <PasswordField
              id="confirmPassword"
              value={confirmPassword}
              onChange={setConfirmPassword}
              autoComplete="new-password"
            />
          </div>
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? "Saving…" : "Change password"}
          </button>
        </form>

        <p className="auth-switch">
          <Link to="/dashboard">Back to dashboard</Link>
        </p>
      </div>
    </div>
  );
}
