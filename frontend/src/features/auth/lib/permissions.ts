import type { CurrentSession, LoginResponse } from "@/lib/api/contracts";

type PermissionSession = Pick<LoginResponse | CurrentSession, "permissions"> | null | undefined;

export function hasAnyPermission(session: PermissionSession, permissions?: string[]) {
  if (!permissions?.length) return true;
  const grantedPermissions = new Set(session?.permissions ?? []);
  return permissions.some((permission) => grantedPermissions.has(permission));
}
