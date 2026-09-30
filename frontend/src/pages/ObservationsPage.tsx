import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";
import { ApiRequestError } from "../api/client";
import * as observationsApi from "../api/observationsApi";
import type { Observation } from "../api/observationsApi";

function formatDate(iso: string): string {
  return new Date(iso + "T00:00:00").toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function sourceLabel(source: Observation["source"]): string {
  return source === "DOCUMENT_EXTRACTION" ? "From document" : "Manual entry";
}

const EMPTY_FORM = { displayName: "", value: "", unit: "", referenceRange: "", effectiveDate: "" };

export default function ObservationsPage() {
  const { user, token } = useAuth();

  const [observations, setObservations] = useState<Observation[]>([]);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);
  const [codeFilter, setCodeFilter] = useState("");

  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const todayIso = new Date().toISOString().slice(0, 10);

  const loadObservations = useCallback(() => {
    if (!token) return;
    setLoading(true);
    observationsApi
      .listObservations(token, 0, 50, codeFilter || undefined)
      .then((page) => setObservations(page.content))
      .catch(() => setListError("Couldn't load your observations. Try refreshing the page."))
      .finally(() => setLoading(false));
  }, [token, codeFilter]);

  useEffect(() => {
    loadObservations();
  }, [loadObservations]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!token) return;
    setFormError(null);
    setSaving(true);
    try {
      await observationsApi.createObservation(token, {
        displayName: form.displayName,
        value: form.value,
        unit: form.unit || undefined,
        referenceRange: form.referenceRange || undefined,
        effectiveDate: form.effectiveDate,
      });
      setForm(EMPTY_FORM);
      setShowForm(false);
      loadObservations();
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setFormError(err.details.length > 0 ? err.details.join(" ") : err.message);
      } else {
        setFormError("Couldn't reach the server. Check your connection and try again.");
      }
    } finally {
      setSaving(false);
    }
  }

  if (user && user.role !== "PATIENT") {
    return (
      <div className="dashboard">
        <h1>Observations</h1>
        <p className="subtitle">This is only available for patient accounts.</p>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <h1>Your health observations</h1>
      <p className="subtitle">
        Confirmed measurements — from reviewed documents, or entered directly (e.g. a home reading).
      </p>

      {listError && <div className="form-error">{listError}</div>}

      <div className="field" style={{ maxWidth: 280 }}>
        <label htmlFor="codeFilter">Filter by measurement</label>
        <input
          id="codeFilter"
          placeholder="e.g. Body Weight"
          value={codeFilter}
          onChange={(e) => setCodeFilter(e.target.value)}
        />
      </div>

      {!showForm && (
        <button className="btn-secondary" style={{ marginBottom: 20 }} onClick={() => setShowForm(true)}>
          Add a measurement
        </button>
      )}

      {showForm && (
        <form onSubmit={handleSubmit} noValidate style={{ marginBottom: 24 }}>
          {formError && <div className="form-error">{formError}</div>}

          <div className="field">
            <label htmlFor="displayName">Measurement name</label>
            <input
              id="displayName"
              required
              placeholder="e.g. Body Weight"
              value={form.displayName}
              onChange={(e) => setForm((f) => ({ ...f, displayName: e.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="value">Value</label>
            <input
              id="value"
              required
              placeholder="e.g. 70.5"
              value={form.value}
              onChange={(e) => setForm((f) => ({ ...f, value: e.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="unit">Unit (optional)</label>
            <input
              id="unit"
              placeholder="e.g. kg"
              value={form.unit}
              onChange={(e) => setForm((f) => ({ ...f, unit: e.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="referenceRange">Reference range (optional)</label>
            <input
              id="referenceRange"
              value={form.referenceRange}
              onChange={(e) => setForm((f) => ({ ...f, referenceRange: e.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="effectiveDate">Date</label>
            <input
              id="effectiveDate"
              type="date"
              required
              max={todayIso}
              value={form.effectiveDate}
              onChange={(e) => setForm((f) => ({ ...f, effectiveDate: e.target.value }))}
            />
          </div>

          <div style={{ display: "flex", gap: 12 }}>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? "Saving…" : "Save"}
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => {
                setShowForm(false);
                setForm(EMPTY_FORM);
                setFormError(null);
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p className="subtitle">Loading…</p>
      ) : observations.length === 0 ? (
        <p className="subtitle">No observations yet.</p>
      ) : (
        <table className="document-table">
          <thead>
            <tr>
              <th>Measurement</th>
              <th>Value</th>
              <th>Reference range</th>
              <th>Date</th>
              <th>Source</th>
            </tr>
          </thead>
          <tbody>
            {observations.map((o) => (
              <tr key={o.id}>
                <td>{o.displayName}</td>
                <td>
                  {o.rawValue}
                  {o.unit ? ` ${o.unit}` : ""}
                </td>
                <td>{o.referenceRange ?? "—"}</td>
                <td>{formatDate(o.effectiveDate)}</td>
                <td>{sourceLabel(o.source)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
