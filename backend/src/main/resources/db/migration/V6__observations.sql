-- Structured Observations (P0-05). A flexible model — code/displayName
-- are free-form, not a fixed enum of test types (master spec §17:
-- "Do not hardcode the system around only these measurements").
--
-- code is a simple deterministic normalization of the display name
-- (uppercased, non-alphanumeric collapsed to underscores) — NOT a
-- real clinical coding system (LOINC etc.). Documented honestly as
-- such; see CodeNormalizer and ADR-011.
--
-- Observations are only ever created already patient-verified (via a
-- confirmed/corrected extraction candidate, or typed in directly) —
-- there is no "auto-accepted" path. No update/delete endpoint exists
-- yet; that's deferred to a later phase (see docs/DATABASE.md on
-- never silently overwriting conflicting data).

CREATE TABLE observations (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    code                 VARCHAR(100) NOT NULL,
    display_name         VARCHAR(255) NOT NULL,

    raw_value            VARCHAR(255) NOT NULL,
    numeric_value         DOUBLE PRECISION,
    unit                 VARCHAR(50),
    reference_range      VARCHAR(100),

    effective_date       DATE NOT NULL,

    source               VARCHAR(32) NOT NULL CHECK (source IN ('DOCUMENT_EXTRACTION', 'MANUAL_ENTRY')),
    source_document_id   UUID REFERENCES documents(id) ON DELETE SET NULL,
    source_candidate_id  UUID REFERENCES extraction_candidates(id) ON DELETE SET NULL,

    confidence           VARCHAR(16) NOT NULL CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    status               VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE')),

    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Matches the query shape future longitudinal/trend features need:
-- "this patient's readings for this code over time".
CREATE INDEX idx_observations_patient_code_date ON observations(patient_id, code, effective_date);
