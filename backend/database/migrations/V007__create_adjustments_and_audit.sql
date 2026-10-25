CREATE TABLE inventory_adjustments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    branch_id uuid NOT NULL REFERENCES branches (id),
    location_id uuid NOT NULL REFERENCES locations (id),
    adjustment_number varchar(50) NOT NULL,
    status varchar(25) NOT NULL DEFAULT 'DRAFT',
    adjustment_type varchar(50) NOT NULL,
    direction varchar(3) NOT NULL,
    notes text,
    created_by uuid NOT NULL REFERENCES users (id),
    approved_by uuid REFERENCES users (id),
    rejected_by uuid REFERENCES users (id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at timestamptz,
    rejected_at timestamptz,
    rejection_reason text,
    CONSTRAINT uq_inventory_adjustments_company_number UNIQUE (company_id, adjustment_number),
    CONSTRAINT ck_inventory_adjustments_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_inventory_adjustments_direction CHECK (direction IN ('IN', 'OUT'))
);

CREATE TABLE inventory_adjustment_lines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    adjustment_id uuid NOT NULL REFERENCES inventory_adjustments (id),
    product_id uuid NOT NULL REFERENCES products (id),
    presentation_id uuid REFERENCES product_presentations (id),
    quantity_presentation numeric(19,6),
    conversion_factor numeric(19,6),
    quantity_base numeric(19,6) NOT NULL,
    unit_cost numeric(19,4),
    notes text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inventory_adjustment_lines_quantity_base CHECK (quantity_base > 0),
    CONSTRAINT ck_inventory_adjustment_lines_quantity_presentation CHECK (quantity_presentation IS NULL OR quantity_presentation > 0),
    CONSTRAINT ck_inventory_adjustment_lines_conversion_factor CHECK (conversion_factor IS NULL OR conversion_factor > 0),
    CONSTRAINT ck_inventory_adjustment_lines_unit_cost CHECK (unit_cost IS NULL OR unit_cost >= 0)
);

CREATE TABLE audit_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies (id),
    branch_id uuid REFERENCES branches (id),
    user_id uuid NOT NULL REFERENCES users (id),
    action varchar(30) NOT NULL,
    entity_type varchar(80) NOT NULL,
    entity_id uuid,
    old_value jsonb,
    new_value jsonb,
    ip_address inet,
    user_agent text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_audit_logs_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'DEACTIVATE', 'LOGIN', 'LOGOUT', 'CONFIRM', 'VOID', 'CANCEL', 'APPROVE', 'REJECT', 'DISPATCH', 'RECEIVE', 'AUTHORIZE'))
);
