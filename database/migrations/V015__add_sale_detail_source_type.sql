-- Distinguish commercial sale lines from internal combo component expansion.
ALTER TABLE sale_details
    ADD COLUMN source_type varchar(20) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN combo_line_id uuid;

ALTER TABLE sale_details
    ADD CONSTRAINT ck_sale_details_source_type
        CHECK (source_type IN ('NORMAL', 'COMBO_LINE')),
    ADD CONSTRAINT fk_sale_details_combo_line
        FOREIGN KEY (combo_line_id) REFERENCES sale_combo_lines (id);

CREATE INDEX idx_sale_details_combo_line_id
    ON sale_details (combo_line_id)
    WHERE combo_line_id IS NOT NULL;
