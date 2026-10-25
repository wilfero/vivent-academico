-- Refresh token support for the academic variant (access + refresh token flow).
-- Tokens are stored hashed (SHA-256); raw tokens are never persisted or logged.
CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    user_id uuid NOT NULL REFERENCES users (id),
    token_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address varchar(45),
    user_agent varchar(255),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_company_id ON refresh_tokens (company_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
