import type { Paginated } from "@/features/admin/shared/admin-types";

export type SaleStatus = "DRAFT" | "CONFIRMED" | "CANCELLED";
export type SalePaymentMethod = "CASH" | "CARD" | "TRANSFER" | "CREDIT" | "OTHER";
export type SaleDetailSourceType = "NORMAL" | "COMBO_LINE";

export type SaleDetail = {
  id: string;
  productId: string;
  productName: string;
  presentationId: string;
  presentationName: string;
  barcode: string | null;
  quantityPresentation: number;
  conversionFactor: number;
  quantityBase: number;
  unitPrice: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  fifoCostTotal: number;
  sourceType: SaleDetailSourceType;
  comboLineId: string | null;
};

export type SaleComboLine = {
  id: string;
  comboOfferId: string;
  comboName: string;
  comboBarcode: string | null;
  quantity: number;
  comboUnitPrice: number;
  totalAmount: number;
};

export type Sale = {
  id: string;
  branchId: string;
  branchName: string;
  customerId: string | null;
  customerName: string | null;
  saleNumber: string;
  saleDate: string;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  paymentMethod: SalePaymentMethod;
  status: SaleStatus;
  notes: string | null;
  createdBy: string;
  createdByUsername: string | null;
  cancelledBy: string | null;
  cancelledByUsername: string | null;
  cancelReason: string | null;
  cancelDisposition: string | null;
  timezoneSnapshot: string;
  createdAt: string;
  updatedAt: string;
  cancelledAt: string | null;
  details: SaleDetail[];
  comboLines: SaleComboLine[];
};

export type SaleFilters = {
  page: number;
  pageSize: number;
  branchId: string;
  customerId: string;
  status: SaleStatus | "";
  paymentMethod: SalePaymentMethod | "";
  fromDate: string;
  toDate: string;
  query: string;
};

export type SaleDetailPayload = {
  presentationId: string | null;
  barcode: string | null;
  quantityPresentation: number;
  unitPrice: number | null;
  discountAmount: number;
  taxAmount: number;
};

export type SaleComboLinePayload = {
  comboOfferId: string;
  quantity: number;
};

export type SalePayload = {
  branchId: string;
  customerId: string | null;
  saleNumber: string | null;
  paymentMethod: SalePaymentMethod;
  status: "DRAFT";
  discountAmount: number;
  notes: string | null;
  details: SaleDetailPayload[];
  comboLines?: SaleComboLinePayload[];
};

export type SalesPage = Paginated<Sale>;
