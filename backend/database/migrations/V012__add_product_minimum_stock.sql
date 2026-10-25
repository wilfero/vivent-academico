ALTER TABLE products
    ADD COLUMN minimum_stock numeric(19,6) NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_products_minimum_stock CHECK (minimum_stock >= 0);

UPDATE products
SET minimum_stock = CASE id
    WHEN '22222222-2222-2222-2222-222222222224' THEN 24
    WHEN '22222222-2222-2222-2222-222222222228' THEN 12
    ELSE minimum_stock
END
WHERE company_id = '11111111-1111-1111-1111-111111111111';
