const ROOT_DOMAIN = "corpoviva.com";

export function resolveTenantSlug(hostname: string): string {
  const fallbackTenant = process.env.NEXT_PUBLIC_DEFAULT_TENANT ?? "demo";

  if (hostname === "localhost" || hostname === "127.0.0.1" || hostname.endsWith(".local")) {
    return fallbackTenant;
  }

  if (hostname.endsWith(ROOT_DOMAIN)) {
    const [tenantSlug] = hostname.replace(`.${ROOT_DOMAIN}`, "").split(".");
    return tenantSlug || fallbackTenant;
  }

  return fallbackTenant;
}
