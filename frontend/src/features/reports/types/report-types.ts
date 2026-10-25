import type { Paginated } from "@/features/admin/shared/admin-types";

export type SalesReportSummary = {
  totalSales: number;
  confirmedSales: number;
  cancelledSales: number;
  confirmedTotal: number;
  cancelledTotal: number;
  discountTotal: number;
};

export type PurchasesReportSummary = {
  totalPurchases: number;
  confirmedPurchases: number;
  voidedPurchases: number;
  confirmedTotal: number;
  voidedTotal: number;
  taxTotal: number;
};

export type ProductReportItem = {
  productId: string;
  productName: string;
  categoryName: string | null;
  branchId: string | null;
  branchName: string | null;
  baseUnitCode: string;
  quantity: number;
  amount: number;
  movementCount: number;
};

export type ProductsReport = {
  topSelling: ProductReportItem[];
  mostMoved: ProductReportItem[];
  noMovement: ProductReportItem[];
  lowStock: ProductReportItem[];
  outOfStock: ProductReportItem[];
};

export type TransfersReportSummary = {
  totalTransfers: number;
  requestedTransfers: number;
  approvedTransfers: number;
  rejectedTransfers: number;
  inTransitTransfers: number;
  partiallyReceivedTransfers: number;
  receivedTransfers: number;
  cancelledTransfers: number;
  pendingTransfers: number;
  requestedBaseTotal: number;
  approvedBaseTotal: number;
  receivedBaseTotal: number;
};

export type AdjustmentsReportSummary = {
  totalAdjustments: number;
  draftAdjustments: number;
  pendingApprovalAdjustments: number;
  approvedAdjustments: number;
  rejectedAdjustments: number;
  cancelledAdjustments: number;
  quantityInBase: number;
  quantityOutBase: number;
};

export type CashClosuresReportSummary = {
  totalClosures: number;
  openClosures: number;
  closedClosures: number;
  approvedClosures: number;
  reopenedClosures: number;
  expectedTotal: number;
  countedTotal: number;
  differenceTotal: number;
};

export type CashClosureStatus = "OPEN" | "CLOSED" | "APPROVED" | "REOPENED";
export type ClosureType = "DAILY";

export type CashClosure = {
  id: string;
  branchId: string;
  branchName: string;
  userId: string;
  username: string;
  closureType: ClosureType;
  closureDate: string;
  status: CashClosureStatus;
  confirmedSalesTotal: number;
  cancelledSalesTotal: number;
  discountTotal: number;
  expectedTotal: number;
  countedTotal: number;
  differenceAmount: number;
  paymentsByMethod: Record<string, number>;
  outdated: boolean;
  salesAfterClosureCount: number;
  notes: string | null;
  timezoneSnapshot: string;
  createdAt: string;
  closedAt: string | null;
  approvedAt: string | null;
};

export type CashClosurePreview = {
  branchId: string;
  branchName: string;
  closureDate: string;
  confirmedSalesTotal: number;
  cancelledSalesTotal: number;
  discountTotal: number;
  expectedTotal: number;
  paymentsByMethod: Record<string, number>;
};

export type CashClosureFilters = {
  page: number;
  pageSize: number;
  branchId: string;
  status: CashClosureStatus | "";
  fromDate: string;
  toDate: string;
};

export type CashClosurePayload = {
  branchId: string;
  closureDate: string;
  countedTotal: number;
  notes: string | null;
};

export type CashClosuresPage = Paginated<CashClosure>;
