ALTER TABLE inventory_layers ADD COLUMN timezone_snapshot varchar(80);
ALTER TABLE stock_movements ADD COLUMN timezone_snapshot varchar(80);
ALTER TABLE purchases ADD COLUMN timezone_snapshot varchar(80);
ALTER TABLE sales ADD COLUMN timezone_snapshot varchar(80);
ALTER TABLE cash_closures ADD COLUMN timezone_snapshot varchar(80);
ALTER TABLE transfers ADD COLUMN timezone_snapshot varchar(80);
ALTER TABLE inventory_adjustments ADD COLUMN timezone_snapshot varchar(80);
ALTER TABLE audit_logs ADD COLUMN timezone_snapshot varchar(80);

UPDATE inventory_layers target
SET timezone_snapshot = COALESCE(company.timezone, 'UTC')
FROM companies company
WHERE company.id = target.company_id;

UPDATE stock_movements target
SET timezone_snapshot = COALESCE(company.timezone, 'UTC')
FROM companies company
WHERE company.id = target.company_id;

UPDATE purchases target
SET timezone_snapshot = COALESCE(company.timezone, 'UTC')
FROM companies company
WHERE company.id = target.company_id;

UPDATE sales target
SET timezone_snapshot = COALESCE(company.timezone, 'UTC')
FROM companies company
WHERE company.id = target.company_id;

UPDATE cash_closures target
SET timezone_snapshot = COALESCE(company.timezone, 'UTC')
FROM companies company
WHERE company.id = target.company_id;

UPDATE transfers target
SET timezone_snapshot = COALESCE(company.timezone, 'UTC')
FROM companies company
WHERE company.id = target.company_id;

UPDATE inventory_adjustments target
SET timezone_snapshot = COALESCE(company.timezone, 'UTC')
FROM companies company
WHERE company.id = target.company_id;

UPDATE audit_logs target
SET timezone_snapshot = COALESCE(company.timezone, 'UTC')
FROM companies company
WHERE company.id = target.company_id;

UPDATE inventory_layers SET timezone_snapshot = 'UTC' WHERE timezone_snapshot IS NULL;
UPDATE stock_movements SET timezone_snapshot = 'UTC' WHERE timezone_snapshot IS NULL;
UPDATE purchases SET timezone_snapshot = 'UTC' WHERE timezone_snapshot IS NULL;
UPDATE sales SET timezone_snapshot = 'UTC' WHERE timezone_snapshot IS NULL;
UPDATE cash_closures SET timezone_snapshot = 'UTC' WHERE timezone_snapshot IS NULL;
UPDATE transfers SET timezone_snapshot = 'UTC' WHERE timezone_snapshot IS NULL;
UPDATE inventory_adjustments SET timezone_snapshot = 'UTC' WHERE timezone_snapshot IS NULL;
UPDATE audit_logs SET timezone_snapshot = 'UTC' WHERE timezone_snapshot IS NULL;

ALTER TABLE inventory_layers ALTER COLUMN timezone_snapshot SET NOT NULL;
ALTER TABLE stock_movements ALTER COLUMN timezone_snapshot SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN timezone_snapshot SET NOT NULL;
ALTER TABLE sales ALTER COLUMN timezone_snapshot SET NOT NULL;
ALTER TABLE cash_closures ALTER COLUMN timezone_snapshot SET NOT NULL;
ALTER TABLE transfers ALTER COLUMN timezone_snapshot SET NOT NULL;
ALTER TABLE inventory_adjustments ALTER COLUMN timezone_snapshot SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN timezone_snapshot SET NOT NULL;

CREATE OR REPLACE FUNCTION set_company_timezone_snapshot()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    resolved_timezone varchar(80);
BEGIN
    IF NEW.timezone_snapshot IS NULL THEN
        SELECT timezone INTO resolved_timezone
        FROM companies
        WHERE id = NEW.company_id;

        NEW.timezone_snapshot = COALESCE(resolved_timezone, 'UTC');
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_inventory_layers_set_timezone_snapshot
    BEFORE INSERT ON inventory_layers
    FOR EACH ROW
    EXECUTE FUNCTION set_company_timezone_snapshot();

CREATE TRIGGER trg_stock_movements_set_timezone_snapshot
    BEFORE INSERT ON stock_movements
    FOR EACH ROW
    EXECUTE FUNCTION set_company_timezone_snapshot();

CREATE TRIGGER trg_purchases_set_timezone_snapshot
    BEFORE INSERT ON purchases
    FOR EACH ROW
    EXECUTE FUNCTION set_company_timezone_snapshot();

CREATE TRIGGER trg_sales_set_timezone_snapshot
    BEFORE INSERT ON sales
    FOR EACH ROW
    EXECUTE FUNCTION set_company_timezone_snapshot();

CREATE TRIGGER trg_cash_closures_set_timezone_snapshot
    BEFORE INSERT ON cash_closures
    FOR EACH ROW
    EXECUTE FUNCTION set_company_timezone_snapshot();

CREATE TRIGGER trg_transfers_set_timezone_snapshot
    BEFORE INSERT ON transfers
    FOR EACH ROW
    EXECUTE FUNCTION set_company_timezone_snapshot();

CREATE TRIGGER trg_inventory_adjustments_set_timezone_snapshot
    BEFORE INSERT ON inventory_adjustments
    FOR EACH ROW
    EXECUTE FUNCTION set_company_timezone_snapshot();

CREATE TRIGGER trg_audit_logs_set_timezone_snapshot
    BEFORE INSERT ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION set_company_timezone_snapshot();
