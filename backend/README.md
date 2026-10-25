# vivent-back

Spring Boot backend for Vivent.

Detailed backend documentation is maintained in:

```text
../docs/BACKEND.md
```

Any backend change that modifies endpoints, DTOs, permissions, branch scope, migrations, seeds, FIFO/Kardex behavior or validation commands must update that file in the same task.

## Current Scope

The backend now includes the base platform plus the first security slice for the MVP:

- Java 25
- Spring Boot 4.0.6
- PostgreSQL datasource configuration
- Flyway enabled
- SQL source kept in `vivent-back/database/` for backend and Docker build packaging
- base package structure created for future modules
- public tenant configuration by subdomain
- login by `tenantSlug + username + password`
- stateless JWT authentication
- permission resolution from role permissions plus user overrides
- tenant-scoped company, branch, user, role, permission and subscription APIs
- plan limit enforcement for active branches and active users
- tenant-scoped product catalog APIs for units, categories, products, presentations and barcode lookup
- inventory balances, FIFO layers, Kardex and stock operation foundation
- purchase registration, confirmation into FIFO inventory and void reversal
- POS sales registration, FIFO stock out and sale cancellation reversal
- branch transfer request, reservation, dispatch and partial/full receipt
- inventory adjustment approval flow and audit log querying
- supplier read endpoints used by purchase flows
- strict endpoint permissions with mapped `PERMISSION_DENIED` responses that include the missing permission code
- branch visibility scoped by role: only `COMPANY_ADMIN` sees all branches; all other users see only their assigned branch
- UTC timestamp persistence with `timezone_snapshot` stored for operational records that need tenant-correct display
- demo-readiness seed with official active demo users and two branches for branch-scoped transfer validation
- private SaaS platform administration module (`platform/`) with cookie sessions, full company provisioning, plan/subscription management, admin password reset and platform audit trail

## Dependency Notes

Added:
- `spring-boot-starter-validation`
- `jjwt-api`
- `jjwt-impl`
- `jjwt-jackson`

Kept:
- webmvc, security, data-jpa, jdbc, flyway, postgresql, lombok

Docker Compose behavior:
- `spring-boot-docker-compose` is present as an optional runtime dependency, but `spring.docker.compose.enabled=false` disables automatic compose startup.
- Local PostgreSQL can be started from the root `compose.yml`.
- Production-oriented backend/frontend/db orchestration lives in `deploy/compose.yml`.

## Flyway Source

`pom.xml` packages these folders as classpath resources:

```text
database/migrations
database/seed
```

That keeps SQL inside the backend build context and avoids duplicating migration files inside `src/main/resources`.

Runtime behavior:
- schema migrations run from `db/migrations`
- demo seeds run from `db/seed`
- seeds use a dedicated Flyway history table: `flyway_seed_history`
- the clean bootstrap seed set creates only `Demo Vivent GT`, `Main Branch`, `Main Warehouse`, one `COMPANY_ADMIN` user (`admin` / `admin`) and the default plans

## Auth Endpoints

Public:

```text
GET /api/v1/public/tenant-config?subdomain=demo
POST /api/v1/auth/login
```

Authenticated:

```text
GET /api/v1/auth/me
```

## Phase 3 Endpoints

Settings and tenant management:

```text
GET /api/v1/companies/current
PUT /api/v1/companies/current

GET /api/v1/subscriptions/current

GET /api/v1/permissions

GET /api/v1/roles
GET /api/v1/roles/{roleId}
POST /api/v1/roles
PUT /api/v1/roles/{roleId}

GET /api/v1/branches
GET /api/v1/branches/{branchId}
POST /api/v1/branches
PUT /api/v1/branches/{branchId}
```

User management:

```text
GET /api/v1/users
GET /api/v1/users/{userId}
POST /api/v1/users
PUT /api/v1/users/{userId}
POST /api/v1/users/{userId}/activate
POST /api/v1/users/{userId}/deactivate
POST /api/v1/users/{userId}/branch
POST /api/v1/users/{userId}/password
DELETE /api/v1/users/{userId}
```

