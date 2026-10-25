-- Academic demo data: at least five records per catalog so the dashboard,
-- the three integrated processes and the nine reports have enough fictional
-- data right after a clean bootstrap.
-- Company: Demo Vivent GT (tenant 'demo')
-- Branches: Main Branch (...1112) + Demo Branch 2 (...1116) from S008

-- =====================================================
-- Categories (5)
-- =====================================================
INSERT INTO categories (id, company_id, name, description, is_active)
VALUES
    ('22222222-2222-2222-2222-222222222101', '11111111-1111-1111-1111-111111111111', 'Bebidas', 'Gaseosas, aguas y jugos', true),
    ('22222222-2222-2222-2222-222222222102', '11111111-1111-1111-1111-111111111111', 'Snacks', 'Bolsas y paquetes para tienda', true),
    ('22222222-2222-2222-2222-222222222103', '11111111-1111-1111-1111-111111111111', 'Abarrotes', 'Productos de despensa y granel', true),
    ('22222222-2222-2222-2222-222222222104', '11111111-1111-1111-1111-111111111111', 'Limpieza', 'Jabones, detergentes y desinfectantes', true),
    ('22222222-2222-2222-2222-222222222105', '11111111-1111-1111-1111-111111111111', 'Papelería', 'Útiles escolares y de oficina', true)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Suppliers (4 additional; total 5 with S008)
-- =====================================================
INSERT INTO suppliers (
    id, company_id, name, tax_id, email, phone, address, contact_name, is_active
)
VALUES
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111111', 'Bebidas del Norte, S.A.', '9876543-K', 'pedidos@bebidasnorte.gt', '+502 2345 6701', 'Zona 5, Guatemala', 'María López', true),
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111111', 'Distribución Snacks GT', '8765432-J', 'ventas@snacksgt.gt', '+502 2345 6702', 'Vista Hermosa, Mixco', 'Carlos Ramírez', true),
    ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111111', 'Granel Central', '7654321-H', 'contacto@granelcentral.gt', '+502 2345 6703', 'Carretera a El Salvador km 15', 'Luis Fernández', true),
    ('22222222-2222-2222-2222-222222222204', '11111111-1111-1111-1111-111111111111', 'Suministros Escolares del Centro', '6543210-G', 'info@suministrosec.gt', '+502 2345 6704', 'Zona 9, Guatemala', 'Ana Ruiz', true)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Customers (4 additional registered; total 5 with S008 CF)
-- =====================================================
INSERT INTO customers (
    id, company_id, name, tax_id, email, phone, address, is_generic_consumer, is_active
)
VALUES
    ('22222222-2222-2222-2222-222222222301', '11111111-1111-1111-1111-111111111111', 'Tienda La Bendición', '1234567-K', 'labendicion@correo.gt', '+502 5555 2001', 'San Raymundo', false, true),
    ('22222222-2222-2222-2222-222222222302', '11111111-1111-1111-1111-111111111111', 'Comedor Doña María', '2345678-L', 'comedoramaria@correo.gt', '+502 5555 2002', 'Cuilapa, Santa Rosa', false, true),
    ('22222222-2222-2222-2222-222222222303', '11111111-1111-1111-1111-111111111111', 'Ferretería El Progreso', '3456789-M', 'ferreteriaprogreso@correo.gt', '+502 5555 2003', 'Barberena, Santa Rosa', false, true),
    ('22222222-2222-2222-2222-222222222304', '11111111-1111-1111-1111-111111111111', 'Escuela Particular Mañana Mejor', '4567890-N', 'admin@mananamejor.gt', '+502 5555 2004', 'Cuilapa, Santa Rosa', false, true)
ON CONFLICT DO NOTHING;

-- =====================================================
-- Products (5 additional with presentations; Coca Cola comes from S008)
-- =====================================================
INSERT INTO products (
    id, company_id, name, description, base_unit_code, sku, minimum_stock,
    is_stockable, has_expiration_date, has_lot_tracking, has_serial_tracking, is_active
)
VALUES
    ('22222222-2222-2222-2222-222222222401', '11111111-1111-1111-1111-111111111111', 'Agua Pura Botella 600ml', 'Agua mineral 600ml', 'bottle', 'AGUA-600', 24, true, false, false, false, true),
    ('22222222-2222-2222-2222-222222222402', '11111111-1111-1111-1111-111111111111', 'Papas Fritas Bolsa 150g', 'Snack de papa 150g', 'bag', 'PAPAS-150', 12, true, true, false, false, true),
    ('22222222-2222-2222-2222-222222222403', '11111111-1111-1111-1111-111111111111', 'Café de Origen Tostado', 'Café molido de origen, bolsa 1lb', 'bag', 'CAFE-1LB', 6, true, false, false, false, true),
    ('22222222-2222-2222-2222-222222222404', '11111111-1111-1111-1111-111111111111', 'Jabón de Manos 250ml', 'Jabón líquido para manos', 'bottle', 'JABON-250', 10, true, false, false, false, true),
    ('22222222-2222-2222-2222-222222222405', '11111111-1111-1111-1111-111111111111', 'Libreta de Notas 100h', 'Libreta cosida 100 hojas', 'unit', 'LIBRETA-100', 15, true, false, false, false, true)
ON CONFLICT DO NOTHING;

INSERT INTO product_presentations (
    id, company_id, product_id, name, barcode, conversion_factor,
    purchase_price_suggested, sale_price, is_default_purchase, is_default_sale, is_active
)
VALUES
    -- Agua Pura 600ml
    ('22222222-2222-2222-2222-222222222511', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222401', 'Botella 600ml', '740000000101', 1, 2.50, 5.00, true, true, true),
    ('22222222-2222-2222-2222-222222222512', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222401', 'Pack 12', '740000000112', 12, 27.00, 50.00, false, false, true),
    -- Papas Fritas 150g
    ('22222222-2222-2222-2222-222222222521', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222402', 'Bolsa 150g', '740000000201', 1, 6.00, 9.00, true, true, true),
    ('22222222-2222-2222-2222-222222222522', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222402', 'Caja 24', '740000000224', 24, 130.00, 200.00, false, false, true),
    -- Café de Origen 1lb
    ('22222222-2222-2222-2222-222222222531', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222403', 'Bolsa 1lb', '740000000301', 1, 40.00, 65.00, true, true, true),
    ('22222222-2222-2222-2222-222222222532', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222403', 'Caja 10', '740000000310', 10, 380.00, 600.00, false, false, true),
    -- Jabón de manos 250ml
    ('22222222-2222-2222-2222-222222222541', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222404', 'Botella 250ml', '740000000401', 1, 8.00, 14.00, true, true, true),
    ('22222222-2222-2222-2222-222222222542', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222404', 'Docena', '740000000412', 12, 90.00, 150.00, false, false, true),
    -- Libreta 100 hojas
    ('22222222-2222-2222-2222-222222222551', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222405', 'Unidad', '740000000501', 1, 6.50, 12.00, true, true, true),
    ('22222222-2222-2222-2222-222222222552', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222405', 'Caja 50', '740000000550', 50, 300.00, 540.00, false, false, true)
ON CONFLICT DO NOTHING;
