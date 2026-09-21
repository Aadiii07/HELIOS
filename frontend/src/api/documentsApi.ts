import { apiRequest, apiUpload, apiDownload } from "./client";

export interface DocumentMeta {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  checksumSha256: string;
  createdAt: string;
}

export interface DocumentPage {
  content: DocumentMeta[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
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
