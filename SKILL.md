# SKILL.md — Habilidades del agente para Vivent académico

Archivo de habilidades exigido por la asignatura. Cada habilidad describe cómo debe proceder el agente en tareas frecuentes del proyecto, con su evidencia de uso en `memory.md`.

## Skill 1 — endpoint-nuevo

Cuando se agrega un endpoint:

1. Definir el contrato primero (ruta HTTP, `ApiResponse<T>`, DTOs, código de error).
2. Crear/actualizar DTO con Jakarta Validation (obligatorios, formatos, rangos).
3. Implementar en `service/` (nunca lógica de negocio en el controller).
4. Aplicar permiso estricto por endpoint (`module.action`) + aislamiento `company_id` + scope de sucursal si aplica.
5. Si cambia comportamiento: actualizar `docs/`, `memory.md` y (si es regla global) `AGENTS.md`.
6. Verificar con `./mvnw test` y probar en Swagger/Postman.

## Skill 2 — migracion-seed

Cuando se requiere cambio de esquema o datos:

1. Nunca editar migraciones/seeds ya aplicadas (inmutables).
2. Crear archivo nuevo con timestamp UTC de 14 dígitos: `V<YYYYMMDDHHMMSS>__<descripcion_en_ingles>.sql` o `S<...>__<...>.sql`.
3. Todo seed nuevo idempotente (`ON CONFLICT DO NOTHING` / `WHERE NOT EXISTS`).
4. Confirmar que el bootstrap limpio funciona: BD nueva → `./mvnw spring-boot:run` → revisar log de Flyway.

## Skill 3 — validacion-frontend

Cuando se tocan formularios o pantallas:

1. Schemas con Zod fuera del componente; `z.coerce.number()` para números.
2. React Hook Form con `defaultValues` y `reValidateMode: "onBlur"`.
3. Textos visibles por `next-intl` (actualizar `es-GT.json` y `en-US.json` en la misma tarea).
4. Errores del backend mostrados con `formatApiErrorMessage` (nunca crudos).
5. Verificar con `pnpm run lint`, `tsc --noEmit` y build con `NEXT_PRIVATE_BUILD_WORKER=1`.

## Skill 4 — pruebas-evidencia

Cuando se valida un flujo (proceso integrado, reporte, integración externa):

1. Registrar caso, datos usados, resultado esperado/obtenido y estado en `docs/evidencias/` (plantilla QA del proyecto).
2. Incluir en la colección de Postman los ejemplos de éxito y de error del endpoint probado.
3. Las capturas y logs van a `docs/evidencias/` sin datos sensibles.

## Skill 5 — token-sesion

Para trabajo de autenticación/sesiones (requisito de la asignatura):

1. Access token corto + refresh token rotativo; renovación automática en el cliente HTTP.
2. Revocar/invalidar refresh al cerrar sesión; comparar `authVersion` en cada request.
3. Nunca loggear tokens; el refresh se guarda en `sessionStorage` (coherente con el diseño actual) y se valida en backend.
4. Documentar el flujo en `docs/` y agregar casos de éxito/error a Postman.
