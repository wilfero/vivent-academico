INSERT INTO roles (code, name, translation_key, description, is_system)
SELECT role_seed.code,
       role_seed.name,
       role_seed.translation_key,
       role_seed.description,
       true
FROM (
    VALUES
        ('COMPANY_ADMIN', 'Company Admin', 'role.company_admin', 'System role with company-wide administration permissions')
) AS role_seed(code, name, translation_key, description)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM roles role_record
JOIN permissions permission_record ON permission_record.code <> 'inventory.allow_negative_stock'
WHERE role_record.code = 'COMPANY_ADMIN'
  AND role_record.company_id IS NULL
ON CONFLICT DO NOTHING;
