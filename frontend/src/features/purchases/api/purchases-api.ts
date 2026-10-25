import { apiRequest } from "@/lib/api/http";
import type { Purchase, PurchaseFilters, PurchasePayload, PurchasesPage, SuppliersPage } from "@/features/purchases/types/purchase-types";

export const purchasesQueryKeys = {
  all: ["purchases"] as const,
  lists: () => [...purchasesQueryKeys.all, "list"] as const,
  list: (filters: PurchaseFilters) => [...purchasesQueryKeys.lists(), filters] as const,
  detail: (purchaseId: string) => [...purchasesQueryKeys.all, "detail", purchaseId] as const,
  suppliers: ["purchases", "suppliers"] as const,
};

export function listPurchases(filters: PurchaseFilters) {
  const params = new URLSearchParams({ page: String(filters.page), pageSize: String(filters.pageSize) });
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.supplierId) params.set("supplierId", filters.supplierId);
  if (filters.status) params.set("status", filters.status);
  if (filters.fromDate) params.set("fromDate", filters.fromDate);
  if (filters.toDate) params.set("toDate", filters.toDate);
  if (filters.query) params.set("query", filters.query);
  return apiRequest<PurchasesPage>(`/api/v1/purchases?${params.toString()}`, { authenticated: true });
}

export function getPurchase(purchaseId: string) {
  return apiRequest<Purchase>(`/api/v1/purchases/${purchaseId}`, { authenticated: true });
}

export function createPurchase(payload: PurchasePayload) {
  return apiRequest<Purchase>("/api/v1/purchases", { method: "POST", body: payload, authenticated: true });
}

export function updatePurchase(purchaseId: string, payload: PurchasePayload) {
  return apiRequest<Purchase>(`/api/v1/purchases/${purchaseId}`, { method: "PUT", body: payload, authenticated: true });
}

export function confirmPurchase(purchaseId: string) {
  return apiRequest<Purchase>(`/api/v1/purchases/${purchaseId}/confirm`, { method: "POST", authenticated: true });
}

export function voidPurchase(purchaseId: string, reason: string) {
  return apiRequest<Purchase>(`/api/v1/purchases/${purchaseId}/void`, { method: "POST", body: { reason }, authenticated: true });
}

export function listSuppliers(query = "", active?: boolean) {
  const params = new URLSearchParams({ page: "1", pageSize: "100" });
  if (query) params.set("query", query);
  if (active !== undefined) params.set("active", String(active));
  return apiRequest<SuppliersPage>(`/api/v1/suppliers?${params.toString()}`, { authenticated: true });
}