Notes:
- user deletion is logical only
- system roles are readable but not editable
- custom roles are tenant-scoped
- active branch creation and user activation respect current subscription limits
- user update does not change password or branch; password reset and branch change use dedicated endpoints
- moving a user back to a previous branch reuses the existing branch assignment row

## Phase 4 Endpoints

Product catalog:

```text
GET /api/v1/product-units

GET /api/v1/categories
GET /api/v1/categories/{categoryId}
POST /api/v1/categories
PUT /api/v1/categories/{categoryId}
DELETE /api/v1/categories/{categoryId}

GET /api/v1/products
GET /api/v1/products/{productId}
POST /api/v1/products
PUT /api/v1/products/{productId}
DELETE /api/v1/products/{productId}

GET /api/v1/products/by-barcode/{barcode}

GET /api/v1/products/{productId}/presentations
GET /api/v1/products/{productId}/presentations/{presentationId}
POST /api/v1/products/{productId}/presentations
PUT /api/v1/products/{productId}/presentations/{presentationId}
DELETE /api/v1/products/{productId}/presentations/{presentationId}
```

Notes:
- products use `baseUnitCode`, backed by `product_units`
- presentations convert to base units through `conversionFactor`
- barcode uniqueness is tenant-scoped
- product deletion is logical only
- active product creation respects current subscription limits

## Phase 5 Endpoints

Inventory foundation:

```text
GET /api/v1/inventory/balances
GET /api/v1/inventory/layers
GET /api/v1/inventory/kardex
POST /api/v1/inventory/stock-in
POST /api/v1/inventory/stock-out
```

Notes:
- quantities are stored in product base unit
- `stock-in` creates FIFO layer, balance update and Kardex movement
- `stock-out` consumes oldest open FIFO layers first
- negative stock is blocked by default
- these operations are foundation endpoints for validation; purchases and sales now call the same inventory rules
- FIFO layers and Kardex movements include `timezoneSnapshot` in API responses

## Phase 6 Endpoints

Purchases:

```text
GET /api/v1/purchases
GET /api/v1/purchases/{purchaseId}
POST /api/v1/purchases
PUT /api/v1/purchases/{purchaseId}
POST /api/v1/purchases/{purchaseId}/confirm
POST /api/v1/purchases/{purchaseId}/void
```

Notes:
- purchases can be saved as `DRAFT` or `PENDING_APPROVAL`
- confirmed purchases cannot be edited
- confirming a purchase creates FIFO inventory layers and Kardex `PURCHASE_IN` movements
- confirming or voiding a purchase locks the purchase row with `PESSIMISTIC_WRITE`
- purchase details resolve product and base quantity from the selected presentation conversion factor
- voiding a confirmed purchase requires a reason, locks the original receipt layers by `purchaseDetailId`, marks those layers `VOIDED` and creates `PURCHASE_VOID_OUT` stock movements only if the original layers are still fully available and unreserved
- confirmation and void write audit logs with affected purchase branch
- purchase responses include `timezoneSnapshot` captured from the company timezone at creation

## Phase 7 Endpoints

Sales POS:

```text
GET /api/v1/sales
GET /api/v1/sales/{saleId}
POST /api/v1/sales
PUT /api/v1/sales/{saleId}
POST /api/v1/sales/{saleId}/confirm
POST /api/v1/sales/{saleId}/cancel
```

Notes:
- sales are saved as `DRAFT` and confirmed explicitly
- sale details can resolve presentations by `presentationId` or `barcode`
- confirming a sale consumes FIFO layers and creates Kardex `SALE_OUT` movements
- update, confirm, cancel and single draft delete lock the sale row with `PESSIMISTIC_WRITE`
- negative stock remains blocked by inventory validation
- cancelling a confirmed sale requires a reason and returns stock with `SALE_CANCEL_IN`
- confirmation and cancellation write audit logs with affected sale branch

## Phase 8 Endpoints

Transfers:

```text
GET /api/v1/transfers
GET /api/v1/transfers/{transferId}
POST /api/v1/transfers
POST /api/v1/transfers/{transferId}/approve
POST /api/v1/transfers/{transferId}/reject
POST /api/v1/transfers/{transferId}/dispatch
POST /api/v1/transfers/{transferId}/receive
POST /api/v1/transfers/{transferId}/cancel
```

