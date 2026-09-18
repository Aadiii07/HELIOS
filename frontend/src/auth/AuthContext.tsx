import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import * as authApi from "../api/authApi";
import type { CurrentUser, Role } from "../api/authApi";

const TOKEN_STORAGE_KEY = "helios.accessToken";

// Trade-off, documented: the access token is kept in localStorage so it
// survives a page reload and is easy to attach as an Authorization
// header (matching the backend's stateless Bearer-token design). This
// is readable by any script on the page, so it's vulnerable if the app
// ever has an XSS bug elsewhere. An httpOnly-cookie-based session would
// avoid that but needs CSRF handling and a cookie-issuing backend
// endpoint — not built here (P1 backlog if this needs hardening).

interface AuthState {
  user: CurrentUser | null;
  token: string | null;
  isLoading: boolean;
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, role: Role) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ user: null, token: null, isLoading: true });

  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!storedToken) {
      setState({ user: null, token: null, isLoading: false });
      return;
    }
    authApi
      .me(storedToken)
      .then((user) => setState({ user, token: storedToken, isLoading: false }))
      .catch(() => {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        setState({ user: null, token: null, isLoading: false });
      });
  }, []);

  async function login(email: string, password: string) {
    const response = await authApi.login({ email, password });
    localStorage.setItem(TOKEN_STORAGE_KEY, response.accessToken);
    const user = await authApi.me(response.accessToken);
    setState({ user, token: response.accessToken, isLoading: false });
  }

  async function register(email: string, password: string, role: Role) {
    await authApi.register({ email, password, role });
    // Registration does not log the person in (see master spec MVP
    // workflow: register then login are separate steps) — the caller
    // navigates to the login page after this resolves.
  }

  async function logout() {
    if (state.token) {
      try {
        await authApi.logout(state.token);
      } catch {
        // Even if the server call fails, still clear the local session —
        // there's nothing server-side to revoke for a stateless JWT
        // (see backend ADR-006), so failing to reach the server is not
        // a reason to keep the person logged in on this device.
      }
    }
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    setState({ user: null, token: null, isLoading: false });
  }

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
