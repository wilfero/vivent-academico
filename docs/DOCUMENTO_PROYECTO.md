# Documento del proyecto — Vivent

## Portada

| Dato | Detalle |
| --- | --- |
| Universidad | Universidad Mariano Gálvez de Guatemala — Centro Universitario Cuilapa, Santa Rosa |
| Carrera | Ingeniería en Sistemas de Información y Ciencias de la Computación |
| Curso | Desarrollo Web |
| Catedrático | M.A. Ing. Elvis Morales |
| Ciclo | Octubre 2026 |
| Proyecto | Vivent — Sistema web SaaS multiempresa de gestión de inventarios |
| Modalidad | Sistema web alternativo con autorización del docente; alcance funcional equivalente al ERP solicitado |
| Integrantes | Christian Méndez · Wilfero Delgado · Fernando Palencia |
| Repositorio académico | `https://github.com/wilfero/vivent-academico` (privado, acceso al docente) |
| Fecha de entrega | 25 de octubre de 2026 |

---

## Índice

- 1. Negocio y giro
- 2. Problema
- 3. Objetivos
- 4. Alcance
- 5. Los seis módulos del negocio
- 6. Reglas del negocio (30)
- 7. Diseño del sistema
    - 7.1 Modelo conceptual
    - 7.2 Modelo de datos físico
    - 7.3 Arquitectura
    - 7.4 Flujos de los tres procesos integrados
- 8. Matriz de responsabilidades
- 9. Stack tecnológico y equivalencia
- 10. Repositorio, Docker y publicación
- 11. Anexos

---

## 1. Negocio y giro

Vivent atiende **empresas comerciales y distribuidoras pequeñas** de Guatemala: tiendas de barrio, distribuidoras de bebidas y abarrotes, papelerías y comedores que manejan mercadería física y, en varios casos, más de una sucursal.

El negocio modelo de la demostración es **Demo Vivent GT** (`demo.corpoviva.com`): una distribuidora de bebidas, snacks, abarrotes, productos de limpieza y papelería, con dos sucursales (Main Branch y Demo Branch 2), un almacén principal por sucursal, seis usuarios con distintos roles y operaciones diarias de compra, venta, traslado y ajuste.

Vivent es **SaaS multiempresa**: cada compañía se identifica por subdominio, tiene su propio color, logo, idioma (es-GT / en-US), moneda (GTQ por defecto) y zona horaria, y sus datos están aislados de los de cualquier otra compañía.

## 2. Problema

En este tipo de empresas el inventario se administra con cuadernos, hojas de cálculo o sistemas que solo registran la venta y no el costo del producto. Las consecuencias típicas:

- **No se sabe cuánto hay ni dónde está.** El conteo físico descubre diferencias sin explicación ni responsable.
- **El costo de lo vendido se calcula a mano.** Al no controlar capas de costo, la utilidad del mes es una estimación.
- **Las correcciones no dejan evidencia.** Daños, pérdidas y vencimientos se anotan sueltos o no se anotan.
- **El traslado entre sucursales es informal.** Se despacha por teléfono, se recibe de menos y nadie sabe qué faltó.
- **El acceso es total o nulo.** Todos ven y editan todo, sin permisos por rol ni bitácora.
- **La caja no cuadra y no se puede auditar.** No existe un cierre por usuario y sucursal con diferencia calculada.

## 3. Objetivos

**Objetivo general.** Desarrollar un sistema web que registre las operaciones de inventario de una empresa comercial multiempresa, aplicando reglas del negocio verificadas en el backend y dejando trazabilidad completa de cada movimiento.

**Objetivos específicos.**

1. Registrar compras y ventas por cualquier presentación, convirtiendo siempre a unidad base.
2. Costear el inventario con **FIFO/PEPS por capas** y reflejar todo movimiento en el **kardex**.
3. Controlar el acceso con **roles y permisos** `módulo.acción`, aplicados también en la API.
4. Operar **traslados entre sucursales** con reserva, despacho y recepción total o parcial.
5. Corregir el inventario con **ajustes** por motivo y aprobación.
6. Generar **nueve reportes** con filtros y exportación, más un dashboard con indicadores reales.
7. **Auditar** las acciones sensibles y conservar la trazabilidad con eliminación lógica.
8. Publicar base de datos, backend y frontend en la nube y demostrar un proceso completo en línea.

## 4. Alcance

### 4.1 Incluye

