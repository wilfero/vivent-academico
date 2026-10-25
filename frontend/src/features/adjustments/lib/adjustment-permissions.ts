import { can, canAny } from "@/features/admin/catalog/shared/catalog-permissions";

export function canReadAdjustments() {
  return can("inventory.view");
}

export function canCreateAdjustments() {
  return can("inventory.adjust");
}

export function canUpdateAdjustments() {
  return can("inventory.adjust");
}

export function canApproveAdjustments() {
  return can("inventory.adjust_approve");
}

export function canRejectAdjustments() {
  return can("inventory.adjust_approve");
}

export function canCancelAdjustments() {
  return can("inventory.adjust");
}

export function canReadAdjustmentBranches() {
  return can("settings.view");
}

export function canLookupAdjustmentProducts() {
  return canAny(["products.view"]);
}

export function canLookupAdjustmentPresentations() {
  return canAny(["product_presentations.view"]);
}

export function canReadAdjustmentFifoLayers() {
  return canAny(["inventory.view_layers"]);
}
