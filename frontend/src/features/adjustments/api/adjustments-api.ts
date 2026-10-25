import { apiRequest } from "@/lib/api/http";
import type { Adjustment, AdjustmentFilters, AdjustmentPayload, AdjustmentsPage } from "@/features/adjustments/types/adjustment-types";

export const adjustmentsQueryKeys = {
  all: ["adjustments"] as const,
  lists: () => [...adjustmentsQueryKeys.all, "list"] as const,
  list: (filters: AdjustmentFilters) => [...adjustmentsQueryKeys.lists(), filters] as const,
  approvals: (filters: AdjustmentFilters) => [...adjustmentsQueryKeys.all, "approvals", filters] as const,
  detail: (adjustmentId: string) => [...adjustmentsQueryKeys.all, "detail", adjustmentId] as const,
};

export function listAdjustments(filters: AdjustmentFilters) {
  const params = new URLSearchParams({ page: String(filters.page), pageSize: String(filters.pageSize) });
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.status) params.set("status", filters.status);
  if (filters.fromDate) params.set("fromDate", dateBoundaryIso(filters.fromDate, "start"));
  if (filters.toDate) params.set("toDate", dateBoundaryIso(filters.toDate, "end"));
  if (filters.query) params.set("query", filters.query);
  return apiRequest<AdjustmentsPage>(`/api/v1/inventory/adjustments?${params.toString()}`, { authenticated: true });
}

function dateBoundaryIso(value: string, boundary: "start" | "end") {
  const date = new Date(`${value}T00:00:00`);
  if (boundary === "end") date.setHours(23, 59, 59, 999);
  return date.toISOString();
}

export function getAdjustment(adjustmentId: string) {
  return apiRequest<Adjustment>(`/api/v1/inventory/adjustments/${adjustmentId}`, { authenticated: true });
}

export function createAdjustment(payload: AdjustmentPayload) {
  return apiRequest<Adjustment>("/api/v1/inventory/adjustments", { method: "POST", body: payload, authenticated: true });
}

export function updateAdjustment(adjustmentId: string, payload: AdjustmentPayload) {
  return apiRequest<Adjustment>(`/api/v1/inventory/adjustments/${adjustmentId}`, { method: "PUT", body: payload, authenticated: true });
}

export function approveAdjustment(adjustmentId: string) {
  return apiRequest<Adjustment>(`/api/v1/inventory/adjustments/${adjustmentId}/approve`, { method: "POST", authenticated: true });
}

export function rejectAdjustment(adjustmentId: string, reason: string) {
  return apiRequest<Adjustment>(`/api/v1/inventory/adjustments/${adjustmentId}/reject`, { method: "POST", body: { reason }, authenticated: true });
}

export function cancelAdjustment(adjustmentId: string, reason: string) {
  return apiRequest<Adjustment>(`/api/v1/inventory/adjustments/${adjustmentId}/cancel`, { method: "POST", body: { reason }, authenticated: true });
}

export function toSubmitAdjustmentPayload(adjustment: Adjustment): AdjustmentPayload {
  return {
    branchId: adjustment.branchId,
    locationId: adjustment.locationId,
    adjustmentNumber: adjustment.adjustmentNumber,
    adjustmentType: adjustment.adjustmentType,
    direction: adjustment.direction,
    status: "PENDING_APPROVAL",
    notes: adjustment.notes ?? "Submitted for approval",
    lines: adjustment.lines.map((line) => ({
      productId: line.presentationId ? null : line.productId,
      presentationId: line.presentationId,
      quantityPresentation: line.quantityPresentation,
      quantityBase: line.presentationId ? null : line.quantityBase,
      unitCost: line.unitCost,
      notes: line.notes,
    })),
  };
}
