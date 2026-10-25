ALTER TABLE inventory_layers
    ADD CONSTRAINT fk_inventory_layers_purchase_detail
    FOREIGN KEY (purchase_detail_id) REFERENCES purchase_details (id);

CREATE UNIQUE INDEX ux_roles_code_system
    ON roles (code)
    WHERE company_id IS NULL;

CREATE UNIQUE INDEX ux_roles_code_company
    ON roles (company_id, code)
    WHERE company_id IS NOT NULL;

CREATE UNIQUE INDEX ux_locations_default_branch
    ON locations (branch_id)
    WHERE is_default;

CREATE UNIQUE INDEX ux_products_company_sku_not_deleted
    ON products (company_id, sku)
    WHERE sku IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_product_presentations_company_barcode_not_deleted
    ON product_presentations (company_id, barcode)
    WHERE barcode IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_customers_generic_consumer_company
    ON customers (company_id)
    WHERE is_generic_consumer AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_user_branch_assignments_primary_active
    ON user_branch_assignments (user_id)
    WHERE is_primary AND status = 'ACTIVE';

CREATE UNIQUE INDEX ux_subscriptions_company_current
    ON subscriptions (company_id)
    WHERE status IN ('TRIAL', 'ACTIVE', 'SUSPENDED');

CREATE INDEX idx_branches_company_id ON branches (company_id);
CREATE INDEX idx_locations_company_branch ON locations (company_id, branch_id);
CREATE INDEX idx_permissions_module_code ON permissions (module_code);
CREATE INDEX idx_users_company_branch_status ON users (company_id, branch_id, status);
CREATE INDEX idx_users_role_id ON users (role_id);
CREATE INDEX idx_user_permission_overrides_user_id ON user_permission_overrides (user_id);
CREATE INDEX idx_user_branch_assignments_branch_id ON user_branch_assignments (branch_id);
CREATE INDEX idx_categories_company_parent ON categories (company_id, parent_category_id);
CREATE INDEX idx_products_company_category ON products (company_id, category_id);
CREATE INDEX idx_product_presentations_product_id ON product_presentations (product_id);
CREATE INDEX idx_suppliers_company_id ON suppliers (company_id);
CREATE INDEX idx_customers_company_id ON customers (company_id);
CREATE INDEX idx_inventory_balances_branch_location ON inventory_balances (branch_id, location_id);
CREATE INDEX idx_inventory_layers_product_received_at ON inventory_layers (product_id, received_at);
CREATE INDEX idx_stock_movements_product_created_at ON stock_movements (product_id, created_at);
CREATE INDEX idx_purchases_company_branch_date ON purchases (company_id, branch_id, purchase_date);
CREATE INDEX idx_purchase_details_purchase_id ON purchase_details (purchase_id);
CREATE INDEX idx_sales_company_branch_date ON sales (company_id, branch_id, sale_date);
CREATE INDEX idx_sale_details_sale_id ON sale_details (sale_id);
CREATE INDEX idx_cash_closures_branch_date ON cash_closures (branch_id, closure_date);
CREATE INDEX idx_transfers_company_status ON transfers (company_id, status);
CREATE INDEX idx_transfer_details_transfer_id ON transfer_details (transfer_id);
CREATE INDEX idx_inventory_adjustments_company_status ON inventory_adjustments (company_id, status);
CREATE INDEX idx_inventory_adjustment_lines_adjustment_id ON inventory_adjustment_lines (adjustment_id);
CREATE INDEX idx_audit_logs_company_created_at ON audit_logs (company_id, created_at);
CREATE INDEX idx_subscriptions_company_status ON subscriptions (company_id, status);

CREATE TRIGGER trg_companies_set_updated_at
    BEFORE UPDATE ON companies
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_branches_set_updated_at
    BEFORE UPDATE ON branches
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_locations_set_updated_at
    BEFORE UPDATE ON locations
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_permissions_set_updated_at
    BEFORE UPDATE ON permissions
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_roles_set_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_permission_overrides_set_updated_at
    BEFORE UPDATE ON user_permission_overrides
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_branch_assignments_set_updated_at
    BEFORE UPDATE ON user_branch_assignments
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_categories_set_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_products_set_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_product_presentations_set_updated_at
    BEFORE UPDATE ON product_presentations
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_suppliers_set_updated_at
    BEFORE UPDATE ON suppliers
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_customers_set_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_inventory_balances_set_updated_at
    BEFORE UPDATE ON inventory_balances
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_inventory_layers_set_updated_at
    BEFORE UPDATE ON inventory_layers
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_purchases_set_updated_at
    BEFORE UPDATE ON purchases
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_sales_set_updated_at
    BEFORE UPDATE ON sales
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_cash_closures_set_updated_at
    BEFORE UPDATE ON cash_closures
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_transfers_set_updated_at
    BEFORE UPDATE ON transfers
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_inventory_adjustments_set_updated_at
    BEFORE UPDATE ON inventory_adjustments
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_plans_set_updated_at
    BEFORE UPDATE ON plans
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_subscriptions_set_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
