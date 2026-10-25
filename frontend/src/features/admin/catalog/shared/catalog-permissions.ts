import { getStoredSession } from "@/features/auth/lib/session-storage";
import { hasAnyPermission } from "@/features/auth/lib/permissions";

export function can(permission: string) {
	return hasAnyPermission(getStoredSession(), [permission]);
}

export function canAny(permissions: string[]) {
	return hasAnyPermission(getStoredSession(), permissions);
}
