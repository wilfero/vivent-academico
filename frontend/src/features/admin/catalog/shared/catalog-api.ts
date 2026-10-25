import { apiRequest } from "@/lib/api/http";
import type { BarcodeLookup, CatalogPage, Category, Product, ProductPresentation, ProductUnit } from "@/features/admin/catalog/shared/catalog-types";

export type CategoryPayload = Pick<Category, "parentCategoryId" | "name" | "description" | "active">;
export type ProductPayload = Pick<Product, "name" | "description" | "baseUnitCode" | "sku" | "minimumStock" | "stockable" | "hasExpirationDate" | "hasLotTracking" | "hasSerialTracking" | "active"> & { categoryId: string | null };
export type PresentationPayload = Pick<ProductPresentation, "name" | "barcode" | "conversionFactor" | "purchasePriceSuggested" | "salePrice" | "defaultPurchase" | "defaultSale" | "active">;
export type UnitPayload = Pick<ProductUnit, "code" | "name" | "translationKey" | "allowsDecimal" | "active">;

export function listCategories(query = "", active?: boolean) {
  const params = new URLSearchParams({ page: "1", pageSize: "100" });
  if (query) params.set("query", query);
  if (active !== undefined) params.set("active", String(active));
  return apiRequest<CatalogPage<Category>>(`/api/v1/categories?${params.toString()}`, { authenticated: true });
}

export function createCategory(payload: CategoryPayload) {
  return apiRequest<Category>("/api/v1/categories", { method: "POST", body: payload, authenticated: true });
}

export function updateCategory(categoryId: string, payload: CategoryPayload) {
  return apiRequest<Category>(`/api/v1/categories/${categoryId}`, { method: "PUT", body: payload, authenticated: true });
}

export function deactivateCategory(categoryId: string) {
  return apiRequest<void>(`/api/v1/categories/${categoryId}`, { method: "DELETE", authenticated: true });
}

export function listUnits() {
	return apiRequest<ProductUnit[]>("/api/v1/product-units?includeInactive=true", { authenticated: true });
}

export function createUnit(payload: UnitPayload) {
	return apiRequest<ProductUnit>("/api/v1/product-units", { method: "POST", body: payload, authenticated: true });
}

export function updateUnit(code: string, payload: UnitPayload) {
	return apiRequest<ProductUnit>(`/api/v1/product-units/${encodeURIComponent(code)}`, { method: "PUT", body: payload, authenticated: true });
}

export function deactivateUnit(code: string) {
	return apiRequest<void>(`/api/v1/product-units/${encodeURIComponent(code)}`, { method: "DELETE", authenticated: true });
}

export function listProducts(query = "", categoryId = "", active?: boolean) {
  const params = new URLSearchParams({ page: "1", pageSize: "100" });
  if (query) params.set("query", query);
  if (categoryId) params.set("categoryId", categoryId);
  if (active !== undefined) params.set("active", String(active));
  return apiRequest<CatalogPage<Product>>(`/api/v1/products?${params.toString()}`, { authenticated: true });
}

export function createProduct(payload: ProductPayload) {
  return apiRequest<Product>("/api/v1/products", { method: "POST", body: payload, authenticated: true });
}

export function updateProduct(productId: string, payload: ProductPayload) {
  return apiRequest<Product>(`/api/v1/products/${productId}`, { method: "PUT", body: payload, authenticated: true });
}

export function deactivateProduct(productId: string) {
  return apiRequest<void>(`/api/v1/products/${productId}`, { method: "DELETE", authenticated: true });
}

export function listPresentations(productId: string) {
	return apiRequest<ProductPresentation[]>(`/api/v1/products/${productId}/presentations`, { authenticated: true });
}

export function listGlobalPresentations(query = "", productId = "", active?: boolean) {
	const params = new URLSearchParams({ page: "1", pageSize: "100" });
	if (query) params.set("query", query);
	if (productId) params.set("productId", productId);
	if (active !== undefined) params.set("active", String(active));
	return apiRequest<CatalogPage<ProductPresentation>>(`/api/v1/product-presentations?${params.toString()}`, { authenticated: true });
}

export function createPresentation(productId: string, payload: PresentationPayload) {
  return apiRequest<ProductPresentation>(`/api/v1/products/${productId}/presentations`, { method: "POST", body: payload, authenticated: true });
}

export function updatePresentation(productId: string, presentationId: string, payload: PresentationPayload) {
  return apiRequest<ProductPresentation>(`/api/v1/products/${productId}/presentations/${presentationId}`, { method: "PUT", body: payload, authenticated: true });
}

export function deactivatePresentation(productId: string, presentationId: string) {
  return apiRequest<void>(`/api/v1/products/${productId}/presentations/${presentationId}`, { method: "DELETE", authenticated: true });
}

export function lookupBarcode(barcode: string) {
  return apiRequest<BarcodeLookup>(`/api/v1/products/by-barcode/${encodeURIComponent(barcode)}`, { authenticated: true });
}
