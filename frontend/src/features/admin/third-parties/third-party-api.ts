import { apiRequest } from "@/lib/api/http";
import type { Paginated } from "@/features/admin/shared/admin-types";

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

export type Customer = {
  id: string;
  companyId: string;
  name: string;
  taxId: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  genericConsumer: boolean;
  active: boolean;
};

export type SupplierPayload = Omit<Supplier, "id" | "companyId">;
export type CustomerPayload = Omit<Customer, "id" | "companyId">;

export function listSuppliers(query = "", active?: boolean) {
  const params = new URLSearchParams({ page: "1", pageSize: "100" });
  if (query) params.set("query", query);
  if (active !== undefined) params.set("active", String(active));
  return apiRequest<Paginated<Supplier>>(`/api/v1/suppliers?${params.toString()}`, { authenticated: true });
}

export function createSupplier(payload: SupplierPayload) {
  return apiRequest<Supplier>("/api/v1/suppliers", { method: "POST", body: payload, authenticated: true });
}

export function updateSupplier(supplierId: string, payload: SupplierPayload) {
  return apiRequest<Supplier>(`/api/v1/suppliers/${supplierId}`, { method: "PUT", body: payload, authenticated: true });
}

export function deactivateSupplier(supplierId: string) {
  return apiRequest<void>(`/api/v1/suppliers/${supplierId}`, { method: "DELETE", authenticated: true });
}

export function listCustomers(query = "", active?: boolean) {
  const params = new URLSearchParams({ page: "1", pageSize: "100" });
  if (query) params.set("query", query);
  if (active !== undefined) params.set("active", String(active));
  return apiRequest<Paginated<Customer>>(`/api/v1/customers?${params.toString()}`, { authenticated: true });
}

export function createCustomer(payload: CustomerPayload) {
  return apiRequest<Customer>("/api/v1/customers", { method: "POST", body: payload, authenticated: true });
}

export function updateCustomer(customerId: string, payload: CustomerPayload) {
  return apiRequest<Customer>(`/api/v1/customers/${customerId}`, { method: "PUT", body: payload, authenticated: true });
}

export function deactivateCustomer(customerId: string) {
  return apiRequest<void>(`/api/v1/customers/${customerId}`, { method: "DELETE", authenticated: true });
}
