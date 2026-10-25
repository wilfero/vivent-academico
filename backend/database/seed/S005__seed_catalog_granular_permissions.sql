INSERT INTO permissions (code, translation_key, module_code, action_code, description, is_system)
SELECT permission_seed.code,
       'permission.' || permission_seed.code,
       split_part(permission_seed.code, '.', 1),
       split_part(permission_seed.code, '.', 2),
       permission_seed.description,
       true
FROM (
    VALUES
        ('product_categories.view', 'View product categories'),
        ('product_categories.create', 'Create product categories'),
        ('product_categories.update', 'Update product categories'),
        ('product_categories.deactivate', 'Deactivate product categories'),
        ('units.view', 'View product units'),
        ('units.create', 'Create product units'),
        ('units.update', 'Update product units'),
        ('units.deactivate', 'Deactivate product units'),
        ('product_presentations.view', 'View product presentations'),
        ('product_presentations.create', 'Create product presentations'),
        ('product_presentations.update', 'Update product presentations'),
        ('product_presentations.deactivate', 'Deactivate product presentations')
) AS permission_seed(code, description)
ON CONFLICT (code) DO NOTHING;

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
