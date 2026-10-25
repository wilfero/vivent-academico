import type { PageResponse } from "@/lib/api/contracts";

export type TransferStatus = "REQUESTED" | "APPROVED" | "REJECTED" | "IN_TRANSIT" | "PARTIALLY_RECEIVED" | "RECEIVED" | "CANCELLED";

export type TransferDetail = {
  id: string;
  productId: string;
  productName: string;
  presentationId: string;
  presentationName: string;
  quantityRequestedPresentation: number;
  conversionFactor: number;
  quantityRequestedBase: number;
  quantityApprovedBase: number;
  quantityReceivedBase: number;
};

export type Transfer = {
  id: string;
  sourceBranchId: string;
  sourceBranchName: string;
  destinationBranchId: string;
  destinationBranchName: string;
  transferNumber: string;
  status: TransferStatus;
  requestedBy: string;
  requestedByUsername: string | null;
  approvedBy: string | null;
  approvedByUsername: string | null;
  dispatchedBy: string | null;
  dispatchedByUsername: string | null;
  receivedBy: string | null;
  receivedByUsername: string | null;
  cancelledBy: string | null;
  cancelledByUsername: string | null;
  requestedAt: string;
  approvedAt: string | null;
  dispatchedAt: string | null;
  receivedAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  notes: string | null;
  timezoneSnapshot: string;
  details: TransferDetail[];
};

export type TransferFilters = {
  page: number;
  pageSize: number;
  sourceBranchId: string;
  destinationBranchId: string;
  status: TransferStatus | "";
  fromDate: string;
  toDate: string;
  query: string;
};

export type TransfersPage = PageResponse<Transfer>;

export type TransferPayload = {
  sourceBranchId: string;
  destinationBranchId: string;
  transferNumber: string | null;
  notes: string | null;
  details: Array<{
    presentationId: string;
    quantityRequestedPresentation: number;
  }>;
};

export type ReceiveTransferPayload = {
  details: Array<{
    detailId: string;
    quantityReceivedBase: number;
  }>;
};
