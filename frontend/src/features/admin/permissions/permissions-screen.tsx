"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useCallback, useMemo } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { listPermissions } from "@/features/admin/shared/admin-api";
import type { Permission } from "@/features/admin/shared/admin-types";
import { AdminPage } from "@/features/admin/shared/admin-page";

export function PermissionsScreen() {
  const t = useTranslations();
  const permissionsQuery = useQuery({ queryKey: ["admin", "permissions"], queryFn: listPermissions });
  const translateValue = useCallback((namespace: string, value: string) => {
    const safeValue = value.replaceAll(".", "_");
    const key = `${namespace}.${safeValue}`;
    return t.has(key) ? t(key) : value;
  }, [t]);
  const groups = useMemo(() => groupPermissions(permissionsQuery.data ?? []), [permissionsQuery.data]);

  return <AdminPage title={t("admin.permissions.title")} description={t("admin.permissions.description")} isLoading={permissionsQuery.isLoading} error={permissionsQuery.error}>
    <div className="grid gap-3">
      {groups.map((group) => <details key={group.moduleCode} className="rounded-2xl border bg-card shadow-sm"><summary className="cursor-pointer px-4 py-3 font-bold">{translateValue("permissionModules", group.moduleCode)} <span className="font-mono text-xs text-muted-foreground">({group.permissions.length})</span></summary><div className="grid gap-3 border-t p-3 md:grid-cols-2">{group.permissions.map((permission) => <Card key={permission.code}><CardContent className="space-y-2 p-4"><div><p className="font-mono text-xs font-bold">{permission.code}</p><p className="text-sm text-muted-foreground">{permission.description ?? translateValue("permissionCodes", permission.code)}</p></div><div className="flex flex-wrap gap-2 text-xs"><span className="rounded-full bg-muted px-2 py-1">{translateValue("permissionActions", permission.actionCode)}</span>{getPermissionDependencies(permission.code).map((dependency) => <span key={dependency} className="rounded-full border border-amber-300 bg-amber-50 px-2 py-1 text-amber-800 dark:border-amber-800 dark:bg-amber-950/40 dark:text-amber-100">Requiere {dependency}</span>)}</div></CardContent></Card>)}</div></details>)}
    </div>
  </AdminPage>;
}

function groupPermissions(permissions: Permission[]) {
  const groups = new Map<string, Permission[]>();
  for (const permission of permissions) {
    groups.set(permission.moduleCode, [...(groups.get(permission.moduleCode) ?? []), permission]);
  }
  return Array.from(groups.entries()).map(([moduleCode, modulePermissions]) => ({
    moduleCode,
    permissions: modulePermissions.sort((left, right) => left.code.localeCompare(right.code)),
  })).sort((left, right) => left.moduleCode.localeCompare(right.moduleCode));
}

function getPermissionDependencies(permissionCode: string) {
  const dependencies: Record<string, string[]> = {
    "purchases.create": ["settings.view", "suppliers.view", "products.view", "product_presentations.view"],
    "purchases.confirm": ["purchases.view"],
    "purchases.void": ["purchases.view"],
    "sales.create": ["products.view", "product_presentations.view", "inventory.view", "combos.view"],
    "sales.cancel": ["sales.view"],
    "combos.create": ["products.view", "product_presentations.view"],
    "combos.update": ["products.view", "product_presentations.view"],
    "combos.delete": ["combos.view"],
    "transfers.request": ["settings.view", "products.view", "product_presentations.view", "inventory.view"],
    "transfers.approve": ["transfers.view"],
    "transfers.dispatch": ["transfers.view"],
    "transfers.receive": ["transfers.view"],
    "inventory.adjust": ["inventory.view", "settings.view", "products.view", "product_presentations.view", "inventory.view_layers"],
    "inventory.adjust_approve": ["inventory.view"],
  };
  return dependencies[permissionCode] ?? [];
}
