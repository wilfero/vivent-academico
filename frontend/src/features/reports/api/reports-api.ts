import { apiRequest } from "@/lib/api/http";
import type { InventoryFilters } from "@/features/inventory/types/inventory-types";
import type { PurchaseFilters } from "@/features/purchases/types/purchase-types";
import type { SaleFilters } from "@/features/sales/types/sale-types";
import type { AdjustmentFilters } from "@/features/adjustments/types/adjustment-types";
import type { TransferFilters } from "@/features/transfers/types/transfer-types";
import type { AdjustmentsReportSummary, CashClosure, CashClosureFilters, CashClosurePayload, CashClosurePreview, CashClosuresPage, CashClosuresReportSummary, ProductsReport, PurchasesReportSummary, SalesReportSummary, TransfersReportSummary } from "@/features/reports/types/report-types";

function dateBoundaryIso(value: string, boundary: "start" | "end") {
  const date = new Date(`${value}T00:00:00`);
  if (boundary === "end") date.setHours(23, 59, 59, 999);
  return date.toISOString();
}

export function getSalesReportSummary(filters: SaleFilters) {
  const params = new URLSearchParams();
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.customerId) params.set("customerId", filters.customerId);
  if (filters.status) params.set("status", filters.status);
  if (filters.paymentMethod) params.set("paymentMethod", filters.paymentMethod);
  if (filters.query) params.set("query", filters.query);
  if (filters.fromDate) params.set("fromDate", dateBoundaryIso(filters.fromDate, "start"));
  if (filters.toDate) params.set("toDate", dateBoundaryIso(filters.toDate, "end"));
  return apiRequest<SalesReportSummary>(`/api/v1/reports/sales-summary?${params.toString()}`, { authenticated: true });
}

export function getPurchasesReportSummary(filters: PurchaseFilters) {
  const params = new URLSearchParams();
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.supplierId) params.set("supplierId", filters.supplierId);
  if (filters.status) params.set("status", filters.status);
  if (filters.query) params.set("query", filters.query);
  if (filters.fromDate) params.set("fromDate", filters.fromDate);
  if (filters.toDate) params.set("toDate", filters.toDate);
  return apiRequest<PurchasesReportSummary>(`/api/v1/reports/purchases-summary?${params.toString()}`, { authenticated: true });
}

export function getProductsReport(filters: InventoryFilters) {
  const params = new URLSearchParams();
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.categoryId) params.set("categoryId", filters.categoryId);
  if (filters.productId) params.set("productId", filters.productId);
  if (filters.fromDate) params.set("fromDate", dateBoundaryIso(filters.fromDate, "start"));
  if (filters.toDate) params.set("toDate", dateBoundaryIso(filters.toDate, "end"));
  return apiRequest<ProductsReport>(`/api/v1/reports/products?${params.toString()}`, { authenticated: true });
}

export function getTransfersReportSummary(filters: TransferFilters) {
  const params = new URLSearchParams();
  if (filters.sourceBranchId) params.set("sourceBranchId", filters.sourceBranchId);
  if (filters.destinationBranchId) params.set("destinationBranchId", filters.destinationBranchId);
  if (filters.status) params.set("status", filters.status);
  if (filters.query) params.set("query", filters.query);
  if (filters.fromDate) params.set("fromDate", dateBoundaryIso(filters.fromDate, "start"));
  if (filters.toDate) params.set("toDate", dateBoundaryIso(filters.toDate, "end"));
  return apiRequest<TransfersReportSummary>(`/api/v1/reports/transfers-summary?${params.toString()}`, { authenticated: true });
}

export function getAdjustmentsReportSummary(filters: AdjustmentFilters) {
  const params = new URLSearchParams();
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.status) params.set("status", filters.status);
  if (filters.query) params.set("query", filters.query);
  if (filters.fromDate) params.set("fromDate", dateBoundaryIso(filters.fromDate, "start"));
  if (filters.toDate) params.set("toDate", dateBoundaryIso(filters.toDate, "end"));
  return apiRequest<AdjustmentsReportSummary>(`/api/v1/reports/adjustments-summary?${params.toString()}`, { authenticated: true });
}

export function getCashClosuresReportSummary(filters: CashClosureFilters) {
  const params = new URLSearchParams();
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.status) params.set("status", filters.status);
  if (filters.fromDate) params.set("fromDate", filters.fromDate);
  if (filters.toDate) params.set("toDate", filters.toDate);
  return apiRequest<CashClosuresReportSummary>(`/api/v1/reports/cash-closures-summary?${params.toString()}`, { authenticated: true });
}

export function listCashClosures(filters: CashClosureFilters) {
  const params = new URLSearchParams({ page: String(filters.page), pageSize: String(filters.pageSize) });
  if (filters.branchId) params.set("branchId", filters.branchId);
  if (filters.status) params.set("status", filters.status);
  if (filters.fromDate) params.set("fromDate", filters.fromDate);
  if (filters.toDate) params.set("toDate", filters.toDate);
  return apiRequest<CashClosuresPage>(`/api/v1/cash-closures?${params.toString()}`, { authenticated: true });
}

export function getCashClosure(closureId: string) {
  return apiRequest<CashClosure>(`/api/v1/cash-closures/${closureId}`, { authenticated: true });
}

export function getCashClosurePreview(branchId: string, closureDate: string) {
  const params = new URLSearchParams({ branchId, closureDate });
  return apiRequest<CashClosurePreview>(`/api/v1/cash-closures/preview?${params.toString()}`, { authenticated: true });
}

export function createCashClosure(payload: CashClosurePayload) {
  return apiRequest<CashClosure>("/api/v1/cash-closures", { method: "POST", body: payload, authenticated: true });
}

export function approveCashClosure(closureId: string) {
  return apiRequest<CashClosure>(`/api/v1/cash-closures/${closureId}/approve`, { method: "POST", authenticated: true });
}

export function reopenCashClosure(closureId: string) {
  return apiRequest<CashClosure>(`/api/v1/cash-closures/${closureId}/reopen`, { method: "POST", authenticated: true });
}

export function closeCashClosure(closureId: string, countedTotal?: number) {
  const body = countedTotal !== undefined ? { countedTotal } : {};
  return apiRequest<CashClosure>(`/api/v1/cash-closures/${closureId}/close`, { method: "POST", body, authenticated: true });
}
