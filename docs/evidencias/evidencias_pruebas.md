# Evidencias de pruebas — Vivent académico

Casos y resultados de validaciones, permisos, procesos e integraciones externas.

## 1. Cómo reproducir las pruebas

```bash
# Pruebas de integración backend (requiere Docker Desktop corriendo)
cd backend && ./mvnw test

# Calidad frontend (Node 24 + pnpm 11)
cd frontend
pnpm run lint
pnpm exec tsc --noEmit --pretty false
NEXT_PRIVATE_BUILD_WORKER=1 pnpm run build
```

## 2. Resultado real de la suite (ejecutada en esta entrega)

Corrida del 2026-10-25 04:41 con **PostgreSQL 17.11 en Docker** (contenedor `vivent-academico-db`, puerto 5433) y Java 25:

```text
Flyway: Successfully applied 19 migrations to schema "public", now at version v20261026000000
Flyway: Successfully applied 10 migrations to schema "public", now at version v20261026000001
        (seeds S001–S009 + seed académico, historial propio flyway_seed_history)

[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 28.77 s
[INFO] BUILD SUCCESS
```

Los 9 casos automatizados cubren: carga del contexto, filtros opcionales de inventario,
listado de compañías de plataforma, creación con `taxEnabled` nulo, cambio de contraseña
propia (limpia `mustChangePassword` y rota `auth_version`), **anulación de compra confirmada
que revierte exactamente su capa FIFO**, reutilización de asignación al volver de sucursal,
venta en borrador compuesta solo por combos y **rotación del refresh token** (el token
anterior deja de servir y el nuevo queda persistido y utilizable).

Reproducir:

```bash
cd docker && docker compose -f compose.db.yml up -d     # PostgreSQL en 5433
cd ../backend && ./mvnw test                            # 8 pruebas de integración
```

## 3. Casos de validación y permisos

| # | Caso | Resultado esperado | Estado |
| --- | --- | --- | --- |
| V-01 | Login con credenciales inválidas | `INVALID_CREDENTIALS`, sin revelar cuál campo falló | ✅ |
| V-02 | Usuario inactivo intenta login | `USER_INACTIVE` | ✅ |
| V-03 | Token expirado (30 min) → renueva con refresh | Nuevo access + refresh sin re-login | ✅ |
| V-04 | Refresh token presentado dos veces | Segundo uso → `INVALID_TOKEN` (rotación) | ✅ |
| V-05 | Logout revoca el refresh | `INVALID_TOKEN` en siguiente uso | ✅ |
| V-06 | Cambio de contraseña revoca refresh previos | Tokens antiguos → `INVALID_TOKEN` | ✅ |
| V-07 | Vendedor (SELLER) accede por URL a /admin/security/users | Pantalla "Acceso denegado" | ✅ |
| V-08 | Acción con permiso en frontend pero sin permiso backend | API rechaza con `PERMISSION_DENIED` | ✅ |
| V-09 | Usuario de sucursal consulta sucursal ajena (cross-branch) | 403 `PERMISSION_DENIED` (backend) | ✅ |
| V-10 | Token inválido inyectado en sessionStorage | Redirección a /login | ✅ |

## 4. Casos de procesos integrados (API real, empresa demo)

| # | Proceso | Flujo ejecutado | Resultado | Estado |
| --- | --- | --- | --- | --- |
| P-01 | Compra | Crear DRAFT → confirmar | Capa FIFO creada, balance sube, kardex `PURCHASE_IN`, audit CONFIRM; doble confirmación rechazada | ✅ |
| P-02 | Venta POS | Crear DRAFT (CF) → confirmar | Consume FIFO, balance baja, kardex `SALE_OUT` con costo FIFO, audit CONFIRM | ✅ |
| P-03 | Venta sin stock | Confirmar más de lo disponible | `INSUFFICIENT_STOCK`, stock queda 0 (nunca negativo) | ✅ |
| P-04 | Traslado | Solicitar → aprobar (reserva) → despachar → recibir total | Estado `RECEIVED`, kardex `TRANSFER_OUT`/`TRANSFER_IN`, exceso de recepción rechazado | ✅ |
| P-05 | Traslado parcial | Recibir menos de lo aprobado | Estado `PARTIALLY_RECEIVED` con evidencia del faltante | ✅ |
| P-06 | Ajuste | Crear DAMAGE OUT → aprobar | Kardex `DAMAGE_OUT`; ajuste rechazado no toca inventario | ✅ |
| P-07 | Cierre de caja | Preview + crear con conteo | Único por usuario/sucursal/fecha; duplicado rechazado; diferencia calculada | ✅ |

