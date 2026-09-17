-- Baseline migration: required PostgreSQL extensions only.
-- No domain tables yet — those arrive with the phase that owns them
-- (e.g. users/roles with P0-01 Authentication, observations with
-- P0-05). See docs/DATABASE.md for migration conventions.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
