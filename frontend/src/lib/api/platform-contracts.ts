import type { ApiResponse, PageResponse } from "@/lib/api/contracts";

export type PlatformSession = {
  userId: string;
  username: string;
  displayName: string;
  expiresAt: string;
};

export type PlatformLoginRequest = {
  username: string;
  password: string;
};

export type PlatformCompany = {
  id: string;
  legalName: string;
  commercialName: string;
  tenantSlug: string;
  domain: string;
  logoUrl: string | null;
  primaryColor: string;
  secondaryColor: string;
  defaultTheme: string;
  supportedLocales: string[];
  defaultLocale: string;
  currencyCode: string;
  timezone: string;
  taxEnabled: boolean;
  taxRate: number;
  status: string;
};

export type PlatformPlan = {
  id: string;
  name: string;
  code: string;
  description: string | null;
  price: number;
  currencyCode: string;
  maxBranches: number;
  maxUsers: number;
  maxProducts: number;
  enabledModules: string[];
  custom: boolean;
  active: boolean;
};

export type PlatformSubscription = {
  id: string;
  companyId: string;
  status: string;
  startsAt: string;
  endsAt: string | null;
  trialEndsAt: string | null;
  notes: string | null;
  plan: PlatformPlan;
};

export type PlatformCompanySummary = {
  id: string;
  legalName: string;
  commercialName: string;
  tenantSlug: string;
  domain: string;
  status: string;
  planCode: string | null;
  planName: string | null;
  subscriptionStatus: string | null;
  createdAt: string;
};

export type PlatformCompanyDetail = {
  company: PlatformCompany;
  subscription: PlatformSubscription | null;
  usage: {
    branches: number;
    users: number;
    products: number;
  };
  admin: {
    userId: string;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
  } | null;
};

export type ProvisionedCompany = {
  companyId: string;
  tenantSlug: string;
  domain: string;
  adminUsername: string;
  adminPassword: string;
};

export type ResetAdminPasswordResult = {
  userId: string;
  username: string;
  password: string;
};

export type PlatformAuditLog = {
  id: string;
  platformUserId: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  oldValue: string | null;
  newValue: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
};

export type PlatformApiResponse<T> = ApiResponse<T>;
export type PlatformPageResponse<T> = PageResponse<T>;