| Área | Alcance entregado |
| --- | --- |
| Autenticación | Login con `subdominio + usuario + contraseña` validado en el backend; **access token JWT (30 min) + refresh token (7 días)** con rotación, renovación y revocación en logout. |
| Usuarios y permisos | 6 roles del sistema, permisos `módulo.acción`, overrides GRANT/DENY por usuario; restricciones replicadas en la API. |
| Dashboard | Indicadores reales calculados desde la base de datos. |
| Módulos del negocio | 6 módulos completos (de 11 disponibles). |
| Procesos integrados | 3 procesos, uno por integrante, cada uno conectando al menos dos módulos. |
| Consultas | Búsqueda por texto, filtros combinables por módulo y ordenamiento por columnas. |
| Reportes | 9 reportes con filtros y exportación. |
| Validaciones | Frontend (Zod + React Hook Form) y backend (Bean Validation + reglas de servicio), con mensajes comprensibles. |
| APIs externas | 3 integraciones reales (Photon, Open Food Facts, tipo de cambio). |
| Publicación | Guía y configuración para BD + backend + frontend en la nube, con cuentas de prueba. |

### 4.2 No incluye (fase 2)

Facturación legal, integración tributaria, pagos automáticos reales, adjuntos S3, aplicación móvil, e-commerce público y cuentas por pagar/cobrar detalladas. La arquitectura no bloquea su incorporación posterior.

## 5. Los seis módulos del negocio

> Los módulos de login, dashboard y configuración general no se contabilizan, conforme al enunciado.

### 5.1 Catálogo (responsable: Christian Méndez)

- **Propósito:** definir qué se compra y se vende, con su unidad base, presentaciones y códigos de barras.
- **Actores:** COMPANY_ADMIN, BRANCH_ADMIN, MANAGER (lectura para vendedores).
- **Entidades:** `product_units`, `categories`, `products`, `product_presentations`.
- **Operaciones:** alta, edición y desactivación lógica de unidades, categorías, productos y presentaciones; consulta por código de barras.
- **Pantallas:** Unidades, Categorías, Productos (con su detalle de presentaciones), buscador de catálogo.
- **Reglas aplicadas:** R11, R12, R13, R29.
- **Permisos:** `units.*`, `product_categories.*`, `products.*`, `product_presentations.*`.

### 5.2 Inventario (responsable: Christian Méndez)

- **Propósito:** conocer la existencia real por sucursal y ubicación, su costo por capas y su historial.
- **Entidades:** `inventory_balances`, `inventory_layers`, `stock_movements`, `locations`.
- **Operaciones:** consulta de existencias por sucursal y global, kardex por producto, ajuste manual controlado del stock, consulta de capas FIFO activas.
- **Reglas aplicadas:** R14 a R18, R25, R28.
- **Permisos:** `inventory.view`, `inventory.view_kardex`, `inventory.adjust`, `inventory.adjust_approve`.

### 5.3 Compras (responsable: Wilfero Delgado)

- **Propósito:** ingresar mercadería de proveedores y crear las capas de costo.
- **Entidades:** `purchases`, `purchase_details`, `suppliers`.
- **Estados:** `DRAFT → CONFIRMED → VOIDED` (y `PENDING_APPROVAL` opcional).
- **Operaciones:** crear borrador, editar borrador, confirmar, listar con filtros, ver detalle, anular con motivo.
- **Reglas aplicadas:** R14, R15, R19, R20, R28.
- **Permisos:** `purchases.view`, `purchases.create`, `purchases.update`, `purchases.confirm`, `purchases.void`.

### 5.4 Ventas POS (responsable: Wilfero Delgado)

- **Propósito:** vender al mostrador con escaneo de código de barras, descuentos, cliente y método de pago.
- **Entidades:** `sales`, `sale_details`, `customers`.
- **Estados:** `DRAFT → CONFIRMED → CANCELLED`.
- **Operaciones:** abrir venta, identificar cliente (o consumidor final), escanear o buscar presentación, ajustar cantidades, aplicar descuentos, finalizar, anular con motivo y disposición de productos, consultar historial.
- **Reglas aplicadas:** R16, R17, R18, R21, R27, R28.
- **Permisos:** `sales.view`, `sales.create`, `sales.cancel`, `sales.apply_discount`, `sales.remove_item`, `sales.supervisor_authorize`.

### 5.5 Traslados (responsable: Fernando Palencia)

- **Propósito:** mover mercadería entre sucursales de la misma compañía con control y evidencia.
- **Entidades:** `transfers`, `transfer_details` + reservas en `inventory_balances`.
- **Estados:** `REQUESTED → APPROVED → IN_TRANSIT → RECEIVED / PARTIALLY_RECEIVED`, con `REJECTED` y `CANCELLED`.
- **Operaciones:** solicitar, aprobar (reserva), rechazar, despachar, recibir total o parcial, cancelar con motivo.
- **Separación de funciones:** la aprobación exige `transfers.approve` (MANAGER o COMPANY_ADMIN); el despacho exige `transfers.dispatch` (WAREHOUSE_USER). En la demo, `transfers_destination` solicita y recibe, `admin` aprueba y `transfers_origin` despacha: así se demuestra que un usuario sin permiso recibe 403.
- **Reglas aplicadas:** R10, R22, R23, R24, R28.
- **Permisos:** `transfers.view`, `transfers.request`, `transfers.approve`, `transfers.dispatch`, `transfers.receive`, `transfers.cancel`.

