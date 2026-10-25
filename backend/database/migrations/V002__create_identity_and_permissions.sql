CREATE TABLE permissions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(100) NOT NULL,
    translation_key varchar(150) NOT NULL,
    module_code varchar(50) NOT NULL,
    action_code varchar(50) NOT NULL,
    description varchar(255),
    is_system boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE TABLE roles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid REFERENCES companies (id),
    code varchar(100) NOT NULL,
    name varchar(150) NOT NULL,
    translation_key varchar(150) NOT NULL,
    description text,
    is_system boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id uuid NOT NULL REFERENCES roles (id),
    permission_id uuid NOT NULL REFERENCES permissions (id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    branch_id uuid NOT NULL REFERENCES branches (id),
    role_id uuid NOT NULL REFERENCES roles (id),
    username varchar(80) NOT NULL,
    password_hash varchar(255) NOT NULL,
    first_name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    email varchar(150) NOT NULL,
    phone varchar(50),
    birth_date date,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uq_users_company_username UNIQUE (company_id, username),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED'))
);

CREATE TABLE user_permission_overrides (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users (id),
    permission_id uuid NOT NULL REFERENCES permissions (id),
    effect varchar(10) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_permission_overrides_user_permission UNIQUE (user_id, permission_id),
    CONSTRAINT ck_user_permission_overrides_effect CHECK (effect IN ('GRANT', 'DENY'))
);

CREATE TABLE user_branch_assignments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users (id),
    branch_id uuid NOT NULL REFERENCES branches (id),
    is_primary boolean NOT NULL DEFAULT false,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    start_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_branch_assignments_user_branch UNIQUE (user_id, branch_id),
    CONSTRAINT ck_user_branch_assignments_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
