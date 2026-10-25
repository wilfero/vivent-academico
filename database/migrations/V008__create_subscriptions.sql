CREATE TABLE plans (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(150) NOT NULL,
    code varchar(50) NOT NULL,
    description text,
    price numeric(19,4) NOT NULL DEFAULT 0,
    currency_code varchar(3) NOT NULL DEFAULT 'GTQ',
    max_branches integer NOT NULL,
    max_users integer NOT NULL,
    max_products integer NOT NULL,
    enabled_modules text[] NOT NULL DEFAULT ARRAY[]::text[],
    is_custom boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_plans_code UNIQUE (code),
    CONSTRAINT ck_plans_price CHECK (price >= 0),
    CONSTRAINT ck_plans_max_branches CHECK (max_branches > 0),
    CONSTRAINT ck_plans_max_users CHECK (max_users > 0),
    CONSTRAINT ck_plans_max_products CHECK (max_products > 0)
);

CREATE TABLE subscriptions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    plan_id uuid NOT NULL REFERENCES plans (id),
    status varchar(20) NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz,
    trial_ends_at timestamptz,
    notes text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_subscriptions_status CHECK (status IN ('TRIAL', 'ACTIVE', 'EXPIRED', 'SUSPENDED', 'CANCELLED'))
);
