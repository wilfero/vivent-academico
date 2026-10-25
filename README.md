# Vivent — Variante académica (Desarrollo Web)

Sistema web de gestión de inventarios para empresas (equivalencia de alcance al ERP solicitado), desarrollado por Christian Méndez, Wilfero Delgado y Fernando Palencia para el curso **Desarrollo Web** (Universidad Mariano Gálvez, Centro Universitario Cuilapa).

> **Aviso de alcance:** este repositorio es la **variante académica** del proyecto Vivent. El despliegue comercial (corpoviva.com) es un producto separado y no se ve afectado por este repositorio.

## Descripción

Vivent permite administrar inventario multi-sucursal con control de productos por unidad base y presentaciones, compras a proveedores con costeo FIFO/PEPS, ventas tipo POS, traslados entre sucursales, ajustes de inventario, cierres de caja, reportes y auditoría, con autenticación por roles y permisos.

## Stack

| Capa | Tecnología |
| --- | --- |
| Base de datos | PostgreSQL 16/17 (Docker) + Flyway (migraciones y seeds versionados) |
| Backend | Java 25 LTS · Spring Boot 4.0.6 · Spring Security (JWT) · Spring Data JPA · Jakarta Validation · OpenAPI/Swagger |
| Frontend | Next.js 16 (App Router) · React 19 · TypeScript · Tailwind CSS v4 · TanStack Query/Table · React Hook Form + Zod · next-intl (es-GT/en-US) |
| Pruebas | Testcontainers (PostgreSQL real) · Playwright E2E (opcional) |
| Despliegue | Docker Compose (BD + backend + frontend) |

> Nota sobre el stack: la asignatura prevé tres stacks posibles; este proyecto usa Spring Boot 4 por equivalencia técnica documentada (solicitud y tabla de equivalencias en `docs/JUSTIFICACION_STACK_SPRING_BOOT.md`), sujeta a autorización del docente.

## Estructura

```text
backend/     API, reglas del negocio, acceso a datos, autenticación y validaciones
frontend/    Interfaz, navegación, formularios y consumo de la API
database/    Migraciones (esquema) y seeds (datos de prueba) — empaquetadas también en el backend
docker/      compose.db.yml (solo BD) · compose.local.yml (stack completo)
docs/        Diagramas, matriz de cumplimiento, justificación de stack, manuales y evidencias
postman/     Colección de Postman (auth, módulos, APIs externas)
```

## Requisitos previos

- Docker Desktop (motor corriendo) y Git — **mínimo indispensable**
- Para modo desarrollo: Node.js 24 LTS + pnpm 11 (Corepack) y Java JDK 25

## Instalación y ejecución (local)

**Modo A — todo en Docker (recomendado para primera vez):**

```bash
cd docker
docker compose -f compose.local.yml up -d --build
```

**Modo B — desarrollo (BD en Docker + servicios en caliente):**

```bash
cd docker && docker compose -f compose.db.yml up -d
cd ../backend && ./mvnw spring-boot:run      # VIVENT_DB_* apuntando a localhost:5433
cd ../frontend && pnpm install && pnpm dev
```

Instrucciones completas (incluye usuarios demo, flujo de demostración, variables de entorno y solución de problemas): [`INSTRUCCIONES_LOCAL.md`](INSTRUCCIONES_LOCAL.md).

> **En otra PC o en un servidor:** el mismo comando funciona siempre que Docker esté instalado y los puertos 3000/8080 estén libres. El frontend deduce la URL de la API del host desde el que se abre (`localhost` o IP de la red). Si se publica con un dominio, compilar con `NEXT_PUBLIC_API_BASE_URL=https://api.midominio.com` (Next.js inyecta esa variable en tiempo de build). Detalle en `INSTRUCCIONES_LOCAL.md` §3.1.

## Integraciones externas (una por integrante)

| Integración | Integrante | Endpoint | Función en el negocio |
| --- | --- | --- | --- |
| Photon (geocodificación) | Christian Méndez | `GET /api/v1/integrations/geocode?q=...` | Dirección → coordenadas para rutas de entrega |
| Open Food Facts (barcode) | Wilfero Delgado | `GET /api/v1/products/barcode-lookup/{barcode}` | Si el código no existe, el POS ofrece registrar el producto y continuar la venta |
| open.er-api.com (tipo de cambio) | Fernando Palencia | `GET /api/v1/integrations/exchange-rate?base=USD&target=GTQ` | Tasa de referencia para costos en moneda extranjera |

Ejemplos de éxito y error en [`docs/APIS_EXTERNAS.md`](docs/APIS_EXTERNAS.md); las tres están en la carpeta “Integración externa” de Postman.

## URLs locales

```text
Frontend:   http://localhost:3000
API:        http://localhost:8080/api/v1
Swagger UI: http://localhost:8080/swagger-ui/index.html
BD:         localhost:5433 (vivent/vivent)
```

## Cuentas de prueba (empresa demo, tenant `demo`)

Contraseña compartida: `ChangeMe123!`

| Usuario | Rol |
| --- | --- |
| `admin` | COMPANY_ADMIN |
| `seller` | SELLER |
| `inventory` | WAREHOUSE_USER |
| `transfers_origin` | WAREHOUSE_USER |
| `transfers_destination` | WAREHOUSE_USER |
| `auditor` | AUDITOR |

## Contratos de utilidad

- `common/util/RequestMetadata.java` centraliza la captura de IP (con soporte `X-Forwarded-For`) y User-Agent para auditoría de sesiones.
- `database/bootstrap_demo.py` genera operaciones demo (compras, ventas, traslado, ajuste, cierre) vía API real tras el primer arranque.

## Documentación

- [`docs/MATRIZ_CUMPLIMIENTO_DESARROLLO_WEB.md`](docs/MATRIZ_CUMPLIMIENTO_DESARROLLO_WEB.md) — cumplimiento requisito por requisito
- [`docs/JUSTIFICACION_STACK_SPRING_BOOT.md`](docs/JUSTIFICACION_STACK_SPRING_BOOT.md) — solicitud de equivalencia técnica
- [`INSTRUCCIONES_LOCAL.md`](INSTRUCCIONES_LOCAL.md) — levantamiento local paso a paso
- `AGENTS.md`, `SKILL.md`, `memory.md` — agente de IA del proyecto (instrucciones, habilidades y memoria)
