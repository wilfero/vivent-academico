# Solicitud de autorización y equivalencia técnica de stack

**Universidad Mariano Gálvez de Guatemala — Centro Universitario Cuilapa, Santa Rosa**
**Curso:** Desarrollo Web · **Catedrático:** M.A. Ing. Elvis Morales
**Equipo:** Christian Méndez · Wilfero Delgado · Fernando Palencia
**Proyecto propuesto:** Vivent — Sistema web SaaS multiempresa de gestión de inventarios (equivalencia de alcance al ERP solicitado)
**Fecha:** 2026-10-25

---

## 1. Solicitud

El enunciado del proyecto prevé dos vías que solicitamos formalmente:

1. **Sistema alterno con autorización del docente:** desarrollar **Vivent** en lugar de un ERP genérico, cumpliendo "los mismos requisitos técnicos, de integración, seguridad, documentación y presentación" con "alcance funcional equivalente al solicitado para el ERP" (párrafo de *Enunciado del proyecto*).
2. **Equivalencia técnica del backend:** el enunciado fija tres stacks de referencia y establece que "cualquier cambio posterior deberá acordarse con el docente". Solicitamos acordar el uso de **Java 25 + Spring Boot 4** como framework backend en lugar de NestJS, manteniendo **exactamente** la base de datos y el frontend del Stack 2 (PostgreSQL + Next.js con TypeScript).

Si ambas autorizaciones se otorgan, el proyecto cumple la totalidad del enunciado; en caso de no autorizarse el cambio de backend, el equipo migrará el backend a NestJS sobre el mismo esquema de PostgreSQL (el frontend y la base de datos no cambiarían).

## 2. Justificación funcional: alcance equivalente al ERP

Un ERP integra áreas del negocio "para registrar operaciones, consultar información, controlar el acceso y generar información útil para la toma de decisiones". Vivent cubre ese perfil con procesos completos e interconectados:

| Área ERP típica | Equivalente en Vivent | Proceso integrado |
| --- | --- | --- |
| Compras / cuentas de proveedores | Módulo de compras por presentación con flujo DRAFT→CONFIRMED→VOIDED | Compra → conversión a unidad base → capas FIFO → kardex → inventario |
| Ventas / facturación interna | Ventas tipo POS (sin factura legal) con descuentos y disposiciones de anulación | Venta → validación de stock → consumo FIFO → kardex → cierre de caja |
| Inventario / almacén | Balances por sucursal/ubicación, capas FIFO, kardex completo, ajustes con aprobación | Traslado → reserva en origen → despacho → recepción → kardex en ambas sucursales |
| Administración / configuración | Compañías, sucursales, usuarios, roles, permisos, suscripciones, auditoría | — (transversal) |
| Información para decisiones | Dashboard con indicadores reales + 9 reportes con filtros y exportación | — |

**Módulos del negocio (seis requeridos, once disponibles):** productos/catálogo, compras, ventas POS, inventario (balances/capas/kardex), traslados, ajustes, cierres de caja, proveedores, clientes, combos y storefront (comercio digital). **Tres procesos integrados por equipo** con reglas de negocio reales (FIFO/PEPS, stock no negativo, reserva de traslados, unicidades por compañía).

## 3. Justificación técnica: equivalencia Spring Boot ↔ NestJS (Stack 2)

El enunciado no fija NestJS por un componente exclusivo del stack, sino como marco backend de referencia. La equivalencia componente por componente es directa:

| Requisito técnico del curso | Stack 2 (NestJS) | Vivent (Spring Boot 4) | Equivalencia |
| --- | --- | --- | --- |
| Lenguaje backend | TypeScript sobre Node.js | Java 25 LTS | Ambos lenguajes tipados con verificación estática; el criterio de tipado fuerte se cumple |
| Framework backend | NestJS (capas, DI, módulos) | Spring Boot 4 (capas, IoC/DI, módulos de negocio) | Mismo patrón: controladores delgados, servicios con reglas, módulos por dominio |
| Acceso a datos seguro | Repositorios TypeORM/Prisma (consultas parametrizadas) | Spring Data JPA + Specifications (consultas parametrizadas) | Ninguna consulta concatenada; equivalente |
| Validación | class-validator | Jakarta Validation + Bean Validation en DTOs | Equivalente (frontend: Zod en ambos casos) |
| Seguridad/roles | Guards + roles por decoradores | Spring Security + permisos `module.action` por endpoint | Equivalente y más granular |
| JWT | @nestjs/jwt (access) | Spring Security OAuth2 Resource Server + JWT | Equivalente; el refresh token se implementará igual en la variante académica |
| BD | PostgreSQL | PostgreSQL 16/17 + Flyway | **Idéntico** al Stack 2 |
| Frontend | Next.js (React, TypeScript) | Next.js 16 (React 19, TypeScript) | **Idéntico** al Stack 2 |
| API docs | Swagger/@nestjs/swagger | springdoc-openapi (Swagger UI) | Equivalente |
| Pruebas | Jest + Testcontainers | JUnit 5 + Testcontainers (25 pruebas contra PostgreSQL/PostGIS real) | Equivalente |

**Argumento central:** el requisito real de la asignatura es demostrar arquitectura en capas, acceso seguro a datos, autenticación con roles, validación en ambos extremos y documentación de API — todos los cuales Spring Boot 4 cumple con la misma semántica que NestJS. Reconstruir el backend en NestJS no agregaría valor académico: el sistema ya existe, tiene **25 pruebas de integración automatizadas contra PostgreSQL real**, documentación viva y un flujo de negocio completo probado, lo que permite al equipo enfocarse en los requisitos específicos del curso (refresh token, APIs externas, reportes, publicación) en lugar de reimplementar lo ya construido.

## 4. Riesgos y mitigaciones de esta propuesta

| Riesgo | Mitigación |
| --- | --- |
| El docente no acepta la equivalencia | Plan B: migrar el backend a NestJS reutilizando el esquema PostgreSQL y el frontend (decisión registrada en `memory.md`) |
| Diferencias de convención con el curso | Entregamos Swagger/OpenAPI + colección Postman + manual técnico que muestran las APIs en el formato esperado |
| Historial de contribuciones | Los tres integrantes harán sus commits propios en el repositorio académico con módulos/procesos/reportes/APIs asignados por matriz |

## 5. Compromisos del equipo

1. Cumplir todos los requisitos restantes del enunciado: access + refresh token, ≥5 registros por catálogo, 3 integraciones externas reales (una por integrante), colección de Postman, documentación, manuales, evidencias y publicación en la nube.
2. Mantener el repositorio académico (`wilfero/vivent-academico`) separado del despliegue comercial, sin modificar los repos originales.
3. Presentar la matriz de responsabilidades por integrante y permitir la verificación individual en la defensa.

**Anexos:** `docs/MATRIZ_CUMPLIMIENTO_DESARROLLO_WEB.md` (cumplimiento requisito por requisito con evidencia), documento del sistema v2 (diagramas, requerimientos, arquitectura) y accesos de demostración para revisión.
