# Instrucciones para levantar Vivent localmente (otras PCs)

Proyecto: Vivent — SaaS multiempresa de inventarios (variante académica)
Última actualización: 2026-10-25

Este documento permite levantar el proyecto completo en una PC con los mínimos requeridos, sin depender de la máquina original ni del despliegue principal.

---

## 1. Requisitos mínimos de la PC

| Requisito | Versión | Para qué | ¿Obligatorio? |
| --- | --- | --- | --- |
| Docker Desktop | cualquier reciente (motor WSL2 en Windows) | Base de datos PostgreSQL y (opcional) backend+frontend contenedorizados | **Sí** (obligatorio en ambos modos: la BD siempre corre en Docker) |
| Git | reciente | Clonar el repositorio | **Sí** |
| Node.js | 24 LTS (24.15.0) | Frontend (solo modo desarrollo; en modo "todo Docker" no hace falta) | Solo modo desarrollo |
| pnpm | 11.1.3 (vía Corepack) | Gestor de paquetes del frontend | Solo modo desarrollo |
| Java JDK | 25 LTS (Temurin 25) | Backend y sus pruebas (solo modo desarrollo) | Solo modo desarrollo |
| RAM | 8 GB recomendado | Contenedores + builds | Recomendado |

Verificaciones rápidas:

```bash
docker --version
git --version
node -v        # v24.x (solo modo desarrollo)
pnpm -v        # 11.x  (solo modo desarrollo)
java -version  # 25.x  (solo modo desarrollo)
```

Instalar Node con un gestor de versiones (recomendado):

```bash
# Windows (fnm)                      # macOS/Linux (fnm o nvm)
fnm install 24.15.0                  nvm install 24
fnm use 24.15.0                      nvm use 24
corepack enable                      corepack enable
corepack prepare pnpm@11.1.3 --activate
```

> El frontend usa **pnpm** (no usar `npm install` ni `package-lock.json`).

---

## 2. Obtener el código

```bash
git clone https://github.com/wilfero/vivent-academico.git
cd vivent-academico
```

> El repositorio es **privado**: se comparte acceso al docente desde GitHub (Settings → Collaborators).

Estructura del entregable:

```text
vivent-academico/
├── backend/     API Spring Boot 4 (Java 25), reglas, seguridad, validaciones
├── frontend/    App Next.js 16 (React 19, TypeScript, Tailwind, PWA)
├── database/    Migraciones y seeds (fuente única de esquema y datos demo)
├── docker/      Compose para levantar la base de datos (y stack completo)
├── docs/        Diagramas, reglas del negocio, manuales y evidencias
├── postman/     Colección de Postman (módulos, auth, APIs externas)
├── .env.example Variables de ejemplo SIN credenciales reales
├── .gitignore
├── AGENTS.md    Instrucciones para agentes de IA
├── SKILL.md     Habilidades del agente para este proyecto
└── memory.md    Memoria del proyecto (estado, decisiones, pendientes)
```

---

## 3. Modo A — Todo en Docker (el más simple)

**Solo requiere Docker Desktop y Git.** El primer build descarga dependencias (tarda varios minutos).

```bash
cd docker
docker compose -f compose.local.yml up -d --build
```

Qué levanta:

```text
db        PostgreSQL 16/17  → localhost:5433  (usuario/pass: vivent/vivent)
backend   Spring Boot 4     → http://localhost:8080
frontend  Next.js 16        → http://localhost:3000
```

Al arrancar el backend por primera vez, **Flyway crea el esquema y carga los seeds automáticamente** (migraciones versionadas + seeds idempotentes, incluida la empresa demo).

Probar:

```text
Frontend:   http://localhost:3000          (tenant demo, login)
Swagger UI: http://localhost:8080/swagger-ui/index.html
OpenAPI:    http://localhost:8080/v3/api-docs
```

Detener / reiniciar / borrar datos:

```bash
docker compose -f compose.local.yml down        # detener (conserva datos)
docker compose -f compose.local.yml up -d       # reanudar
docker compose -f compose.local.yml down -v     # ⚠️ borra el volumen de la BD
```

### 3.1 El mismo comando sirve en otra PC o en un servidor

