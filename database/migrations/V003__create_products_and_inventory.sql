CREATE TABLE categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    parent_category_id uuid REFERENCES categories (id),
    name varchar(150) NOT NULL,
    description text,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uq_categories_company_name UNIQUE (company_id, name)
);

CREATE TABLE products (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    category_id uuid REFERENCES categories (id),
    name varchar(150) NOT NULL,
    description text,
    base_unit_code varchar(30) NOT NULL,
    sku varchar(80),
    is_stockable boolean NOT NULL DEFAULT true,
    has_expiration_date boolean NOT NULL DEFAULT false,
    has_lot_tracking boolean NOT NULL DEFAULT false,
    has_serial_tracking boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz
);

CREATE TABLE product_presentations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    product_id uuid NOT NULL REFERENCES products (id),
    name varchar(150) NOT NULL,
    barcode varchar(60),
    conversion_factor numeric(19,6) NOT NULL,
    purchase_price_suggested numeric(19,4) NOT NULL DEFAULT 0,
    sale_price numeric(19,4) NOT NULL DEFAULT 0,
    is_default_purchase boolean NOT NULL DEFAULT false,
    is_default_sale boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uq_product_presentations_product_name UNIQUE (product_id, name),
    CONSTRAINT ck_product_presentations_conversion_factor CHECK (conversion_factor > 0),
    CONSTRAINT ck_product_presentations_purchase_price_suggested CHECK (purchase_price_suggested >= 0),
    CONSTRAINT ck_product_presentations_sale_price CHECK (sale_price >= 0)
);

CREATE TABLE suppliers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    name varchar(150) NOT NULL,
    tax_id varchar(50),
    email varchar(150),
    phone varchar(50),
    address varchar(255),
    contact_name varchar(150),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz
);

CREATE TABLE customers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    name varchar(150) NOT NULL,
    tax_id varchar(50),
    email varchar(150),
    phone varchar(50),
    address varchar(255),
    is_generic_consumer boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz
);

CREATE TABLE inventory_balances (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    branch_id uuid NOT NULL REFERENCES branches (id),
    location_id uuid NOT NULL REFERENCES locations (id),
    product_id uuid NOT NULL REFERENCES products (id),
    quantity_on_hand numeric(19,6) NOT NULL DEFAULT 0,
    quantity_reserved numeric(19,6) NOT NULL DEFAULT 0,
    quantity_available numeric(19,6) GENERATED ALWAYS AS (quantity_on_hand - quantity_reserved) STORED,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inventory_balances_scope UNIQUE (company_id, branch_id, location_id, product_id),
    CONSTRAINT ck_inventory_balances_quantity_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT ck_inventory_balances_quantity_reserved CHECK (quantity_reserved >= 0)
);

CREATE TABLE inventory_layers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    branch_id uuid NOT NULL REFERENCES branches (id),
    location_id uuid NOT NULL REFERENCES locations (id),
    product_id uuid NOT NULL REFERENCES products (id),
    purchase_detail_id uuid,
    source_type varchar(40) NOT NULL,
    source_id uuid NOT NULL,
    quantity_received numeric(19,6) NOT NULL,
    quantity_remaining numeric(19,6) NOT NULL,
    unit_cost numeric(19,4) NOT NULL,
    received_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiration_date date,
    lot_number varchar(80),
    status varchar(20) NOT NULL DEFAULT 'OPEN',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inventory_layers_quantity_received CHECK (quantity_received > 0),
    CONSTRAINT ck_inventory_layers_quantity_remaining CHECK (quantity_remaining >= 0 AND quantity_remaining <= quantity_received),
    CONSTRAINT ck_inventory_layers_unit_cost CHECK (unit_cost >= 0),
    CONSTRAINT ck_inventory_layers_status CHECK (status IN ('OPEN', 'CONSUMED', 'VOIDED'))
);

CREATE TABLE stock_movements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    branch_id uuid NOT NULL REFERENCES branches (id),
    location_id uuid NOT NULL REFERENCES locations (id),
    product_id uuid NOT NULL REFERENCES products (id),
    movement_type varchar(40) NOT NULL,
    source_type varchar(40) NOT NULL,
    source_id uuid NOT NULL,
    quantity_in numeric(19,6) NOT NULL DEFAULT 0,
    quantity_out numeric(19,6) NOT NULL DEFAULT 0,
    unit_cost numeric(19,4),
    total_cost numeric(19,4),
    balance_after numeric(19,6),
    notes text,
    created_by uuid REFERENCES users (id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_stock_movements_direction CHECK (
        (quantity_in > 0 AND quantity_out = 0) OR
        (quantity_out > 0 AND quantity_in = 0)
    )
);
