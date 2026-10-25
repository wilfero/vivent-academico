"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useForm, type UseFormRegisterReturn, type UseFormSetError } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { activateUser, changeUserBranch, createUser, deactivateUser, listBranches, listRoles, listUsers, resetUserPassword, updateUser, type UserCreatePayload, type UserUpdatePayload } from "@/features/admin/shared/admin-api";
import type { User } from "@/features/admin/shared/admin-types";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { withSessionBranchFallback } from "@/features/auth/lib/session-branch";

const userSchema = z.object({ username: z.string().min(1), password: z.string(), firstName: z.string().min(1), lastName: z.string().min(1), email: z.string().email(), phone: z.string().nullable(), birthDate: z.string().nullable(), status: z.enum(["ACTIVE", "INACTIVE", "LOCKED", "DELETED"]), branchId: z.string().min(1), roleId: z.string().min(1) });
type UserForm = z.infer<typeof userSchema>;
const emptyUserValues: UserForm = { username: "", password: "", firstName: "", lastName: "", email: "", phone: "", birthDate: "", status: "ACTIVE", branchId: "", roleId: "" };
type ResetPasswordForm = { password: string };

export function UsersScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<User | null>(null);
  const [resetting, setResetting] = useState<User | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isResetPasswordOpen, setIsResetPasswordOpen] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const canReadSettings = canAny(["settings.view"]);
  const canCreate = canAny(["users.create"]);
  const canUpdate = canAny(["users.update"]);
  const canChangeBranch = canAny(["users.change_branch"]);
  const canDeactivate = canAny(["users.deactivate"]);
  const usersQuery = useQuery({ queryKey: ["admin", "users"], queryFn: () => listUsers() });
  const branchesQuery = useQuery({ queryKey: ["admin", "branches"], queryFn: () => listBranches(), enabled: canReadSettings });
  const rolesQuery = useQuery({ queryKey: ["admin", "roles"], queryFn: () => listRoles(), enabled: canReadSettings });
  const branchOptions = useMemo(() => withSessionBranchFallback((branchesQuery.data?.items ?? []).map((branch) => ({ value: branch.id, label: branch.name }))), [branchesQuery.data?.items]);
  const singleBranchId = branchOptions.length === 1 ? branchOptions[0].value : "";
  const form = useForm<UserForm>({ resolver: zodResolver(userSchema), defaultValues: emptyUserValues });
  const resetPasswordForm = useForm<ResetPasswordForm>({ defaultValues: { password: "" } });

  useEffect(() => {
    if (!editing) return;
    form.reset({ username: editing.username, password: "", firstName: editing.firstName, lastName: editing.lastName, email: editing.email, phone: editing.phone ?? "", birthDate: editing.birthDate ?? "", status: editing.status, branchId: editing.branch.id, roleId: editing.role.id });
  }, [editing, form]);

  useEffect(() => {
    if (editing || !singleBranchId || form.getValues("branchId")) return;
    form.setValue("branchId", singleBranchId, { shouldValidate: true });
  }, [editing, form, singleBranchId]);

  const saveMutation = useMutation({
    mutationFn: async (values: UserForm) => {
      if (editing) {
        const payload: UserUpdatePayload = { username: values.username, firstName: values.firstName, lastName: values.lastName, email: values.email, phone: values.phone || null, birthDate: values.birthDate || null, roleId: values.roleId, permissionOverrides: [] };
        const user = await updateUser(editing.id, payload);
        if (values.branchId !== editing.branch.id && canChangeBranch) await changeUserBranch(editing.id, values.branchId);
        return user;
      }

      const payload: UserCreatePayload = { ...values, phone: values.phone || null, birthDate: values.birthDate || null, permissionOverrides: [] };
      return createUser(payload);
    },
    onSuccess: () => { setSuccessMessage(t("admin.states.saved")); setIsFormOpen(false); setEditing(null); form.reset({ ...emptyUserValues, branchId: singleBranchId }); queryClient.invalidateQueries({ queryKey: ["admin", "users"] }); },
  });
  const statusMutation = useMutation({ mutationFn: ({ id, active }: { id: string; active: boolean }) => active ? activateUser(id) : deactivateUser(id), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] }) });
  const resetPasswordMutation = useMutation({
    mutationFn: (values: ResetPasswordForm) => resetting ? resetUserPassword(resetting.id, { password: values.password }) : Promise.reject(new Error(t("admin.states.error"))),
    onSuccess: () => { setSuccessMessage(t("admin.users.passwordReset")); setIsResetPasswordOpen(false); setResetting(null); resetPasswordForm.reset({ password: "" }); },
  });

  const columns = useMemo<ColumnDef<User>[]>(() => [
    { header: t("admin.fields.username"), accessorKey: "username" },
    { header: t("admin.fields.name"), cell: ({ row }) => `${row.original.firstName} ${row.original.lastName}` },
    { header: t("admin.fields.branch"), cell: ({ row }) => row.original.branch.name },
    { header: t("admin.fields.role"), cell: ({ row }) => row.original.role.code },
    { header: t("admin.fields.status"), accessorKey: "status" },
    { header: t("admin.fields.actions"), cell: ({ row }) => {
      const actions: ReactNode[] = [];
      if (canUpdate && canReadSettings) {
        actions.push(<Button key="edit" variant="outline" onClick={() => { setEditing(row.original); setIsFormOpen(true); }}>{t("admin.actions.edit")}</Button>);
      }
      if (canUpdate) {
        actions.push(<Button key="reset-password" variant="outline" onClick={() => { setResetting(row.original); resetPasswordForm.reset({ password: "" }); setIsResetPasswordOpen(true); }}>{t("admin.actions.resetPassword")}</Button>);
      }
      if (row.original.status === "ACTIVE" && canDeactivate) {
        actions.push(<Button key="deactivate" variant="outline" onClick={() => statusMutation.mutate({ id: row.original.id, active: false })}>{t("admin.actions.deactivate")}</Button>);
      }
      if (row.original.status !== "ACTIVE" && canUpdate) {
        actions.push(<Button key="activate" variant="outline" onClick={() => statusMutation.mutate({ id: row.original.id, active: true })}>{t("admin.actions.activate")}</Button>);
      }
      return actions.length > 0 ? <div className="flex gap-2">{actions}</div> : <span className="text-muted-foreground">-</span>;
    } },
  ], [canDeactivate, canReadSettings, canUpdate, resetPasswordForm, statusMutation, t]);

  return <AdminPage title={t("admin.users.title")} description={t("admin.users.description")} isLoading={usersQuery.isLoading || (canReadSettings && (branchesQuery.isLoading || rolesQuery.isLoading))} error={usersQuery.error ?? (canReadSettings ? branchesQuery.error : null) ?? (canReadSettings ? rolesQuery.error : null) ?? saveMutation.error ?? statusMutation.error ?? resetPasswordMutation.error} successMessage={successMessage}>
    {canCreate && canReadSettings ? <div className="flex justify-end"><Button type="button" onClick={() => { setEditing(null); form.reset({ ...emptyUserValues, branchId: singleBranchId }); setIsFormOpen(true); }}>{t("admin.actions.create")}</Button></div> : null}
    <FormDialog open={isFormOpen} onOpenChange={(open) => { setIsFormOpen(open); if (!open) { setEditing(null); form.reset({ ...emptyUserValues, branchId: singleBranchId }); } }} title={editing ? t("admin.actions.update") : t("admin.actions.create")} description={t("admin.users.description")}>
      <form className="grid gap-4 md:grid-cols-3" onSubmit={form.handleSubmit((values) => handleSubmit(values, editing, canChangeBranch, form.setError, saveMutation.mutate, t))}>
        <Field label={t("admin.fields.username")} registration={form.register("username")} error={form.formState.errors.username?.message} />
        {!editing ? <Field label={t("admin.fields.password")} registration={form.register("password")} type="password" error={form.formState.errors.password?.message} /> : null}
        <Field label={t("admin.fields.email")} registration={form.register("email")} error={form.formState.errors.email?.message} />
        <Field label={t("admin.fields.firstName")} registration={form.register("firstName")} error={form.formState.errors.firstName?.message} />
        <Field label={t("admin.fields.lastName")} registration={form.register("lastName")} error={form.formState.errors.lastName?.message} />
        <Field label={t("admin.fields.phone")} registration={form.register("phone")} error={form.formState.errors.phone?.message} />
        <Field label={t("admin.fields.birthDate")} registration={form.register("birthDate")} type="date" error={form.formState.errors.birthDate?.message} />
        <Select label={t("admin.fields.branch")} registration={form.register("branchId")} options={branchOptions} disabled={Boolean(editing) && !canChangeBranch} error={form.formState.errors.branchId?.message} />
        {editing && !canChangeBranch ? <p className="rounded-2xl border bg-muted/35 p-3 text-xs text-muted-foreground md:col-span-3">{t("admin.users.branchChangePermissionHint")}</p> : null}
        <Select label={t("admin.fields.role")} registration={form.register("roleId")} options={rolesQuery.data?.items.map((role) => ({ value: role.id, label: role.code })) ?? []} error={form.formState.errors.roleId?.message} />
        {!editing ? <Select label={t("admin.fields.status")} registration={form.register("status")} options={["ACTIVE", "INACTIVE", "LOCKED", "DELETED"].map((status) => ({ value: status, label: status }))} error={form.formState.errors.status?.message} /> : null}
        <div className="flex gap-2 md:col-span-3"><Button disabled={saveMutation.isPending} type="submit">{editing ? t("admin.actions.update") : t("admin.actions.create")}</Button><Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>{t("admin.actions.cancel")}</Button></div>
      </form>
    </FormDialog>
    <FormDialog open={isResetPasswordOpen} onOpenChange={(open) => { setIsResetPasswordOpen(open); if (!open) { setResetting(null); resetPasswordForm.reset({ password: "" }); } }} title={t("admin.users.resetPasswordTitle")} description={t("admin.users.resetPasswordDescription")}>
      <form className="space-y-4" onSubmit={resetPasswordForm.handleSubmit((values) => handleResetPasswordSubmit(values, resetPasswordForm.setError, resetPasswordMutation.mutate, t))}>
        <Field label={t("admin.fields.password")} registration={resetPasswordForm.register("password")} type="password" error={resetPasswordForm.formState.errors.password?.message} />
        <div className="flex gap-2"><Button disabled={resetPasswordMutation.isPending} type="submit">{t("admin.actions.resetPassword")}</Button><Button type="button" variant="outline" onClick={() => setIsResetPasswordOpen(false)}>{t("admin.actions.cancel")}</Button></div>
      </form>
    </FormDialog>
    <AdminTable columns={columns} data={usersQuery.data?.items ?? []} />
  </AdminPage>;
}

