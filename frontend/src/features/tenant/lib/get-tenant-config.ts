import type { PublicTenantConfig } from "@/lib/api/contracts";
import { apiRequest } from "@/lib/api/http";

export function getTenantConfig(tenantSlug: string) {
  const params = new URLSearchParams({ subdomain: tenantSlug });
  return apiRequest<PublicTenantConfig>(`/api/v1/public/tenant-config?${params.toString()}`);
}
