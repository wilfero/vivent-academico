import { apiRequest } from "@/lib/api/http";
import type { Branch, Company, Paginated, Permission, Role, Subscription, User } from "@/features/admin/shared/admin-types";

export type CompanyPayload = Pick<
  Company,
  "legalName" | "commercialName" | "logoUrl" | "primaryColor" | "secondaryColor" | "defaultTheme" | "supportedLocales" | "defaultLocale" | "currencyCode" | "timezone" | "taxEnabled" | "taxRate"
>;

export type BranchPayload = Pick<Branch, "name" | "code" | "address" | "phone" | "email" | "active">;

export type RolePayload = {
  code: string;
  name: string;
  description: string | null;
  permissionCodes: string[];
};

export type UserCreatePayload = {
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  birthDate: string | null;
  status: User["status"];
  branchId: string;
  roleId: string;
  permissionOverrides: Array<{ permissionCode: string; effect: "GRANT" | "DENY" }>;
};

export type UserUpdatePayload = Omit<UserCreatePayload, "branchId" | "status" | "password">;

export type ResetUserPasswordPayload = {
  password: string;
};

export function getCompany() {
  return apiRequest<Company>("/api/v1/companies/current", { authenticated: true });
}

export function updateCompany(payload: CompanyPayload) {
  return apiRequest<Company>("/api/v1/companies/current", { method: "PUT", body: payload, authenticated: true });
}

export function listBranches(query = "") {
  return apiRequest<Paginated<Branch>>(`/api/v1/branches?page=1&pageSize=100&query=${encodeURIComponent(query)}`, { authenticated: true });
}

export function createBranch(payload: BranchPayload) {
  return apiRequest<Branch>("/api/v1/branches", { method: "POST", body: payload, authenticated: true });
}

export function updateBranch(branchId: string, payload: BranchPayload) {
  return apiRequest<Branch>(`/api/v1/branches/${branchId}`, { method: "PUT", body: payload, authenticated: true });
}

export function listUsers(query = "") {
  return apiRequest<Paginated<User>>(`/api/v1/users?page=1&pageSize=100&query=${encodeURIComponent(query)}`, { authenticated: true });
}

export function createUser(payload: UserCreatePayload) {
  return apiRequest<User>("/api/v1/users", { method: "POST", body: payload, authenticated: true });
}

export function updateUser(userId: string, payload: UserUpdatePayload) {
  return apiRequest<User>(`/api/v1/users/${userId}`, { method: "PUT", body: payload, authenticated: true });
}

export function activateUser(userId: string) {
  return apiRequest<User>(`/api/v1/users/${userId}/activate`, { method: "POST", authenticated: true });
}

export function deactivateUser(userId: string) {
  return apiRequest<User>(`/api/v1/users/${userId}/deactivate`, { method: "POST", authenticated: true });
}

export function changeUserBranch(userId: string, branchId: string) {
  return apiRequest<User>(`/api/v1/users/${userId}/branch`, { method: "POST", body: { branchId }, authenticated: true });
}

export function resetUserPassword(userId: string, payload: ResetUserPasswordPayload) {
  return apiRequest<User>(`/api/v1/users/${userId}/password`, { method: "POST", body: payload, authenticated: true });
}

export function listRoles(query = "") {
  return apiRequest<Paginated<Role>>(`/api/v1/roles?page=1&pageSize=100&query=${encodeURIComponent(query)}`, { authenticated: true });
}

export function createRole(payload: RolePayload) {
  return apiRequest<Role>("/api/v1/roles", { method: "POST", body: payload, authenticated: true });
}

export function updateRole(roleId: string, payload: RolePayload) {
  return apiRequest<Role>(`/api/v1/roles/${roleId}`, { method: "PUT", body: payload, authenticated: true });
}

export function listPermissions() {
  return apiRequest<Permission[]>("/api/v1/permissions", { authenticated: true });
}

export function getCurrentSubscription() {
  return apiRequest<Subscription>("/api/v1/subscriptions/current", { authenticated: true });
}
