CREATE TABLE product_units (
    code varchar(30) PRIMARY KEY,
    name varchar(100) NOT NULL,
    translation_key varchar(150) NOT NULL,
    allows_decimal boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO product_units (code, name, translation_key, allows_decimal, is_active)
VALUES
    ('unit', 'Unit', 'unit.unit', false, true),
    ('can', 'Can', 'unit.can', false, true),
    ('bottle', 'Bottle', 'unit.bottle', false, true),
    ('bag', 'Bag', 'unit.bag', false, true),
    ('kg', 'Kilogram', 'unit.kg', true, true),
    ('lb', 'Pound', 'unit.lb', true, true),
    ('meter', 'Meter', 'unit.meter', true, true),
    ('liter', 'Liter', 'unit.liter', true, true)
ON CONFLICT (code) DO NOTHING;

ALTER TABLE products
    ADD CONSTRAINT fk_products_base_unit_code
    FOREIGN KEY (base_unit_code) REFERENCES product_units (code);

CREATE INDEX idx_product_presentations_company_product
    ON product_presentations (company_id, product_id);

CREATE TRIGGER trg_product_units_set_updated_at
    BEFORE UPDATE ON product_units
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
