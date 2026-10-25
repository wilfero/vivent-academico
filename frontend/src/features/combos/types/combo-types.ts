import type { Paginated } from "@/features/admin/shared/admin-types";

export type ComboOfferItem = {
  id: string;
  productId: string;
  productName: string;
  presentationId: string;
  presentationName: string;
  quantityPresentation: number;
  conversionFactor: number;
  quantityBase: number;
  regularUnitPriceSnapshot: number;
};

export type ComboOffer = {
  id: string;
  name: string;
  code: string;
  barcode: string | null;
  description: string | null;
  comboPrice: number;
  startAt: string | null;
  endAt: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  items: ComboOfferItem[];
};

export type ComboOfferFilters = {
  page: number;
  pageSize: number;
  active: boolean | "";
  query: string;
};

export type ComboOfferItemPayload = {
  productId: string;
  presentationId: string;
  quantityPresentation: number;
};

export type ComboOfferPayload = {
  name: string;
  code: string;
  barcode: string | null;
  description: string | null;
  comboPrice: number;
  startAt: string | null;
  endAt: string | null;
  isActive: boolean;
  items: ComboOfferItemPayload[];
};

export type ComboOffersPage = Paginated<ComboOffer>;

export type SaleComboLine = {
  id: string;
  comboOfferId: string;
  comboName: string;
  comboBarcode: string | null;
  quantity: number;
  comboUnitPrice: number;
  totalAmount: number;
};