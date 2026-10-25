# Manual de usuario — Vivent

Guía ilustrada para acceder y ejecutar los procesos principales. Las capturas se completan durante la demostración en vivo (indicadas como 📷).

## 1. Acceder al sistema

1. Abre la dirección del sistema (en la nube o `http://localhost:3000`).
2. La pantalla de login muestra el nombre y colores de la compañía.
3. Ingresa tu **usuario** y **contraseña** y presiona **Iniciar sesión**.

📷 CAPTURA 1: pantalla de login con branding de la demo.

> Si es tu primer ingreso con contraseña temporal, el sistema te pedirá crear una nueva antes de continuar.

## 2. Reconocer la pantalla principal (dashboard)

Al entrar verás el **dashboard** con indicadores reales: existencias disponibles, productos activos, ventas del día, stock bajo y movimientos recientes. El menú lateral solo muestra los módulos autorizados a tu rol.

📷 CAPTURA 2: dashboard con tarjetas y menú lateral filtrado por rol.

## 3. Proceso 1 — Registrar una compra

1. Menú **Compras** → botón **Nueva compra**.
2. Selecciona el **proveedor** (p. ej., Distribuidora Demo).
3. Agrega líneas: elige el **producto** y su **presentación** (Lata, Six Pack, Caja 24), la **cantidad** y el **costo unitario**.
4. El sistema convierte automáticamente a **unidad base** (ej. 2 cajas de 24 = 48 latas).
5. Guarda el **borrador** o presiona **Confirmar compra**.

Al confirmar: el inventario del almacén aumenta, se crea la capa FIFO con ese costo y el kardex registra la entrada `PURCHASE_IN`.

📷 CAPTURA 3: formulario de compra con detalle por presentación.
📷 CAPTURA 4: detalle de compra confirmada con stock actualizado.

> Si te equivocas en una compra ya confirmada: botón **Anular** con motivo obligatorio. El sistema devuelve el stock exacto de esa compra y lo deja registrado.

## 4. Proceso 2 — Vender en el POS y cerrar caja

1. Menú **Ventas** → **Nueva venta** (abre el POS).
2. Identifica al **cliente**: presiona Enter con el campo vacío para **Consumidor Final (CF)**, o escribe el NIT y presiona Enter.
3. **Escanea** el código de barras (o usa **Ctrl+B** para buscar el producto) y presiona **Enter** para agregar la línea.
4. Ajusta cantidades o quita líneas si es necesario.
5. Aplica **descuentos** si tu rol lo permite.
6. Presiona **Finalizar (Ctrl+Enter)** y elige el **método de pago** (Efectivo, Tarjeta, Transferencia, Crédito, Otro).
7. Confirma: el sistema descuenta el inventario usando FIFO (primero lo más antiguo) y el kardex registra `SALE_OUT`.

📷 CAPTURA 5: POS con carrito y escáner.
📷 CAPTURA 6: venta confirmada con costo FIFO.

Si el producto no existe al escanear: el sistema consulta un **catálogo externo**; si lo encuentra, te ofrece **registrarlo y agregarlo** al carrito en un paso.

**Cierre de caja** (al final del día):
1. Menú **Cierres** → **Nuevo cierre**.
2. Verifica la vista previa: ventas confirmadas, anuladas, descuentos y efectivo esperado.
3. Escribe el **monto contado** y guarda.
4. El sistema calcula la **diferencia**; el administrador puede **Aprobar** el cierre desde el detalle.

📷 CAPTURA 7: cierre con vista previa y diferencia.

## 5. Proceso 3 — Traslado entre sucursales

1. La sucursal que **solicita**: menú **Traslados** → **Nuevo traslado** → elige la **sucursal origen** y agrega productos y cantidades.
2. La sucursal **origen** revisa y **Aprueba** (el stock queda reservado) y luego **Despacha**.
3. La sucursal **destino** confirma la **Recepción** total o **parcial** (el sistema deja evidencia del faltante).
4. El kardex registra `TRANSFER_OUT` en origen y `TRANSFER_IN` en destino.

📷 CAPTURA 8: traslado con estados REQUESTED → APPROVED → IN_TRANSIT → RECEIVED.

## 6. Corregir el inventario (ajustes)

1. Menú **Ajustes** → **Nuevo ajuste**.
2. Elige **tipo** (daño, pérdida, vencimiento, conteo físico…), **dirección** (entrada/salida), producto, presentación o cantidad en unidad base, y **observación**.
3. **Enviar a aprobación**: un usuario autorizado **Aprueba** o **Rechaza** (con motivo).
4. Al aprobarse, el inventario y el kardex se actualizan.

📷 CAPTURA 9: ajuste pendiente y aprobado.

## 7. Consultas y reportes

- Todos los listados tienen **búsqueda por texto**, **filtros combinables** (estado, fechas, sucursal, proveedor, método de pago) y **ordenamiento** por columnas.
- Menú **Reportes**: existencias, kardex, movimientos, productos (top/stock bajo), ventas, compras, traslados, ajustes y cierres de caja.
- Cada reporte tiene filtros de fecha y sucursal, y botón de **exportación** (.xls) de la página visible.

📷 CAPTURA 10: reporte de ventas con filtros aplicados.

### 7.1 Exportar o imprimir un reporte

Cada reporte tiene dos botones en la esquina superior derecha:

- **Exportar CSV**: descarga la tabla tal como se ve, con los filtros aplicados (se abre en Excel y conserva los acentos).
- **Imprimir**: abre el diálogo del navegador con la página limpia (sin menú lateral ni barra superior); desde ahí se puede imprimir o guardar como PDF.

Los nueve reportes disponibles son: ventas, compras, inventario por sucursal, kardex, movimientos, productos (más vendidos, más movidos, sin movimiento, stock bajo y agotados), traslados, ajustes y cierres de caja.

## 8. Cerrar sesión

Botón de **logout** en el header (o en el menú móvil). El sistema revoca tu token de renovación y regresa al login.

## 9. Cuentas de prueba (demo)

Contraseña: `ChangeMe123!`

| Usuario | Qué puede hacer |
| --- | --- |
| `admin` | Todo (administración, reportes, bitácora, cierres) |
| `seller` | POS, clientes y cierre de caja |
| `inventory` | Compras, inventario y ajustes |
| `transfers_origin` | Aprobar/despachar traslados, aprobar ajustes |
| `transfers_destination` | Solicitar/recibir traslados |
| `auditor` | Solo lectura (reportes y bitácora) |