No hay dos proyectos: el **mismo** `compose.local.yml` levanta la base, el backend y el frontend en cualquier máquina con Docker (Windows, macOS o Linux). La única diferencia es desde qué hostname se abre el navegador.

```bash
# 1) Copiar el proyecto (git clone o descomprimir el ZIP)
# 2) Levantar todo
cd vivent-academico/docker
docker compose -f compose.local.yml up -d --build
```

Cómo resuelve la URL de la API el frontend (sin configuración manual):

| Se abre desde | URL del frontend | URL de la API que usa |
| --- | --- | --- |
| La misma PC | `http://localhost:3000` | `http://localhost:8080` |
| Otro equipo de la red local | `http://192.168.1.20:3000` | `http://192.168.1.20:8080` |
| Un dominio público | `https://vivent.midominio.com` | Se define con `NEXT_PUBLIC_API_BASE_URL` **al compilar** |

Si se publica detrás de un dominio (nginx, Render, etc.), hay que compilar con la URL pública de la API, porque Next.js inyecta las variables `NEXT_PUBLIC_*` en tiempo de build:

```bash
# Linux/macOS
NEXT_PUBLIC_API_BASE_URL=https://api.midominio.com docker compose -f compose.local.yml up -d --build

# Windows PowerShell
$env:NEXT_PUBLIC_API_BASE_URL="https://api.midominio.com"; docker compose -f compose.local.yml up -d --build
```

En un servidor Linux con Docker, los puertos 8080 y 3000 deben estar accesibles (o publicados por nginx). El CORS del backend acepta el origen del frontend y el backend valida siempre el token.

---

## 4. Modo B — Desarrollo (BD en Docker + servicios en caliente)

Útil para programar con recarga automática.

**4.1 Base de datos en Docker**

```bash
cd docker
docker compose -f compose.db.yml up -d        # solo PostgreSQL en :5433
```

**4.2 Backend (Java 25 + Maven wrapper)**

```bash
cd backend
# Variables (PowerShell) — o usar .env con su IDE
$env:VIVENT_DB_URL="jdbc:postgresql://localhost:5433/vivent"
$env:VIVENT_DB_USERNAME="vivent"
$env:VIVENT_DB_PASSWORD="vivent"

./mvnw spring-boot:run        # arranca en http://localhost:8080
./mvnw test                   # suite de pruebas de integración (Testcontainers)
```

En Linux/macOS: `JAVA_HOME=<ruta-jdk-25> ./mvnw spring-boot:run`.

**4.3 Frontend (Node 24 + pnpm)**

```bash
cd frontend
pnpm install                  # usa pnpm-lock.yaml (no regenerar con npm)
pnpm dev                      # http://localhost:3000
```

Si no se define `NEXT_PUBLIC_API_BASE_URL`, el cliente HTTP apunta al backend en `http://localhost:8080` por defecto en desarrollo.

---

## 5. Datos de prueba (empresa demo)

Los seeds crean una empresa demo lista para operar:

```text
Compañía:   Demo Vivent GT     Tenant slug: demo     Subdominio local: localhost
Moneda:     GTQ · Idioma: es-GT · Tema: light · Zona horaria: America/Guatemala
Sucursales: Main Branch (+ demo adicional)
Ubicación:  Main Warehouse (por defecto)
Suscripción: TRIAL/BASIC según seed vigente
```

Cuentas demo (contraseña compartida: `ChangeMe123!`):

| Usuario | Rol | Uso previsto |
| --- | --- | --- |
| `admin` | COMPANY_ADMIN | Administración completa, reportes, bitácora, cierres |
| `seller` | SELLER | Venta POS y cierre de caja |
| `inventory` | WAREHOUSE_USER | Compras y ajustes de inventario |
| `transfers_origin` | WAREHOUSE_USER | Despacho de traslados de la sucursal 1 |
| `transfers_destination` | WAREHOUSE_USER | Solicitud/recepción de traslados (sucursal 2) |
| `auditor` | AUDITOR | Reportes y control (sin permisos de escritura) |

> Las contraseñas demo están documentadas a propósito para ambientes locales; en base de datos van hasheadas con BCrypt y **nunca** deben usarse en producción.

Flujo de demostración sugerido (procesos integrados):

