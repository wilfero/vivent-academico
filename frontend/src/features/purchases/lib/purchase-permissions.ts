import { can } from "@/features/admin/catalog/shared/catalog-permissions";

export function canReadPurchases() {
  return can("purchases.view");
}

export function canCreatePurchases() {
  return can("purchases.create");
}

export function canReadPurchaseBranches() {
  return can("settings.view");
}

export function canReadPurchaseProducts() {
  return can("products.view");
}

export function canReadPurchasePresentations() {
  return can("product_presentations.view");
}

export function canReadPurchaseSuppliers() {
	return can("suppliers.view");
}
