import { apiRequest } from "@/lib/api/http";
import type { AuditLogFilters, AuditLogsPage } from "@/features/audit-logs/types/audit-log-types";

export const auditLogQueryKeys = {
  all: ["auditLogs"] as const,
  lists: () => [...auditLogQueryKeys.all, "list"] as const,
  list: (filters: AuditLogFilters) => [...auditLogQueryKeys.lists(), filters] as const,
};

export function listAuditLogs(filters: AuditLogFilters) {
  const params = new URLSearchParams({ page: String(filters.page), pageSize: String(filters.pageSize) });
  if (filters.entityType) params.set("entityType", filters.entityType);
  if (filters.entityId) params.set("entityId", filters.entityId);
  return apiRequest<AuditLogsPage>(`/api/v1/audit-logs?${params.toString()}`, { authenticated: true });
}
