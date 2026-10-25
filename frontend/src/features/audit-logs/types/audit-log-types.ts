import type { Paginated } from "@/features/admin/shared/admin-types";

export type AuditAction = "CREATE" | "UPDATE" | "DELETE" | "DEACTIVATE" | "LOGIN" | "LOGOUT" | "CONFIRM" | "VOID" | "CANCEL" | "APPROVE" | "REJECT" | "DISPATCH" | "RECEIVE" | "AUTHORIZE";

export type AuditLog = {
  id: string;
  companyId: string;
  branchId: string | null;
  userId: string | null;
  action: AuditAction;
  entityType: string;
  entityId: string;
  oldValue: string | null;
  newValue: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  timezoneSnapshot: string;
  createdAt: string;
};

export type AuditLogFilters = {
  page: number;
  pageSize: number;
  entityType: string;
  entityId: string;
};

export type AuditLogsPage = Paginated<AuditLog>;
