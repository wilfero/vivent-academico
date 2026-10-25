-- Keeps the documented academic demo password (ChangeMe123!) working for the
-- six demo users of the "demo" tenant.
--
-- Why a new seed: Flyway history is immutable, so the earlier bootstrap hashes
-- cannot be edited. This seed is idempotent: it only rewrites rows whose hash
-- differs and bumps auth_version so tokens issued with the old password stop
-- being valid.
--
-- Scope: academic demo tenant only (never run this against production data).

UPDATE users
SET password_hash = '$2b$12$q3pR/Vui5S5qxdt8cBrnR.c32h/0SnTc8TapBpwmveH.3yQkvnhke',
    must_change_password = false,
    status = 'ACTIVE',
    auth_version = auth_version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE company_id = '11111111-1111-1111-1111-111111111111'
  AND username IN ('admin', 'seller', 'inventory', 'transfers_origin', 'transfers_destination', 'auditor')
  AND password_hash <> '$2b$12$q3pR/Vui5S5qxdt8cBrnR.c32h/0SnTc8TapBpwmveH.3yQkvnhke';
