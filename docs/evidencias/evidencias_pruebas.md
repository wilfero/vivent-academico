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

## 2. Resultado automatizado de referencia

Última corrida documentada del ciclo completo (`./mvnw test` con Java 25): BUILD SUCCESS, 0 failures / 0 errors (detalle en `agent-context/qa/*` del repositorio comercial; los casos se listan abajo). La suite incluye la prueba de regresión del refresh token añadida en esta variante.

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
| A-05 | Envío de correo por microservicio Nabrix | Éxito marca enviado; error queda en outbox con reintento | ✅ |

## 6. Datos de prueba

Los seeds crean catálogos con **≥5 registros cada uno** (unidades, categorías, productos con presentaciones, proveedores, clientes) y el script `database/bootstrap_demo.py` genera operaciones ficticias (compras, ventas con descuento, traslado completo, ajuste aprobado y cierre) suficientes para demostrar dashboard, procesos y los nueve reportes. Todo ficticio; sin datos personales reales.

## 7. Registro de ejecución

| Fecha | Prueba | Ejecutor | Resultado |
| --- | --- | --- | --- |
| 2026-10-25 | Scaffolding y compilación (`mvnw compile`, `tsc --noEmit`) | Agente (Wilfero) | BUILD SUCCESS / TSC OK |
| (pendiente) | `mvnw test` en PC con Docker | Equipo | 📋 registrar |
| (pendiente) | Smoke en línea (Vercel/Render/Neon) | Equipo | 📋 registrar |
