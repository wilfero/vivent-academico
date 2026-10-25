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
import { Card, CardContent } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { createPresentation, deactivatePresentation, listGlobalPresentations, listProducts, lookupBarcode, updatePresentation, type PresentationPayload } from "@/features/admin/catalog/shared/catalog-api";
import type { Product, ProductPresentation } from "@/features/admin/catalog/shared/catalog-types";
import { can } from "@/features/admin/catalog/shared/catalog-permissions";

const presentationSchema = z.object({ productId: z.string().min(1), name: z.string().min(1), barcode: z.string().nullable(), conversionFactor: z.number().positive(), purchasePriceSuggested: z.number().min(0), salePrice: z.number().min(0), defaultPurchase: z.boolean(), defaultSale: z.boolean(), active: z.boolean() });
type PresentationForm = z.infer<typeof presentationSchema>;
type PresentationRow = ProductPresentation & { product?: Product };
const defaultPresentationValues: PresentationForm = { productId: "", name: "", barcode: "", conversionFactor: 1, purchasePriceSuggested: 0, salePrice: 0, defaultPurchase: false, defaultSale: false, active: true };

export function PresentationsScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<PresentationRow | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [productFilter, setProductFilter] = useState("");
  const [barcode, setBarcode] = useState("");
  const productsQuery = useQuery({ queryKey: ["catalog", "products", "presentations"], queryFn: () => listProducts() });
  const presentationsQuery = useQuery({ queryKey: ["catalog", "presentations", query, productFilter], queryFn: () => listGlobalPresentations(query, productFilter) });
  const barcodeQuery = useQuery({ queryKey: ["catalog", "barcode", barcode], queryFn: () => lookupBarcode(barcode), enabled: false, retry: false });
  const form = useForm<PresentationForm>({ resolver: zodResolver(presentationSchema), defaultValues: defaultPresentationValues });

  const rows = useMemo<PresentationRow[]>(() => (presentationsQuery.data?.items ?? []).map((presentation) => ({ ...presentation, product: productsQuery.data?.items.find((product) => product.id === presentation.productId) })), [presentationsQuery.data?.items, productsQuery.data?.items]);
  useEffect(() => { if (!editing) return; form.reset({ productId: editing.productId, name: editing.name, barcode: editing.barcode ?? "", conversionFactor: Number(editing.conversionFactor), purchasePriceSuggested: Number(editing.purchasePriceSuggested), salePrice: Number(editing.salePrice), defaultPurchase: editing.defaultPurchase, defaultSale: editing.defaultSale, active: editing.active }); }, [editing, form]);
  const saveMutation = useMutation({ mutationFn: (values: PresentationForm) => { const payload: PresentationPayload = { ...values, barcode: values.barcode || null }; return editing ? updatePresentation(values.productId, editing.id, payload) : createPresentation(values.productId, payload); }, onSuccess: () => { toast.success(t("admin.states.saved")); setIsFormOpen(false); setEditing(null); form.reset(defaultPresentationValues); queryClient.invalidateQueries({ queryKey: ["catalog", "presentations"] }); } });
  const deactivateMutation = useMutation({ mutationFn: (row: PresentationRow) => deactivatePresentation(row.productId, row.id), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["catalog", "presentations"] }) });
  const columns = useMemo<ColumnDef<PresentationRow>[]>(() => [
    { header: t("navigation.products"), cell: ({ row }) => row.original.product?.name ?? row.original.productId },
    { header: t("admin.fields.name"), accessorKey: "name" },
    { header: t("catalog.fields.barcode"), accessorKey: "barcode" },
    { header: t("catalog.fields.conversionFactor"), cell: ({ row }) => `1 ${row.original.name} = ${row.original.conversionFactor} ${row.original.product?.baseUnitCode ?? "base"}` },
    { header: t("catalog.fields.salePrice"), accessorKey: "salePrice" },
    { header: t("admin.fields.status"), cell: ({ row }) => row.original.active ? t("admin.status.active") : t("admin.status.inactive") },
    { header: t("admin.fields.actions"), cell: ({ row }) => <div className="flex gap-2">{can("product_presentations.update") ? <Button variant="outline" onClick={() => { setEditing(row.original); setIsFormOpen(true); }}>{t("admin.actions.edit")}</Button> : null}{can("product_presentations.deactivate") && row.original.active ? <ConfirmDialog trigger={<Button variant="outline">{t("admin.actions.deactivate")}</Button>} title={t("catalog.deactivateTitle")} description={t("catalog.confirmDeactivate")} confirmLabel={t("admin.actions.deactivate")} cancelLabel={t("admin.actions.cancel")} onConfirm={() => deactivateMutation.mutate(row.original)} /> : null}</div> },
  ], [deactivateMutation, t]);

  return <AdminPage title={t("catalog.presentations.title")} description={t("catalog.presentations.description")} isLoading={productsQuery.isLoading || presentationsQuery.isLoading} error={productsQuery.error ?? presentationsQuery.error ?? saveMutation.error ?? deactivateMutation.error ?? barcodeQuery.error}>
    <div className="flex flex-col gap-2 md:flex-row md:max-w-4xl"><Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t("catalog.search")} /><select className="h-11 rounded-xl border bg-background/75 px-3 text-sm" value={productFilter} onChange={(event) => setProductFilter(event.target.value)}><option value="">{t("catalog.allProducts")}</option>{productsQuery.data?.items.map((product) => <option key={product.id} value={product.id}>{product.name}</option>)}</select>{can("product_presentations.create") ? <Button type="button" onClick={() => { setEditing(null); form.reset(defaultPresentationValues); setIsFormOpen(true); }}>{t("admin.actions.create")}</Button> : null}</div>
    <Card><CardContent className="flex flex-col gap-3 p-6 md:flex-row"><Input value={barcode} onChange={(event) => setBarcode(event.target.value)} placeholder={t("catalog.fields.barcode")} /><Button type="button" variant="outline" onClick={() => barcodeQuery.refetch()}>{t("catalog.barcodeLookup")}</Button>{barcodeQuery.data ? <p className="text-sm text-muted-foreground">{barcodeQuery.data.product.name} / {barcodeQuery.data.presentation.name}: 1 = {barcodeQuery.data.presentation.conversionFactor} {barcodeQuery.data.product.baseUnitCode}</p> : null}</CardContent></Card>
    <FormDialog open={isFormOpen} onOpenChange={(open) => { setIsFormOpen(open); if (!open) { setEditing(null); form.reset(defaultPresentationValues); } }} title={editing ? t("admin.actions.update") : t("admin.actions.create")} description={t("catalog.presentations.description")}>
      <form className="grid gap-4 md:grid-cols-3" onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))}>
      <Select label={t("navigation.products")} registration={form.register("productId")} options={productsQuery.data?.items.map((product) => ({ value: product.id, label: `${product.name} (${product.baseUnitCode})` })) ?? []} />
      <Field label={t("admin.fields.name")} registration={form.register("name")} />
      <Field label={t("catalog.fields.barcode")} registration={form.register("barcode")} />
      <Field label={t("catalog.fields.conversionFactor")} registration={form.register("conversionFactor", { valueAsNumber: true })} type="number" />
      <Field label={t("catalog.fields.purchasePriceSuggested")} registration={form.register("purchasePriceSuggested", { valueAsNumber: true })} type="number" />
      <Field label={t("catalog.fields.salePrice")} registration={form.register("salePrice", { valueAsNumber: true })} type="number" />
      <div className="grid gap-2 text-sm md:col-span-3"><label><input type="checkbox" {...form.register("defaultPurchase")} /> {t("catalog.fields.defaultPurchase")}</label><label><input type="checkbox" {...form.register("defaultSale")} /> {t("catalog.fields.defaultSale")}</label><label><input type="checkbox" {...form.register("active")} /> {t("admin.fields.active")}</label></div>
      <div className="flex gap-2 md:col-span-3"><Button disabled={saveMutation.isPending} type="submit">{editing ? t("admin.actions.update") : t("admin.actions.create")}</Button><Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>{t("admin.actions.cancel")}</Button></div>
      </form>
    </FormDialog>
    <AdminTable columns={columns} data={rows} />
  </AdminPage>;
}

function Field({ label, registration, type = "text" }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string }>) { return <div className="space-y-2"><Label>{label}</Label><Input type={type} step="0.000001" {...registration} /></div>; }
function Select({ label, registration, options }: Readonly<{ label: string; registration: UseFormRegisterReturn; options: Array<{ value: string; label: string }> }>) { return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" {...registration}><option value="">-</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>; }
