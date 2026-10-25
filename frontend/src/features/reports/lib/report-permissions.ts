import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";

export function canViewReports() {
	return canAny(["reports.view", "reports.view_branch", "reports.view_company"]);
}

export function canViewSalesReport() {
	return canAny(["reports.sales.view", "reports.view", "reports.view_branch", "reports.view_company"]);
}

export function canViewPurchasesReport() {
	return canAny(["reports.purchases.view", "reports.view", "reports.view_branch", "reports.view_company"]);
}

export function canViewInventoryReport() {
	return canAny(["reports.inventory.view", "reports.view", "reports.view_branch", "reports.view_company"]);
}

export function canViewMovementsReport() {
	return canAny(["reports.movements.view", "inventory.view_movements"]);
}

export function canViewKardexReport() {
	return canAny(["reports.kardex.view", "inventory.view_kardex"]);
}

export function canViewProductsReport() {
	return canAny(["reports.products.view", "reports.view", "reports.view_branch", "reports.view_company"]);
}

export function canViewCashClosures() {
	return canAny(["closures.view", "cash_closures.view", "cash_closures.create", "cash_closures.approve"]);
}

export function canCreateCashClosures() {
	return canAny(["closures.create", "cash_closures.create"]);
}

export function canApproveCashClosures() {
	return canAny(["closures.approve", "cash_closures.approve"]);
}
