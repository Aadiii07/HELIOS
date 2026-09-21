import { apiRequest } from "./client";

export interface PatientProfile {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  phoneNumber: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  preferredLanguage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PatientProfileInput {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  phoneNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  preferredLanguage?: string;
}

export function getProfile(token: string): Promise<PatientProfile> {
  return apiRequest<PatientProfile>("/patient/profile", { token });
}

export function upsertProfile(token: string, input: PatientProfileInput): Promise<PatientProfile> {
  return apiRequest<PatientProfile>("/patient/profile", { method: "PUT", body: input, token });
}
