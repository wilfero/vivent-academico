# Documento del proyecto — Vivent (Desarrollo Web)

**Universidad Mariano Gálvez de Guatemala — Centro Universitario Cuilapa, Santa Rosa**
**Carrera:** Ingeniería en Sistemas de Información y Ciencias de la Computación
**Curso:** Desarrollo Web · **Catedrático:** M.A. Ing. Elvis Morales · **Ciclo:** octubre 2026
**Equipo:** Christian Méndez · Wilfero Delgado · Fernando Palencia
**Proyecto:** Vivent — Sistema web SaaS multiempresa de gestión de inventarios (equivalencia de alcance al ERP académico, con autorización del docente)

---

## 1. Negocio y giro del negocio

Vivent atiende **empresas comerciales y distribuidoras pequeñas** (tiendas, distribuidoras de bebidas y abarrotes, papelerías) que administran mercadería entre una o varias sucursales. Es una plataforma **SaaS multiempresa**: cada compañía opera aislada por subdominio, con su propia identidad visual, moneda (GTQ), idioma (es-GT / en-US) y datos.

## 2. Problema

Las pequeñas empresas llevan el inventario en cuadernos u hojas de cálculo desconectados de la caja: no saben con certeza qué tienen, dónde está y cómo se mueve; el costo de lo vendido se reconstruye a mano; las correcciones (daños, pérdidas, conteos) quedan sin evidencia; y mover mercadería entre sucursales se hace por teléfono sin control ni trazabilidad.

## 3. Objetivos

1. Registrar operaciones de compra y venta integradas al inventario en unidad base.
2. Costear el inventario con FIFO/PEPS por capas y registrar todo movimiento en kardex.
3. Controlar el acceso por roles y permisos aplicados en backend y frontend.
4. Operar traslados entre sucursales con reserva, despacho y recepción (total o parcial) evidenciados.
5. Corregir el inventario con ajustes controlados por motivo y aprobación.
6. Generar información útil: dashboard con indicadores reales y nueve reportes con filtros y exportación.
7. Auditar toda acción sensible y conservar trazabilidad con eliminación lógica.

## 4. Alcance

Incluye: autenticación con access + refresh token, dos o más roles con permisos distintos, dashboard real, seis módulos del negocio completos (de once disponibles), tres procesos integrados con reglas del negocio, consultas con búsqueda/filtros/orden, nueve reportes, tres integraciones con APIs externas (una por integrante), validaciones en frontend y backend, datos de prueba ficticios (≥5 por catálogo), publicación en la nube de BD + backend + frontend.

Fuera de alcance: facturación legal, integración tributaria, pagos automáticos reales, adjuntos S3 y e-commerce.

## 5. Módulos del negocio (6 contabilizados)

| # | Módulo | Responsable | Alcance |
| --- | --- | --- | --- |
| 1 | Catálogo (unidades, categorías, productos, presentaciones, barcodes) | Christian Méndez | CRUD completo + lookup por barcode |
| 2 | Inventario (balances, capas FIFO, kardex, movimientos, stock manual) | Christian Méndez | Existencias en unidad base, kardex completo |
| 3 | Compras (flujo DRAFT→CONFIRMED→VOIDED con motivo) | Wilfero Delgado | Compra por cualquier presentación; FIFO al confirmar |
| 4 | Ventas POS (flujo DRAFT→CONFIRMED→CANCELLED con disposición) | Wilfero Delgado | Escaneo, descuentos, métodos de pago |
| 5 | Traslados (REQUESTED→APPROVED→IN_TRANSIT→RECEIVED/PARTIAL) | Fernando Palencia | Reserva, despacho, recepción parcial |
| 6 | Ajustes de inventario (motivos + aprobación) | Fernando Palencia | IN/OUT por daño, pérdida, vencimiento, conteo |

Complementarios: cierres de caja (por usuario/sucursal/fecha), proveedores, clientes (con consumidor final genérico), combos y storefront.

## 6. Reglas del negocio (30)

