import { useNavigate } from "react-router";
import { useAuth } from "../auth/AuthContext";

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="dashboard">
      <h1>Welcome</h1>
      <p className="subtitle">
        This is a placeholder landing page confirming your session is authenticated — no health
        data features exist yet (see project roadmap).
      </p>

      <div className="info-row">
        <span className="label">Email</span>
        <span>{user?.email}</span>
      </div>
      <div className="info-row">
        <span className="label">Role</span>
        <span>{user?.role}</span>
      </div>

      <div style={{ marginTop: 24 }}>
        <button className="btn-secondary" onClick={handleLogout}>
          Sign out
        </button>
      </div>
    </div>
  );
}
