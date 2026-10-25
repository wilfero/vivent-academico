import type { Paginated } from "@/features/admin/shared/admin-types";

export type AdjustmentStatus = "DRAFT" | "PENDING_APPROVAL" | "APPROVED" | "REJECTED" | "CANCELLED";
export type AdjustmentType = "PHYSICAL_COUNT" | "DAMAGE" | "LOSS" | "THEFT" | "EXPIRED" | "INTERNAL_USE" | "REGISTER_ERROR" | "SUPPLIER_RETURN" | "CUSTOM";
export type AdjustmentDirection = "IN" | "OUT";

export type AdjustmentLine = {
  id: string;
  productId: string;
  productName: string;
  presentationId: string | null;
  presentationName: string | null;
  quantityPresentation: number | null;
  conversionFactor: number | null;
  quantityBase: number;
  unitCost: number | null;
  notes: string | null;
};

export type Adjustment = {
  id: string;
  branchId: string;
  branchName: string;
  locationId: string;
  locationName: string;
  adjustmentNumber: string;
  status: AdjustmentStatus;
  adjustmentType: AdjustmentType;
  direction: AdjustmentDirection;
  notes: string | null;
  createdBy: string;
  createdByUsername: string | null;
  approvedBy: string | null;
  approvedByUsername: string | null;
  rejectedBy: string | null;
  rejectedByUsername: string | null;
  createdAt: string;
  updatedAt: string;
  approvedAt: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  timezoneSnapshot: string;
  lines: AdjustmentLine[];
};

export type AdjustmentFilters = {
  page: number;
  pageSize: number;
  branchId: string;
  status: AdjustmentStatus | "";
  fromDate: string;
  toDate: string;
  query: string;
};

export type AdjustmentLinePayload = {
  productId: string | null;
  presentationId: string | null;
  quantityPresentation: number | null;
  quantityBase: number | null;
  unitCost: number | null;
  notes: string | null;
};

export type AdjustmentPayload = {
  branchId: string | null;
  locationId: string | null;
  adjustmentNumber: string | null;
  adjustmentType: AdjustmentType;
  direction: AdjustmentDirection;
  status: "DRAFT" | "PENDING_APPROVAL";
  notes: string;
  lines: AdjustmentLinePayload[];
};

export type AdjustmentsPage = Paginated<Adjustment>;
