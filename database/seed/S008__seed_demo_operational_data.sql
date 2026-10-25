-- Additional system roles required by the demo tenant.
-- These roles are referenced by S005/S006/S007 but were not created yet.
INSERT INTO roles (code, name, translation_key, description, is_system)
SELECT role_seed.code, role_seed.name, role_seed.translation_key, role_seed.description, true
FROM (
    VALUES
        ('BRANCH_ADMIN', 'Branch Admin', 'role.branch_admin', 'System role with branch-level administration permissions'),
        ('MANAGER', 'Manager', 'role.manager', 'System role with operational management permissions'),
        ('WAREHOUSE_USER', 'Warehouse User', 'role.warehouse_user', 'System role for warehouse and purchase operations'),
        ('SELLER', 'Seller', 'role.seller', 'System role for POS sales operations'),
        ('AUDITOR', 'Auditor', 'role.auditor', 'System role with read-only access across modules')
) AS role_seed(code, name, translation_key, description)
ON CONFLICT DO NOTHING;

-- Assign base permissions to BRANCH_ADMIN (same as COMPANY_ADMIN minus allow_negative_stock)
INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code <> 'inventory.allow_negative_stock'
WHERE role_record.code = 'BRANCH_ADMIN'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

-- Assign base permissions to MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY[
        'products.view', 'products.create', 'products.update',
        'product_categories.view', 'product_categories.create', 'product_categories.update',
        'units.view',
        'product_presentations.view', 'product_presentations.create', 'product_presentations.update',
        'purchases.view', 'purchases.create', 'purchases.confirm', 'purchases.void',
        'suppliers.view', 'suppliers.create', 'suppliers.update',
        'sales.view', 'sales.create', 'sales.cancel', 'sales.apply_discount',
        'customers.view', 'customers.create', 'customers.update',
        'inventory.view', 'inventory.adjust', 'inventory.view_kardex', 'inventory.view_layers', 'inventory.view_movements',
        'transfers.view', 'transfers.request', 'transfers.approve', 'transfers.dispatch', 'transfers.receive', 'transfers.cancel',
        'reports.view_branch',
        'users.view', 'users.create', 'users.update',
        'settings.view',
        'cash_closures.view', 'cash_closures.create', 'cash_closures.approve'
    ]
)
WHERE role_record.code = 'MANAGER'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

-- Assign base permissions to WAREHOUSE_USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY[
        'products.view',
        'product_categories.view',
        'units.view',
        'product_presentations.view',
        'purchases.view', 'purchases.create', 'purchases.confirm', 'purchases.void',
        'suppliers.view', 'suppliers.create',
        'inventory.view', 'inventory.adjust', 'inventory.view_kardex', 'inventory.view_layers', 'inventory.view_movements',
        'transfers.view', 'transfers.request', 'transfers.dispatch', 'transfers.receive',
        'reports.view_branch',
        'cash_closures.view'
    ]
)
WHERE role_record.code = 'WAREHOUSE_USER'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

-- Assign base permissions to SELLER
INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY[
        'products.view',
        'product_categories.view',
        'units.view',
        'product_presentations.view',
        'sales.view', 'sales.create', 'sales.remove_item',
        'customers.view', 'customers.create',
        'inventory.view',
        'cash_closures.view', 'cash_closures.create'
    ]
)
WHERE role_record.code = 'SELLER'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

-- Assign base permissions to AUDITOR (read-only across modules)
INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY[
        'products.view',
        'product_categories.view',
        'units.view',
        'product_presentations.view',
        'purchases.view',
        'suppliers.view',
        'sales.view',
        'customers.view',
        'inventory.view', 'inventory.view_kardex', 'inventory.view_layers', 'inventory.view_movements',
        'transfers.view',
        'reports.view_branch', 'reports.view_company',
        'users.view',
        'settings.view',
        'cash_closures.view'
    ]
)
WHERE role_record.code = 'AUDITOR'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

-- Re-assign granular permissions that were skipped because roles did not exist when S005/S006/S007 ran
INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY[
        'product_categories.view', 'product_categories.create', 'product_categories.update', 'product_categories.deactivate',
        'units.view', 'units.create', 'units.update', 'units.deactivate',
        'product_presentations.view', 'product_presentations.create', 'product_presentations.update', 'product_presentations.deactivate'
    ]
)
WHERE role_record.code IN ('COMPANY_ADMIN', 'BRANCH_ADMIN')
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY[
        'product_categories.view', 'product_categories.create', 'product_categories.update',
        'units.view',
        'product_presentations.view', 'product_presentations.create', 'product_presentations.update'
    ]
)
WHERE role_record.code = 'MANAGER'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY['product_categories.view', 'units.view', 'product_presentations.view']
)
WHERE role_record.code IN ('WAREHOUSE_USER', 'SELLER', 'AUDITOR')
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code IN ('inventory.view_layers', 'inventory.view_movements')
WHERE role_record.code IN ('COMPANY_ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'WAREHOUSE_USER', 'AUDITOR')
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY[
        'suppliers.view', 'suppliers.create', 'suppliers.update', 'suppliers.delete',
        'customers.view', 'customers.create', 'customers.update', 'customers.delete'
    ]
)
WHERE role_record.code IN ('COMPANY_ADMIN', 'BRANCH_ADMIN')
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY[
        'suppliers.view', 'suppliers.create', 'suppliers.update',
        'customers.view', 'customers.create', 'customers.update'
    ]
)
WHERE role_record.code = 'MANAGER'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (ARRAY['suppliers.view', 'suppliers.create'])
WHERE role_record.code = 'WAREHOUSE_USER'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (ARRAY['customers.view', 'customers.create'])
WHERE role_record.code = 'SELLER'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (ARRAY['suppliers.view', 'customers.view'])
WHERE role_record.code = 'AUDITOR'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

