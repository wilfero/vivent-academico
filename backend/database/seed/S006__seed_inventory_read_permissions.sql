INSERT INTO permissions (code, translation_key, module_code, action_code, description, is_system)
SELECT permission_seed.code,
       'permission.' || permission_seed.code,
       split_part(permission_seed.code, '.', 1),
       split_part(permission_seed.code, '.', 2),
       permission_seed.description,
       true
FROM (
    VALUES
        ('inventory.view_layers', 'View FIFO inventory layers'),
        ('inventory.view_movements', 'View inventory movements')
) AS permission_seed(code, description)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code IN ('inventory.view_layers', 'inventory.view_movements')
WHERE role_record.code IN ('COMPANY_ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'WAREHOUSE_USER', 'AUDITOR')
ON CONFLICT DO NOTHING;
