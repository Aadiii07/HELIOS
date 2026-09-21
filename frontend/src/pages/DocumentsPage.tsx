import { useCallback, useEffect, useRef, useState, type ChangeEvent } from "react";
import { useAuth } from "../auth/AuthContext";
import { ApiRequestError } from "../api/client";
import * as documentsApi from "../api/documentsApi";
import type { DocumentMeta } from "../api/documentsApi";

const ACCEPTED_TYPES = ".pdf,.jpg,.jpeg,.png";

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

export default function DocumentsPage() {
  const { user, token } = useAuth();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [documents, setDocuments] = useState<DocumentMeta[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);

  const loadDocuments = useCallback(() => {
    if (!token) return;
    setLoading(true);
    documentsApi
      .listDocuments(token)
      .then((page) => setDocuments(page.content))
      .catch(() => setError("Couldn't load your documents. Try refreshing the page."))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    loadDocuments();
  }, [loadDocuments]);

  async function handleFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file || !token) return;

    setError(null);
    setUploading(true);
    try {
      await documentsApi.uploadDocument(token, file);
      loadDocuments();
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message);
      } else {
        setError("Couldn't reach the server. Check your connection and try again.");
      }
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  }

  async function handleDownload(doc: DocumentMeta) {
    if (!token) return;
    setError(null);
    try {
      const { blob, filename } = await documentsApi.downloadDocument(token, doc.id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = filename ?? doc.originalFilename;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setError("Couldn't download that file. Try again.");
    }
  }

  async function handleDelete(doc: DocumentMeta) {
    if (!token) return;
    setPendingDeleteId(doc.id);
    setError(null);
    try {
      await documentsApi.deleteDocument(token, doc.id);
      setDocuments((docs) => docs.filter((d) => d.id !== doc.id));
    } catch {
      setError("Couldn't delete that file. Try again.");
    } finally {
      setPendingDeleteId(null);
    }
  }

  if (user && user.role !== "PATIENT") {
    return (
      <div className="dashboard">
        <h1>Documents</h1>
        <p className="subtitle">The document vault is only available for patient accounts.</p>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <h1>Your documents</h1>
      <p className="subtitle">Upload lab reports and other health documents (PDF, JPG, or PNG).</p>

      {error && <div className="form-error">{error}</div>}

      <div className="field">
        <input
          ref={fileInputRef}
          type="file"
          accept={ACCEPTED_TYPES}
          onChange={handleFileSelected}
          disabled={uploading}
        />
        {uploading && <div className="field-hint">Uploading…</div>}
      </div>

      {loading ? (
        <p className="subtitle">Loading…</p>
      ) : documents.length === 0 ? (
        <p className="subtitle">No documents uploaded yet.</p>
      ) : (
        <table className="document-table">
          <thead>
            <tr>
              <th>File</th>
              <th>Type</th>
              <th>Size</th>
              <th>Uploaded</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {documents.map((doc) => (
              <tr key={doc.id}>
                <td>{doc.originalFilename}</td>
                <td>{doc.contentType}</td>
                <td>{formatSize(doc.sizeBytes)}</td>
                <td>{formatDate(doc.createdAt)}</td>
                <td className="document-actions">
                  <button className="link-button" onClick={() => handleDownload(doc)}>
                    Download
                  </button>
                  <button
                    className="link-button link-button-danger"
                    onClick={() => {
                      if (window.confirm(`Delete "${doc.originalFilename}"? This can't be undone.`)) {
                        handleDelete(doc);
                      }
                    }}
                    disabled={pendingDeleteId === doc.id}
                  >
                    {pendingDeleteId === doc.id ? "Deleting…" : "Delete"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
