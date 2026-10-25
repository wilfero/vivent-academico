import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";

export function canReadSales() {
  return canAny(["sales.view"]);
}

export function canCreateSales() {
  return canAny(["sales.create"]);
}

export function canConfirmSales() {
  return canAny(["sales.create"]);
}

export function canCancelSales() {
  return canAny(["sales.cancel"]);
}

export function canApplySaleDiscounts() {
  return canAny(["sales.apply_discount"]);
}

export function canRemoveSaleItems() {
  return canAny(["sales.remove_item"]);
}

export function canLookupSaleProducts() {
  return canAny(["products.view"]);
}

export function canLookupSalePresentations() {
  return canAny(["product_presentations.view"]);
}
