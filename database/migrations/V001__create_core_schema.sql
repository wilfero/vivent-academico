CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TABLE companies (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name varchar(150) NOT NULL,
    commercial_name varchar(150) NOT NULL,
    tenant_slug varchar(80) NOT NULL,
    domain varchar(255) NOT NULL,
    logo_url varchar(500),
    primary_color varchar(7) NOT NULL DEFAULT '#2563eb',
    secondary_color varchar(7) NOT NULL DEFAULT '#64748b',
    default_theme varchar(20) NOT NULL DEFAULT 'light',
    supported_locales text[] NOT NULL DEFAULT ARRAY['es-GT']::text[],
    default_locale varchar(20) NOT NULL DEFAULT 'es-GT',
    currency_code varchar(3) NOT NULL DEFAULT 'GTQ',
    timezone varchar(80) NOT NULL DEFAULT 'America/Guatemala',
    tax_enabled boolean NOT NULL DEFAULT false,
    tax_rate numeric(5,2) NOT NULL DEFAULT 0,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uq_companies_tenant_slug UNIQUE (tenant_slug),
    CONSTRAINT uq_companies_domain UNIQUE (domain),
    CONSTRAINT ck_companies_tenant_slug CHECK (tenant_slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    CONSTRAINT ck_companies_default_theme CHECK (default_theme IN ('light', 'dark')),
    CONSTRAINT ck_companies_supported_locales CHECK (cardinality(supported_locales) > 0),
    CONSTRAINT ck_companies_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 100),
    CONSTRAINT ck_companies_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE TABLE branches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    name varchar(150) NOT NULL,
    code varchar(50) NOT NULL,
    address varchar(255),
    phone varchar(50),
    email varchar(150),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uq_branches_company_code UNIQUE (company_id, code)
);

CREATE TABLE locations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    branch_id uuid NOT NULL REFERENCES branches (id),
    name varchar(150) NOT NULL,
    code varchar(50) NOT NULL,
    type varchar(20) NOT NULL,
    is_default boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_locations_branch_code UNIQUE (branch_id, code),
    CONSTRAINT ck_locations_type CHECK (type IN ('MAIN', 'SALES_FLOOR', 'DAMAGED', 'IN_TRANSIT', 'CUSTOM'))
);
