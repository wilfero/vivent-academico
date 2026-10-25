# Colección Postman — API de Vivent

Esta carpeta contiene todo lo necesario para probar la API **sin escribir código**:

| Archivo | Para qué sirve |
| --- | --- |
| `Vivent.postman_collection.json` | Colección con **128 requests** organizadas en 14 carpetas (CRUD completo del sistema). |
| `Vivent-Local.postman_environment.json` | Environment `Vivent Local` con la URL base y las credenciales demo. |

Resultado esperado de una corrida completa: **128 requests, 242 aserciones, 0 fallos** (≈25 s).

---

## 0. Colección enfocada: Ventas POS (CRUD)

Además de la colección completa, en esta carpeta está **`Vivent-Ventas-POS.postman_collection.json`**: 19 requests dedicadas a demostrar el proceso de ventas POS en la defensa.

> Guía visual con **todas las requests** (método, URL, body y trucos): abre `docs/TRUCOS_Y_POSTMAN.html` en el navegador. Es autocontenida y se puede imprimir.

Usa el **mismo environment** (`Vivent-Local`) y se ejecuta sola de principio a fin:

```
00 · Preparacion      Login + producto con stock + presentación vendible
01 · Crear (C)        POST /sales  →  GET /sales/{id}
02 · Leer (R)         GET /sales?status=DRAFT&query=POS-CRUD-<runId>
03 · Actualizar (U)   PUT /sales/{id} → GET para verificar
04 · Eliminar (D)     DELETE /sales/{id} → GET (404 esperado)
05 · Cobrar           crear → stock antes → confirmar → stock después → kardex SALE_OUT
06 · Anular           cancel con motivo → kardex SALE_CANCEL_IN
07 · Validaciones     confirmar una venta anulada (400) y crear venta sin líneas (400)
```

Resultado esperado: **19 requests, 65 aserciones, 0 fallos** (≈6 s). Se puede ejecutar repetidamente: el `runId` se genera una sola vez en el login.

```bash
npx newman run postman/Vivent-Ventas-POS.postman_collection.json \
  -e postman/Vivent-Local.postman_environment.json
```

Detalle útil: en el kardex, `sourceId` es el **id del detalle** de la venta (`sourceType = SALE_DETAIL`), no el id de la venta. La colección lo captura como `saleDetailId` y lo usa en las aserciones.

---

## 1. Requisitos

1. **Postman** 10 o superior (también funciona la extensión de VS Code o Newman).
2. El **stack local levantado**:

   ```bash
   cd docker
   docker compose -f compose.local.yml up -d --build
   ```

3. La API respondiendo en `http://localhost:8080` (verifíquelo con
   `http://localhost:8080/api/v1/public/tenant-config?subdomain=demo`).
4. Datos operativos de demostración (una sola vez):

   ```bash
   python database/bootstrap_demo.py
   ```

---

## 2. Instalación en Postman (3 pasos)

1. Abra Postman → botón **Import** → arrastre **los dos archivos** de esta carpeta.
2. Arriba a la derecha, seleccione el environment **Vivent Local**.
3. Abra la carpeta **01 · Autenticación** y ejecute **Login (guarda tokens)**.

Con eso, el resto de requests ya llevan el token: la colección tiene configurado
`Bearer {{accessToken}}` a nivel de colección y el login escribe esa variable
automáticamente (junto con `refreshToken`, `branchId`, `companyId` y `userId`).

> **Importante:** el environment **no** declara `accessToken` a propósito. Si lo
> declarara vacío, tendría prioridad sobre la variable de colección y todos los
> requests responderían `401`.

---

## 3. Orden de ejecución recomendado

| # | Carpeta | Qué demuestra |
| --- | --- | --- |
| 00 | Público | Configuración del tenant por subdominio (sin token). |
| 01 | Autenticación | Login, `/auth/me`, refresh con rotación, logout y login fallido (401). |
| 02 | Configuración del tenant | Empresa, sucursales y suscripción. |
| 03 | Catálogo | CRUD de categorías, unidades, productos, presentaciones y combos. |
| 04 | Terceros | CRUD de proveedores y clientes. |
| 05 | Inventario | Balances, capas FIFO, kardex, movimientos, entradas/salidas y ajustes. |
| 06 | Compras | Crear borrador → confirmar (FIFO + kardex) → anular con motivo. |
| 07 | Ventas | CRUD del borrador → confirmar (consume FIFO) → anular con motivo. |
| 08 | Traslados | Solicitar → aprobar (reserva) → despachar → recibir. |
| 09 | Cierres de caja | Preview, crear, aprobar, reabrir y cerrar con monto contado. |
| 10 | Reportes | Los seis reportes con filtros de fecha. |
| 11 | Integraciones externas | Photon, Open Food Facts y tipo de cambio (éxito y error). |
| 12 | Usuarios y permisos | CRUD de roles y usuarios, overrides, perfil propio. |
| 13 | Auditoría | Bitácora de acciones sensibles. |