function handleSubmit(values: UserForm, editing: User | null, canChangeBranch: boolean, setError: UseFormSetError<UserForm>, submit: (values: UserForm) => void, t: (key: string) => string) {
  if (!editing && values.password.trim().length < 8) {
    setError("password", { message: t("admin.users.passwordRequired") });
    return;
  }

  if (editing && values.branchId !== editing.branch.id && !canChangeBranch) {
    setError("branchId", { message: t("admin.users.branchChangePermissionHint") });
    return;
  }

  submit(values);
}

function handleResetPasswordSubmit(values: ResetPasswordForm, setError: UseFormSetError<ResetPasswordForm>, submit: (values: ResetPasswordForm) => void, t: (key: string) => string) {
  if (values.password.trim().length < 8) {
    setError("password", { message: t("admin.users.passwordRequired") });
    return;
  }

  submit(values);
}

function Field({ label, registration, type = "text", error }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string; error?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type={type} {...registration} />{error ? <p className="text-sm text-destructive">{error}</p> : null}</div>;
}

function Select({ label, registration, options, disabled = false, error }: Readonly<{ label: string; registration: UseFormRegisterReturn; options: Array<{ value: string; label: string }>; disabled?: boolean; error?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm disabled:cursor-not-allowed disabled:opacity-60" disabled={disabled} {...registration}><option value="">-</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>{error ? <p className="text-sm text-destructive">{error}</p> : null}</div>;
}
