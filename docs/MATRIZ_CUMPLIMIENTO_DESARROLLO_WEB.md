# Matriz de cumplimiento — Vivent vs Requisitos "Proyecto Final - Desarrollo Web"

Fecha de análisis: 2026-10-25
Requisitos extraídos de: `Proyecto final - Desarrollio Web.docx` (Universidad Mariano Gálvez, Centro Universitario Cuilapa — Curso: Desarrollo Web, M.A. Ing. Elvis Morales)
Estado del código validado: branch `origin/dev` de `JorgeJr1904/vivent-back` y `vivent-front`, más `nabrix-gt/vivent-landing`.

Leyenda de veredictos: ✅ Cumple · ⚠️ Parcial/Pendiente (con equivalente o acción) · ❌ No cumple (requiere trabajo o autorización)

---

## 1. Alcance funcional

| Requisito | Veredicto | Evidencia / Equivalente / Pendiente |
| --- | --- | --- |
| Sistema ERP web académico; alternativamente otro sistema web **con autorización del docente** y alcance funcional equivalente | ⚠️ | Vivent (SaaS de inventarios) sustituye al ERP con alcance equivalente: compra→inventario FIFO→venta→kardex→traslado→ajuste→cierre→reportes. **Requiere autorización escrita del docente** (el propio documento lo prevé) |
| Autenticación: inicio y cierre de sesión; credenciales verificadas en backend | ✅ | `POST /api/v1/auth/login` valida tenant+usuario+contraseña (BCrypt, estados USER_INACTIVE/INVALID_CREDENTIALS); cierre de sesión en UI; `authVersion` invalida tokens antiguos |
| Usuarios y permisos: ≥2 roles con permisos distintos; restricciones también en la API | ✅ | Roles COMPANY_ADMIN/SELLER/MANAGER/WAREHOUSE_USER/AUDITOR (seed S002); permisos estrictos por endpoint (p. ej., `sales.cancel`, `purchases.confirm`); scope por sucursal en backend; frontend solo como UX |
| Dashboard con indicadores reales de la base de datos | ✅ | `/dashboard` consume APIs reales: `GET /inventory/summary`, summaries de ventas/compras/productos; gating por permisos |
| ≥6 módulos del negocio (2 completos por integrante); login/dashboard/configuración no cuentan | ✅ | Módulos operativos: productos, compras, ventas POS, inventario (balances/capas/kardex), traslados, ajustes, cierres de caja, proveedores, clientes, combos, storefront (dev) — supera 6. Pendiente: matriz de asignación (2 módulos por integrante) |
| 3 procesos integrados (1 por integrante), cada uno conectando ≥2 módulos y aplicando reglas del negocio | ✅ | (1) Compra → convierte presentaciones a unidad base → al confirmar crea capas FIFO + kardex + inventario (conecta compras+inventario+catálogo). (2) Venta POS → valida stock → consume FIFO → kardex → cierre de caja (conecta ventas+inventario+cierres). (3) Traslado → reserva en origen → despacho → recepción total/parcial → kardex en ambas sucursales (conecta traslados+inventario). Pendiente: identificar responsable de cada proceso en la matriz |
| Consultas: búsqueda por texto, combinación de filtros, ordenamiento | ✅ | Listados con `query`, `status`, fechas `fromDate/toDate`, `branchId`, `supplierId`, `paymentMethod`; búsqueda por número y nombre de cliente; ordenamiento en tablas TanStack |
| 9 reportes distintos (3 por integrante) con filtros e impresión/exportación; cambiar solo el filtro no cuenta | ✅ | 9 pantallas distintas: ventas, compras, inventario, kardex, movimientos, productos (más vendidos, más movidos, sin movimiento, stock bajo, agotados), traslados, ajustes y cierres. Filtros de fecha/sucursal/producto y **exportación CSV de la tabla visible** + **impresión** (CSS de impresión que oculta la navegación) implementadas en components/shared/report-actions.tsx |
| Validaciones en frontend y backend con mensajes comprensibles | ✅ | Zod + React Hook Form en frontend; Jakarta Validation en backend; errores localizados (`formatApiErrorMessage`), códigos de error claros (INSUFFICIENT_STOCK, VALIDATION_ERROR…) |
| ≥5 registros por catálogo y operaciones suficientes para demostrar procesos, dashboard y 9 reportes (datos ficticios) | ✅ | S001–S009 + S20261026000001__seed_academic_demo_data.sql: 8 unidades, 5 categorías, 5 proveedores, 5 clientes y 5 productos nuevos con 10 presentaciones y códigos de barras únicos. database/bootstrap_demo.py genera compras, ventas, traslados, ajustes y cierre vía API real |
| 3 integraciones con APIs externas (1 por integrante); HTTP real con datos del proceso, manejo de éxito/error; respuestas fijas internas no cuentan | ✅ | Implementadas y documentadas: (1) **Photon** geocodificación (`GET /api/v1/integrations/geocode`) — Christian; (2) **Open Food Facts** por código de barras (`GET /api/v1/products/barcode-lookup/{barcode}`) con registro rápido en el POS — Wilfero; (3) **open.er-api.com** tipo de cambio (`GET /api/v1/integrations/exchange-rate`) — Fernando. Las tres con HTTP real, caché, timeout de 4 s y manejo explícito de éxito/error (`docs/APIS_EXTERNAS.md`). |
| Access token + refresh token: acceso, expiración, renovación, cierre de sesión | ❌ | Vivent usa access JWT stateless + `authVersion` (invalidación por cambio/reseteo de contraseña). **No hay refresh token ni endpoint de renovación. Acción: implementar flujo access+refresh en la variante académica** (access corto + refresh rotativo revocado al cerrar sesión). Es criterio explícito de la rúbrica (C2) |
| Publicación: BD + backend + frontend conectados en la nube; URL sistema, URL API, evidencia de conexión, cuentas de prueba por rol | ⚠️ | El deploy principal (`deploy/compose.yml` + nginx + corpoviva.com) es de uso comercial y no se usará como entrega académica. **Acción: publicación académica separada** (Neon/Railway/Render + Vercel) con cuentas de prueba por rol |

