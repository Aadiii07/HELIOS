import { apiRequest } from "./client";

export type Role = "PATIENT" | "PROVIDER";

export interface RegisterRequest {
  email: string;
  password: string;
  role: Role;
}

export interface UserSummary {
  id: string;
  email: string;
  role: Role;
  accountStatus: string;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  role: Role;
}

export interface CurrentUser {
  id: string;
  email: string;
  role: Role;
}

export function register(request: RegisterRequest): Promise<UserSummary> {
  return apiRequest<UserSummary>("/auth/register", { method: "POST", body: request });
}

export function login(request: LoginRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/auth/login", { method: "POST", body: request });
}

export function logout(token: string): Promise<void> {
  return apiRequest<void>("/auth/logout", { method: "POST", token });
}

export function me(token: string): Promise<CurrentUser> {
  return apiRequest<CurrentUser>("/auth/me", { token });
}
