-- Patient profile module (P0-02). Separate from users/identity —
-- demographics and contact info, not clinical observations (those
-- get their own table in a later phase). See docs/DATABASE.md.

CREATE TABLE patient_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    date_of_birth       DATE NOT NULL,
    phone_number        VARCHAR(32),
    address_line1       VARCHAR(255),
    address_line2       VARCHAR(255),
    city                VARCHAR(100),
    state               VARCHAR(100),
    postal_code         VARCHAR(20),
    country             VARCHAR(100),
    preferred_language  VARCHAR(10),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ON DELETE CASCADE (unlike audit_events' ON DELETE SET NULL):
-- profile data is meaningless without the user it belongs to, whereas
-- an audit trail should outlive the account it describes.
