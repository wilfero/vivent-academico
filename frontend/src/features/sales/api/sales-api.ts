import { apiRequest } from "@/lib/api/http";
import type { Sale, SaleFilters, SalePayload, SalesPage } from "@/features/sales/types/sale-types";

export const salesQueryKeys = {
  all: ["sales"] as const,
  lists: () => [...salesQueryKeys.all, "list"] as const,
  list: (filters: SaleFilters) => [...salesQueryKeys.lists(), filters] as const,
  detail: (saleId: string) => [...salesQueryKeys.all, "detail", saleId] as const,
};

export function listSales(filters: SaleFilters) {
  const params = new URLSearchParams({ page: String(filters.page), pageSize: String(filters.pageSize) });
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.customerId) params.set("customerId", filters.customerId);
  if (filters.status) params.set("status", filters.status);
  if (filters.paymentMethod) params.set("paymentMethod", filters.paymentMethod);
  if (filters.fromDate) params.set("fromDate", dateBoundaryIso(filters.fromDate, "start"));
  if (filters.toDate) params.set("toDate", dateBoundaryIso(filters.toDate, "end"));
  if (filters.query) params.set("query", filters.query);
  return apiRequest<SalesPage>(`/api/v1/sales?${params.toString()}`, { authenticated: true });
}

function dateBoundaryIso(value: string, boundary: "start" | "end") {
  const date = new Date(`${value}T00:00:00`);
  if (boundary === "end") date.setHours(23, 59, 59, 999);
  return date.toISOString();
}

export function getSale(saleId: string) {
  return apiRequest<Sale>(`/api/v1/sales/${saleId}`, { authenticated: true });
}

export function createSale(payload: SalePayload) {
  return apiRequest<Sale>("/api/v1/sales", { method: "POST", body: payload, authenticated: true });
}

export function updateSale(saleId: string, payload: SalePayload) {
  return apiRequest<Sale>(`/api/v1/sales/${saleId}`, { method: "PUT", body: payload, authenticated: true });
}

export function confirmSale(saleId: string) {
  return apiRequest<Sale>(`/api/v1/sales/${saleId}/confirm`, { method: "POST", authenticated: true });
}

export function cancelSale(saleId: string, reason: string, cancelDisposition?: string) {
  return apiRequest<Sale>(`/api/v1/sales/${saleId}/cancel`, { method: "POST", body: { reason, cancelDisposition }, authenticated: true });
}

export function deleteDraftSales(branchId = "") {
  const params = new URLSearchParams();
  if (branchId) params.set("branchId", branchId);
  const query = params.toString();
  return apiRequest<number>(`/api/v1/sales/drafts${query ? `?${query}` : ""}`, { method: "DELETE", authenticated: true });
}

export function deleteDraftSale(saleId: string) {
  return apiRequest<void>(`/api/v1/sales/${saleId}`, { method: "DELETE", authenticated: true });
}
