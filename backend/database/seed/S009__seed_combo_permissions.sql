-- Combo permissions
INSERT INTO permissions (code, translation_key, module_code, action_code, description, is_system)
SELECT permission_seed.code,
       'permission.' || permission_seed.code,
       split_part(permission_seed.code, '.', 1),
       split_part(permission_seed.code, '.', 2),
       permission_seed.description,
       true
FROM (
    VALUES
        ('combos.view', 'View combos'),
        ('combos.create', 'Create combos'),
        ('combos.update', 'Update combos'),
        ('combos.delete', 'Delete combos')
) AS permission_seed(code, description)
ON CONFLICT (code) DO NOTHING;

-- Assign combo permissions to COMPANY_ADMIN and BRANCH_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY['combos.view', 'combos.create', 'combos.update', 'combos.delete']
)
WHERE role_record.code IN ('COMPANY_ADMIN', 'BRANCH_ADMIN')
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

-- Assign combo.view to MANAGER, WAREHOUSE_USER, SELLER, AUDITOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = 'combos.view'
WHERE role_record.code IN ('MANAGER', 'WAREHOUSE_USER', 'SELLER', 'AUDITOR')
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;

-- Assign combo create/update to MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code = ANY (
    ARRAY['combos.create', 'combos.update']
)
WHERE role_record.code = 'MANAGER'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;