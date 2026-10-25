-- Combo offers: a combo is a sellable group of products with a special price.
-- Combos are not stockable themselves; stock is consumed from their component products.
CREATE TABLE combo_offers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    name varchar(150) NOT NULL,
    code varchar(80) NOT NULL,
    barcode varchar(60),
    description text,
    combo_price numeric(19,4) NOT NULL DEFAULT 0,
    start_at timestamptz,
    end_at timestamptz,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uq_combo_offers_company_code UNIQUE (company_id, code),
    CONSTRAINT ck_combo_offers_combo_price CHECK (combo_price >= 0)
);

CREATE TABLE combo_offer_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    combo_offer_id uuid NOT NULL REFERENCES combo_offers (id),
    product_id uuid NOT NULL REFERENCES products (id),
    presentation_id uuid NOT NULL REFERENCES product_presentations (id),
    quantity_presentation numeric(19,6) NOT NULL,
    conversion_factor numeric(19,6) NOT NULL,
    quantity_base numeric(19,6) NOT NULL,
    regular_unit_price_snapshot numeric(19,4) NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_combo_offer_items_quantity_presentation CHECK (quantity_presentation > 0),
    CONSTRAINT ck_combo_offer_items_conversion_factor CHECK (conversion_factor > 0),
    CONSTRAINT ck_combo_offer_items_quantity_base CHECK (quantity_base > 0),
    CONSTRAINT ck_combo_offer_items_regular_price CHECK (regular_unit_price_snapshot >= 0)
);

-- Sale combo lines: tracks combos sold in a sale for traceability.
-- Each combo line expands into individual sale_details for each component product.
CREATE TABLE sale_combo_lines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id uuid NOT NULL REFERENCES sales (id),
    combo_offer_id uuid NOT NULL REFERENCES combo_offers (id),
    combo_name varchar(150) NOT NULL,
    combo_barcode varchar(60),
    quantity numeric(19,6) NOT NULL,
    combo_unit_price numeric(19,4) NOT NULL,
    total_amount numeric(19,4) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sale_combo_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_sale_combo_lines_combo_unit_price CHECK (combo_unit_price >= 0),
    CONSTRAINT ck_sale_combo_lines_total_amount CHECK (total_amount >= 0)
);

CREATE INDEX idx_combo_offers_company_id ON combo_offers (company_id);
CREATE INDEX idx_combo_offer_items_combo_offer_id ON combo_offer_items (combo_offer_id);
CREATE INDEX idx_sale_combo_lines_sale_id ON sale_combo_lines (sale_id);

CREATE UNIQUE INDEX ux_combo_offers_company_barcode_not_deleted
    ON combo_offers (company_id, barcode)
    WHERE barcode IS NOT NULL AND deleted_at IS NULL;

CREATE TRIGGER trg_combo_offers_set_updated_at
    BEFORE UPDATE ON combo_offers
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();