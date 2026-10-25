INSERT INTO companies (
    id,
    legal_name,
    commercial_name,
    tenant_slug,
    domain,
    logo_url,
    primary_color,
    secondary_color,
    default_theme,
    supported_locales,
    default_locale,
    currency_code,
    timezone,
    tax_enabled,
    tax_rate,
    status
)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Demo Vivent GT',
    'Demo Vivent GT',
    'demo',
    'demo.corpoviva.com',
    NULL,
    '#2563eb',
    '#64748b',
    'light',
    ARRAY['es-GT', 'en-US']::text[],
    'es-GT',
    'GTQ',
    'America/Guatemala',
    false,
    0,
    'ACTIVE'
)
ON CONFLICT (tenant_slug) DO NOTHING;

INSERT INTO branches (
    id,
    company_id,
    name,
    code,
    address,
    phone,
    email,
    is_active
)
VALUES (
    '11111111-1111-1111-1111-111111111112',
    '11111111-1111-1111-1111-111111111111',
    'Main Branch',
    'CENTRAL',
    'Guatemala City',
    '+502 0000 0000',
    'central@demo.corpoviva.com',
    true
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO locations (
    id,
    company_id,
    branch_id,
    name,
    code,
    type,
    is_default,
    is_active
)
VALUES (
    '11111111-1111-1111-1111-111111111113',
    '11111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111112',
    'Main Warehouse',
    'MAIN_WAREHOUSE',
    'MAIN',
    true,
    true
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO subscriptions (
    id,
    company_id,
    plan_id,
    status,
    starts_at,
    trial_ends_at,
    notes
)
VALUES (
    '11111111-1111-1111-1111-111111111114',
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-333333333331',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    NULL,
    'Clean bootstrap subscription'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (
    id,
    company_id,
    branch_id,
    role_id,
    username,
    password_hash,
    first_name,
    last_name,
    email,
    phone,
    status
)
VALUES (
    '11111111-1111-1111-1111-111111111115',
    '11111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111112',
    (SELECT id FROM roles WHERE code = 'COMPANY_ADMIN' AND company_id IS NULL),
    'admin',
    '$2b$12$wv3a.7F/BAL5fKEdIBwYI.LhCXWHQVJOD3NPqPx/ZLAIndfJoJbte',
    'Admin',
    'User',
    'admin@demo.corpoviva.com',
    '+502 5555 1000',
    'ACTIVE'
)
ON CONFLICT (company_id, username) DO NOTHING;

INSERT INTO user_branch_assignments (user_id, branch_id, is_primary, status)
VALUES
    ('11111111-1111-1111-1111-111111111115', '11111111-1111-1111-1111-111111111112', true, 'ACTIVE')
ON CONFLICT (user_id, branch_id) DO NOTHING;
