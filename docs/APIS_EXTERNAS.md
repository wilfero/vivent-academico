# Integraciones con APIs externas (una por integrante)

Tres integraciones **reales** en el código de esta entrega. Cada una hace solicitudes HTTP a un servicio independiente del backend, con manejo explícito de éxito y error, caché para no saturar el servicio y tiempo de espera acotado (timeout 4 s).

| # | Integración | Integrante | Servicio | Endpoint del sistema |
| --- | --- | --- | --- | --- |
| 1 | Geocodificación de direcciones | Christian Méndez | Photon (`photon.komoot.io`) | `GET /api/v1/integrations/geocode?q=...` |
| 2 | Catálogo externo por código de barras | Wilfero Delgado | Open Food Facts | `GET /api/v1/products/barcode-lookup/{barcode}` |
| 3 | Tipo de cambio de referencia | Fernando Palencia | open.er-api.com | `GET /api/v1/integrations/exchange-rate?base=USD&target=GTQ` |

Todas las respuestas usan el contrato estándar `ApiResponse<T>`; cuando el servicio externo falla, la API responde `success=true` con un indicador (`found=false` / `available=false`) y un `message` comprensible, para que la interfaz decida cómo continuar sin romper el proceso del negocio.

---

## 1. Photon — Geocodificación (Christian Méndez)

**Uso en el negocio:** convertir la dirección de una sucursal o de un cliente en coordenadas, útil para rutas de entrega y verificación de direcciones.

**Implementación:** `integrations/geocoding/` (servicio + controlador + DTO). Caché de 10 minutos, límite de entradas, timeout de 4 s.

**Éxito:**

```http
GET https://photon.komoot.io/api/?q=Parque+Central+Cuilapa&limit=1
→ 200 { "features": [ { "geometry": { "coordinates": [-90.298877, 14.275497] },
                       "properties": { "name": "Parque Barrios", "city": "Cuilapa", "state": "Santa Rosa" } } ] }

GET /api/v1/integrations/geocode?q=Parque%20Central%20Cuilapa
→ 200 {"success":true,"data":{"query":"Parque Central Cuilapa","source":"PHOTON","found":true,
        "latitude":14.275497,"longitude":-90.298877,
        "label":"Parque Barrios, Cuilapa, Santa Rosa, Guatemala","message":null}}
```

> Respuesta real capturada el 2026-10-25 contra la pila en Docker. Photon solo acepta
> `default`, `de`, `en` y `fr` en el parámetro `lang`, por lo que no se envía y la etiqueta
> se arma con los nombres del lugar que devuelve el servicio.

**Error:**

```http
GET /api/v1/integrations/geocode?q=
→ { "found": false, "message": "Address query is required" }

Dirección sin coincidencias        → { "found": false, "message": "No match found for the address" }
Servicio caído o timeout (4 s)     → { "found": false, "message": "External geocoding is unavailable right now" }
```

**Error real (verificado el 2026-10-25):**

```json
{"success":true,"data":{"query":"","source":"PHOTON","found":false,"latitude":null,"longitude":null,
 "label":null,"message":"Address query is required"}}
```

---

## 2. Open Food Facts — Código de barras (Wilfero Delgado)

**Uso en el negocio (proceso P2 – POS):** cuando el código escaneado no existe en el catálogo local, el POS consulta el catálogo externo; si el producto existe, ofrece **registrarlo con su presentación unitaria** y continuar la venta sin salir de la pantalla.

**Implementación:** `barcodes/` (servicio + controlador). Caché de 10 minutos, timeout de 4 s. En el frontend: diálogo "Producto del catálogo externo" en `sale-pos-screen.tsx` (nombre, marca, precio de venta y unidad base → crea producto + presentación → agrega la línea al carrito).

**Éxito (Nutella, código 3017620422003):**

```http
GET https://world.openfoodfacts.org/api/v2/product/3017620422003.json?fields=product_name,brands,quantity,image_small_url
→ 200 { "status": 1, "product": { "product_name": "Nutella", "brands": "Nutella, Ferrero", "quantity": "400 g e" } }

GET /api/v1/products/barcode-lookup/3017620422003
→ 200 {"success":true,"data":{"barcode":"3017620422003","source":"OPEN_FOOD_FACTS","found":true,
        "name":"Nutella","brand":"Nutella, Ferrero","quantityText":"400 g e",
        "imageUrl":"https://images.openfoodfacts.org/images/products/301/762/042/2003/front_en.879.200.jpg",
        "message":null}}
```

> Respuesta real capturada el 2026-10-25 contra la pila en Docker.

**Error:**

```http
Código inexistente        → { "found": false, "message": "Product not found in the external catalog" }
Producto sin nombre útil  → { "found": false, "message": "Product found without usable name" }
Servicio caído o timeout  → { "found": false, "message": "External lookup is unavailable right now" }
```

---

## 3. open.er-api.com — Tipo de cambio (Fernando Palencia)

**Uso en el negocio:** obtener el tipo de cambio de referencia USD→GTQ para comparar costos de compras en moneda extranjera sin capturarlo a mano.

**Implementación:** `integrations/exchange/` (servicio + controlador + DTO). Caché de 1 hora (la fuente actualiza diario), timeout de 4 s.

**Éxito:**

```http
GET https://open.er-api.com/v6/latest/USD
→ 200 { "result": "success", "time_last_update_utc": "Sun, 25 Oct 2026 00:02:31 +0000",
        "rates": { "GTQ": 7.645339, "MXN": 17.2, ... } }

GET /api/v1/integrations/exchange-rate?base=USD&target=GTQ
→ 200 {"success":true,"data":{"baseCurrency":"USD","targetCurrency":"GTQ","available":true,
        "rate":7.645339,"lastUpdatedUtc":"Sun, 25 Oct 2026 00:02:31 +0000",
        "source":"OPEN_ER_API","message":null}}
```

> Respuesta real capturada el 2026-10-25 contra la pila en Docker.

**Error:**

```http
Moneda no soportada        → { "available": false, "message": "Target currency is not supported by the external service" }
Servicio rechaza la petición → { "available": false, "message": "External exchange service rejected the request" }
Servicio caído o timeout   → { "available": false, "message": "External exchange service is unavailable right now" }
```

---

## Nota sobre correo electrónico

La variante comercial del sistema (rama `dev` de los repositorios originales) incluye además un microservicio de correo (Nabrix Email) con outbox e idempotencia. **No forma parte de esta entrega académica** porque depende de credenciales privadas de un proveedor; por eso el tercer integrante documenta y demuestra el tipo de cambio, que sí se ejecuta en vivo sin credenciales.

## Verificación en Postman

La carpeta **"Integración externa"** de la colección incluye, para las tres APIs, las solicitudes de éxito y de error con la respuesta esperada.
