"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useEffect, useMemo, useState } from "react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { createRole, listPermissions, listRoles, updateRole, type RolePayload } from "@/features/admin/shared/admin-api";
import type { Permission, Role } from "@/features/admin/shared/admin-types";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AdminTable } from "@/features/admin/shared/admin-table";

const roleSchema = z.object({ code: z.string().min(1), name: z.string().min(1), description: z.string().nullable(), permissionCodes: z.array(z.string()).min(1) });
type RoleForm = z.infer<typeof roleSchema>;
const emptyRoleForm: RoleForm = { code: "", name: "", description: "", permissionCodes: [] };

export function RolesScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Role | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const canUpdateSettings = canAny(["settings.update"]);
  const rolesQuery = useQuery({ queryKey: ["admin", "roles"], queryFn: () => listRoles() });
  const permissionsQuery = useQuery({ queryKey: ["admin", "permissions"], queryFn: listPermissions });
  const form = useForm<RoleForm>({ resolver: zodResolver(roleSchema), defaultValues: emptyRoleForm });

  useEffect(() => {
    if (!editing) return;
    form.reset({ code: editing.code, name: editing.name, description: editing.description ?? "", permissionCodes: editing.permissionCodes });
  }, [editing, form]);

  const mutation = useMutation({
    mutationFn: (values: RoleForm) => {
      const payload: RolePayload = { code: values.code, name: values.name, description: values.description || null, permissionCodes: values.permissionCodes };
      return editing ? updateRole(editing.id, payload) : createRole(payload);
    },
    onSuccess: () => { setSuccessMessage(t("admin.states.saved")); setIsFormOpen(false); setEditing(null); form.reset(emptyRoleForm); queryClient.invalidateQueries({ queryKey: ["admin", "roles"] }); },
  });

  const columns = useMemo<ColumnDef<Role>[]>(() => [
    { header: t("admin.fields.code"), accessorKey: "code" },
    { header: t("admin.fields.name"), accessorKey: "name" },
    { header: t("admin.fields.permissions"), cell: ({ row }) => row.original.permissionCodes.length },
    { header: t("admin.fields.system"), cell: ({ row }) => row.original.system ? t("admin.status.yes") : t("admin.status.no") },
    { header: t("admin.fields.actions"), cell: ({ row }) => canUpdateSettings && !row.original.system ? <Button variant="outline" onClick={() => { setEditing(row.original); setIsFormOpen(true); }}>{t("admin.actions.edit")}</Button> : <span className="text-muted-foreground">-</span> },
  ], [canUpdateSettings, t]);

  return <AdminPage title={t("admin.roles.title")} description={t("admin.roles.description")} isLoading={rolesQuery.isLoading || permissionsQuery.isLoading} error={rolesQuery.error ?? permissionsQuery.error ?? mutation.error} successMessage={successMessage}>
    {canUpdateSettings ? <div className="flex justify-end"><Button type="button" onClick={() => { setEditing(null); form.reset(emptyRoleForm); setIsFormOpen(true); }}>{t("admin.actions.create")}</Button></div> : null}
    <FormDialog open={isFormOpen} onOpenChange={(open) => { setIsFormOpen(open); if (!open) { setEditing(null); form.reset(emptyRoleForm); } }} title={editing ? t("admin.actions.update") : t("admin.actions.create")} description={t("admin.roles.description")}>
      <form className="grid gap-4 md:grid-cols-2" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
        <Field label={t("admin.fields.code")} registration={form.register("code")} />
        <Field label={t("admin.fields.name")} registration={form.register("name")} />
        <Field label={t("admin.fields.description")} registration={form.register("description")} />
        <PermissionPicker
          availablePermissions={permissionsQuery.data ?? []}
          label={t("admin.fields.permissionCodes")}
          // eslint-disable-next-line react-hooks/incompatible-library
          selected={form.watch("permissionCodes") ?? []}
          onChange={(permissionCodes) => form.setValue("permissionCodes", permissionCodes, { shouldDirty: true, shouldValidate: true })}
        />
        <div className="flex gap-2 md:col-span-2"><Button disabled={mutation.isPending} type="submit">{editing ? t("admin.actions.update") : t("admin.actions.create")}</Button><Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>{t("admin.actions.cancel")}</Button></div>
      </form>
    </FormDialog>
    <AdminTable columns={columns} data={rolesQuery.data?.items ?? []} />
  </AdminPage>;
}

function Field({ label, registration }: Readonly<{ label: string; registration: UseFormRegisterReturn }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input {...registration} /></div>;
}

