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
import { createBranch, listBranches, updateBranch, type BranchPayload } from "@/features/admin/shared/admin-api";
import type { Branch } from "@/features/admin/shared/admin-types";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AdminTable } from "@/features/admin/shared/admin-table";

const branchSchema = z.object({ name: z.string().min(1), code: z.string().min(1), address: z.string().nullable(), phone: z.string().nullable(), email: z.string().email().or(z.literal("")), active: z.boolean() });
type BranchForm = z.infer<typeof branchSchema>;

export function BranchesScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Branch | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const canUpdateSettings = canAny(["settings.update"]);
  const branchesQuery = useQuery({ queryKey: ["admin", "branches"], queryFn: () => listBranches() });
  const form = useForm<BranchForm>({ resolver: zodResolver(branchSchema), defaultValues: { name: "", code: "", address: "", phone: "", email: "", active: true } });

  useEffect(() => {
    if (!editing) return;
    form.reset({ name: editing.name, code: editing.code, address: editing.address ?? "", phone: editing.phone ?? "", email: editing.email ?? "", active: editing.active });
  }, [editing, form]);

  const mutation = useMutation({
    mutationFn: (values: BranchForm) => {
      const payload: BranchPayload = { ...values, address: values.address || null, phone: values.phone || null, email: values.email || null };
      return editing ? updateBranch(editing.id, payload) : createBranch(payload);
    },
    onSuccess: () => { setSuccessMessage(t("admin.states.saved")); setIsFormOpen(false); setEditing(null); form.reset({ name: "", code: "", address: "", phone: "", email: "", active: true }); queryClient.invalidateQueries({ queryKey: ["admin", "branches"] }); },
  });

  const columns = useMemo<ColumnDef<Branch>[]>(() => [
    { header: t("admin.fields.name"), accessorKey: "name" },
    { header: t("admin.fields.code"), accessorKey: "code" },
    { header: t("admin.fields.email"), accessorKey: "email" },
    { header: t("admin.fields.status"), cell: ({ row }) => row.original.active ? t("admin.status.active") : t("admin.status.inactive") },
    { header: t("admin.fields.actions"), cell: ({ row }) => canUpdateSettings ? <Button variant="outline" onClick={() => { setEditing(row.original); setIsFormOpen(true); }}>{t("admin.actions.edit")}</Button> : <span className="text-muted-foreground">-</span> },
  ], [canUpdateSettings, t]);

  return <AdminPage title={t("admin.branches.title")} description={t("admin.branches.description")} isLoading={branchesQuery.isLoading} error={branchesQuery.error ?? mutation.error} successMessage={successMessage}>
    {canUpdateSettings ? <div className="flex justify-end"><Button type="button" onClick={() => { setEditing(null); form.reset({ name: "", code: "", address: "", phone: "", email: "", active: true }); setIsFormOpen(true); }}>{t("admin.actions.create")}</Button></div> : null}
    <FormDialog open={isFormOpen} onOpenChange={(open) => { setIsFormOpen(open); if (!open) { setEditing(null); form.reset({ name: "", code: "", address: "", phone: "", email: "", active: true }); } }} title={editing ? t("admin.actions.update") : t("admin.actions.create")} description={t("admin.branches.description")}>
      <form className="grid gap-4 md:grid-cols-3" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
        <Field label={t("admin.fields.name")} registration={form.register("name")} />
        <Field label={t("admin.fields.code")} registration={form.register("code")} />
        <Field label={t("admin.fields.email")} registration={form.register("email")} />
        <Field label={t("admin.fields.phone")} registration={form.register("phone")} />
        <Field label={t("admin.fields.address")} registration={form.register("address")} />
        <label className="flex items-center gap-3 pt-6 text-sm"><input type="checkbox" {...form.register("active")} />{t("admin.fields.active")}</label>
        <div className="flex gap-2 md:col-span-3"><Button disabled={mutation.isPending} type="submit">{editing ? t("admin.actions.update") : t("admin.actions.create")}</Button><Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>{t("admin.actions.cancel")}</Button></div>
      </form>
    </FormDialog>
    <AdminTable columns={columns} data={branchesQuery.data?.items ?? []} />
  </AdminPage>;
}

function Field({ label, registration }: Readonly<{ label: string; registration: UseFormRegisterReturn }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input {...registration} /></div>;
}
