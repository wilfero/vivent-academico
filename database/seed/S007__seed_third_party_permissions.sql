INSERT INTO permissions (code, translation_key, module_code, action_code, description, is_system)
SELECT permission_seed.code,
       'permission.' || permission_seed.code,
       split_part(permission_seed.code, '.', 1),
       split_part(permission_seed.code, '.', 2),
       permission_seed.description,
       true
FROM (
    VALUES
        ('suppliers.view', 'View suppliers'),
        ('suppliers.create', 'Create suppliers'),
        ('suppliers.update', 'Update suppliers'),
        ('suppliers.delete', 'Delete suppliers'),
        ('customers.view', 'View customers'),
        ('customers.create', 'Create customers'),
        ('customers.update', 'Update customers'),
        ('customers.delete', 'Delete customers')
) AS permission_seed(code, description)
ON CONFLICT (code) DO NOTHING;

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
