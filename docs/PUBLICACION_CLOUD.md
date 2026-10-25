# Publicación del sistema en la nube (variante académica)

> **Regla:** esta publicación es **independiente** del despliegue comercial (corpoviva.com). Nada de lo que se haga aquí afecta el deploy principal.

Arquitectura de publicación:

```text
Frontend (Vercel)  →  API base URL  →  Backend (Render, Docker)  →  PostgreSQL (Neon, gratuito)
```

## 1. Base de datos — Neon (gratuito)

1. Crea cuenta en `neon.tech` y un proyecto nuevo.
2. Copia la cadena de conexión PostgreSQL (ejemplo `postgresql://usuario:clave@ep-xxx.aws.neon.tech/vivent`).
3. La URL JDBC equivalente para el backend: `jdbc:postgresql://ep-xxx.aws.neon.tech/vivent?sslmode=require`.

Evidencia: captura del dashboard de Neon con la base activa.

## 2. Backend — Render (Docker)

1. Crea cuenta en `render.com` → **New → Web Service** → conecta el repositorio `wilfero/vivent-academico`.
2. Configuración:
   - **Root directory:** `backend`
   - **Runtime:** Docker (usa el `Dockerfile` existente)
   - **Variables de entorno:**
     - `VIVENT_DB_URL=jdbc:postgresql://ep-xxx.aws.neon.tech/vivent?sslmode=require`
     - `VIVENT_DB_USERNAME=<usuario-neon>`
     - `VIVENT_DB_PASSWORD=<password-neon>`
3. Al arrancar, Flyway aplica migraciones y seeds (logs visibles en Render).

Evidencia: URL base de la API (ejemplo `https://vivent-academico.onrender.com`), log de Flyway y `GET /api/v1/public/tenant-config?subdomain=demo` respondiendo 200.

> Si el servicio gratuito se duerme por inactividad, la primera petición tarda ~1 min (avisar en la demo).

## 3. Frontend — Vercel

1. Crea cuenta en `vercel.com` → **Add New → Project** → importa `wilfero/vivent-academico`.
2. Configuración:
   - **Root directory:** `frontend`
   - **Framework preset:** Next.js (detecta el `next.config.ts`)
   - **Variables de entorno:**
     - `NEXT_PUBLIC_API_BASE_URL=https://<tu-backend>.onrender.com`
     - `NEXT_PUBLIC_DEFAULT_TENANT=demo`
3. Deploy → obtén la URL del sistema (ejemplo `https://vivent-academico.vercel.app`).

Evidencia: login en la URL de Vercel autenticando contra la API de Render y mostrando datos de la BD de Neon (captura del dashboard con indicadores reales).

## 4. Cuentas de prueba para la entrega

Contraseña: `ChangeMe123!` — usuarios: `admin` (COMPANY_ADMIN), `seller` (SELLER), `inventory` (WAREHOUSE_USER), `auditor` (AUDITOR).

## 5. Demo de un proceso completo en línea

1. Login `inventory` → nueva compra → **Confirmar** (inventario sube).
2. Login `seller` → POS → escanear → **Finalizar** (inventario baja).
3. **Cierre de caja** del día (preview + monto contado).
4. Login `admin` → **Reportes** con los datos creados + **Bitácora** con la auditoría.

## 6. Checklist de entrega

```text
[ ] URL del sistema (frontend) funcionando
[ ] URL base de la API funcionando
[ ] Evidencia de conexión frontend→API→BD (dashboard con datos)
[ ] Cuentas de prueba por rol documentadas
[ ] Un proceso completo demostrado en línea
[ ] Credenciales de administración enviadas por el medio privado del docente
```
