"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useEffect, useMemo, useState } from "react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { createCategory, deactivateCategory, listCategories, updateCategory, type CategoryPayload } from "@/features/admin/catalog/shared/catalog-api";
import type { Category } from "@/features/admin/catalog/shared/catalog-types";
import { can } from "@/features/admin/catalog/shared/catalog-permissions";

const categorySchema = z.object({ parentCategoryId: z.string().nullable(), name: z.string().min(1), description: z.string().nullable(), active: z.boolean() });
type CategoryForm = z.infer<typeof categorySchema>;
const defaultCategoryValues: CategoryForm = { parentCategoryId: "", name: "", description: "", active: true };

export function CategoriesScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState<Category | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const categoriesQuery = useQuery({ queryKey: ["catalog", "categories", query], queryFn: () => listCategories(query) });
  const form = useForm<CategoryForm>({ resolver: zodResolver(categorySchema), defaultValues: defaultCategoryValues });

  useEffect(() => {
    if (!editing) return;
    form.reset({ parentCategoryId: editing.parentCategoryId ?? "", name: editing.name, description: editing.description ?? "", active: editing.active });
  }, [editing, form]);

  const saveMutation = useMutation({
    mutationFn: (values: CategoryForm) => {
      const payload: CategoryPayload = { parentCategoryId: values.parentCategoryId || null, name: values.name, description: values.description || null, active: values.active };
      return editing ? updateCategory(editing.id, payload) : createCategory(payload);
    },
    onSuccess: () => { toast.success(t("admin.states.saved")); setIsFormOpen(false); setEditing(null); form.reset(defaultCategoryValues); queryClient.invalidateQueries({ queryKey: ["catalog", "categories"] }); },
  });
  const deactivateMutation = useMutation({ mutationFn: deactivateCategory, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["catalog", "categories"] }) });
  const columns = useMemo<ColumnDef<Category>[]>(() => [
    { header: t("admin.fields.name"), accessorKey: "name" },
    { header: t("admin.fields.description"), accessorKey: "description" },
    { header: t("admin.fields.status"), cell: ({ row }) => row.original.active ? t("admin.status.active") : t("admin.status.inactive") },
    { header: t("admin.fields.actions"), cell: ({ row }) => <div className="flex gap-2">{can("product_categories.update") ? <Button variant="outline" onClick={() => { setEditing(row.original); setIsFormOpen(true); }}>{t("admin.actions.edit")}</Button> : null}{can("product_categories.deactivate") && row.original.active ? <ConfirmDialog trigger={<Button variant="outline">{t("admin.actions.deactivate")}</Button>} title={t("catalog.deactivateTitle")} description={t("catalog.confirmDeactivate")} confirmLabel={t("admin.actions.deactivate")} cancelLabel={t("admin.actions.cancel")} onConfirm={() => deactivateMutation.mutate(row.original.id)} /> : null}</div> },
  ], [deactivateMutation, t]);

  return <AdminPage title={t("catalog.categories.title")} description={t("catalog.categories.description")} isLoading={categoriesQuery.isLoading} error={categoriesQuery.error ?? saveMutation.error ?? deactivateMutation.error}>
    <div className="flex max-w-3xl flex-col gap-2 sm:flex-row"><Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t("catalog.search")} />{can("product_categories.create") ? <Button type="button" onClick={() => { setEditing(null); form.reset(defaultCategoryValues); setIsFormOpen(true); }}>{t("admin.actions.create")}</Button> : null}</div>
    <FormDialog open={isFormOpen} onOpenChange={(open) => { setIsFormOpen(open); if (!open) { setEditing(null); form.reset(defaultCategoryValues); } }} title={editing ? t("admin.actions.update") : t("admin.actions.create")} description={t("catalog.categories.description")}>
      <form className="grid gap-4 md:grid-cols-2" onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))}>
      <Field label={t("admin.fields.name")} registration={form.register("name")} />
      <Field label={t("admin.fields.description")} registration={form.register("description")} />
      <Select label={t("catalog.fields.parentCategory")} registration={form.register("parentCategoryId")} options={categoriesQuery.data?.items.filter((category) => category.id !== editing?.id).map((category) => ({ value: category.id, label: category.name })) ?? []} />
      <label className="flex items-center gap-3 pt-6 text-sm"><input type="checkbox" {...form.register("active")} />{t("admin.fields.active")}</label>
      <div className="flex gap-2 md:col-span-2"><Button disabled={saveMutation.isPending} type="submit">{editing ? t("admin.actions.update") : t("admin.actions.create")}</Button><Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>{t("admin.actions.cancel")}</Button></div>
      </form>
    </FormDialog>
    <AdminTable columns={columns} data={categoriesQuery.data?.items ?? []} />
  </AdminPage>;
}

function Field({ label, registration }: Readonly<{ label: string; registration: UseFormRegisterReturn }>) { return <div className="space-y-2"><Label>{label}</Label><Input {...registration} /></div>; }
function Select({ label, registration, options }: Readonly<{ label: string; registration: UseFormRegisterReturn; options: Array<{ value: string; label: string }> }>) { return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" {...registration}><option value="">-</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>; }