### 5.6 Ajustes (responsable: Fernando Palencia)

- **Propósito:** corregir el inventario con motivo, dirección y aprobación.
- **Entidades:** `inventory_adjustments`, `inventory_adjustment_lines`.
- **Tipos:** `PHYSICAL_COUNT`, `DAMAGE`, `LOSS`, `THEFT`, `EXPIRED`, `INTERNAL_USE`, `REGISTER_ERROR`, `SUPPLIER_RETURN`, `CUSTOM`. **Dirección:** `IN` / `OUT`.
- **Estados:** `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `CANCELLED`.
- **Operaciones:** crear, editar borrador, enviar a aprobación, aprobar, rechazar con motivo, cancelar con motivo.
- **Reglas aplicadas:** R25, R28, R29.
- **Permisos:** `inventory.adjust`, `inventory.adjust_approve`.

## 6. Reglas del negocio (30)

| # | Regla | Dónde se hace cumplir |
| --- | --- | --- |
| R1 | Una compañía no ve ni afecta datos de otra compañía. | Filtro obligatorio por `company_id` en servicios y consultas |
| R2 | Cada compañía se identifica por su subdominio. | Resolución de tenant en login y configuración pública |
| R3 | Un usuario pertenece a una sola compañía. | FK `users.company_id` + validación en token |
| R4 | Un usuario pertenece a una sucursal activa y puede trasladarse a otra de la misma compañía. | `users.branch_id` + historial de asignaciones |
| R5 | El username es único por compañía. | Índice único `(company_id, username)` |
| R6 | El login valida subdominio + usuario + contraseña. | `AuthService.login` |
| R7 | La sesión usa access token (30 min) y refresh token (7 días). | `JwtService` + `RefreshTokenService` |
| R8 | El refresh token rota en cada uso y se revoca al cerrar sesión y al cambiar contraseña. | Tabla `refresh_tokens`, `auth_version` |
| R9 | Los permisos se resuelven por rol y luego por overrides DENY y GRANT. | `PermissionResolutionService` |
| R10 | Un usuario no-COMPANY_ADMIN solo opera su sucursal. | Validación de branch/scope en servicios |
| R11 | Todo producto declara una unidad base. | `products.base_unit_code` obligatorio |
| R12 | Toda presentación se convierte a unidad base con factor > 0. | `product_presentations.conversion_factor` con CHECK |
| R13 | El código de barras pertenece a la presentación y es único por compañía. | Índice único parcial |
| R14 | Compras y ventas se registran por presentación y se convierten a unidad base. | Cálculo en servicios de compra/venta |
| R15 | Al confirmar una compra se crean capas FIFO. | `InventoryLayerService` |
| R16 | La venta consume primero las capas más antiguas con saldo. | `FifoConsumptionService` |
| R17 | `disponible = existencia − reservado` es columna generada. | `inventory_balances` (GENERATED ALWAYS) |
| R18 | No se vende sin stock disponible; el stock nunca es negativo. | CHECK en BD + validación en servicio |
| R19 | La compra confirmada es inmutable; se corrige anulándola con motivo. | Estado `CONFIRMED` sin edición |
| R20 | La anulación de compra revierte exactamente sus capas originales. | Reverso por `purchase_detail_id` |
| R21 | La anulación de venta exige permiso, motivo y disposición de productos. | `SalesService.cancel` |
| R22 | El traslado reserva stock desde la aprobación. | `quantity_reserved` en consolidación |
| R23 | La recepción puede ser parcial y deja evidencia del faltante. | `quantity_approved_base` vs `quantity_received_base` |
| R24 | No se traslada entre compañías ni se recibe más de lo aprobado. | Validación de tenant y cantidades |
| R25 | Los ajustes se aprueban antes de impactar el inventario. | `AdjustmentsService.approve` |
| R26 | El cierre de caja es único por usuario, sucursal y fecha. | Restricción única en BD |
| R27 | Los descuentos requieren permiso y quedan registrados. | `sales.apply_discount` + auditoría |
| R28 | Toda acción sensible se audita con valores antes/después. | `AuditLogService` + tablas de auditoría |
| R29 | Nada se borra físicamente (`deleted_at` / estados). | Repositorios con filtro `deleted_at IS NULL` |
| R30 | No se versionan credenciales; las contraseñas se guardan con BCrypt. | `.gitignore`, `.env.example`, `BCryptPasswordEncoder` |

## 7. Diseño del sistema

### 7.1 Modelo conceptual

El sistema se organiza en cinco dominios: **identidad**, **catálogo**, **inventario**, **operaciones** y **administración**.

```text
┌──────────────────────────── IDENTIDAD ────────────────────────────┐
│ Company 1 ──< Branch 1 ──< Location                                │
│    │            │                                                  │
│    │            └──< User >── Role >── RolePermission >── Permission│
│    │                     │                                         │
│    └──< Subscription >── PlatformPlan                              │
└───────────────────────────────────────────────────────────────────┘

