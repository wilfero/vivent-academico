# memory.md — Memoria del proyecto Vivent académico

Memoria viva del agente de IA y del equipo. Última actualización: 2026-10-25.
Reglas de uso: registrar aquí hechos críticos, decisiones y pendientes por tarea; el detalle de contratos va a `docs/` y las reglas globales a `AGENTS.md`.

## Hechos críticos

| Hecho | Detalle |
| --- | --- |
| Origen del código | Variante académica de `JorgeJr1904/vivent-back` + `vivent-front` (rama `main` al 2026-10-25). Los repos originales **no se modifican** |
| Despliegue comercial | `deploy/` + corpoviva.com es producto separado; esta variante no lo afecta |
| Puertos locales | DB 5433 (vivent/vivent) · backend 8080 · frontend 3000 |
| Cuentas demo | admin (COMPANY_ADMIN), seller (SELLER), inventory / transfers_origin / transfers_destination (WAREHOUSE_USER), auditor (AUDITOR) — contraseña `ChangeMe123!` (BCrypt en BD, seed S008) |
| Migraciones/seeds | 19 migraciones (V001–V018 + refresh tokens) y 10 seeds (S001–S009 + seed académico); Flyway aplica seeds con prefijo `S` e historial propio `flyway_seed_history` |
| Pruebas | 8 `@Test` de integración en `ViventApplicationTests` (compras/FIFO, anulación exacta, combos, cambio de sucursal, cambio de contraseña); requieren PostgreSQL en `localhost:5433` |
| PostGIS | No se usa en esta copia; Photon se integra por REST con caché en memoria |

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
- ✅ **3 APIs externas activas y verificadas en el código de esta copia** (2026-10-25): Photon `GET /api/v1/integrations/geocode` (Christian), Open Food Facts `GET /api/v1/products/barcode-lookup/{barcode}` + registro rápido en POS (Wilfero), tipo de cambio open.er-api.com `GET /api/v1/integrations/exchange-rate` (Fernando). Todas con caché, timeout de 4 s y manejo explícito de error (found/available=false). La integración Nabrix Email existe solo en la rama comercial `dev` y **no** forma parte de esta entrega.
- ✅ **Documentación completa** (2026-10-25): `DOCUMENTO_PROYECTO.md` + PDF de **22 páginas** (portada propio formato del curso en página 1, índice en página 2, negocio, problema, objetivos, alcance, 6 módulos, 30 reglas, diseño: modelo conceptual, modelo de datos físico, arquitectura y flujos paso a paso de los 3 procesos, matriz de responsabilidades, stack, Docker, **API/Postman/CRUD**, anexos y nota de rúbrica), `MANUAL_TECNICO.md` + PDF, `MANUAL_USUARIO.md` + PDF, `APIS_EXTERNAS.md`, `PUBLICACION_CLOUD.md`, `evidencias/evidencias_pruebas.md`.
- ✅ **Portada e índice** (2026-10-25): la portada replica el diseño del documento de Análisis de Sistemas 2 (mismo esquema tipográfico y cromático, curso "DESARROLLO WEB"), ocupa la página 1 sola; el índice va en página 2 como tabla `# | Sección` porque Markdown convertía "- 1. …" en listas anidadas con numeración reiniciada. Los saltos de página son `page-break-after` inline (el pipeline solo define `@page` global).
- ✅ **Colección Postman validada de punta a punta** (2026-10-25): 128 requests y 242 aserciones, **0 fallos**, ejecutada dos veces seguidas sobre una pila aislada (misma imagen Docker, base nueva + `bootstrap_demo.py`) para no contaminar los datos demo. Guía en `postman/README.md`; evidencia en `docs/evidencias/evidencias_pruebas.md` §8.
- 🐞 **Defectos de la colección detectados y corregidos** (2026-10-25): environment declaraba `accessToken` vacío y tapaba la variable de colección (401 en cadena); las bajas lógicas usaban el registro de trabajo y rompían la cadena posterior (se crean registros temporales); los reportes mezclan fecha-hora ISO y fecha simple según endpoint; el cierre de caja es único por usuario/sucursal/día (se calcula una fecha libre en pre-request); orden de estados del cierre crear→aprobar→reabrir→cerrar; `Listar presentaciones` debía ir después de crear.
- ⚠️ **Portal master fuera de la colección** (2026-10-25): `/api/v1/platform/**` exige un usuario maestro que no existe en los seeds; se excluyó para que la corrida no muestre 401 permanentes y se documentó el motivo en `postman/README.md` §5.
- ✅ **Arranque reproducible** (2026-10-25): `docker/compose.local.yml` con build args de `NEXT_PUBLIC_API_BASE_URL` (Next.js las inyecta en build) y autodetección de host en `src/lib/api/http.ts` (localhost/LAN → `http://<host>:8080`), de modo que el mismo compose sirve en la PC, en otra PC de la red y en un servidor con Docker.
- ✅ **Salida de reportes** (2026-10-25): componente `components/shared/report-actions.tsx` con **Exportar CSV** (genera el CSV desde la tabla visible, respeta los filtros activos, BOM UTF-8 para Excel) e **Imprimir** (CSS `@media print` que oculta navegación), disponible en las 9 pantallas de reportes.
- ✅ **Swagger público para revisión** (2026-10-25): `/v3/api-docs/**` y `/swagger-ui/**` en `permitAll` para que el docente consulte la documentación sin token; el resto de la API sigue autenticada.
- ✅ **Verificación en vivo** (2026-10-25): `docker compose -f compose.local.yml up -d --build` construyó y arrancó db + backend + frontend; login, tenant-config, refresh/logout y las tres APIs externas probados contra la pila real en Docker.
- 🐞 **Bugs reales encontrados y corregidos al probar en Docker** (2026-10-25):
  - El refresh token emitido por `renewSession` no se persistía porque el método era `@Transactional(readOnly = true)`; el logout y la siguiente renovación devolvían `INVALID_TOKEN`. Corregido a transacción de escritura y cubierto por prueba de regresión.
  - `refresh` lanzaba `LazyInitializationException` al usar la entidad fuera de la transacción de `consume()`. Corregido con `UserRepository.findWithAssociationsById` (`@EntityGraph` de company, branch, role, role.permissions y overrides).
  - Los usuarios demo no tenían la contraseña documentada: `admin` estaba sembrado con `admin`. Nuevo seed `S20261026000002__reset_academic_demo_passwords.sql` (idempotente) fija `ChangeMe123!` y sube `auth_version`.