## 2. Stack tecnológico

| Requisito | Veredicto | Análisis |
| --- | --- | --- |
| Elegir UNO de: Stack 1 (SQL Server + ASP.NET Core + React) · Stack 2 (PostgreSQL + NestJS + Next.js) · Stack 3 (MongoDB + Laravel + Vue) | ❌ | Vivent usa **Java 25 / Spring Boot 4** + Next.js + PostgreSQL. Comparte con Stack 2 la base de datos (PostgreSQL) y el framework frontend (Next.js), pero el backend exigido es **NestJS**. Dos caminos: **(A)** pedir al docente equivalencia técnica de Spring Boot (mismo patrón capas/repositorio/validación/JWT) — el propio documento admite cambios de stack "acordados con el docente" para la propuesta autorizada; **(B)** construir la variante académica con backend NestJS sobre el mismo esquema PostgreSQL (reutilizando migraciones SQL), manteniendo el frontend. **DECISIÓN CRÍTICA PENDIENTE DEL EQUIPO/DOCENTE** |
| HTML y CSS para interfaces | ✅ | Next.js/React genera HTML+CSS (Tailwind CSS v4); landing Astro también |
| Docker para la BD localmente | ✅ | `compose.yml` (postgres:17-alpine :5433) y `deploy/compose.yml` |
| Postman/Swagger para APIs | ✅ | Swagger publicado por springdoc (`/swagger-ui/index.html`); colección `postman/Vivent.postman_collection.json` con **128 requests** (CRUD completo, auth, integraciones externas) validada con Newman: 242 aserciones, 0 fallos |
| Git y GitHub | ✅ | Repos en GitHub con historial; en el repo académico deben figurar commits de los 3 integrantes |
| Agentes de IA con archivos de instrucciones, habilidades y memoria | ⚠️ | `AGENTS.md` ✅ y memoria modular (`agent-context/`) ✅; **crear `SKILL.md` y `memory.md`** explícitos en la raíz del repo académico (pueden referenciar `agent-context/` como fuente) |
| Publicación en la nube de BD/backend/frontend | ⚠️ | Ver requisito de publicación arriba |

## 3. Estructura esperada del repositorio

| Carpeta/archivo | Veredicto | Acción en repo académico |
| --- | --- | --- |
| `backend/` | ✅ | Copia adaptada del código (variante académica) |
| `frontend/` | ✅ | Copia adaptada del código (variante académica) |
| `database/` | ⚠️ | Hoy vive dentro de `backend/database/`; en el repo académico crear carpeta `database/` top-level (migraciones + seeds) documentando la equivalencia |
| `docker/` | ⚠️ | Hoy el compose está en la raíz; crear `docker/` con el compose de la BD (+ opcional full-stack) y referenciarlo desde el README |
| `docs/` | ✅ | Diagramas, reglas del negocio, manuales y evidencias (se completará con manuales y matriz) |
| `README.md` | ✅ | Reescribir para el alcance académico (instalación, ejecución, publicación) |
| `.env.example` | ⚠️ | Crear en raíz (hoy el ejemplo vive en `deploy/.env.example`) |
| `.gitignore` | ✅ | Ya excluye dependencias, builds y secretos |
| `AGENTS.md`, `SKILL.md`, `memory.md` | ⚠️ | Crear `SKILL.md` + `memory.md`; documentar su uso durante el desarrollo |