┌──────────────────────── CATÁLOGO ─────────────────────────────────┐
│ ProductUnit   Category (jerárquica)                               │
│      │            │                                                │
│      └──────< Product >──< ProductPresentation (barcode, factor)  │
└───────────────────────────────────────────────────────────────────┘

┌──────────────────────── INVENTARIO ───────────────────────────────┐
│ InventoryBalance (on_hand, reserved, available)                   │
│ InventoryLayer   (cantidad recibida/restante, costo, lote, venc.) │
│ StockMovement    (in/out, tipo, origen, costo, saldo posterior)   │
└───────────────────────────────────────────────────────────────────┘

┌──────────────────────── OPERACIONES ──────────────────────────────┐
│ Supplier >──< Purchase >──< PurchaseDetail >── Presentation       │
│ Customer >──< Sale     >──< SaleDetail     >── Presentation       │
│ Transfer >──< TransferDetail >── Presentation                     │
│ InventoryAdjustment >──< AdjustmentLine >── Product/Presentation  │
│ CashClosure (usuario, sucursal, fecha, esperado, contado)          │
└───────────────────────────────────────────────────────────────────┘

┌──────────────────────── ADMINISTRACIÓN ───────────────────────────┐
│ AuditLog        RefreshToken        PlatformAudit                  │
│ UserPermissionOverride (GRANT/DENY)                                │
└───────────────────────────────────────────────────────────────────┘
```

**Relaciones principales**

| Relación | Cardinalidad | Regla asociada |
| --- | --- | --- |
| Company → Branch | 1 : N | R1, R2 |
| Branch → Location | 1 : N (una por defecto) | R10 |
| Company → User | 1 : N | R3, R5 |
| Role → Permission | N : M | R9 |
| Product → Presentation | 1 : N | R11, R12 |
| Presentation → SaleDetail / PurchaseDetail | 1 : N | R13, R14 |
| Purchase → InventoryLayer | 1 : N | R15 |
| Sale → StockMovement (SALE_OUT) | 1 : N | R16, R18 |
| Transfer → StockMovement (OUT/IN) | 1 : N | R22, R23 |

### 7.2 Modelo de datos físico

Motor: **PostgreSQL 16/17**. Esquema creado desde cero con migraciones Flyway versionadas (`V*.sql`); datos de prueba con seeds idempotentes (`S*.sql`, historial propio en `flyway_seed_history`).

**Tablas núcleo y columnas relevantes**

| Tabla | Columnas clave | Restricciones |
| --- | --- | --- |
| `companies` | `id`, `tenant_slug`, `domain`, `commercial_name`, `currency_code`, `timezone`, `status` | UNIQUE `tenant_slug` |
| `branches` | `id`, `company_id`, `code`, `name`, `is_active` | UNIQUE `(company_id, code)` |
| `locations` | `id`, `company_id`, `branch_id`, `code`, `type`, `is_default` | UNIQUE `(branch_id, code)` |
| `users` | `id`, `company_id`, `branch_id`, `username`, `password_hash`, `role_id`, `auth_version`, `status` | UNIQUE `(company_id, username)` |
| `permissions` | `id`, `code` (`module.action`), `module_code`, `action_code` | UNIQUE `code` |
| `role_permissions` | `role_id`, `permission_id` | PK compuesta |
| `user_permission_overrides` | `user_id`, `permission_id`, `effect` | CHECK `effect IN ('GRANT','DENY')` |
| `refresh_tokens` | `id`, `user_id`, `company_id`, `token_hash`, `expires_at`, `revoked_at` | UNIQUE `token_hash`, índice por usuario |
| `product_units` | `code` (PK), `name`, `allows_decimal` | CHECK factor decimal |
| `categories` | `id`, `company_id`, `parent_category_id`, `name` | UNIQUE `(company_id, name)` |
| `products` | `id`, `company_id`, `base_unit_code`, `sku`, `minimum_stock`, `is_stockable` | FK unidad base, UNIQUE `(company_id, sku)` |
| `product_presentations` | `id`, `product_id`, `barcode`, `conversion_factor`, `sale_price` | CHECK `conversion_factor > 0`, UNIQUE barcode |
| `inventory_balances` | `company_id`, `branch_id`, `location_id`, `product_id`, `quantity_on_hand`, `quantity_reserved`, `quantity_available` (generada) | CHECK `on_hand >= 0`, `reserved >= 0` |
| `inventory_layers` | `id`, `product_id`, `quantity_received`, `quantity_remaining`, `unit_cost`, `purchase_detail_id`, `expiration_date`, `lot_number` | CHECK `remaining >= 0` |
| `stock_movements` | `id`, `movement_type`, `source_type`, `source_id`, `quantity_in`, `quantity_out`, `unit_cost`, `balance_after` | Índice por producto/fecha |
| `purchases` | `id`, `branch_id`, `supplier_id`, `purchase_number`, `status`, `total_amount` | UNIQUE `(company_id, purchase_number)` |
| `purchase_details` | `id`, `purchase_id`, `presentation_id`, `quantity_presentation`, `quantity_base`, `unit_cost_base` | CHECK cantidades > 0 |
| `sales` | `id`, `branch_id`, `customer_id`, `sale_number`, `status`, `payment_method`, `total_amount` | UNIQUE `(company_id, sale_number)` |
| `sale_details` | `id`, `sale_id`, `presentation_id`, `quantity_base`, `unit_price`, `discount_amount`, `fifo_cost_total` | CHECK cantidades > 0 |
| `transfers` | `id`, `source_branch_id`, `destination_branch_id`, `status`, `requested_by`, `approved_by`, `received_by` | CHECK `source <> destination` |
| `transfer_details` | `id`, `transfer_id`, `quantity_requested_base`, `quantity_approved_base`, `quantity_received_base` | CHECK recibido ≤ aprobado |
| `inventory_adjustments` | `id`, `branch_id`, `adjustment_type`, `direction`, `status`, `reason` | CHECK dirección IN/OUT |
| `cash_closures` | `id`, `branch_id`, `user_id`, `closure_date`, `expected_total`, `counted_total`, `difference`, `status` | UNIQUE `(branch_id, user_id, closure_date)` |
| `audit_logs` | `id`, `company_id`, `branch_id`, `user_id`, `action`, `entity_type`, `entity_id`, `old_value`, `new_value`, `created_at` | Índice por entidad y fecha |

**Decisiones de integridad**

- El saldo disponible **no se escribe a mano**: es una columna generada (`on_hand − reserved`).
- Los CHECK de cantidades impiden valores negativos, de modo que ninguna ruta de código puede dejar stock negativo.
- El cierre de caja tiene restricción única por usuario/sucursal/fecha, evitando duplicados.
- Los índices únicos por compañía (username, sku, número de compra/venta) implementan las reglas de unicidad.

### 7.3 Arquitectura

**Patrón:** monolito modular en capas. Un solo despliegue, módulos separados por dominio, dependencias en una sola dirección (controller → service → repository → entity) y reglas del negocio **solo** en la capa de servicio.

```text
┌────────────────────────── Frontend (Next.js 16) ──────────────────────────┐
│ App Router · React 19 · TypeScript · Tailwind · shadcn/ui + Radix         │
│ TanStack Query/Table · React Hook Form + Zod · next-intl · next-themes    │
│  features/*: auth tenant products purchases sales transfers adjustments   │
│              inventory closures reports users                              │
│  lib/api/http.ts: fetch central + renovación transparente de tokens       │
└───────────────────────────────────┬───────────────────────────────────────┘
                                    │ HTTPS / JSON  (ApiResponse<T>)
┌───────────────────────────────────▼───────────────────────────────────────┐
│                        Backend (Spring Boot 4 · Java 25)                   │
│  seguridad: JwtAuthenticationFilter → PasswordChangeRequiredFilter        │
│  controllers → services (reglas) → repositories (JPA) → PostgreSQL        │
│  módulos: auth tenant companies branches users permissions products       │
│           inventory purchases sales transfers adjustments closures        │
│           reports audit subscriptions platform + integrations             │
│  transversales: ApiResponse, manejo global de errores, auditoría          │
└───────────────────────────────────┬───────────────────────────────────────┘
                                    │ JDBC (pool)
┌───────────────────────────────────▼───────────────────────────────────────┐
│                      PostgreSQL 16/17 (Docker / Neon)                      │
│  migraciones V*.sql + seeds S*.sql (Flyway, dos historiales)              │
└───────────────────────────────────────────────────────────────────────────┘
```

**Seguridad de la arquitectura**

- Toda petición privada pasa por el filtro JWT y por `@PreAuthorize` con el permiso `módulo.acción`.
- El tenant se toma del token, nunca del cuerpo de la petición, y cada consulta filtra por `company_id`.
- Los usuarios sin rol COMPANY_ADMIN quedan limitados a su sucursal en la capa de servicio.
- El frontend oculta acciones sin permiso, pero **la API siempre revalida**.

### 7.4 Flujos de los tres procesos integrados

Cada proceso conecta al menos dos módulos y aplica reglas del negocio; su responsable se indica en el título.

#### P1 — Compra con entrada a inventario FIFO (Christian Méndez)

**Módulos conectados:** Compras + Inventario (y Catálogo como origen de presentaciones).
**Actores:** bodeguero (`inventory`, rol WAREHOUSE_USER) crea y confirma; la anulación exige además `purchases.void`.
**Precondiciones:** proveedor activo, presentaciones activas con unidad base, usuario con `purchases.create` y `purchases.confirm`.

```text
Bodeguero            API / Servicio               Base de datos
   |                        |                          |
   | POST /purchases (DRAFT)|                          |
   |----------------------->| valida proveedor,        |
   |                        | presentaciones y tenant  |
   |                        |------------------------->| INSERT purchases
   |                        |                          | INSERT purchase_details
   |<-----------------------| 201 DRAFT                |
   |                        |                          |
   | POST /{id}/confirm     |                          |
   |----------------------->| valida estado y detalle  |
   |                        | qty_base = qty x factor  |
   |                        |------------------------->| INSERT inventory_layers
   |                        |                          | UPDATE balances (+)
   |                        |                          | INSERT movements (PURCHASE_IN)
   |                        |                          | INSERT audit (CONFIRM)
   |<-----------------------| 200 CONFIRMED            |
```

**Paso a paso**

1. El bodeguero abre **Compras → Nueva compra** y elige proveedor y fecha.
2. Agrega líneas por **presentación** (Lata, Six Pack, Caja 24), con cantidad y costo de la presentación.
3. El backend convierte cada línea a unidad base (`cantidad × factor`) y calcula subtotales.
4. Guarda como **borrador** (`DRAFT`); puede editar o eliminar el borrador.
5. Al **confirmar**, el servicio valida: estado `DRAFT`, al menos una línea, cantidades > 0, tenant y sucursal del usuario.
6. Por cada línea crea una **capa FIFO** con cantidad recibida, costo unitario en unidad base, lote y vencimiento opcionales.
7. Aumenta `quantity_on_hand` del balance de la sucursal/almacén y recalcula `quantity_available`.
8. Registra un movimiento `PURCHASE_IN` en el kardex por línea, con costo y saldo posterior.
9. Marca la compra `CONFIRMED`, guarda `confirmed_by` y `confirmed_at` y escribe la auditoría.
10. Una compra confirmada **ya no se edita**; si hubo error se anula con motivo (R19) y el sistema revierte exactamente las capas creadas por esa compra (R20).

**Datos afectados:** `purchases`, `purchase_details`, `inventory_layers`, `inventory_balances`, `stock_movements`, `audit_logs`.
**Errores manejados:** sin líneas, producto/presentación inactiva, proveedor de otra compañía, doble confirmación, anulación sin motivo, anulación de compra ya anulada.
**Evidencia:** detalle de compra con estado, kardex del producto con `PURCHASE_IN`, capas FIFO visibles con costo y saldo.

#### P2 — Venta en POS, consumo FIFO y cierre de caja (Wilfero Delgado)

**Módulos conectados:** Ventas + Inventario + Cierres de caja.
**Actores:** vendedor (`seller`) opera el POS y crea el cierre; administrador aprueba el cierre.
**Precondiciones:** caja abierta del día, stock disponible, usuario con `sales.create`; descuentos requieren `sales.apply_discount`.

```text
Vendedor             API / Servicio               Base de datos
   |                        |                          |
   | GET /by-barcode/{code} |                          |
   |----------------------->| resuelve presentacion,   |
   |<-----------------------| factor y precio          |
   |                        |                          |
   | POST /sales (DRAFT)    |                          |
   |----------------------->| valida disponible        |
   |                        | (on_hand - reserved)     |
   |                        | si no alcanza: INSUFFICIENT_STOCK
   |                        |------------------------->| INSERT sales/details
   |<-----------------------| 201 DRAFT                |
   |                        |                          |
   | POST /{id}/confirm     |                          |
   |----------------------->| consume capas FIFO       |
   |                        |------------------------->| UPDATE layers (-)
   |                        |                          | UPDATE balances (-)
   |                        |                          | INSERT movements (SALE_OUT)
   |                        |                          | UPDATE fifo_cost_total
   |                        |                          | INSERT audit (CONFIRM)
   |<-----------------------| 200 CONFIRMED            |
   |                        |                          |
   | POST /cash-closures    |                          |
   |----------------------->| esperado = efectivo      |
   |                        | - anuladas - descuentos  |
   |                        |------------------------->| INSERT cash_closures
   |<-----------------------| 200 con diferencia       |
```

**Paso a paso**

1. El vendedor abre **Ventas → Nueva venta**; el POS toma la sucursal de su sesión.
2. Identifica al cliente por NIT o usa **Consumidor Final**.
3. Escanea el código de barras; el backend resuelve producto, presentación, factor y precio.
4. Si el código no existe en el catálogo local, el POS consulta el **catálogo externo** (Open Food Facts) y ofrece registrar el producto con su presentación unitaria en un paso.
5. Cada línea se agrega al carrito en cantidad de presentación; el backend valida **stock disponible** antes de aceptar.
6. Aplica descuentos si el rol tiene `sales.apply_discount` (por línea o por venta).
7. Elige método de pago y **finaliza** (`DRAFT → CONFIRMED`).
8. El servicio consume capas **FIFO** de la más antigua a la más nueva, calcula el costo real (`fifo_cost_total`) y descuenta el balance.
9. Registra `SALE_OUT` en el kardex por línea y la auditoría con totales.
10. La venta queda disponible para el **cierre de caja**: vista previa con ventas confirmadas, anuladas, descuentos y totales por método de pago.
11. El vendedor registra el **monto contado**; el sistema calcula la **diferencia** y deja el cierre en estado abierto/cerrado.
12. El administrador puede **aprobar** el cierre o **reabrirlo** para recalcular si hubo correcciones.

**Anulación:** exige permiso `sales.cancel`, motivo obligatorio y disposición de los productos (devolver a inventario, enviar a daño, enviar a pérdida o no devolver). Devuelve o ajusta exactamente las cantidades vendidas, con movimientos `SALE_CANCEL_IN` o de ajuste.
**Datos afectados:** `sales`, `sale_details`, `inventory_layers`, `inventory_balances`, `stock_movements`, `cash_closures`, `audit_logs`.
**Errores manejados:** stock insuficiente, cantidad cero o negativa, producto inactivo, descuento sin permiso, doble confirmación, anulación sin motivo, cierre duplicado del día.

#### P3 — Traslado entre sucursales (Fernando Palencia)

**Módulos conectados:** Traslados + Inventario en origen y destino.
**Actores:** `transfers_destination` (WAREHOUSE_USER, sucursal 2) solicita y recibe; `admin` (COMPANY_ADMIN) aprueba; `transfers_origin` (WAREHOUSE_USER, sucursal 1) despacha.
**Precondiciones:** dos sucursales activas de la **misma** compañía, stock disponible en origen, permisos `transfers.request/approve/dispatch/receive`.

```text
Destino              API / Servicio               Base de datos
   | POST /transfers        |                          |
   |----------------------->| valida misma compania    |
   |                        | y que haya detalle       |
   |                        |------------------------->| INSERT transfers/details
   |<-----------------------| 201 REQUESTED            |
   |                        |                          |
Aprueba: POST /{id}/approve |                          |
   |----------------------->| valida stock en origen   |
   |                        |------------------------->| balances (+reserved, -available)
   |<-----------------------| 200 APPROVED             |
   |                        |                          |
Origen: POST /{id}/dispatch |                          |
   |----------------------->| registra salida fisica   |
   |                        |------------------------->| movements (TRANSFER_RESERVED)
   |<-----------------------| 200 IN_TRANSIT           |
   |                        |                          |
Destino: POST /{id}/receive (total o parcial)          |
   |----------------------->| valida recibido <= aprobado
   |                        |------------------------->| origen (-on_hand, -reserved)
   |                        |                          | destino (+on_hand)
   |                        |                          | movements OUT / IN
   |                        |                          | estado RECEIVED/PARTIALLY
   |<-----------------------| 200                      |
```

**Paso a paso**

1. La sucursal destino crea la solicitud indicando sucursal origen, productos y cantidades (en presentación o unidad base).
2. El backend valida que ambas sucursales pertenezcan a la misma compañía (R24) y que haya detalle.
3. Un usuario con `transfers.approve` (MANAGER o COMPANY_ADMIN; en la demo, `admin`) revisa y **aprueba**: se reserva stock en origen (`quantity_reserved` sube, `quantity_available` baja) sin descontar existencia física (R22). Si `transfers_origin` intenta aprobar, la API responde `PERMISSION_DENIED`.
4. Si no hay stock suficiente para reservar, la aprobación se rechaza.
5. La sucursal origen **despacha**: el traslado pasa a `IN_TRANSIT` y se registra el movimiento de reserva/tránsito.
6. La sucursal destino **recibe**. Puede recibir **menos** de lo aprobado: el sistema guarda `quantity_received_base` y deja la diferencia como evidencia (R23), con estado `PARTIALLY_RECEIVED`.
7. Al recibir se descuenta la existencia y la reserva en origen, se suma la existencia en destino y se generan `TRANSFER_OUT` y `TRANSFER_IN` en el kardex de cada sucursal.
8. Cualquier cancelación exige motivo y libera la reserva.
9. Todo el ciclo queda auditado con usuario, fecha y cantidades.

**Datos afectados:** `transfers`, `transfer_details`, `inventory_balances` (origen y destino), `stock_movements`, `audit_logs`.
**Errores manejados:** traslado entre compañías, sin detalle, recepción mayor a lo aprobado, aprobar un traslado ya aprobado, recibir un traslado no despachado, cancelar sin motivo.

## 8. Matriz de responsabilidades

| Integrante | Módulos (2) | Proceso (1) | Reportes (3) | API externa (1) | Componentes compartidos |
| --- | --- | --- | --- | --- | --- |
| **Christian Méndez** | Catálogo · Inventario | P1 Compra → FIFO | Existencias por sucursal · Kardex por producto · Productos con stock bajo | **Photon** (geocodificación real) | Autenticación y permisos · Auditoría |
| **Wilfero Delgado** | Compras · Ventas POS | P2 Venta → Cierre | Ventas por período · Compras por proveedor · Cierre de caja | **Open Food Facts** (barcode externo) | Dashboard · Docker y publicación |
| **Fernando Palencia** | Traslados · Ajustes | P3 Traslado entre sucursales | Traslados por estado · Ajustes de inventario · Movimientos de inventario | **open.er-api.com** (tipo de cambio) | Datos de prueba · Revisión de seguridad |

**Transversales distribuidos:** login (los tres), dashboard (Wilfero), publicación (Wilfero + Christian), datos de prueba (Fernando), documentación y evidencias (los tres).

## 9. Stack tecnológico y equivalencia

| Capa | Tecnología | Justificación |
| --- | --- | --- |
| Base de datos | PostgreSQL 16/17 | Coincide con el Stack 2 del enunciado. |
| Backend | Java 25 LTS + Spring Boot 4 | Equivalente funcional a NestJS: API REST, inyección de dependencias, validación, seguridad, ORM y migraciones. Ver `JUSTIFICACION_STACK_SPRING_BOOT.md`. |
| Frontend | Next.js 16 (React 19) + TypeScript | Coincide con el Stack 2. |
| Estilos | Tailwind CSS + shadcn/ui + Radix | HTML/CSS exigido, con accesibilidad. |
| Estado y formularios | TanStack Query/Table, React Hook Form, Zod | Consultas, tablas y validación en frontend. |
| i18n / tema | next-intl, next-themes | es-GT / en-US, claro / oscuro. |
| Contenedores | Docker + Docker Compose | BD local y stack completo. |
| Pruebas de API | Swagger/OpenAPI + Postman | Colección por módulo, auth e integraciones. |
| Control de versiones | Git + GitHub | Historial de los tres integrantes. |
| Agente de IA | `AGENTS.md`, `SKILL.md`, `memory.md` | Instrucciones, habilidades y memoria del proyecto. |

**Solicitud de equivalencia:** el proyecto usa Spring Boot en lugar de NestJS. El alcance funcional, la seguridad, la arquitectura y la documentación son equivalentes; la solicitud formal está en `JUSTIFICACION_STACK_SPRING_BOOT.md`.

## 10. Repositorio, Docker y publicación

**Repositorio:** `https://github.com/wilfero/vivent-academico` (privado; el docente recibe acceso).
Estructura: `backend/`, `frontend/`, `database/`, `docker/`, `docs/`, `postman/`, `README.md`, `.env.example`, `.gitignore`, `AGENTS.md`, `SKILL.md`, `memory.md`.

**Arranque local con Docker (una sola orden):**

```bash
cd docker
docker compose -f compose.local.yml up -d --build
```

Servicios: PostgreSQL en `localhost:5433`, backend en `localhost:8080`, frontend en `localhost:3000`. En el primer arranque el backend aplica migraciones y seeds automáticamente.

**Operaciones demo (opcional, idempotente):**

```bash
python database/bootstrap_demo.py
```

Crea compras, ventas con descuento, un traslado completo, un ajuste aprobado y el cierre del día a través de la API real.

**Publicación en la nube:** guía en `PUBLICACION_CLOUD.md` (Neon + Render + Vercel), con evidencia de conexión y cuentas de prueba.

## 11. Anexos

**Cuentas demo** (contraseña `ChangeMe123!`, solo ambiente de prueba):

| Usuario | Rol | Uso demostrativo |
| --- | --- | --- |
| `admin` | COMPANY_ADMIN | Administración completa; aprueba traslados, ajustes y cierres; reportes y auditoría |
| `seller` | SELLER | POS y cierre de caja |
| `inventory` | WAREHOUSE_USER | Compras (crear/confirmar) y ajustes |
| `transfers_origin` | WAREHOUSE_USER | Despacho de traslados de la sucursal 1 |
| `transfers_destination` | WAREHOUSE_USER (sucursal 2) | Solicitar y recibir traslados |
| `auditor` | AUDITOR | Solo lectura de reportes y bitácora |

**Documentos complementarios**

- `MATRIZ_CUMPLIMIENTO_DESARROLLO_WEB.md` — requisito por requisito con evidencia.
- `MANUAL_TECNICO.md` — instalación, configuración, estructura y APIs externas.
- `MANUAL_USUARIO.md` — guía ilustrada de los procesos principales.
- `APIS_EXTERNAS.md` — las tres integraciones con ejemplos de éxito y error.
- `PUBLICACION_CLOUD.md` — publicación y checklist de entrega.
- `evidencias/evidencias_pruebas.md` — casos y resultados de pruebas.
- Colección Postman: `postman/Vivent.postman_collection.json`.
