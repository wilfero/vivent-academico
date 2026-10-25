# Database

This directory is the SQL source of truth for the local Vivent MVP.

## Layout

- `migrations/`: schema migrations executed by Flyway in version order
- `seed/`: deterministic demo data executed by Flyway after schema creation

## Current Scope

Phase 1 creates:
- core multi-tenant tables
- identity, roles and permissions
- products and inventory base tables
- purchases, sales, transfers, adjustments and audit tables
- plans and subscriptions
- demo tenant data for `demo.corpoviva.com`

Phase 4 adds:
- `product_units` reference table
- FK from `products.base_unit_code` to `product_units.code`
- tenant/product index for presentations

Phase 5 uses existing inventory tables:
- `inventory_balances`
- `inventory_layers`
- `stock_movements`

No new inventory table was required for FIFO/Kardex foundation.

Phase 11 adds timezone snapshots:
- `timezone_snapshot` on operational tables such as purchases, sales, transfers, adjustments, FIFO layers, Kardex movements, cash closures and audit logs
- backfill from `companies.timezone` with `UTC` fallback
- insert triggers that keep snapshots populated if application code omits the value

Application timestamps are persisted as UTC instants; `timezone_snapshot` preserves the company display timezone used when the operational record was created.

Phase 17 adds tenant-user credential state:
- `users.must_change_password` forces a temporary credential change before normal operation
- `users.auth_version` revokes older JWTs after password changes or resets
- existing users are backfilled with `false` and version `0`; newly provisioned company administrators are marked by application code

## Collaborative Versioning Convention

Starting after `V018` (schema) and `S009` (seeds), all new files must follow **14-digit UTC timestamp versioning**:

```text
Schema migrations: migrations/V<YYYYMMDDHHMMSS>__<description_in_english>.sql
Seed scripts:      seed/S<YYYYMMDDHHMMSS>__<description_in_english>.sql
```

Historical files (`V001..V018` and `S001..S009`) are immutable. Both schema and seed Flyway runners have `out-of-order = true` enabled.

To generate a new file with the correct UTC timestamp, use the helper script:

```bash
# Create a schema migration:
./database/new-migration.sh migration "add_customer_credit_limit"

# Create a seed script:
./database/new-migration.sh seed "default_discounts"
```

Rules:
1. Always use UTC (`date -u`).
2. Seeds must be idempotent (`ON CONFLICT DO NOTHING` or `WHERE NOT EXISTS`).
3. Add `DEFAULT` values when adding `NOT NULL` columns to existing tables.
4. Always `git pull origin dev` before merging to test concurrent migrations locally.

## Clean Bootstrap Seed Highlights

- Company: `Demo Vivent GT`
- Tenant slug: `demo`
- Branch: `Main Branch`
- Default location: `Main Warehouse`
- User: `admin`
- Password: `admin`
- Role: `COMPANY_ADMIN`
- Plans: `BASIC`, `PLUS`, `PRO`, `UNLIMITED`

Passwords are stored with BCrypt hashes in the SQL seeds.

The clean bootstrap intentionally does not create demo catalog items, suppliers, customers, secondary branches or operational users beyond the first administrator.
