import type { PageResponse } from "@/lib/api/contracts";

export type InventoryPage<T> = PageResponse<T>;

export type InventoryBalance = {
  id: string;
  companyId: string;
  branchId: string;
  branchName: string;
  locationId: string;
  locationCode: string;
  productId: string;
  productName: string;
  categoryName: string | null;
  baseUnitCode: string;
  minimumStock: number | null;
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
  updatedAt: string;
};

export type InventoryLayerStatus = "OPEN" | "CONSUMED" | "VOIDED";

export type InventoryLayer = {
  id: string;
  branchId: string;
  branchName: string;
  locationId: string;
  locationCode: string;
  productId: string;
  productName: string;
  baseUnitCode: string;
  sourceType: string;
  sourceId: string;
  quantityReceived: number;
  quantityRemaining: number;
  unitCost: number;
  receivedAt: string;
  expirationDate: string | null;
  lotNumber: string | null;
  status: InventoryLayerStatus;
  timezoneSnapshot: string;
};

export type StockMovementType =
  | "PURCHASE_IN"
  | "SALE_OUT"
  | "SALE_CANCEL_IN"
  | "PURCHASE_VOID_OUT"
  | "TRANSFER_RESERVED"
  | "TRANSFER_OUT"
  | "TRANSFER_IN"
  | "ADJUSTMENT_IN"
  | "ADJUSTMENT_OUT"
  | "STOCK_COUNT_IN"
  | "STOCK_COUNT_OUT"
  | "LOSS_OUT"
  | "DAMAGE_OUT"
  | "EXPIRED_OUT";

export type StockMovement = {
  id: string;
  branchId: string;
  branchName: string;
  locationId: string;
  locationCode: string;
  productId: string;
  productName: string;
  baseUnitCode: string;
  movementType: StockMovementType;
  sourceType: string;
  sourceId: string;
  quantityIn: number;
  quantityOut: number;
  unitCost: number | null;
  totalCost: number | null;
  balanceAfter: number | null;
  notes: string | null;
  createdBy: string;
  createdByUsername: string | null;
  timezoneSnapshot: string;
  createdAt: string;
};

export type InventorySummary = {
  productsWithStock: number;
  productsWithoutStock: number;
  lowStockProducts: number | null;
  totalMovements: number;
};

export type StockManualPayload = {
  branchId: string;
  locationId: string | null;
  productId: string;
  quantity: number;
  unitCost?: number;
  expirationDate?: string | null;
  lotNumber?: string | null;
  notes: string;
};

export type StockOperationResponse = {
  operationId: string;
  balance: InventoryBalance;
  movement: StockMovement;
  affectedLayers: InventoryLayer[];
};

export type InventoryFilters = {
  branchId: string;
  locationId: string;
  productId: string;
  categoryId: string;
  query: string;
  movementType: StockMovementType | "";
  fromDate: string;
  toDate: string;
  page: number;
  pageSize: number;
};
