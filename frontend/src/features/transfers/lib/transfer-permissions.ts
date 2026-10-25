import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";

export function canReadTransfers() {
  return canAny(["transfers.view"]);
}

export function canRequestTransfers() {
  return canAny(["transfers.request"]);
}

export function canApproveTransfers() {
  return canAny(["transfers.approve"]);
}

export function canRejectTransfers() {
  return canAny(["transfers.approve"]);
}

export function canDispatchTransfers() {
  return canAny(["transfers.dispatch"]);
}

export function canReceiveTransfers() {
  return canAny(["transfers.receive"]);
}

export function canCancelTransfers() {
  return canAny(["transfers.cancel"]);
}

export function canLookupTransferProducts() {
  return canAny(["products.view"]);
}

export function canLookupTransferPresentations() {
  return canAny(["product_presentations.view"]);
}