- ✅ **Swagger/OpenAPI** (2026-10-25): se agregó `springdoc-openapi-starter-webmvc-ui` **3.2.0** (línea compatible con Spring Boot 4 / Spring Framework 7); antes la ruta `/v3/api-docs` no existía y devolvía 404/500.
- ✅ **Las tres APIs externas verificadas en vivo** (2026-10-25 12:26 UTC, pila en Docker): Photon devolvió `Parque Barrios, Cuilapa, Santa Rosa` (14.275497, -90.298877); open.er-api.com devolvió USD→GTQ **7.645339**; Open Food Facts devolvió **Nutella** para el código 3017620422003. Los tres casos de error (query vacía, moneda inexistente, código inexistente) responden con `found=false`/`available=false` y mensaje. Evidencia en `docs/evidencias/evidencias_pruebas.md` §5.1.
- 🐞 **Integraciones corregidas** (2026-10-25): Spring Boot 4 usa **Jackson 3**, y los servicios deserializaban con `com.fasterxml.jackson.databind.JsonNode` (Jackson 2) → `HttpMessageConversionException` y siempre "unavailable". Migrados a `tools.jackson.databind.JsonNode`. Photon rechazaba `lang=es` (solo `default/de/en/fr`): se eliminó el parámetro.
- ✅ **Datos de demostración reales** (2026-10-25): `database/bootstrap_demo.py` ejecutado contra la API: 2 compras confirmadas (FIFO), 2 ventas confirmadas, 1 traslado recibido, 1 ajuste aprobado, 20 movimientos de kardex y 1 cierre de caja. Segunda corrida idempotente. Corregido el script para usar la cuenta que tiene cada permiso (`seller` confirma ventas, `admin` aprueba traslado/ajuste, `transfers_origin` despacha) y para avanzar el traslado por estados, lo que permite retomar ejecuciones interrumpidas.
- ⚠️ Pendiente operativo del equipo: correr `./mvnw test` en una PC con Docker Desktop (Testcontainers requiere el daemon; en la PC de desarrollo el daemon estaba apagado), smoke del despliegue en la nube (Neon/Render/Vercel) y commits de los tres integrantes.

## Pendientes por tarea

1. **[Backend]** Implementar access + refresh token: endpoint `/api/v1/auth/refresh`, rotación y revocación (tabla `refresh_tokens` con expiración), invalidación al logout, renovación transparente en `src/lib/api/http.ts`, pruebas de expiración/renovación/cierre. ✅ Hecho (V20261026000000 + endpoints + http.ts).
2. **[DB]** Seed académico `S*__seed_academic_demo_data.sql`: ≥5 productos con presentaciones, ≥5 categorías, ≥5 proveedores, ≥5 clientes, ≥5 unidades, compras/ventas/traslados/ajustes/cierres demo suficientes para dashboard y 9 reportes. ✅ Hecho (S20261026000001 + `database/bootstrap_demo.py` para las operaciones).
3. **[Backend]** APIs externas por integrante con éxito/error documentado. ✅ Hecho: Photon, Open Food Facts y tipo de cambio (ver `docs/APIS_EXTERNAS.md`).
4. **[Frontend]** Renovación automática de token + manejo de expiración/cierre de sesión. ✅ Hecho.
5. **[Docs]** Documento del proyecto, manual técnico, manual de usuario y evidencias. ✅ Hecho (documento de 22 páginas, portada e índice en páginas separadas).
6. **[Postman]** Colección completa por módulo con ejemplos éxito/error (incluye auth e integraciones externas). ✅ Hecho y validado: 128 requests / 242 aserciones / 0 fallos (dos corridas consecutivas).
7. **[Publicación]** Despliegue académico: BD (Neon/Render), backend (Render/Railway), frontend (Vercel); URLs + evidencia + cuentas por rol + demo de un proceso en línea. ⏳ Pendiente (requiere cuentas del equipo; guía lista en `PUBLICACION_CLOUD.md`).
8. **[Equipo]** Commits de los tres integrantes en el repo académico y defensa individual (módulos, proceso, reportes, API). ⏳ Pendiente.
9. **[Verificación]** Correr `./mvnw test` con la BD de Docker arriba y hacer un smoke de `compose.local.yml` (login + tenant-config) para adjuntar evidencia real. ⏳ Pendiente: el daemon de Docker estaba apagado en la PC de desarrollo.

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
| 2026-10-25 | **Rediseño de portada + sección 11 (API/Postman/CRUD) + entrada 13 Rúbrica** | `docs/DOCUMENTO_PROYECTO.md`, `docs/documento-proyecto.pdf` (22 pág.) |
| 2026-10-25 | **Validación Newman de la colección en pila aislada** (2 corridas, 242 aserciones, 0 fallos) | `postman/README.md`, `docs/evidencias/evidencias_pruebas.md` §8 |
