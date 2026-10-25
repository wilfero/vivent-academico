# AGENTS.md — Vivent académico (Desarrollo Web)

Guía global para agentes de IA que trabajen en este repositorio. El estado operativo y las decisiones viven en `memory.md`; las habilidades en `SKILL.md`.

## 1. Contexto

Este repositorio es la **variante académica** del proyecto Vivent para el curso Desarrollo Web (UMG). El código base proviene de los repositorios comerciales (`JorgeJr1904/vivent-back`, `vivent-front`) y se desarrolla aquí **de forma aislada**: ningún cambio debe tocar el despliegue comercial ni sus repos originales.

## 2. Reglas globales

1. Todo código interno (tablas, columnas, enums, permisos, rutas de API) en inglés; la interfaz se traduce desde i18n (es-GT / en-US).
2. El backend es **autoritativo**: permisos, tenant, stock y estados se validan en el servidor; el frontend solo gana UX.
3. Aislamiento estricto por `company_id`; scope por sucursal para usuarios no-COMPANY_ADMIN.
4. El inventario se controla en unidad base; las presentaciones solo son vistas de venta con factor de conversión.
5. Todo movimiento de inventario genera kardex; toda acción sensible genera auditoría.
6. Eliminación siempre lógica (`deleted_at` / estados). Nada se borra físicamente.
7. Cambios de esquema/datos **solo** con migraciones o seeds nuevos de Flyway (idempotentes, timestamps UTC de 14 dígitos); nunca editar archivos ya aplicados.
8. Sin secretos en el repo: variables por `.env` (ejemplo en `.env.example`); contraseñas siempre hasheadas (BCrypt).
9. Respuestas API con `ApiResponse<T>` (`success/data/message/errorCode`) y códigos de error estables.
10. Sin facturación legal, pagos automáticos reales ni adjuntos S3 en esta fase.

## 3. Estructura del repositorio

```text
backend/     Spring Boot 4 (Java 25) — módulos: auth, tenant, companies, branches,
             users, permissions, products, inventory, purchases, sales, transfers,
             adjustments, closures, reports, audit, subscriptions, platform, common, security
frontend/    Next.js 16 — src/{app,components,features,lib,messages}
database/    database/migrations (V*.sql) y database/seed (S*.sql)
docker/      compose.db.yml y compose.local.yml
docs/        matriz de cumplimiento, justificación de stack, manuales, evidencias
postman/     colección de Postman
```

## 4. Comandos de verificación

```bash
# Backend (requiere JDK 25)
cd backend && ./mvnw test

# Frontend (Node 24 + pnpm 11)
cd frontend
pnpm run lint
pnpm exec tsc --noEmit --pretty false
NEXT_PRIVATE_BUILD_WORKER=1 pnpm run build
```

Build si se cuelga en `Running TypeScript...`: usar `NEXT_PRIVATE_BUILD_WORKER=1`.

## 5. Entorno local

- BD: Docker en `localhost:5433` (vivent/vivent) — ver `docker/compose.db.yml`.
- Stack completo: `docker/compose.local.yml` (db 5433, backend 8080, frontend 3000).
- Cuentas demo: ver `README.md` (contraseña compartida `ChangeMe123!`, solo local).
- Flyway aplica migraciones y seeds al arrancar el backend.

## 6. Pendientes de la variante académica

Ver `memory.md`. Resumen: refresh token (access+refresh), seed académico expandido (≥5 por catálogo), activación y demo de 3 APIs externas, colección Postman completa, manuales, publicación en la nube separada y matriz de responsabilidades del equipo.

## 7. Documentación viva

Cualquier cambio de comportamiento (endpoint, DTO, permiso, migración, estado, FIFO/kardex, auth) debe quedar documentado en la misma tarea en: `memory.md` (estado/decisión) y `docs/` (contrato o manual si aplica), además de actualizar `AGENTS.md` si la regla es global.
