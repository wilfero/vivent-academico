import type { Paginated } from "@/features/admin/shared/admin-types";

export type Category = {
  id: string;
  companyId: string;
  parentCategoryId: string | null;
  name: string;
  description: string | null;
  active: boolean;
};

export type ProductUnit = {
  code: string;
  name: string;
  translationKey: string;
  allowsDecimal: boolean;
  active: boolean;
};

export type Product = {
  id: string;
  companyId: string;
  category: { id: string; name: string } | null;
  name: string;
  description: string | null;
  baseUnitCode: string;
  sku: string | null;
  minimumStock: number;
  stockable: boolean;
  hasExpirationDate: boolean;
  hasLotTracking: boolean;
  hasSerialTracking: boolean;
  active: boolean;
};

export type ProductPresentation = {
  id: string;
  companyId: string;
  productId: string;
  name: string;
  barcode: string | null;
  conversionFactor: number;
  purchasePriceSuggested: number;
  salePrice: number;
  defaultPurchase: boolean;
  defaultSale: boolean;
  active: boolean;
};

export type BarcodeLookup = {
  product: Product;
  presentation: ProductPresentation;
};

export type CatalogPage<T> = Paginated<T>;
