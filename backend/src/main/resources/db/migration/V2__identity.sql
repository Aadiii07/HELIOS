-- Identity module (P0-01 Authentication): users + audit trail.
-- See docs/DATABASE.md for conventions.

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL
                        CHECK (role IN ('PATIENT', 'PROVIDER', 'ORGANIZATION_ADMIN', 'SYSTEM_ADMIN')),
    account_status  VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE'
                        CHECK (account_status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- audit_events.actor_user_id is intentionally nullable: some
-- security-relevant events (e.g. a failed login with an unknown
-- email) have no resolvable user.
CREATE TABLE audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_email     VARCHAR(255),
    action          VARCHAR(64)  NOT NULL,
    resource_type   VARCHAR(64),
    resource_id     VARCHAR(128),
    result          VARCHAR(32)  NOT NULL CHECK (result IN ('SUCCESS', 'FAILURE')),
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_events_actor_user_id ON audit_events(actor_user_id);
CREATE INDEX idx_audit_events_action ON audit_events(action);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);
