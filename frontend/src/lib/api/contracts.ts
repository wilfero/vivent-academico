export type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
  errorCode: string | null;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
};

export type PublicTenantConfig = {
  companyId: string;
  tenantSlug: string;
  commercialName: string;
  logoUrl: string | null;
  primaryColor: string;
  secondaryColor: string;
  defaultTheme: "light" | "dark" | string;
  supportedLocales: string[];
  defaultLocale: string;
  currencyCode: string;
  timezone: string;
  subscriptionStatus: string;
  enabledModules: string[];
};

export type LoginRequest = {
  tenantSlug: string;
  username: string;
  password: string;
};

export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  companyId: string;
  branchId: string;
  branchName?: string;
  userId: string;
  tenantSlug: string;
  username: string;
  roleCode: string;
  permissions: string[];
  mustChangePassword: boolean;
};

export type CurrentSession = Omit<LoginResponse, "accessToken" | "tokenType">;

export type UserProfile = {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  birthDate: string | null;
  branch: { id: string; name: string; code: string };
  role: { id: string; code: string; name: string };
  mustChangePassword: boolean;
};

export type DashboardCard = {
  titleKey: string;
  value: string;
  descriptionKey: string;
};