1. Una compañía no ve ni afecta datos de otra (aislamiento por `company_id` en la API).
2. Cada compañía se identifica por subdominio (`tenant_slug` + dominio).
3. Un usuario pertenece a una sola compañía.
4. Un usuario pertenece a una sucursal activa y puede trasladarse a otra sucursal de la misma compañía.
5. El nombre de usuario puede repetirse entre compañías, pero no dentro de la misma.
6. La autenticación valida el conjunto `subdomain + username + password` en el backend.
7. La sesión usa access token JWT (30 min) + refresh token opaco hasheado (7 días) con rotación en cada uso.
8. El refresh token se revoca al cerrar sesión y al cambiar/resetear la contraseña (`authVersion` invalida tokens antiguos).
9. Los roles son plantillas de permisos `module.action`; los overrides por usuario aplican DENY antes que GRANT.
10. Los usuarios no-COMPANY_ADMIN solo ven y operan su sucursal asignada.
11. Todo producto declara una unidad base; el inventario real se controla en unidad base.
12. Toda presentación se convierte a unidad base con `conversion_factor > 0` (admite decimales, p. ej. 0.5).
13. El código de barras vive en la presentación y es la llave de escaneo del POS.
14. Compras y ventas pueden registrarse por cualquier presentación; el sistema convierte a unidad base.
15. Al confirmar una compra se crean capas FIFO (cantidad, costo unitario, lote y vencimiento opcionales).
16. La venta consume primero las capas FIFO más antiguas con cantidad restante.
17. `quantity_available = quantity_on_hand − quantity_reserved` es columna generada por la base de datos.
18. Está prohibido vender sin stock disponible (error `INSUFFICIENT_STOCK`); el stock nunca es negativo.
19. La compra confirmada es inmutable; para corregir se anula con motivo obligatorio.
20. La anulación de compra revierte exactamente sus capas FIFO originales (por `purchase_detail_id`), nunca capas de otra compra.
21. La venta confirmada solo se anula con permiso, motivo obligatorio y disposición de productos (retorno a stock, daño, pérdida o no retorno).
22. El traslado reserva stock en el origen desde la aprobación; el descuento formal ocurre en la recepción.
23. La recepción del traslado puede ser parcial y deja evidencia del faltante (aprobado vs recibido).
24. No se permite trasladar entre compañías ni recibir más de lo aprobado.
25. Los ajustes de inventario se registran por tipo (daño, pérdida, vencimiento, conteo, robo, etc.), dirección IN/OUT y observación; los aprobados impactan inventario y kardex.
26. El cierre de caja es único por usuario/sucursal/fecha; calcula esperado vs contado y su diferencia; puede reabrirse y recalcularse.
27. Los descuentos de venta o línea requieren el permiso `sales.apply_discount` (validado en backend).
28. Toda acción sensible (login, confirmar, anular, aprobar, recibir, autorizar) se registra en auditoría con valores antes/después.
29. Las eliminaciones de catálogo, maestros y usuarios son lógicas (`deleted_at` / estados); nada se borra físicamente.
30. Las credenciales reales no se versionan: configuración por variables de entorno (`.env.example` sin secretos) y contraseñas siempre con hash BCrypt.

## 7. Procesos integrados (3, uno por integrante)

| Proceso | Responsable | Módulos conectados | Reglas aplicadas |
| --- | --- | --- | --- |
| P1: Registrar y confirmar compra | Christian Méndez | Catálogo + Compras + Inventario | 11, 14, 15, 19, 20, 24 |
| P2: Vender en POS y cerrar caja | Wilfero Delgado | Ventas + Inventario + Cierres | 16, 17, 18, 21, 26, 27 |
| P3: Traslado entre sucursales | Fernando Palencia | Traslados + Inventario (origen y destino) | 10, 22, 23, 24 |

## 8. Matriz de responsabilidades del equipo

| Integrante | Módulos (2) | Proceso (1) | Reportes (3) | API externa (1) | Compartidos |
| --- | --- | --- | --- | --- | --- |
| Christian Méndez | Catálogo, Inventario | P1 Compra→FIFO | Existencias, Kardex, Productos | Photon (geocodificación HTTP real, con caché y fallback) | Autenticación, permisos, auditoría |
| Wilfero Delgado | Compras, Ventas POS | P2 Venta→Cierre | Ventas, Compras, Cierre de caja | Open Food Facts (lookup externo de barcode, HTTP real éxito/error) | Dashboard, publicaciones, Docker |
| Fernando Palencia | Traslados, Ajustes | P3 Traslado | Traslados, Ajustes, Stock bajo | Nabrix Email (microservicio HTTP de correo, contrato con manejo de éxito/error) | Datos de prueba, revisión de seguridad |

Distribución equilibrada de transversales: autenticación/roles (todos la explican), dashboard (Wilfero), publicación en la nube (Wilfero + Christian), datos de prueba (Fernando).

## 9. Stack registrado (propuesta inicial)

PostgreSQL 16/17 + Java 25 / Spring Boot 4 (backend) + Next.js 16 / React 19 / TypeScript (frontend). Equivalencia técnica de backend respecto al Stack 2 de referencia: ver `JUSTIFICACION_STACK_SPRING_BOOT.md`. Complementos exigidos por el curso: HTML/CSS (Tailwind), Docker, Swagger/OpenAPI + Postman, Git/GitHub, agente de IA (AGENTS.md, SKILL.md, memory.md).