```text
1. Login admin → /purchases/new → compra por presentación → Confirmar
   (crea capas FIFO, kardex PURCHASE_IN y sube inventario)
2. Login seller → /sales/new → escanear/agregar → Confirmar
   (consume FIFO, kardex SALE_OUT, baja inventario)
3. /closures/new → cierre de caja del día (preview + counted)
4. Login transfers_destination → /transfers/new → solicitar
   Login admin → aprobar (reserva stock)  →  Login transfers_origin → despachar
   Login transfers_destination → recibir (total o parcial)
5. /reports → 9 reportes con filtros y exportación
```

---

## 6. Variables de entorno (referencia)

Ver `.env.example` en la raíz. Las principales:

| Variable | Dónde | Ejemplo local |
| --- | --- | --- |
| `VIVENT_DB_URL` | backend | `jdbc:postgresql://localhost:5433/vivent` |
| `VIVENT_DB_USERNAME` / `VIVENT_DB_PASSWORD` | backend | `vivent` / `vivent` |
| `NEXT_PUBLIC_API_BASE_URL` | frontend | `http://localhost:8080` |
| `VIVENT_PLATFORM_*` (bootstrap de plataforma, opcional) | backend | ver `.env.example` del deploy; el panel master es opcional en local |

Copiar ejemplo y ajustar sin credenciales reales:

```bash
cp .env.example .env    # PowerShell: Copy-Item .env.example .env
```

---

## 7. Problemas frecuentes

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `mvnw test` falla con `Connection refused` o `Unable to obtain connection` | La base de datos no está levantada | Ejecutar `cd docker && docker compose -f compose.db.yml up -d` (o el stack completo) y esperar el estado `healthy` antes de las pruebas |
| El frontend llama a `localhost:8080` desde otra PC y falla | Se compiló con una URL fija de API | Dejar `NEXT_PUBLIC_API_BASE_URL` vacío (autodetección por host) o recompilar con la URL pública correcta |
| Puerto 5433/8080/3000 ocupado | Otro servicio local | Cambiar el mapeo de puertos en el compose y las variables de URL correspondientes |
| Docker responde `500 Internal Server Error` en cualquier comando | Motor Linux (WSL2) colgado o sin espacio en disco | Ejecutar `wsl --shutdown`, cerrar y volver a abrir Docker Desktop; liberar espacio en `C:` (`docker system prune -af` si el disco de Docker está en C:) |
| El build falla con “no space left on device” | Disco lleno | Liberar espacio (`docker builder prune -f`, limpiar `%TEMP%` y cachés) y reintentar |
| El build del frontend se detiene en `Running TypeScript...` | Worker de build del entorno local | Usar `NEXT_PRIVATE_BUILD_WORKER=1 pnpm run build` |
| Cámara del POS no abre en el teléfono (LAN) | El navegador exige contexto seguro | Probar con build de producción sobre HTTPS, o usar el input manual de código de barras |
| Login dice usuario inactivo | Seed histórica con admin inactivo | Los seeds actuales activan los usuarios demo con `ChangeMe123!`; verificar que la BD se creó limpia |
| Duplicidad de migraciones | Se editó un archivo ya aplicado | Regla: **nunca editar migraciones/seeds aplicadas**; crear archivos nuevos con timestamp UTC (Flyway `out-of-order=true`) |
| Reset completo de datos | — | `docker compose ... down -v` y volver a levantar (Flyway re-crea todo) |
| Fechas se muestran en otra zona horaria | Vista sin tenant timezone | El frontend usa la zona horaria de la compañía cargada del tenant-config (America/Guatemala en demo) |

---

## 8. Verificación final (checklist de levantamiento)

```text
[ ] docker compose levanta sin errores (db healthy)
[ ] Flyway aplicó migraciones y seeds (log del backend)
[ ] http://localhost:3000 carga el login con branding de la demo
[ ] Login admin / ChangeMe123! entra al dashboard
[ ] Swagger responde en :8080
[ ] Compra → Confirmar → kardex/inventario visibles
[ ] Venta → Confirmar → inventario baja
[ ] Cierre de caja crea registro del día
[ ] Reportes (9) cargan con filtros y exportan
```

Con este checklist la PC queda lista para demostrar los tres procesos integrados, el dashboard, los nueve reportes y la seguridad por roles del requisito académico.