## 4. Seguridad y calidad

| Requisito | Veredicto |
| --- | --- |
| Contraseñas con hash del framework | ✅ BCrypt |
| Autenticación y permisos verificados en backend | ✅ |
| Consultas parametrizadas / acceso seguro | ✅ JPA + Specifications (sin SQL concatenado) |
| Validación de obligatorios, formatos, duplicados y reglas | ✅ (unicidades por compañía, CHECK constraints, Zod + Jakarta) |
| Eliminaciones/anulaciones/inactivaciones coherentes con trazabilidad | ✅ soft-delete + anulaciones con motivo + kardex + auditoría |
| Datos ficticios; credenciales reales excluidas | ✅ (semillas demo; `.gitignore`; secretos por variables de entorno) |
| Access + refresh token | ❌ → implementar (ver arriba) |
| Permisos por rol en backend | ✅ |

## 5. Entregables documentales

| Entregable | Veredicto | Acción |
| --- | --- | --- |
| Documento del proyecto (portada, integrantes, negocio, problema, objetivos, alcance, 6 módulos, ≥25 reglas, matriz de responsabilidades) | ⚠️ | Insumos listos (factibilidades, 30 reglas de negocio en AGENTS.md, PDF v2); **crear el documento específico del curso** con la matriz por integrante |
| Diseño del sistema (modelo conceptual, modelo de datos del stack, arquitectura, flujos de los 3 procesos + responsable) | ✅/⚠️ | PDF v2 cubre modelo conceptual, E/R, arquitectura y flujos (secuencias/actividades); agregar responsable por proceso |
| Repositorio GitHub con historial de los 3 integrantes | ⚠️ | Crear repo académico alterno y que cada integrante haga commits propios |
| BD y Docker (creación, datos de prueba, contenedor, instrucciones de carga) | ✅/⚠️ | Flyway + seeds + compose; empaquetar en la carpeta entregable con instrucciones |
| Colección de Postman (módulos, auth, integraciones externas; éxito y error) | ✅ | `postman/Vivent.postman_collection.json`: **128 requests** en 14 carpetas (CRUD por recurso, auth con refresh rotado, reportes e integraciones con éxito/error) + environment y `postman/README.md`; validada con Newman (242 aserciones, 0 fallos) |
| Sistema funcional (6 módulos, 2 roles, dashboard, 3 procesos, 9 reportes, 3 APIs externas) | ✅ | 6 módulos + 11 módulos backend, 6 roles, dashboard real, 3 procesos documentados con flujo paso a paso, 9 reportes con exportación, 3 APIs externas activas |
| Publicación en la nube (URLs, evidencia, cuentas de prueba, demo en línea) | ⚠️ | Despliegue académico separado del principal |
| Manual técnico | ✅ | `docs/MANUAL_TECNICO.md` + PDF (requisitos, instalación, configuración, ejecución, estructura, APIs externas por integrante) |
| Manual de usuario | ⚠️ | Crear ilustrado (capturas de los flujos principales) |
| Evidencias de pruebas | ✅/⚠️ | `docs/QA_FUNCTIONAL_CHECKLIST.md` + `agent-context/qa/*` se adaptan como evidencias |
| Presentación final + defensa individual | — | Tarea del equipo (no generable por el agente) |

---

## 6. Resumen de incumplimientos y acción requerida

| # | Requisito no cumplido | Gravedad | Acción |
| --- | --- | --- | --- |
| 1 | Stack permitido (NestJS u otro de los 3) | **Crítica** | Decidir: autorización de equivalencia Spring Boot (opción A) o backend NestJS académico (opción B) |
| 2 | Access + refresh token (con renovación y revocación) | **Alta** (criterio de rúbrica C2) | Implementar en la variante académica |
| 3 | ≥5 registros por catálogo | Alta | Seed académico expandido idempotente |
| 4 | 3 APIs externas reales activas y documentadas | Alta | ✅ Hecho: Photon, Open Food Facts y tipo de cambio (open.er-api.com), documentadas con éxito/error y en Postman |
| 5 | Publicación académica en la nube separada del deploy principal | Alta | Desplegar variante académica en servicios gratuitos con cuentas de prueba |
| 6 | `SKILL.md` + `memory.md` + `.env.example` + `docker/` + colección Postman | Media | Crear en el repo académico |
| 7 | Documento del proyecto, manual técnico, manual de usuario, matriz de responsabilidades, evidencias | Media | Generar documentos en `docs/` del repo académico |
| 8 | Historial de contribuciones de los 3 integrantes | Media | Que cada integrante haga sus commits propios en el repo académico |