Notes:
- transfer requests require different source and destination branches
- approval reserves stock in the source branch and reduces available quantity
- dispatch changes status to `IN_TRANSIT`
- receiving can be full or partial and moves stock with `TRANSFER_OUT` plus `TRANSFER_IN`
- cancellation releases remaining reserved stock when applicable
- transfer listing supports `fromDate` and `toDate` filters on `requestedAt`
- approve, reject, dispatch, receive and cancel lock the transfer row with `PESSIMISTIC_WRITE`
- approve, reject, dispatch, receive and cancel write audit logs

## Phase 9 Endpoints

Adjustments and audit:

```text
GET /api/v1/inventory/adjustments
GET /api/v1/inventory/adjustments/{adjustmentId}
POST /api/v1/inventory/adjustments
PUT /api/v1/inventory/adjustments/{adjustmentId}
POST /api/v1/inventory/adjustments/{adjustmentId}/approve
POST /api/v1/inventory/adjustments/{adjustmentId}/reject
POST /api/v1/inventory/adjustments/{adjustmentId}/cancel

GET /api/v1/audit-logs
```

Notes:
- adjustments can be saved as `DRAFT` or `PENDING_APPROVAL`
- approval applies inventory movement and writes Kardex
- `IN` adjustments create stock layers using the supplied unit cost or zero when omitted
- `OUT` adjustments consume FIFO and block insufficient stock
- adjustment listing supports `fromDate` and `toDate` filters on `createdAt`
- update, approve, reject and cancel lock the adjustment row with `PESSIMISTIC_WRITE`
- create, update, approve, reject and cancel actions write audit logs with affected adjustment branch

## Reports And Cash Closures

Report and closure endpoints:

```text
GET /api/v1/reports/sales-summary
GET /api/v1/reports/purchases-summary
GET /api/v1/reports/products
GET /api/v1/reports/transfers-summary
GET /api/v1/reports/adjustments-summary
GET /api/v1/reports/cash-closures-summary
GET /api/v1/cash-closures
GET /api/v1/cash-closures/{closureId}
POST /api/v1/cash-closures
POST /api/v1/cash-closures/{closureId}/approve
POST /api/v1/cash-closures/{closureId}/reopen
```

Notes:
- cash closures are daily closures for the authenticated user, branch and date
- transfer, adjustment and closure report cards use dedicated filtered summary endpoints
- creating a closure stores status `CLOSED` and rejects duplicate user/branch/date closures
- approving a closure moves `CLOSED` to `APPROVED`; reopening moves `APPROVED` to `REOPENED`
- approve and reopen lock the closure row with `PESSIMISTIC_WRITE`
- closure create, approve and reopen write audit logs with affected branch
- closures do not modify inventory, FIFO, Kardex, sales or purchases

Login payload:

```json
{
  "tenantSlug": "demo",
  "username": "admin",
  "password": "ChangeMe123!"
}
```

Official local demo users, all with password `ChangeMe123!`:

```text
admin                 -> COMPANY_ADMIN, Main Branch
seller                -> SELLER, Main Branch
inventory             -> WAREHOUSE_USER, Main Branch
transfers_origin      -> MANAGER, Main Branch
transfers_destination -> WAREHOUSE_USER, Secondary Branch
auditor               -> AUDITOR, Main Branch
```

The recommended transfer demo flow is:

```text
transfers_destination creates the transfer request to receive stock.
transfers_origin approves and dispatches from Main Branch.
transfers_destination receives in Secondary Branch.
```

## Run Locally

1. Start PostgreSQL from the project root:

```bash
docker compose up -d
```

If `5433` is already occupied locally, update the root compose port mapping and export a matching `VIVENT_DB_URL` before starting the backend.

2. Ensure `JAVA_HOME` points to Java 25.

Optional JWT override for local development:

```bash
export VIVENT_JWT_SECRET="your-long-local-jwt-secret"
```

3. Start the backend from this directory:

```bash
./mvnw spring-boot:run
```

Flyway will create the schema and load demo data on startup.
