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
import { createProduct, deactivateProduct, listCategories, listProducts, listUnits, updateProduct, type ProductPayload } from "@/features/admin/catalog/shared/catalog-api";
import type { Product } from "@/features/admin/catalog/shared/catalog-types";
import { can } from "@/features/admin/catalog/shared/catalog-permissions";

const productSchema = z.object({ categoryId: z.string().nullable(), name: z.string().min(1), description: z.string().nullable(), baseUnitCode: z.string().min(1), sku: z.string().nullable(), minimumStock: z.number().min(0), stockable: z.boolean(), hasExpirationDate: z.boolean(), hasLotTracking: z.boolean(), hasSerialTracking: z.boolean(), active: z.boolean() });
type ProductForm = z.infer<typeof productSchema>;
const defaultProductValues: ProductForm = { categoryId: "", name: "", description: "", baseUnitCode: "", sku: "", minimumStock: 0, stockable: true, hasExpirationDate: false, hasLotTracking: false, hasSerialTracking: false, active: true };

export function ProductsScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [editing, setEditing] = useState<Product | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const productsQuery = useQuery({ queryKey: ["catalog", "products", query, categoryId], queryFn: () => listProducts(query, categoryId) });
  const categoriesQuery = useQuery({ queryKey: ["catalog", "categories", "active"], queryFn: () => listCategories("", true) });
  const unitsQuery = useQuery({ queryKey: ["catalog", "units"], queryFn: listUnits });
  const form = useForm<ProductForm>({ resolver: zodResolver(productSchema), defaultValues: defaultProductValues });

  useEffect(() => { if (!editing) return; form.reset({ categoryId: editing.category?.id ?? "", name: editing.name, description: editing.description ?? "", baseUnitCode: editing.baseUnitCode, sku: editing.sku ?? "", minimumStock: editing.minimumStock, stockable: editing.stockable, hasExpirationDate: editing.hasExpirationDate, hasLotTracking: editing.hasLotTracking, hasSerialTracking: editing.hasSerialTracking, active: editing.active }); }, [editing, form]);

  const saveMutation = useMutation({ mutationFn: (values: ProductForm) => { const payload: ProductPayload = { ...values, categoryId: values.categoryId || null, description: values.description || null, sku: values.sku || null }; return editing ? updateProduct(editing.id, payload) : createProduct(payload); }, onSuccess: () => { toast.success(t("admin.states.saved")); setIsFormOpen(false); setEditing(null); form.reset(defaultProductValues); queryClient.invalidateQueries({ queryKey: ["catalog", "products"] }); } });
  const deactivateMutation = useMutation({ mutationFn: deactivateProduct, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["catalog", "products"] }) });
  const columns = useMemo<ColumnDef<Product>[]>(() => [
    { header: t("admin.fields.name"), accessorKey: "name" },
    { header: "SKU", accessorKey: "sku" },
    { header: t("navigation.categories"), cell: ({ row }) => row.original.category?.name ?? "-" },
    { header: t("catalog.fields.baseUnit"), accessorKey: "baseUnitCode" },
    { header: t("catalog.fields.minimumStock"), accessorKey: "minimumStock" },
    { header: t("admin.fields.status"), cell: ({ row }) => row.original.active ? t("admin.status.active") : t("admin.status.inactive") },
    { header: t("admin.fields.actions"), cell: ({ row }) => <div className="flex gap-2">{can("products.update") ? <Button variant="outline" onClick={() => { setEditing(row.original); setIsFormOpen(true); }}>{t("admin.actions.edit")}</Button> : null}{can("products.delete") && row.original.active ? <ConfirmDialog trigger={<Button variant="outline">{t("admin.actions.deactivate")}</Button>} title={t("catalog.deactivateTitle")} description={t("catalog.confirmDeactivate")} confirmLabel={t("admin.actions.deactivate")} cancelLabel={t("admin.actions.cancel")} onConfirm={() => deactivateMutation.mutate(row.original.id)} /> : null}</div> },
  ], [deactivateMutation, t]);

  return <AdminPage title={t("catalog.products.title")} description={t("catalog.products.description")} isLoading={productsQuery.isLoading || categoriesQuery.isLoading || unitsQuery.isLoading} error={productsQuery.error ?? categoriesQuery.error ?? unitsQuery.error ?? saveMutation.error ?? deactivateMutation.error}>
    <div className="flex flex-col gap-2 md:flex-row md:max-w-4xl"><Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t("catalog.search")} /><select className="h-11 rounded-xl border bg-background/75 px-3 text-sm" value={categoryId} onChange={(event) => setCategoryId(event.target.value)}><option value="">{t("catalog.allCategories")}</option>{categoriesQuery.data?.items.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select>{can("products.create") ? <Button type="button" onClick={() => { setEditing(null); form.reset(defaultProductValues); setIsFormOpen(true); }}>{t("admin.actions.create")}</Button> : null}</div>
    <FormDialog open={isFormOpen} onOpenChange={(open) => { setIsFormOpen(open); if (!open) { setEditing(null); form.reset(defaultProductValues); } }} title={editing ? t("admin.actions.update") : t("admin.actions.create")} description={t("catalog.products.description")}>
      <form className="grid gap-4 md:grid-cols-3" onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))}>
      <Field label={t("admin.fields.name")} registration={form.register("name")} />
      <Field label="SKU" registration={form.register("sku")} />
      <Select label={t("navigation.categories")} registration={form.register("categoryId")} options={categoriesQuery.data?.items.map((category) => ({ value: category.id, label: category.name })) ?? []} />
      <Select label={t("catalog.fields.baseUnit")} registration={form.register("baseUnitCode")} options={unitsQuery.data?.map((unit) => ({ value: unit.code, label: `${unit.code} - ${unit.name}` })) ?? []} />
      <Field label={t("catalog.fields.minimumStock")} type="number" registration={form.register("minimumStock", { valueAsNumber: true })} />
      <Field label={t("admin.fields.description")} registration={form.register("description")} />
      <div className="grid gap-2 text-sm md:col-span-3"><label><input type="checkbox" {...form.register("stockable")} /> {t("catalog.fields.stockable")}</label><label><input type="checkbox" {...form.register("hasExpirationDate")} /> {t("catalog.fields.hasExpirationDate")}</label><label><input type="checkbox" {...form.register("hasLotTracking")} /> {t("catalog.fields.hasLotTracking")}</label><label><input type="checkbox" {...form.register("hasSerialTracking")} /> {t("catalog.fields.hasSerialTracking")}</label><label><input type="checkbox" {...form.register("active")} /> {t("admin.fields.active")}</label></div>
      <div className="flex gap-2 md:col-span-3"><Button disabled={saveMutation.isPending} type="submit">{editing ? t("admin.actions.update") : t("admin.actions.create")}</Button><Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>{t("admin.actions.cancel")}</Button></div>
      </form>
    </FormDialog>
    <AdminTable columns={columns} data={productsQuery.data?.items ?? []} />
  </AdminPage>;
}

function Field({ label, registration, type }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string }>) { return <div className="space-y-2"><Label>{label}</Label><Input type={type} {...registration} /></div>; }
function Select({ label, registration, options }: Readonly<{ label: string; registration: UseFormRegisterReturn; options: Array<{ value: string; label: string }> }>) { return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" {...registration}><option value="">-</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>; }
