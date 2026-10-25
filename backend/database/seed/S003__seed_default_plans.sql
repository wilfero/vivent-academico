INSERT INTO plans (id, name, code, description, price, currency_code, max_branches, max_users, max_products, enabled_modules, is_custom, is_active)
VALUES
    (
        '33333333-3333-3333-3333-333333333331',
        'Basic',
        'BASIC',
        'Bootstrap plan for first tenant setup',
        10,
        'USD',
        2,
        10,
        100,
        ARRAY['products', 'purchases', 'sales', 'inventory', 'transfers', 'reports', 'users', 'settings', 'cash_closures']::text[],
        false,
        true
    ),
    (
        '33333333-3333-3333-3333-333333333332',
        'Plus',
        'PLUS',
        'Growth plan with double capacity over Basic',
        20,
        'USD',
        4,
        20,
        200,
        ARRAY['products', 'purchases', 'sales', 'inventory', 'transfers', 'reports', 'users', 'settings', 'cash_closures']::text[],
        false,
        true
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        'Pro',
        'PRO',
        'Advanced plan with five times Basic capacity',
        45,
        'USD',
        10,
        50,
        500,
        ARRAY['products', 'purchases', 'sales', 'inventory', 'transfers', 'reports', 'users', 'settings', 'cash_closures']::text[],
        false,
        true
    ),
    (
        '33333333-3333-3333-3333-333333333334',
        'Unlimited',
        'UNLIMITED',
        'High-capacity plan with practical unlimited limits',
        80,
        'USD',
        200,
        1000,
        10000,
        ARRAY['products', 'purchases', 'sales', 'inventory', 'transfers', 'reports', 'users', 'settings', 'cash_closures']::text[],
        false,
        true
    )
ON CONFLICT (id) DO NOTHING;
