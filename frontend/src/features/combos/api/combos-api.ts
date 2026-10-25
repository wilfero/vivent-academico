import { apiRequest } from "@/lib/api/http";
import type { ComboOffer, ComboOfferFilters, ComboOfferPayload, ComboOffersPage } from "@/features/combos/types/combo-types";

export const combosQueryKeys = {
  all: ["combos"] as const,
  lists: () => [...combosQueryKeys.all, "list"] as const,
  list: (filters: ComboOfferFilters) => [...combosQueryKeys.lists(), filters] as const,
  detail: (comboId: string) => [...combosQueryKeys.all, "detail", comboId] as const,
};

export function listCombos(filters: ComboOfferFilters) {
  const params = new URLSearchParams({ page: String(filters.page), pageSize: String(filters.pageSize) });
  if (filters.active !== "") params.set("active", String(filters.active));
  if (filters.query) params.set("query", filters.query);
  return apiRequest<ComboOffersPage>(`/api/v1/combos?${params.toString()}`, { authenticated: true });
}

export function getCombo(comboId: string) {
  return apiRequest<ComboOffer>(`/api/v1/combos/${comboId}`, { authenticated: true });
}

export function getComboByBarcode(barcode: string) {
  return apiRequest<ComboOffer>(`/api/v1/combos/by-barcode/${encodeURIComponent(barcode)}`, { authenticated: true });
}

export function createCombo(payload: ComboOfferPayload) {
  return apiRequest<ComboOffer>("/api/v1/combos", { method: "POST", body: payload, authenticated: true });
}

export function updateCombo(comboId: string, payload: ComboOfferPayload) {
  return apiRequest<ComboOffer>(`/api/v1/combos/${comboId}`, { method: "PUT", body: payload, authenticated: true });
}

export function deleteCombo(comboId: string) {
  return apiRequest<void>(`/api/v1/combos/${comboId}`, { method: "DELETE", authenticated: true });
}