-- =====================================================
-- Demo Branch 2 with its default location
-- =====================================================
INSERT INTO branches (
    id, company_id, name, code, address, phone, email, is_active
)
VALUES (
    '11111111-1111-1111-1111-111111111116',
    '11111111-1111-1111-1111-111111111111',
    'Demo Branch 2',
    'BRANCH2',
    'Zone 2, Guatemala City',
    '+502 0000 0001',
    'branch2@demo.corpoviva.com',
    true
)
ON CONFLICT DO NOTHING;

INSERT INTO locations (
    id, company_id, branch_id, name, code, type, is_default, is_active
)
VALUES (
    '11111111-1111-1111-1111-111111111117',
    '11111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111116',
    'Branch 2 Warehouse',
    'BRANCH2_WAREHOUSE',
    'MAIN',
    true,
    true
)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Demo supplier
-- =====================================================
INSERT INTO suppliers (
    id, company_id, name, tax_id, email, phone, address, contact_name, is_active
)
VALUES (
    '11111111-1111-1111-1111-111111111123',
    '11111111-1111-1111-1111-111111111111',
    'Distribuidora Demo',
    '1234567-8',
    'ventas@distribuidorademo.com',
    '+502 2222 3333',
    'Km 12 Carretera al Atlantico, Guatemala',
    'Juan Perez',
    true
)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Demo generic customer (Consumidor Final)
-- =====================================================
INSERT INTO customers (
    id, company_id, name, tax_id, email, phone, address, is_generic_consumer, is_active
)
VALUES (
    '11111111-1111-1111-1111-111111111124',
    '11111111-1111-1111-1111-111111111111',
    'Consumidor Final',
    'CF',
    null,
    null,
    'CIUDAD',
    true,
    true
)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Demo product with presentations
-- =====================================================
INSERT INTO products (
    id, company_id, name, description, base_unit_code, sku, minimum_stock,
    is_stockable, has_expiration_date, has_lot_tracking, has_serial_tracking, is_active
)
VALUES (
    '11111111-1111-1111-1111-111111111125',
    '11111111-1111-1111-1111-111111111111',
    'Coca Cola Lata',
    'Bebida carbonatada 350ml',
    'can',
    'COCA-CAN-350',
    24,
    true,
    false,
    false,
    false,
    true
)
ON CONFLICT DO NOTHING;

INSERT INTO product_presentations (
    id, company_id, product_id, name, barcode, conversion_factor,
    purchase_price_suggested, sale_price, is_default_purchase, is_default_sale, is_active
)
VALUES
    (
        '11111111-1111-1111-1111-111111111126',
        '11111111-1111-1111-1111-111111111111',
        '11111111-1111-1111-1111-111111111125',
        'Lata',
        '740000000001',
        1,
        5.00,
        10.00,
        true,
        true,
        true
    ),
    (
        '11111111-1111-1111-1111-111111111127',
        '11111111-1111-1111-1111-111111111111',
        '11111111-1111-1111-1111-111111111125',
        'Six Pack',
        '740000000006',
        6,
        28.00,
        55.00,
        false,
        false,
        true
    ),
    (
        '11111111-1111-1111-1111-111111111128',
        '11111111-1111-1111-1111-111111111111',
        '11111111-1111-1111-1111-111111111125',
        'Caja 24',
        '740000000024',
        24,
        100.00,
        210.00,
        false,
        false,
        true
    )
ON CONFLICT DO NOTHING;

