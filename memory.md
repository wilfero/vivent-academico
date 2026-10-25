# memory.md — Memoria del proyecto Vivent académico

Memoria viva del agente de IA y del equipo. Última actualización: 2026-10-25.
Reglas de uso: registrar aquí hechos críticos, decisiones y pendientes por tarea; el detalle de contratos va a `docs/` y las reglas globales a `AGENTS.md`.

## Hechos críticos

| Hecho | Detalle |
| --- | --- |
| Origen del código | Variante académica de `JorgeJr1904/vivent-back` + `vivent-front` (branch `dev` vigente al 2026-10-25). Los repos originales **no se modifican** |
| Despliegue comercial | `deploy/` + corpoviva.com es producto separado; esta variante no lo afecta |
| Puertos locales | DB 5433 (vivent/vivent) · backend 8080 · frontend 3000 |
| Cuentas demo | admin, seller, inventory, transfers_origin, transfers_destination, auditor — contraseña `ChangeMe123!` (BCrypt en BD, seed S20261024000100) |
| Migraciones/seeds | 34 migraciones + 21 seeds en `dev`; nuevas con timestamp UTC 14 dígitos, idempotentes, out-of-order=true |
| PostGIS | Habilitado por `V20261023191207__enable_postgis_and_commerce_foundations.sql` (geocodificación Photon con caché) |
| pruebas | 25 `@Test` de integración en `ViventApplicationTests` (Testcontainers + PostGIS) |

## Decisiones

| # | Decisión | Motivo | Fecha |
| --- | --- | --- | --- |
| D1 | Repositorio académico alterno `wilfero/vivent-academico` (privado) | Requisito de la asignatura: repo alterno si se modifica el código original; protege el deploy comercial | 2026-10-25 |
| D2 | Mantener Spring Boot 4/Java 25 con **solicitud de equivalencia técnica** ante el docente (en lugar de NestJS) | El sistema ya existe, está probado y su equivalencia técnica con el Stack 2 (NestJS) está documentada componente por componente; reconstruir en NestJS arriesga calidad sin beneficio académico | 2026-10-25 |
| D3 | Estructura monorepo académica exigida (backend/, frontend/, database/, docker/, docs/, postman/) | Estructura esperada del proyecto en el enunciado | 2026-10-25 |
| D4 | Publicación académica separada de la comercial | El deploy principal tiene otro uso (comercial) | 2026-10-25 |

## Estado del cumplimiento (resumen)

- ✅ Autenticación backend, roles/permisos en API, dashboard real, 11+ módulos, 3 procesos integrados, consultas con filtros, 9 reportes con exportación, validaciones FE+BE, seguridad (BCrypt, parametrizadas, soft-delete, trazabilidad).
- ✅ **Refresh token implementado** (2026-10-25): migración `V20261026000000__create_refresh_tokens.sql`, endpoint `POST /api/v1/auth/refresh` con rotación, `POST /api/v1/auth/logout` con auditoría LOGOUT, tokens opacos hasheados (SHA-256), revocación total al cambiar/resetear contraseña, renewSession(user, ip, ua) en cambio de contraseña propio; frontend con renovación proactiva (1 min antes de expirar) + reactiva (401 INVALID_TOKEN, single-flight) en `src/lib/api/http.ts`.
- ✅ **Catálogos con ≥5 registros** (2026-10-25): seed `S20261026000001__seed_academic_demo_data.sql` (5 categorías, 4 proveedores extra=5 total, 4 clientes extra=5 total, 5 productos nuevos con 10 presentaciones y barcodes únicos; unidades 8 de V010).
- ✅ **3 APIs externas activas** (2026-10-25): Photon (integrada, con caché y fallback), Open Food Facts (nueva: backend + diálogo de registro rápido en el POS), Nabrix Email (contrato implementado con outbox/reintentos; activación con credenciales de prueba).
- ✅ **Documentación completa**: `DOCUMENTO_PROYECTO.md` (+pdf, 4 págs: negocio, problema, objetivos, alcance, 6 módulos, 30 reglas, matriz de responsabilidades, procesos), `MANUAL_TECNICO.md` (+pdf), `MANUAL_USUARIO.md` (+pdf), `APIS_EXTERNAS.md`, `PUBLICACION_CLOUD.md`, `RequestMetadata.java`.
- ⚠️ Pendiente operativo del equipo: correr `./mvnw test` en una PC con Docker Desktop (Testcontainers requiere el daemon; en la PC de desarrollo el daemon estaba apagado), smoke del despliegue en la nube (Neon/Render/Vercel) y commits de los tres integrantes.

## Pendientes por tarea

1. **[Backend]** Implementar access + refresh token: endpoint `/api/v1/auth/refresh`, rotación y revocación (tabla `refresh_tokens` con expiración), invalidación al logout, renovación transparente en `src/lib/api/http.ts`, pruebas de expiración/renovación/cierre.
2. **[DB]** Seed académico `S*__seed_academic_demo_data.sql`: ≥5 productos con presentaciones, ≥5 categorías, ≥5 proveedores, ≥5 clientes, ≥5 unidades, compras/ventas/traslados/ajustes/cierres demo suficientes para dashboard y 9 reportes.
3. **[Backend]** APIs externas: activar nabrix-email (API key), decidir 3ª integración real (pagos sandbox Stripe/Wompi o tipo de cambio GTQ), documentar por integrante con manejo de éxito/error.
4. **[Frontend]** Renovación automática de token + manejo de expiración/cierre de sesión.
5. **[Docs]** Documento del proyecto (portada, negocio, problema, objetivos, alcance, 6 módulos, ≥25 reglas, matriz de responsabilidades), manual técnico, manual de usuario (ilustrado), evidencias de pruebas.
6. **[Postman]** Colección completa por módulo con ejemplos éxito/error (incluye auth e integraciones externas).
7. **[Publicación]** Despliegue académico: BD (Neon/Render), backend (Render/Railway), frontend (Vercel); URLs + evidencia + cuentas por rol + demo de un proceso en línea.
8. **[Equipo]** Commits de los tres integrantes en el repo académico y defensa individual (módulos, proceso, reportes, API).

## Evidencias de uso del agente (asignatura)

| Fecha | Tarea | Archivo/resultado |
| --- | --- | --- |
| 2026-10-25 | Análisis de requisitos vs repos (lectura del DOCX, verificación en `origin/dev`) | `docs/MATRIZ_CUMPLIMIENTO_DESARROLLO_WEB.md` |
| 2026-10-25 | Scaffolding del repo académico y archivos de agentes | `AGENTS.md`, `SKILL.md`, `memory.md`, `.env.example`, `docker/`, `postman/` |
| 2026-10-25 | Documento de justificación de stack para el docente | `docs/JUSTIFICACION_STACK_SPRING_BOOT.md` |
| 2026-10-25 | **Implementación refresh token** (backend + frontend) | migración + endpoints + RequestMetadata + auto-renewal; `mvnw compile` BUILD SUCCESS, `tsc --noEmit` OK |
| 2026-10-25 | **Seed académico ≥5 por catálogo** | `S20261026000001__seed_academic_demo_data.sql` |
| 2026-10-25 | **Documentos del curso** | `DOCUMENTO_PROYECTO.md`+pdf, `MANUAL_TECNICO.md`+pdf, `MANUAL_USUARIO.md`+pdf, `APIS_EXTERNAS.md`, `PUBLICACION_CLOUD.md` |
| 2026-10-25 | Colección Postman completa con refresh/logout + integración externa | `postman/Vivent.postman_collection.json` |
