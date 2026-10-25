import { platformApiRequest } from "@/lib/api/platform-http";
import type {
  PlatformAuditLog,
  PlatformCompanyDetail,
  PlatformCompanySummary,
  PlatformPageResponse,
  PlatformPlan,
  PlatformSession,
  ProvisionedCompany,
  ResetAdminPasswordResult,
  PlatformSubscription,
} from "@/lib/api/platform-contracts";

export function platformLogin(username: string, password: string) {
  return platformApiRequest<PlatformSession>("/api/v1/platform/auth/login", {
    method: "POST",
    body: { username, password },
  });
}

export function platformLogout() {
  return platformApiRequest<void>("/api/v1/platform/auth/logout", { method: "POST" });
}

export function platformMe() {
  return platformApiRequest<PlatformSession>("/api/v1/platform/auth/me");
}

export function listPlatformCompanies(params: {
  page: number;
  pageSize: number;
  query?: string;
  status?: string;
}) {
  const search = new URLSearchParams();
  search.set("page", String(params.page));
  search.set("pageSize", String(params.pageSize));
  if (params.query) search.set("query", params.query);
  if (params.status) search.set("status", params.status);

  return platformApiRequest<PlatformPageResponse<PlatformCompanySummary>>(
    `/api/v1/platform/companies?${search.toString()}`,
  );
}

export function getPlatformCompany(companyId: string) {
  return platformApiRequest<PlatformCompanyDetail>(`/api/v1/platform/companies/${companyId}`);
}

export type CreatePlatformCompanyInput = {
  legalName: string;
  commercialName: string;
  tenantSlug: string;
  planId: string;
  branchName: string;
  branchCode: string;
  adminUsername: string;
  adminFirstName: string;
  adminLastName: string;
  adminEmail: string;
  currencyCode?: string;
  timezone?: string;
  defaultLocale?: string;
  defaultTheme?: string;
  primaryColor?: string;
  secondaryColor?: string;
  taxEnabled?: boolean;
  taxRate?: number;
};

export function createPlatformCompany(input: CreatePlatformCompanyInput) {
  return platformApiRequest<ProvisionedCompany>("/api/v1/platform/companies", {
    method: "POST",
    body: input,
  });
}

export type UpdatePlatformCompanyInput = {
  legalName: string;
  commercialName: string;
  logoUrl?: string | null;
  primaryColor: string;
  secondaryColor: string;
  defaultTheme: string;
  supportedLocales: string[];
  defaultLocale: string;
  currencyCode: string;
  timezone: string;
  taxEnabled: boolean;
  taxRate: number;
};

export function updatePlatformCompany(companyId: string, input: UpdatePlatformCompanyInput) {
  return platformApiRequest<PlatformCompanyDetail>(`/api/v1/platform/companies/${companyId}`, {
    method: "PUT",
    body: input,
  });
}

export function updateCompanyStatus(companyId: string, status: string) {
  return platformApiRequest<PlatformCompanySummary>(`/api/v1/platform/companies/${companyId}/status`, {
    method: "PUT",
    body: { status },
  });
}

export function updateCompanySubscription(
  companyId: string,
  input: { planId: string; status: string; endsAt?: string | null; trialEndsAt?: string | null; notes?: string | null },
) {
  return platformApiRequest<PlatformSubscription>(`/api/v1/platform/companies/${companyId}/subscription`, {
    method: "PUT",
    body: input,
  });
}

export function resetCompanyAdminPassword(companyId: string) {
  return platformApiRequest<ResetAdminPasswordResult>(
    `/api/v1/platform/companies/${companyId}/admin-password/reset`,
    { method: "POST" },
  );
}

export function listPlatformPlans() {
  return platformApiRequest<PlatformPlan[]>("/api/v1/platform/plans");
}

export function listPlatformAuditLogs(page: number, pageSize: number) {
  return platformApiRequest<PlatformPageResponse<PlatformAuditLog>>(
    `/api/v1/platform/audit-logs?page=${page}&pageSize=${pageSize}`,
  );
}
