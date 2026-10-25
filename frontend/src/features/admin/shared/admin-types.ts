import type { PageResponse } from "@/lib/api/contracts";

export type Company = {
  id: string;
  legalName: string;
  commercialName: string;
  tenantSlug: string;
  domain: string;
  logoUrl: string | null;
  primaryColor: string;
  secondaryColor: string;
  defaultTheme: "light" | "dark";
  supportedLocales: string[];
  defaultLocale: string;
  currencyCode: string;
  timezone: string;
  taxEnabled: boolean;
  taxRate: number;
  status: string;
};

export type Branch = {
  id: string;
  companyId: string;
  name: string;
  code: string;
  address: string | null;
  phone: string | null;
  email: string | null;
  active: boolean;
};

export type Role = {
  id: string;
  companyId: string | null;
  code: string;
  name: string;
  translationKey: string;
  description: string | null;
  system: boolean;
  permissionCodes: string[];
};

export type User = {
  id: string;
  companyId: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  birthDate: string | null;
  status: "ACTIVE" | "INACTIVE" | "LOCKED" | "DELETED";
  lastLoginAt: string | null;
  branch: { id: string; name: string; code: string; active: boolean };
  role: { id: string; code: string; name: string; system: boolean };
  permissionOverrides: Array<{ permissionCode: string; effect: "GRANT" | "DENY" }>;
  resolvedPermissions: string[];
};

export type Permission = {
  id: string;
  code: string;
  translationKey: string;
  moduleCode: string;
  actionCode: string;
  description: string | null;
  system: boolean;
};

export type Subscription = {
  id: string;
  companyId: string;
  status: "TRIAL" | "ACTIVE" | "EXPIRED" | "SUSPENDED" | "CANCELLED";
  startsAt: string;
  endsAt: string | null;
  trialEndsAt: string | null;
  notes: string | null;
  plan: {
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
};

export type Paginated<T> = PageResponse<T>;
