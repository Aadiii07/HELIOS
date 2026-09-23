import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { useAuth } from "../auth/AuthContext";
import * as documentsApi from "../api/documentsApi";
import type { CandidateMeta } from "../api/documentsApi";

function confidenceLabel(c: CandidateMeta["confidence"]): string {
  return c === "HIGH" ? "High confidence" : c === "MEDIUM" ? "Medium confidence" : "Low confidence";
}

interface CorrectionDraft {
  fieldLabel: string;
  rawValue: string;
  unit: string;
}

export default function CandidateReviewPage() {
  const { id: documentId } = useParams<{ id: string }>();
  const { user, token } = useAuth();

  const [candidates, setCandidates] = useState<CandidateMeta[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [correcting, setCorrecting] = useState<Record<string, CorrectionDraft>>({});

  useEffect(() => {
    if (!token || !documentId) return;
    documentsApi
      .listCandidates(token, documentId)
      .then(setCandidates)
      .catch(() => setError("Couldn't load extracted data for this document."))
      .finally(() => setLoading(false));
  }, [token, documentId]);

  function startCorrecting(candidate: CandidateMeta) {
    setCorrecting((c) => ({
      ...c,
      [candidate.id]: {
        fieldLabel: candidate.fieldLabel,
        rawValue: candidate.rawValue,
        unit: candidate.unit ?? "",
      },
    }));
  }

  function cancelCorrecting(candidateId: string) {
    setCorrecting((c) => {
      const next = { ...c };
      delete next[candidateId];
      return next;
    });
  }

  async function review(
    candidate: CandidateMeta,
    reviewStatus: "CONFIRMED" | "REJECTED" | "CORRECTED",
    draft?: CorrectionDraft
  ) {
    if (!token || !documentId) return;
    setBusyId(candidate.id);
    setError(null);
    try {
      const updated = await documentsApi.reviewCandidate(token, documentId, candidate.id, {
        reviewStatus,
        correctedFieldLabel: draft?.fieldLabel,
        correctedRawValue: draft?.rawValue,
        correctedUnit: draft?.unit,
      });
      setCandidates((list) => list.map((c) => (c.id === updated.id ? updated : c)));
      cancelCorrecting(candidate.id);
    } catch {
      setError("Couldn't save your review. Try again.");
    } finally {
      setBusyId(null);
    }
  }

  if (user && user.role !== "PATIENT") {
    return (
      <div className="dashboard">
        <h1>Review extracted data</h1>
        <p className="subtitle">This is only available for patient accounts.</p>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <h1>Review extracted data</h1>
      <p className="subtitle">
        This information was extracted automatically and hasn't been verified. Confirm, correct, or
        reject each item below. <Link to="/documents">Back to documents</Link>
      </p>

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <p className="subtitle">Loading…</p>
      ) : candidates.length === 0 ? (
        <p className="subtitle">No data was found to extract from this document.</p>
      ) : (
        candidates.map((candidate) => {
          const draft = correcting[candidate.id];
          return (
            <div key={candidate.id} className="candidate-card">
              <div className="candidate-card-header">
                <strong>{candidate.fieldLabel}</strong>
                <span className={`confidence-badge confidence-badge-${candidate.confidence.toLowerCase()}`}>
                  {confidenceLabel(candidate.confidence)}
                </span>
              </div>

              <div className="candidate-card-value">
                {candidate.rawValue}
                {candidate.unit ? ` ${candidate.unit}` : ""}
                {candidate.referenceRange ? (
                  <span className="candidate-range"> (reference: {candidate.referenceRange})</span>
                ) : null}
              </div>

              <div className="candidate-source">Source line: “{candidate.sourceExcerpt}”</div>

              {candidate.reviewStatus === "PENDING" && !draft && (
                <div className="candidate-actions">
                  <button
                    className="link-button"
                    onClick={() => review(candidate, "CONFIRMED")}
                    disabled={busyId === candidate.id}
                  >
                    Confirm
                  </button>
                  <button className="link-button" onClick={() => startCorrecting(candidate)}>
                    Correct
                  </button>
                  <button
                    className="link-button link-button-danger"
                    onClick={() => review(candidate, "REJECTED")}
                    disabled={busyId === candidate.id}
                  >
                    Reject
                  </button>
                </div>
              )}

              {draft && (
                <div className="candidate-correction-form">
                  <div className="field">
                    <label>Field name</label>
                    <input
                      value={draft.fieldLabel}
                      onChange={(e) =>
                        setCorrecting((c) => ({ ...c, [candidate.id]: { ...draft, fieldLabel: e.target.value } }))
                      }
                    />
                  </div>
                  <div className="field">
                    <label>Value</label>
                    <input
                      value={draft.rawValue}
                      onChange={(e) =>
                        setCorrecting((c) => ({ ...c, [candidate.id]: { ...draft, rawValue: e.target.value } }))
                      }
                    />
                  </div>
                  <div className="field">
                    <label>Unit</label>
                    <input
                      value={draft.unit}
                      onChange={(e) =>
                        setCorrecting((c) => ({ ...c, [candidate.id]: { ...draft, unit: e.target.value } }))
                      }
                    />
                  </div>
                  <div className="candidate-actions">
                    <button
                      className="link-button"
                      onClick={() => review(candidate, "CORRECTED", draft)}
                      disabled={busyId === candidate.id}
                    >
                      Save correction
                    </button>
                    <button className="link-button" onClick={() => cancelCorrecting(candidate.id)}>
                      Cancel
                    </button>
                  </div>
                </div>
              )}

              {candidate.reviewStatus !== "PENDING" && (
                <div className={`review-status review-status-${candidate.reviewStatus.toLowerCase()}`}>
                  {candidate.reviewStatus === "CONFIRMED" && "Confirmed"}
                  {candidate.reviewStatus === "REJECTED" && "Rejected"}
                  {candidate.reviewStatus === "CORRECTED" &&
                    `Corrected to: ${candidate.correctedFieldLabel} — ${candidate.correctedRawValue}${
                      candidate.correctedUnit ? ` ${candidate.correctedUnit}` : ""
                    }`}
                </div>
              )}
            </div>
          );
        })
      )}
    </div>
  );
}
