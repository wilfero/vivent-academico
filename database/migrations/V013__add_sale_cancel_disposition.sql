-- Add cancel disposition column to sales for advanced sale cancellation
ALTER TABLE sales
    ADD COLUMN cancel_disposition varchar(20),
    ADD CONSTRAINT ck_sales_cancel_disposition CHECK (
        cancel_disposition IS NULL OR
        cancel_disposition IN ('RETURN_TO_STOCK', 'DAMAGED', 'LOSS', 'DO_NOT_RETURN')
    );