function PermissionPicker({ availablePermissions, label, selected, onChange }: Readonly<{ availablePermissions: Permission[]; label: string; selected: string[]; onChange: (permissions: string[]) => void }>) {
  const t = useTranslations();
  const [filter, setFilter] = useState("");
  const groupedPermissions = useMemo(() => groupPermissions(availablePermissions), [availablePermissions]);
  const selectedSet = useMemo(() => new Set(selected), [selected]);
  const normalizedFilter = filter.trim().toLowerCase();

  function togglePermission(permissionCode: string) {
    onChange(selectedSet.has(permissionCode) ? selected.filter((item) => item !== permissionCode) : [...selected, permissionCode].sort());
  }

  function selectModule(permissionCodes: string[]) {
    onChange(Array.from(new Set([...selected, ...permissionCodes])).sort());
  }

  function clearModule(permissionCodes: string[]) {
    const moduleSet = new Set(permissionCodes);
    onChange(selected.filter((permissionCode) => !moduleSet.has(permissionCode)));
  }

  const selectedPermissions = selected.map((permissionCode) => availablePermissions.find((permission) => permission.code === permissionCode)).filter((permission): permission is Permission => Boolean(permission));

  return <div className="space-y-3 md:col-span-2"><div className="flex flex-wrap items-center justify-between gap-2"><Label>{label}</Label><span className="text-xs text-muted-foreground">{selected.length} {t("admin.fields.permissions")}</span></div><div className="rounded-xl border bg-background/75 p-3">
    <Input value={filter} onChange={(event) => setFilter(event.target.value)} placeholder={t("admin.roles.searchPermissions")} />
    <div className="mt-3 rounded-2xl border bg-muted/35 p-3">
      <p className="text-xs font-bold uppercase tracking-[0.16em] text-muted-foreground">{t("admin.roles.selectedPermissions")}</p>
      <div className="mt-2 flex flex-wrap gap-2">
        {selectedPermissions.length > 0 ? selectedPermissions.map((permission) => <button key={permission.code} type="button" className="rounded-full border bg-background px-3 py-1 text-left text-xs shadow-sm transition hover:border-destructive hover:text-destructive" onClick={() => togglePermission(permission.code)} title={permission.code}>{translateValue(t, "permissionCodes", permission.code)} <span className="ml-1 font-mono text-[10px] text-muted-foreground">x</span></button>) : <span className="text-sm text-muted-foreground">{t("admin.roles.noPermissionsSelected")}</span>}
      </div>
    </div>
    <div className="mt-3 grid gap-3 lg:grid-cols-2">
      {groupedPermissions.map((group) => {
        const visiblePermissions = group.permissions.filter((permission) => !normalizedFilter || permission.code.toLowerCase().includes(normalizedFilter) || translateValue(t, "permissionCodes", permission.code).toLowerCase().includes(normalizedFilter));
        if (visiblePermissions.length === 0) return null;
        const moduleCodes = group.permissions.map((permission) => permission.code);
        const selectedInModule = moduleCodes.filter((permissionCode) => selectedSet.has(permissionCode)).length;
        return <section key={group.moduleCode} className="rounded-2xl border bg-card p-3"><div className="flex flex-wrap items-start justify-between gap-2"><div><p className="font-bold">{translateValue(t, "permissionModules", group.moduleCode)}</p><p className="font-mono text-xs text-muted-foreground">{selectedInModule}/{moduleCodes.length}</p></div><div className="flex gap-2"><Button className="h-8 px-3 text-xs" type="button" variant="outline" onClick={() => selectModule(moduleCodes)}>{t("admin.roles.selectAll")}</Button><Button className="h-8 px-3 text-xs" type="button" variant="ghost" onClick={() => clearModule(moduleCodes)}>{t("admin.roles.clear")}</Button></div></div><div className="mt-3 grid gap-2">{visiblePermissions.map((permission) => <label key={permission.code} className="flex cursor-pointer items-start gap-2 rounded-xl border bg-background/70 p-2 text-sm"><input className="mt-1" type="checkbox" checked={selectedSet.has(permission.code)} onChange={() => togglePermission(permission.code)} /><span><span className="block font-mono text-xs font-semibold">{permission.code}</span><span className="text-xs text-muted-foreground">{translateValue(t, "permissionCodes", permission.code)}</span></span></label>)}</div></section>;
      })}
    </div>
  </div></div>;
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

function translateValue(t: ReturnType<typeof useTranslations>, namespace: string, value: string) {
  const safeValue = value.replaceAll(".", "_");
  const key = `${namespace}.${safeValue}`;
  return t.has(key) ? t(key) : value;
}
