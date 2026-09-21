import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { useNavigate } from "react-router";
import { useAuth } from "../auth/AuthContext";
import { ApiRequestError } from "../api/client";
import * as patientApi from "../api/patientApi";
import type { PatientProfileInput } from "../api/patientApi";

const EMPTY_FORM: PatientProfileInput = {
  firstName: "",
  lastName: "",
  dateOfBirth: "",
  phoneNumber: "",
  addressLine1: "",
  addressLine2: "",
  city: "",
  state: "",
  postalCode: "",
  country: "",
  preferredLanguage: "",
};

export default function ProfilePage() {
  const { user, token } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState<PatientProfileInput>(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [isNewProfile, setIsNewProfile] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!token) return;
    patientApi
      .getProfile(token)
      .then((profile) => {
        setForm({
          firstName: profile.firstName,
          lastName: profile.lastName,
          dateOfBirth: profile.dateOfBirth,
          phoneNumber: profile.phoneNumber ?? "",
          addressLine1: profile.addressLine1 ?? "",
          addressLine2: profile.addressLine2 ?? "",
          city: profile.city ?? "",
          state: profile.state ?? "",
          postalCode: profile.postalCode ?? "",
          country: profile.country ?? "",
          preferredLanguage: profile.preferredLanguage ?? "",
        });
        setIsNewProfile(false);
      })
      .catch((err) => {
        if (err instanceof ApiRequestError && err.code === "PROFILE_NOT_FOUND") {
          setIsNewProfile(true);
        } else {
          setError("Couldn't load your profile. Try refreshing the page.");
        }
      })
      .finally(() => setLoading(false));
  }, [token]);

  function field(name: keyof PatientProfileInput) {
    return {
      value: form[name] ?? "",
      onChange: (e: ChangeEvent<HTMLInputElement>) =>
        setForm((f) => ({ ...f, [name]: e.target.value })),
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!token) return;
    setError(null);
    setSuccess(false);
    setSaving(true);
    try {
      // Send blank optional fields as undefined rather than "" so the
      // backend's @Pattern validation (e.g. on phoneNumber) isn't
      // triggered by an empty string that was never actually filled in.
      const payload: PatientProfileInput = {
        firstName: form.firstName,
        lastName: form.lastName,
        dateOfBirth: form.dateOfBirth,
        phoneNumber: form.phoneNumber || undefined,
        addressLine1: form.addressLine1 || undefined,
        addressLine2: form.addressLine2 || undefined,
        city: form.city || undefined,
        state: form.state || undefined,
        postalCode: form.postalCode || undefined,
        country: form.country || undefined,
        preferredLanguage: form.preferredLanguage || undefined,
      };
      await patientApi.upsertProfile(token, payload);
      setIsNewProfile(false);
      setSuccess(true);
      // Brief pause so "Profile saved" is actually visible before
      // leaving the page, then return to the dashboard.
      setTimeout(() => navigate("/dashboard"), 900);
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.details.length > 0 ? err.details.join(" ") : err.message);
      } else {
        setError("Couldn't reach the server. Check your connection and try again.");
      }
    } finally {
      setSaving(false);
    }
  }

  if (user && user.role !== "PATIENT") {
    return (
      <div className="dashboard">
        <h1>Profile</h1>
        <p className="subtitle">Profile setup is only available for patient accounts.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="dashboard">
        <h1>Profile</h1>
        <p className="subtitle">Loading…</p>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <h1>{isNewProfile ? "Set up your profile" : "Your profile"}</h1>
      <p className="subtitle">
        {isNewProfile
          ? "This is used to identify your health records — it's not shared with anyone unless you choose to."
          : "Update your details below."}
      </p>

      {success && <div className="form-success">Profile saved.</div>}
      {error && <div className="form-error">{error}</div>}

      <form onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor="firstName">First name</label>
          <input id="firstName" required {...field("firstName")} />
        </div>
        <div className="field">
          <label htmlFor="lastName">Last name</label>
          <input id="lastName" required {...field("lastName")} />
        </div>
        <div className="field">
          <label htmlFor="dateOfBirth">Date of birth</label>
          <input id="dateOfBirth" type="date" required {...field("dateOfBirth")} />
        </div>
        <div className="field">
          <label htmlFor="phoneNumber">Phone number</label>
          <input id="phoneNumber" type="tel" {...field("phoneNumber")} />
        </div>
        <div className="field">
          <label htmlFor="addressLine1">Address line 1</label>
          <input id="addressLine1" {...field("addressLine1")} />
        </div>
        <div className="field">
          <label htmlFor="addressLine2">Address line 2</label>
          <input id="addressLine2" {...field("addressLine2")} />
        </div>
        <div className="field">
          <label htmlFor="city">City</label>
          <input id="city" {...field("city")} />
        </div>
        <div className="field">
          <label htmlFor="state">State / Province</label>
          <input id="state" {...field("state")} />
        </div>
        <div className="field">
          <label htmlFor="postalCode">Postal code</label>
          <input id="postalCode" {...field("postalCode")} />
        </div>
        <div className="field">
          <label htmlFor="country">Country</label>
          <input id="country" {...field("country")} />
        </div>
        <div className="field">
          <label htmlFor="preferredLanguage">Preferred language (e.g. en, es)</label>
          <input id="preferredLanguage" {...field("preferredLanguage")} />
        </div>

        <button type="submit" className="btn-primary" disabled={saving}>
          {saving ? "Saving…" : "Save profile"}
        </button>
      </form>
    </div>
  );
}