Cada carpeta se puede ejecutar con **Run folder** (runner de Postman) siempre que
antes se haya ejecutado el login. Para una corrida completa use
**Run collection** (necesita el environment seleccionado).

---

## 4. Variables

### 4.1 Variables de entrada (las edita el usuario)

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `baseUrl` | `http://localhost:8080` | URL de la API. Cámbiela si publica el sistema en otro host. |
| `tenantSlug` | `demo` | Subdominio de la empresa (multi-tenant). |
| `username` | `admin` | Usuario demo. |
| `password` | `ChangeMe123!` | Contraseña demo (solo ambiente local). |

### 4.2 Variables que se llenan solas

| Variable | La escribe | Se usa en |
| --- | --- | --- |
| `accessToken`, `refreshToken` | Login / Refresh | Todos los requests privados. |
| `companyId`, `branchId`, `userId`, `roleCode` | Login | Contexto del token. |
| `branch2Id` | `GET Sucursales` | Traslados. |
| `categoryId`, `productId`, `presentationId`, `comboId`, `supplierId`, `customerId` | Creaciones del catálogo y terceros | Compras, ventas, traslados, ajustes. |
| `purchaseId`, `saleId`, `transferId`, `transferDetailId`, `adjustmentId`, `closureId` | Creaciones de operaciones | Confirmar, anular, recibir, aprobar. |
| `roleId`, `newUserId` | Roles / Usuarios | Permisos y perfil. |
| `tempCategoryId`, `tempProductId`, `tempPresentationId`, `tempSupplierId` | Creaciones temporales | Demostrar la baja lógica sin romper la cadena. |
| `runId` | Pre-request de la colección y login | Sufijo único por corrida (nombres, SKU, códigos de barras). |
| `today`, `yesterday`, `nowIso` | Pre-request de la colección | Fechas de reportes y cierres. |
| `closureDate` | Pre-request de *Crear cierre* | Fecha única del cierre. |

---

## 5. Detalles que conviene conocer

- **`runId`**: al ejecutar el login se genera un sufijo numérico nuevo. Por eso la
  colección se puede correr **varias veces seguidas** sin chocar con SKU, códigos
  de barras, usuarios o números de documento ya existentes.
- **Errores esperados**: algunos requests prueban validaciones y por diseño
  responden 4xx (`Login con clave incorrecta`, `Rechazar ajuste (ya aprobado)`,
  `Cancelar traslado (ya recibido)`, `Cambiar mi contraseña (validación)`). Sus
  tests verifican `success = false`, así que **no cuentan como fallo**.
- **Fechas de reportes**: `sales-summary`, `products`, `transfers-summary` y
  `adjustments-summary` reciben fecha-hora ISO
  (`2026-10-01T00:00:00Z`); `purchases-summary` y `cash-closures-summary` reciben
  fecha simple (`2026-10-01`). Los requests ya vienen con el formato correcto.
- **Usuarios demo**: `admin`, `seller`, `inventory`, `transfers_origin`,
  `transfers_destination`, `auditor` — todos con `ChangeMe123!` (solo local).
- **Portal master**: la API tiene endpoints de plataforma (`/api/v1/platform/**`),
  pero no se incluyen porque el usuario maestro del SaaS se crea fuera de los
  seeds y no forma parte del alcance del curso.

---

## 6. Ejecución por línea de comandos (opcional)

Útil para evidencias reproducibles sin abrir Postman:

```bash
npx newman run postman/Vivent.postman_collection.json \
  -e postman/Vivent-Local.postman_environment.json \
  --reporters cli
```

Salida esperada (últimas líneas):

```text
requests:  128 executed, 0 failed
assertions: 242 executed, 0 failed
```
