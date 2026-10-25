import type { Branch, Paginated } from "@/features/admin/shared/admin-types";
import type { Product, ProductPresentation } from "@/features/admin/catalog/shared/catalog-types";

export type PurchaseStatus = "DRAFT" | "PENDING_APPROVAL" | "CONFIRMED" | "VOIDED";
export type PaymentMethod = "CASH" | "CARD" | "TRANSFER" | "CHECK" | "OTHER";
export type PaymentType = "CASH" | "CREDIT";

export type Supplier = {
  id: string;
  companyId: string;
  name: string;
  taxId: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  contactName: string | null;
  active: boolean;
};

export type PurchaseDetail = {
  id: string;
  productId: string;
  productName: string;
  presentationId: string;
  presentationName: string;
  quantityPresentation: number;
  conversionFactor: number;
  quantityBase: number;
  unitCostPresentation: number;
  unitCostBase: number;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  expirationDate: string | null;
  lotNumber: string | null;
};

export type Purchase = {
  id: string;
  branchId: string;
  branchName: string;
  supplierId: string;
  supplierName: string;
  purchaseNumber: string;
  supplierInvoiceNumber: string | null;
  purchaseDate: string;
  paymentMethod: PaymentMethod;
  paymentType: PaymentType;
  subtotal: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  status: PurchaseStatus;
  notes: string | null;
  createdBy: string;
  createdByUsername: string | null;
  confirmedBy: string | null;
  confirmedByUsername: string | null;
  voidedBy: string | null;
  voidedByUsername: string | null;
  voidReason: string | null;
  timezoneSnapshot: string;
  createdAt: string;
  updatedAt: string;
  confirmedAt: string | null;
  voidedAt: string | null;
  details: PurchaseDetail[];
};

export type PurchaseFilters = {
  page: number;
  pageSize: number;
  branchId: string;
  supplierId: string;
  status: PurchaseStatus | "";
  fromDate: string;
  toDate: string;
  query: string;
};

export type PurchaseDetailPayload = {
  presentationId: string;
  quantityPresentation: number;
  unitCostPresentation: number;
  taxAmount: number;
  expirationDate: string | null;
  lotNumber: string | null;
};

export type PurchasePayload = {
  branchId: string;
  supplierId: string;
  purchaseNumber: string | null;
  supplierInvoiceNumber: string | null;
  purchaseDate: string;
  paymentMethod: PaymentMethod;
  paymentType: PaymentType;
  status: "DRAFT" | "PENDING_APPROVAL";
  discountAmount: number;
  notes: string | null;
  details: PurchaseDetailPayload[];
};

export type PurchasesPage = Paginated<Purchase>;
export type SuppliersPage = Paginated<Supplier>;
export type PurchaseLookupData = {
  branches: Branch[];
  suppliers: Supplier[];
  products: Product[];
  presentations: ProductPresentation[];
};
