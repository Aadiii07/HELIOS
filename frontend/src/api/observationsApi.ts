import { apiRequest } from "./client";

export type ObservationSource = "DOCUMENT_EXTRACTION" | "MANUAL_ENTRY";
export type Confidence = "HIGH" | "MEDIUM" | "LOW";

export interface Observation {
  id: string;
  code: string;
  displayName: string;
  rawValue: string;
  numericValue: number | null;
  unit: string | null;
  referenceRange: string | null;
  effectiveDate: string;
  source: ObservationSource;
  sourceDocumentId: string | null;
  sourceCandidateId: string | null;
  confidence: Confidence;
  status: string;
  createdAt: string;
}

export interface ObservationPage {
  content: Observation[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ManualObservationInput {
  displayName: string;
  value: string;
  unit?: string;
  referenceRange?: string;
  effectiveDate: string;
}

export function listObservations(token: string, page = 0, size = 20, code?: string): Promise<ObservationPage> {
  const codeParam = code ? `&code=${encodeURIComponent(code)}` : "";
  return apiRequest<ObservationPage>(`/observations?page=${page}&size=${size}${codeParam}`, { token });
}

export function createObservation(token: string, input: ManualObservationInput): Promise<Observation> {
  return apiRequest<Observation>("/observations", { method: "POST", body: input, token });
}
