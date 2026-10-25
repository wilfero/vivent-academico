CREATE TABLE platform_users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username varchar(80) NOT NULL,
    password_hash varchar(255) NOT NULL,
    display_name varchar(150) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    failed_attempts integer NOT NULL DEFAULT 0,
    locked_until timestamptz,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_platform_users_username UNIQUE (username),
    CONSTRAINT ck_platform_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE TABLE platform_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    platform_user_id uuid NOT NULL REFERENCES platform_users (id),
    token_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    ip_address varchar(45),
    user_agent text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_platform_sessions_token_hash UNIQUE (token_hash)
);

CREATE TABLE platform_audit_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    platform_user_id uuid REFERENCES platform_users (id),
    action varchar(30) NOT NULL,
    entity_type varchar(80) NOT NULL,
    entity_id uuid,
    old_value text,
    new_value text,
    ip_address varchar(45),
    user_agent text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_platform_audit_logs_action CHECK (action IN (
        'LOGIN',
        'LOGOUT',
        'CREATE',
        'UPDATE',
        'STATUS_CHANGE',
        'SUBSCRIPTION_CHANGE',
        'RESET_ADMIN_PASSWORD',
        'PROVISION'
    ))
);

CREATE INDEX idx_platform_sessions_token_hash ON platform_sessions (token_hash);
CREATE INDEX idx_platform_sessions_platform_user_id ON platform_sessions (platform_user_id);
CREATE INDEX idx_platform_audit_logs_created_at ON platform_audit_logs (created_at);

CREATE TRIGGER trg_platform_users_set_updated_at
    BEFORE UPDATE ON platform_users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
