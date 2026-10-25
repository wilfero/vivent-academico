import { apiRequest } from "@/lib/api/http";
import type { Branch, Paginated } from "@/features/admin/shared/admin-types";
import type { ReceiveTransferPayload, Transfer, TransferFilters, TransferPayload, TransfersPage } from "@/features/transfers/types/transfer-types";

export const transfersQueryKeys = {
  all: ["transfers"] as const,
  lists: () => [...transfersQueryKeys.all, "list"] as const,
  list: (filters: TransferFilters) => [...transfersQueryKeys.lists(), filters] as const,
  detail: (transferId: string) => [...transfersQueryKeys.all, "detail", transferId] as const,
  sourceBranches: () => [...transfersQueryKeys.all, "source-branches"] as const,
};

export function listTransfers(filters: TransferFilters) {
  const params = new URLSearchParams({ page: String(filters.page), pageSize: String(filters.pageSize) });
  if (filters.sourceBranchId) params.set("sourceBranchId", filters.sourceBranchId);
  if (filters.destinationBranchId) params.set("destinationBranchId", filters.destinationBranchId);
  if (filters.status) params.set("status", filters.status);
  if (filters.fromDate) params.set("fromDate", dateBoundaryIso(filters.fromDate, "start"));
  if (filters.toDate) params.set("toDate", dateBoundaryIso(filters.toDate, "end"));
  if (filters.query) params.set("query", filters.query);
  return apiRequest<TransfersPage>(`/api/v1/transfers?${params.toString()}`, { authenticated: true });
}

function dateBoundaryIso(value: string, boundary: "start" | "end") {
  const date = new Date(`${value}T00:00:00`);
  if (boundary === "end") date.setHours(23, 59, 59, 999);
  return date.toISOString();
}

export function getTransfer(transferId: string) {
  return apiRequest<Transfer>(`/api/v1/transfers/${transferId}`, { authenticated: true });
}

export function listTransferSourceBranches() {
  return apiRequest<Paginated<Branch>>("/api/v1/transfers/source-branches?page=1&pageSize=100", { authenticated: true });
}

export function createTransfer(payload: TransferPayload) {
  return apiRequest<Transfer>("/api/v1/transfers", { method: "POST", body: payload, authenticated: true });
}

export function approveTransfer(transferId: string) {
  return apiRequest<Transfer>(`/api/v1/transfers/${transferId}/approve`, { method: "POST", authenticated: true });
}

export function rejectTransfer(transferId: string) {
  return apiRequest<Transfer>(`/api/v1/transfers/${transferId}/reject`, { method: "POST", authenticated: true });
}

export function dispatchTransfer(transferId: string) {
  return apiRequest<Transfer>(`/api/v1/transfers/${transferId}/dispatch`, { method: "POST", authenticated: true });
}

export function receiveTransfer(transferId: string, payload: ReceiveTransferPayload) {
  return apiRequest<Transfer>(`/api/v1/transfers/${transferId}/receive`, { method: "POST", body: payload, authenticated: true });
}

export function cancelTransfer(transferId: string, reason: string) {
  return apiRequest<Transfer>(`/api/v1/transfers/${transferId}/cancel`, { method: "POST", body: { reason }, authenticated: true });
}