## 5. Casos de integraciones externas

| # | Caso | Resultado | Estado |
| --- | --- | --- | --- |
| A-01 | Lookup externo de barcode conocido (Nutella) | `found=true` con nombre y marca | ✅ |
| A-02 | Lookup externo de barcode inexistente | `found=false` con mensaje claro | ✅ |
| A-03 | Lookup externo con servicio caído (timeout 4s) | `found=false` + mensaje; POS muestra error local original | ✅ |
| A-04 | Geocodificación Photon con dirección válida | Coordenadas devueltas; sin servicio → fallback manual | ✅ |
| A-05 | Tipo de cambio USD→GTQ (éxito) | `available=true` con tasa de referencia | ✅ |
| A-06 | Tipo de cambio con moneda inexistente | `available=false` con mensaje | ✅ |
| A-07 | Geocodificación con dirección vacía | `found=false` con mensaje | ✅ |

> La integración de correo (Nabrix Email) **no** forma parte de esta entrega: pertenece a la rama comercial `dev`. El detalle de las tres integraciones entregadas está en `docs/APIS_EXTERNAS.md`.

### 5.1 Evidencia real (2026-10-25, pila completa en Docker)

Ejecutado contra `http://localhost:8080` con el contenedor `vivent-academico-backend`:

```json
GET /api/v1/integrations/geocode?q=Parque%20Central%20Cuilapa
{"success":true,"data":{"query":"Parque Central Cuilapa","source":"PHOTON","found":true,
 "latitude":14.275497,"longitude":-90.298877,
 "label":"Parque Barrios, Cuilapa, Santa Rosa, Guatemala","message":null}}

GET /api/v1/integrations/geocode?q=
{"success":true,"data":{"query":"","source":"PHOTON","found":false,"latitude":null,
 "longitude":null,"label":null,"message":"Address query is required"}}

GET /api/v1/integrations/exchange-rate?base=USD&target=GTQ
{"success":true,"data":{"baseCurrency":"USD","targetCurrency":"GTQ","available":true,
 "rate":7.645339,"lastUpdatedUtc":"Sun, 25 Oct 2026 00:02:31 +0000","source":"OPEN_ER_API","message":null}}

GET /api/v1/integrations/exchange-rate?base=USD&target=XXX
{"success":true,"data":{"baseCurrency":"USD","targetCurrency":"XXX","available":false,
 "rate":null,"lastUpdatedUtc":null,"source":"OPEN_ER_API",
 "message":"Target currency is not supported by the external service"}}

GET /api/v1/products/barcode-lookup/3017620422003
{"success":true,"data":{"barcode":"3017620422003","source":"OPEN_FOOD_FACTS","found":true,
 "name":"Nutella","brand":"Nutella, Ferrero","quantityText":"400 g e",
 "imageUrl":"https://images.openfoodfacts.org/images/products/301/762/042/2003/front_en.879.200.jpg",
 "message":null}}

GET /api/v1/products/barcode-lookup/0000000000000
{"success":true,"data":{"barcode":"0000000000000","source":"OPEN_FOOD_FACTS","found":false,
 "name":null,"brand":null,"quantityText":null,"imageUrl":null,
 "message":"Product not found in the external catalog"}}
```

Los dos casos de error de red (servicio caído o timeout de 4 s) se validaron además
provocando la excepción en el servicio: la API responde `success=true` con
`found=false` / `available=false` y un mensaje comprensible, sin romper el proceso del POS.

### 6.1 Datos de demostración (operaciones reales, sin SQL manual)

`database/bootstrap_demo.py` crea las operaciones por la **API real** para que FIFO, kardex,
inventario y auditoría queden correctos por construcción. El script es idempotente y avanza la
máquina de estados del traslado paso a paso, de modo que una ejecución interrumpida se puede
retomar. Usa cada cuenta según sus permisos reales:

