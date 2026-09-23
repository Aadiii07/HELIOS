-- Document Intelligence (P0-04). Extends documents with processing
-- status; extraction_candidates holds candidate data points found in
-- a document — never auto-trusted, always awaiting patient review
-- (master spec §16: "Extracted information is NOT automatically
-- trusted"). Confirmed/corrected candidates feed the Observation
-- model in a later phase — this table does not create Observations
-- itself.

ALTER TABLE documents
    ADD COLUMN extraction_status VARCHAR(32) NOT NULL DEFAULT 'NOT_ATTEMPTED'
        CHECK (extraction_status IN ('NOT_ATTEMPTED', 'PROCESSED', 'UNSUPPORTED_FORMAT', 'FAILED')),
    ADD COLUMN processed_at TIMESTAMPTZ;

CREATE TABLE extraction_candidates (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id             UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    patient_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    field_label             VARCHAR(255) NOT NULL,
    raw_value               VARCHAR(255) NOT NULL,
    unit                    VARCHAR(50),
    reference_range         VARCHAR(100),
    source_excerpt          TEXT NOT NULL,

    confidence              VARCHAR(16) NOT NULL CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    extraction_method       VARCHAR(64) NOT NULL,

    review_status           VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                                 CHECK (review_status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CORRECTED')),

    -- Populated only when review_status = 'CORRECTED'. The original
    -- extracted values above are NEVER overwritten — both the
    -- machine's guess and the patient's correction stay traceable
    -- (master spec §55: never silently discard source values).
    corrected_field_label   VARCHAR(255),
    corrected_raw_value     VARCHAR(255),
    corrected_unit          VARCHAR(50),

    reviewed_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_extraction_candidates_document_id ON extraction_candidates(document_id);
CREATE INDEX idx_extraction_candidates_patient_id ON extraction_candidates(patient_id);
