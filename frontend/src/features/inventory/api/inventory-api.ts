import { apiRequest } from "@/lib/api/http";
import type { InventoryBalance, InventoryFilters, InventoryLayer, InventoryLayerStatus, InventoryPage, InventorySummary, StockManualPayload, StockMovement, StockOperationResponse } from "@/features/inventory/types/inventory-types";

function inventoryParams(filters: InventoryFilters) {
  const params = new URLSearchParams({ page: String(filters.page), pageSize: String(filters.pageSize) });
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.productId) params.set("productId", filters.productId);
  if (filters.categoryId) params.set("categoryId", filters.categoryId);
  if (filters.query) params.set("query", filters.query);
  if (filters.movementType) params.set("movementType", filters.movementType);
  if (filters.fromDate) params.set("fromDate", toDateBoundaryIso(filters.fromDate, "start"));
  if (filters.toDate) params.set("toDate", toDateBoundaryIso(filters.toDate, "end"));
  return params;
}

function toDateBoundaryIso(value: string, boundary: "start" | "end") {
  const date = new Date(`${value}T00:00:00`);

  if (boundary === "end") {
    date.setHours(23, 59, 59, 999);
  }

  return date.toISOString();
}

export function getInventorySummary() {
  return apiRequest<InventorySummary>("/api/v1/inventory/summary", { authenticated: true });
}

export function listInventoryBalances(filters: InventoryFilters) {
  return apiRequest<InventoryPage<InventoryBalance>>(`/api/v1/inventory/balances?${inventoryParams(filters).toString()}`, { authenticated: true });
}

export function listInventoryLayers(filters: InventoryFilters & { status?: InventoryLayerStatus | "" }) {
  const params = inventoryParams(filters);
  if (filters.status) params.set("status", filters.status);
  return apiRequest<InventoryPage<InventoryLayer>>(`/api/v1/inventory/layers?${params.toString()}`, { authenticated: true });
}

export function listKardex(filters: InventoryFilters) {
  return apiRequest<InventoryPage<StockMovement>>(`/api/v1/inventory/kardex?${inventoryParams(filters).toString()}`, { authenticated: true });
}

export function listInventoryMovements(filters: InventoryFilters) {
  return apiRequest<InventoryPage<StockMovement>>(`/api/v1/inventory/movements?${inventoryParams(filters).toString()}`, { authenticated: true });
}

export function stockIn(payload: StockManualPayload) {
  return apiRequest<StockOperationResponse>("/api/v1/inventory/stock-in", { method: "POST", body: payload, authenticated: true });
}

export function stockOut(payload: StockManualPayload) {
  return apiRequest<StockOperationResponse>("/api/v1/inventory/stock-out", { method: "POST", body: payload, authenticated: true });
}