| Paso | Cuenta | Permiso que lo autoriza |
| --- | --- | --- |
| Compra (crear y confirmar) | `inventory` | `purchases.create`, `purchases.confirm` |
| Venta (crear y confirmar) | `seller` | `sales.create` |
| Venta con descuento | `admin` | `sales.apply_discount` (el vendedor no lo tiene) |
| Traslado (crear) | `admin` | `transfers.request` |
| Traslado (aprobar) | `admin` | `transfers.approve` |
| Traslado (despachar) | `transfers_origin` | `transfers.dispatch` |
| Traslado (recibir) | `admin` | `transfers.receive` |
| Ajuste (crear) | `inventory` | `inventory.adjust` |
| Ajuste (aprobar) | `admin` | `inventory.adjust_approve` |
| Cierre de caja | `seller` | `cash_closures.create` |

Resultado verificado en la base (2026-10-25):

```text
purchases 2 | sales 2 | transfers 1 | adjustments 1 | stock_movements 20 | cash_closures 1
```

> Primera versión del script confirmaba la venta y aprobaba el traslado con la cuenta de bodega,
> lo que devolvía `PERMISSION_DENIED`; se corrigió usando la cuenta que realmente tiene cada
> permiso, que es justamente lo que la matriz de permisos exige del backend.

> Bug real corregido en esta misma verificación: Spring Boot 4 usa Jackson 3 y los servicios
> deserializaban con `com.fasterxml.jackson.databind.JsonNode` (Jackson 2), lo que producía
> `HttpMessageConversionException` y hacía que las tres integraciones respondieran siempre
> "unavailable". Se migraron a `tools.jackson.databind.JsonNode` y, en Photon, se dejó de
> enviar `lang=es` (solo admite `default`, `de`, `en`, `fr`).

## 6. Datos de prueba

Los seeds crean catálogos con **≥5 registros cada uno** (unidades, categorías, productos con presentaciones, proveedores, clientes) y el script `database/bootstrap_demo.py` genera operaciones ficticias (compras, ventas con descuento, traslado completo, ajuste aprobado y cierre) suficientes para demostrar dashboard, procesos y los nueve reportes. Todo ficticio; sin datos personales reales.

## 7. Registro de ejecución

| Fecha | Prueba | Ejecutor | Resultado |
| --- | --- | --- | --- |
| 2026-10-25 04:41 | `mvnw test` con PostgreSQL 17.11 en Docker | Agente (Wilfero) | **Tests run: 9, Failures: 0, Errors: 0 — BUILD SUCCESS** (~29 s) |
| 2026-10-25 | Flyway en base limpia | Agente (Wilfero) | 19 migraciones + 10 seeds aplicadas correctamente |
| 2026-10-25 | `tsc --noEmit` del frontend | Agente (Wilfero) | Sin errores |
| 2026-10-25 | `docker compose -f compose.local.yml config` | Agente (Wilfero) | Sintaxis válida |
| 2026-10-25 12:21 | Pila completa en Docker (`up -d --build`) | Agente (Wilfero) | Contenedores db/backend/frontend arriba; healthcheck de BD en `healthy` |
| 2026-10-25 12:22 | Smoke HTTP: login, tenant-config, refresh, logout, `/v3/api-docs`, frontend | Agente (Wilfero) | Login `COMPANY_ADMIN` con 63 permisos; refresh rota el token; logout revoca; Swagger 200; frontend 200 |
| 2026-10-25 12:26 | Las tres APIs externas (éxito y error) | Agente (Wilfero) | Photon con coordenadas, USD→GTQ 7.645339, Nutella por código de barras; errores controlados |
| 2026-10-25 12:41 | `python database/bootstrap_demo.py` (datos de demostración por la API real) | Agente (Wilfero) | 2 compras confirmadas (FIFO), 2 ventas confirmadas, 1 traslado recibido, 1 ajuste aprobado, **20 movimientos de kardex**, 1 cierre de caja; segunda ejecución 100 % idempotente |
| 2026-10-25 12:42 | Reportes `sales-summary`, `purchases-summary`, `transfers-summary`, `adjustments-summary`, `cash-closures-summary` | Agente (Wilfero) | Los cinco responden `success=true` con los datos del bootstrap |
| (pendiente) | Smoke en línea (Vercel/Render/Neon) | Equipo | 📋 registrar |
