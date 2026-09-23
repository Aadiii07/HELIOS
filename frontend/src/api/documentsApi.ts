import { apiRequest, apiUpload, apiDownload } from "./client";

export type ExtractionStatus = "NOT_ATTEMPTED" | "PROCESSED" | "UNSUPPORTED_FORMAT" | "FAILED";
export type Confidence = "HIGH" | "MEDIUM" | "LOW";
export type ReviewStatus = "PENDING" | "CONFIRMED" | "REJECTED" | "CORRECTED";

export interface DocumentMeta {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  checksumSha256: string;
  extractionStatus: ExtractionStatus;
  createdAt: string;
}

export interface DocumentPage {
  content: DocumentMeta[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CandidateMeta {
  id: string;
  documentId: string;
  fieldLabel: string;
  rawValue: string;
  unit: string | null;
  referenceRange: string | null;
  sourceExcerpt: string;
  confidence: Confidence;
  extractionMethod: string;
  reviewStatus: ReviewStatus;
  correctedFieldLabel: string | null;
  correctedRawValue: string | null;
  correctedUnit: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

export interface CandidateReviewInput {
  reviewStatus: "CONFIRMED" | "REJECTED" | "CORRECTED";
  correctedFieldLabel?: string;
  correctedRawValue?: string;
  correctedUnit?: string;
}

export function listDocuments(token: string, page = 0, size = 20): Promise<DocumentPage> {
  return apiRequest<DocumentPage>(`/documents?page=${page}&size=${size}`, { token });
}

export function uploadDocument(token: string, file: File): Promise<DocumentMeta> {
  const formData = new FormData();
  formData.append("file", file);
  return apiUpload<DocumentMeta>("/documents", formData, token);
}

export function downloadDocument(token: string, id: string) {
  return apiDownload(`/documents/${id}/content`, token);
}

export function deleteDocument(token: string, id: string): Promise<void> {
  return apiRequest<void>(`/documents/${id}`, { method: "DELETE", token });
}

export function listCandidates(token: string, documentId: string): Promise<CandidateMeta[]> {
  return apiRequest<CandidateMeta[]>(`/documents/${documentId}/candidates`, { token });
}

export function reviewCandidate(
  token: string,
  documentId: string,
  candidateId: string,
  input: CandidateReviewInput
): Promise<CandidateMeta> {
  return apiRequest<CandidateMeta>(`/documents/${documentId}/candidates/${candidateId}`, {
    method: "PUT",
    body: input,
    token,
  });
}
