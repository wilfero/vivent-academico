# Integraciones con APIs externas (una por integrante)

Cada integrante documenta y demuestra su integración. Todas realizan **solicitudes HTTP reales** hacia servicios independientes del backend principal, con manejo de éxito y error. Ninguna devuelve respuestas fijas generadas dentro del propio módulo.

## 1. Photon — Geocodificación (Christian Méndez)

- **Servicio:** Photon (geocoder público, gratuito).
- **Uso en el proceso:** conversión de direcciones (sucursales/despachos) a coordenadas, con caché y fallback manual.
- **Implementación:** `storefront/service/StorefrontGeocodingService.java` y `storefront/controller/StorefrontGeocodingController.java` (branch `dev`).
- **Éxito:** devuelve latitud/longitud y dirección normalizada.
- **Error:** servicio no disponible → respuesta sin coordenadas y flujo alternativo con coordenadas manuales.

```http
GET https://photon.komoot.io/api/?q=Plaza+Mayor+Cuilapa&limit=1
→ 200 { "features": [ { "geometry": { "coordinates": [-90.28, 14.27] }, "properties": { ... } } ] }
```

## 2. Open Food Facts — Lookup externo de código de barras (Wilfero Delgado)

- **Servicio:** Open Food Facts (catálogo abierto de productos).
- **Uso en el proceso (P2 POS):** cuando el código escaneado no existe en el catálogo local, el POS consulta el catálogo externo; si existe, ofrece **registrar el producto y su presentación unitaria** con un clic y continuar la venta.
- **Implementación:** backend `barcodes/service/BarcodeLookupService.java` (RestClient con timeout 4s, caché TTL 10 min, límite de entradas) + endpoint `GET /api/v1/products/barcode-lookup/{barcode}` (permiso `products.view`); frontend: diálogo en el POS (`sale-pos-screen.tsx`) con registro rápido (crea producto + presentación y agrega la línea al carrito).
- **Éxito (producto conocido, p. ej. Nutella 3017620422003):**

```http
GET https://world.openfoodfacts.org/api/v2/product/3017620422003.json?fields=product_name,brands,quantity,image_small_url
→ 200 { "status": 1, "product": { "product_name": "Nutella", "brands": "Ferrero", ... } }
→ ApiResponse{ found: true, name: "Nutella", brand: "Ferrero" }
```

- **Error (no encontrado o servicio caído):**

```http
GET .../api/v2/product/0000000000000.json
→ { "status": 0 } → ApiResponse{ found: false, message: "Product not found in the external catalog" }
Servicio no disponible → ApiResponse{ found: false, message: "External lookup is unavailable right now" }
```

- **Demo:** escanear el barcode de Nutella en el POS → diálogo "Producto del catálogo externo" → Precio → **Registrar y agregar** → la línea entra al carrito con el barcode.

## 3. Nabrix Email — Microservicio de correo (Fernando Palencia)

- **Servicio:** microservicio de correo de Nabrix (servicio HTTP **independiente** del backend principal; repo privado `nabrix-gt/nabrix-email`).
- **Uso en el proceso:** envío de correos transaccionales (confirmaciones de pedido digital / restablecimiento de contraseña) desacoplado por **outbox** con idempotencia (`email/StorefrontEmailComposer.java`, `email/DigitalOrderStatusEmailHandler.java`, `email/StorefrontPasswordResetEmailHandler.java`).
- **Contrato:** el backend registra el correo pendiente (outbox) y el handler lo envía por HTTP al microservicio; la respuesta de éxito marca el envío y la de error lo deja pendiente con reintento.
- **Configuración para activar:** variables `EmailProperties` (API key del proveedor de correo); en el MVP académico la activación se hace con credenciales de prueba entregadas por el medio privado.
- **Éxito:** `202/200` del microservicio → correo marcado como enviado.
- **Error:** red/5xx → el correo permanece en outbox y se reintenta (el proceso principal no se bloquea).

## Verificación en Postman

La carpeta **"Integración externa"** de la colección incluye las solicitudes de éxito y de no-encontrado del lookup externo; cada request documenta la respuesta esperada.