-- =====================================================
-- Demo users (password = username for each)
-- =====================================================
INSERT INTO users (
    id, company_id, branch_id, role_id, username, password_hash,
    first_name, last_name, email, phone, status
)
VALUES
    (
        '11111111-1111-1111-1111-111111111118',
        '11111111-1111-1111-1111-111111111111',
        '11111111-1111-1111-1111-111111111112',
        (SELECT id FROM roles WHERE code = 'SELLER' AND company_id IS NULL),
        'seller',
        '$2b$12$1M/AQpymrC2E6Xmgwozcw.8jXe8ghwvFbjNbo9YkiDK/GpS3trKfm',
        'Seller',
        'User',
        'seller@demo.corpoviva.com',
        '+502 5555 1001',
        'ACTIVE'
    ),
    (
        '11111111-1111-1111-1111-111111111119',
        '11111111-1111-1111-1111-111111111111',
        '11111111-1111-1111-1111-111111111112',
        (SELECT id FROM roles WHERE code = 'WAREHOUSE_USER' AND company_id IS NULL),
        'inventory',
        '$2b$12$8ePvT/eA.9IbCDmlY8o0/uYt4/XU6kcfCAF5DV4uGUvSIt6rDyppS',
        'Inventory',
        'User',
        'inventory@demo.corpoviva.com',
        '+502 5555 1002',
        'ACTIVE'
    ),
    (
        '11111111-1111-1111-1111-111111111120',
        '11111111-1111-1111-1111-111111111111',
        '11111111-1111-1111-1111-111111111112',
        (SELECT id FROM roles WHERE code = 'WAREHOUSE_USER' AND company_id IS NULL),
        'transfers_origin',
        '$2b$12$HUL0CS6..PXUfgibPlFeKe0QuTa9B9fhv8jQ/613NEmBu6HvDhURW',
        'Transfers',
        'Origin',
        'transfers_origin@demo.corpoviva.com',
        '+502 5555 1003',
        'ACTIVE'
    ),
    (
        '11111111-1111-1111-1111-111111111121',
        '11111111-1111-1111-1111-111111111111',
        '11111111-1111-1111-1111-111111111116',
        (SELECT id FROM roles WHERE code = 'WAREHOUSE_USER' AND company_id IS NULL),
        'transfers_destination',
        '$2b$12$A07IcPczp4Q/OkErlmawhuZjNLduD2u.55tIeOXjl7h3bx4woN7ky',
        'Transfers',
        'Destination',
        'transfers_destination@demo.corpoviva.com',
        '+502 5555 1004',
        'ACTIVE'
    ),
    (
        '11111111-1111-1111-1111-111111111122',
        '11111111-1111-1111-1111-111111111111',
        '11111111-1111-1111-1111-111111111112',
        (SELECT id FROM roles WHERE code = 'AUDITOR' AND company_id IS NULL),
        'auditor',
        '$2b$12$9nfpzqLJaI4763LmU3YOaO7ynuYCNyCyJKNG8KmjG.r2/mAMu64DK',
        'Auditor',
        'User',
        'auditor@demo.corpoviva.com',
        '+502 5555 1005',
        'ACTIVE'
    )
ON CONFLICT (company_id, username) DO NOTHING;

-- User branch assignments
INSERT INTO user_branch_assignments (user_id, branch_id, is_primary, status)
VALUES
    ('11111111-1111-1111-1111-111111111118', '11111111-1111-1111-1111-111111111112', true, 'ACTIVE'),
    ('11111111-1111-1111-1111-111111111119', '11111111-1111-1111-1111-111111111112', true, 'ACTIVE'),
    ('11111111-1111-1111-1111-111111111120', '11111111-1111-1111-1111-111111111112', true, 'ACTIVE'),
    ('11111111-1111-1111-1111-111111111121', '11111111-1111-1111-1111-111111111116', true, 'ACTIVE'),
    ('11111111-1111-1111-1111-111111111122', '11111111-1111-1111-1111-111111111112', true, 'ACTIVE')
ON CONFLICT DO NOTHING;

-- =====================================================
-- Initial demo stock: 48 cans (2 boxes of 24) in Main Branch
-- =====================================================
INSERT INTO inventory_balances (
    id, company_id, branch_id, location_id, product_id,
    quantity_on_hand, quantity_reserved
)
VALUES (
    '11111111-1111-1111-1111-111111111129',
    '11111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111112',
    '11111111-1111-1111-1111-111111111113',
    '11111111-1111-1111-1111-111111111125',
    48,
    0
)
ON CONFLICT DO NOTHING;

INSERT INTO inventory_layers (
    id, company_id, branch_id, location_id, product_id,
    source_type, source_id, quantity_received, quantity_remaining,
    unit_cost, received_at, status, timezone_snapshot
)
VALUES (
    '11111111-1111-1111-1111-111111111130',
    '11111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111112',
    '11111111-1111-1111-1111-111111111113',
    '11111111-1111-1111-1111-111111111125',
    'SEED_INITIAL_STOCK',
    '11111111-1111-1111-1111-111111111130',
    48,
    48,
    5.00,
    CURRENT_TIMESTAMP,
    'OPEN',
    'America/Guatemala'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO stock_movements (
    id, company_id, branch_id, location_id, product_id,
    movement_type, source_type, source_id,
    quantity_in, quantity_out, unit_cost, total_cost,
    balance_after, notes, created_by, timezone_snapshot
)
VALUES (
    '11111111-1111-1111-1111-111111111131',
    '11111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111112',
    '11111111-1111-1111-1111-111111111113',
    '11111111-1111-1111-1111-111111111125',
    'STOCK_COUNT_IN',
    'SEED_INITIAL_STOCK',
    '11111111-1111-1111-1111-111111111131',
    48,
    0,
    5.00,
    240.00,
    48,
    'Initial demo stock seed',
    '11111111-1111-1111-1111-111111111115',
    'America/Guatemala'
)
ON CONFLICT (id) DO NOTHING;
