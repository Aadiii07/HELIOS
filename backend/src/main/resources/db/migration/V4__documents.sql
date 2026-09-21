-- Document vault (P0-03). Only metadata lives here — the actual file
-- bytes live in object storage (local filesystem for dev; an
-- S3-compatible service is the production target, not yet wired in —
-- see ObjectStorageService). storage_key is server-generated and
-- never derived from the client-supplied filename (path-traversal /
-- collision prevention); it is never returned to clients either (see
-- docs/DATABASE.md and DocumentResponse).

CREATE TABLE documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_filename   VARCHAR(255) NOT NULL,
    storage_key         VARCHAR(512) NOT NULL UNIQUE,
    content_type        VARCHAR(100) NOT NULL,
    size_bytes          BIGINT NOT NULL,
    checksum_sha256     VARCHAR(64) NOT NULL,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_patient_id ON documents(patient_id);

-- Soft delete (deleted_at) rather than a hard DELETE: the row (and
-- audit trail referencing it) stays queryable for history even after
-- the underlying object-storage bytes are actually removed.
