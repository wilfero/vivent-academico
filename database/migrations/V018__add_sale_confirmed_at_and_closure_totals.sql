ALTER TABLE sales
    ADD COLUMN confirmed_at timestamptz;

UPDATE sales
SET confirmed_at = sale_date
WHERE status = 'CONFIRMED' AND confirmed_at IS NULL;

CREATE INDEX idx_sales_confirmed_at ON sales (company_id, branch_id, confirmed_at);

ALTER TABLE cash_closures
    ADD COLUMN totals_calculated_at timestamptz;
