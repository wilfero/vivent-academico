"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useEffect, useMemo } from "react";
import { useFieldArray, useForm, useWatch, type FieldErrors, type UseFormRegisterReturn } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { listGlobalPresentations, listProducts } from "@/features/admin/catalog/shared/catalog-api";
import { listBranches } from "@/features/admin/shared/admin-api";
import { withSessionBranchFallback } from "@/features/auth/lib/session-branch";
import { createPurchase, listSuppliers } from "@/features/purchases/api/purchases-api";
import { defaultPurchaseValues, purchaseSchema, toPurchasePayload, type PurchaseForm, type PurchaseFormInput } from "@/features/purchases/schemas/purchase-schema";
import { formatMoney, formatNumber } from "@/features/purchases/components/purchase-shared";
import { canCreatePurchases, canReadPurchaseBranches, canReadPurchasePresentations, canReadPurchaseProducts, canReadPurchaseSuppliers } from "@/features/purchases/lib/purchase-permissions";
import { formatPermissionError } from "@/lib/api/errors";

const paymentMethods = ["CASH", "CARD", "TRANSFER", "CHECK", "OTHER"] as const;
const paymentTypes = ["CASH", "CREDIT"] as const;

export function PurchaseFormScreen() {
  const t = useTranslations();
  const router = useRouter();
  const canCreate = canCreatePurchases();
  const canReadBranches = canReadPurchaseBranches();
  const canReadSuppliers = canReadPurchaseSuppliers();
  const canReadProducts = canReadPurchaseProducts();
  const canReadPresentations = canReadPurchasePresentations();
  const branchesQuery = useQuery({ queryKey: ["purchases", "branches"], queryFn: () => listBranches(), enabled: canCreate && canReadBranches });
  const suppliersQuery = useQuery({ queryKey: ["purchases", "suppliers", "active"], queryFn: () => listSuppliers("", true), enabled: canCreate && canReadSuppliers });
  const productsQuery = useQuery({ queryKey: ["purchases", "products"], queryFn: () => listProducts("", "", true), enabled: canCreate && canReadProducts });
  const presentationsQuery = useQuery({ queryKey: ["purchases", "presentations"], queryFn: () => listGlobalPresentations("", "", true), enabled: canCreate && canReadPresentations });

  const form = useForm<PurchaseFormInput, unknown, PurchaseForm>({ resolver: zodResolver(purchaseSchema), defaultValues: defaultPurchaseValues, reValidateMode: "onBlur" });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: "details" });
  const watchedDetails = useWatch({ control: form.control, name: "details" });
  const discountAmount = Number(useWatch({ control: form.control, name: "discountAmount" }) ?? 0);
  const details = useMemo(() => watchedDetails ?? [], [watchedDetails]);
  const branchOptions = useMemo(() => withSessionBranchFallback((branchesQuery.data?.items ?? []).map((branch) => ({ value: branch.id, label: branch.name }))), [branchesQuery.data?.items]);
  const productById = useMemo(() => new Map((productsQuery.data?.items ?? []).map((product) => [product.id, product])), [productsQuery.data?.items]);
  const presentationById = useMemo(() => new Map((presentationsQuery.data?.items ?? []).map((presentation) => [presentation.id, presentation])), [presentationsQuery.data?.items]);
  const totals = useMemo(() => calculateDraftTotals(details, discountAmount), [details, discountAmount]);
  const error = (canReadBranches ? branchesQuery.error : null) ?? (canReadSuppliers ? suppliersQuery.error : null) ?? (canReadProducts ? productsQuery.error : null) ?? (canReadPresentations ? presentationsQuery.error : null);

  const createMutation = useMutation({
    mutationFn: (values: PurchaseForm) => createPurchase(toPurchasePayload(values)),
    onSuccess: (purchase) => {
      toast.success(t("purchases.messages.created"));
      router.push(`/purchases/${purchase.id}`);
    },
  });

  useEffect(() => {
    if (branchOptions.length === 1 && !form.getValues("branchId")) {
      form.setValue("branchId", branchOptions[0].value, { shouldValidate: true });
    }
  }, [branchOptions, form]);

  if (!canCreate) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  return (
    <form className="space-y-6" onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}>
      {error ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(error, t)}</CardContent></Card> : null}
      {createMutation.error ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(createMutation.error, t)}</CardContent></Card> : null}

      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.12),transparent_32%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.32))]">
        <CardContent className="grid gap-4 p-5 md:grid-cols-4">
          <SelectField label={t("purchases.fields.branch")} registration={form.register("branchId")} options={branchOptions} error={form.formState.errors.branchId?.message} />
          <SelectField label={t("purchases.fields.supplier")} registration={form.register("supplierId")} options={(suppliersQuery.data?.items ?? []).map((supplier) => ({ value: supplier.id, label: supplier.name }))} error={form.formState.errors.supplierId?.message} />
          <Field label={t("purchases.fields.purchaseDate")} registration={form.register("purchaseDate")} type="date" error={form.formState.errors.purchaseDate?.message} />
          <SelectField label={t("purchases.fields.status")} registration={form.register("status")} options={["DRAFT", "PENDING_APPROVAL"].map((status) => ({ value: status, label: t(`purchases.status.${status}`) }))} />
          <Field label={t("purchases.fields.purchaseNumber")} registration={form.register("purchaseNumber")} placeholder={t("purchases.placeholders.autoNumber")} />
          <Field label={t("purchases.fields.supplierInvoiceNumber")} registration={form.register("supplierInvoiceNumber")} />
          <SelectField label={t("purchases.fields.paymentMethod")} registration={form.register("paymentMethod")} options={paymentMethods.map((method) => ({ value: method, label: t(`purchases.paymentMethods.${method}`) }))} />
          <SelectField label={t("purchases.fields.paymentType")} registration={form.register("paymentType")} options={paymentTypes.map((type) => ({ value: type, label: t(`purchases.paymentTypes.${type}`) }))} />
        </CardContent>
      </Card>

      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-black tracking-[-0.04em]">{t("purchases.sections.details")}</h2>
            <p className="text-sm text-muted-foreground">{t("purchases.help.details")}</p>
          </div>
          <Button type="button" variant="outline" onClick={() => append({ presentationId: "", quantityPresentation: 1, unitCostPresentation: 0, taxAmount: 0, expirationDate: "", lotNumber: "" })}>
            <Plus className="mr-2 h-4 w-4" aria-hidden="true" />{t("purchases.actions.addLine")}
          </Button>
        </div>
        <div className="grid gap-4">
          {fields.map((field, index) => {
            const selectedPresentation = presentationById.get(details?.[index]?.presentationId ?? "");
            const selectedProduct = selectedPresentation ? productById.get(selectedPresentation.productId) : null;
            const quantity = Number(details?.[index]?.quantityPresentation ?? 0);
            const cost = Number(details?.[index]?.unitCostPresentation ?? 0);
            const tax = Number(details?.[index]?.taxAmount ?? 0);

            return (
              <Card key={field.id} className="border-dashed">
                <CardContent className="grid gap-4 p-4 md:grid-cols-12">
                  <div className="md:col-span-4">
                    <SelectField label={t("purchases.fields.presentation")} registration={form.register(`details.${index}.presentationId` as const)} options={(presentationsQuery.data?.items ?? []).map((presentation) => {
                      const product = productById.get(presentation.productId);
                      return { value: presentation.id, label: `${product?.name ?? presentation.productId.slice(0, 8)} · ${presentation.name}${presentation.barcode ? ` · ${presentation.barcode}` : ""}` };
                    })} error={lineError(form.formState.errors, index, "presentationId")} />
                  </div>
                  <Field className="md:col-span-2" label={t("purchases.fields.quantityPresentation")} registration={form.register(`details.${index}.quantityPresentation` as const, { valueAsNumber: true })} type="number" error={lineError(form.formState.errors, index, "quantityPresentation")} />
                  <Field className="md:col-span-2" label={t("purchases.fields.unitCostPresentation")} registration={form.register(`details.${index}.unitCostPresentation` as const, { valueAsNumber: true })} type="number" error={lineError(form.formState.errors, index, "unitCostPresentation")} />
                  <Field className="md:col-span-2" label={t("purchases.fields.taxAmount")} registration={form.register(`details.${index}.taxAmount` as const, { valueAsNumber: true })} type="number" />
                  <div className="flex items-end md:col-span-2"><Button className="w-full" type="button" variant="outline" disabled={fields.length === 1} onClick={() => remove(index)}><Trash2 className="mr-2 h-4 w-4" aria-hidden="true" />{t("admin.actions.deactivate")}</Button></div>
                  <Field className="md:col-span-2" label={t("purchases.fields.expirationDate")} registration={form.register(`details.${index}.expirationDate` as const)} type="date" />
                  <Field className="md:col-span-2" label={t("purchases.fields.lotNumber")} registration={form.register(`details.${index}.lotNumber` as const)} />
                  <div className="rounded-2xl border bg-muted/35 p-3 text-sm md:col-span-8">
                    <p className="font-semibold">{selectedProduct?.name ?? t("purchases.placeholders.selectPresentation")}</p>
                    <p className="mt-1 text-muted-foreground">{t("purchases.fields.conversionFactor")}: {selectedPresentation ? formatNumber(selectedPresentation.conversionFactor) : "-"}</p>
                    <p className="mt-1 text-muted-foreground">{t("purchases.fields.quantityBase")}: {selectedPresentation ? formatNumber(quantity * selectedPresentation.conversionFactor) : "-"}</p>
                    <p className="mt-1 font-bold">{t("purchases.fields.lineTotal")}: {formatMoney(quantity * cost + tax)}</p>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      <Card>
        <CardContent className="grid gap-4 p-5 md:grid-cols-[1fr_280px] md:items-end">
          <div className="space-y-4">
            <Field label={t("purchases.fields.discountAmount")} registration={form.register("discountAmount", { valueAsNumber: true })} type="number" error={form.formState.errors.discountAmount?.message} />
            <div className="space-y-2"><Label>{t("purchases.fields.notes")}</Label><Input {...form.register("notes")} /></div>
          </div>
          <div className="rounded-3xl border bg-muted/35 p-4 text-sm">
            <SummaryRow label={t("purchases.fields.subtotal")} value={formatMoney(totals.subtotal)} />
            <SummaryRow label={t("purchases.fields.taxAmount")} value={formatMoney(totals.taxAmount)} />
            <SummaryRow label={t("purchases.fields.discountAmount")} value={formatMoney(discountAmount)} />
            <div className="mt-3 border-t pt-3"><SummaryRow strong label={t("purchases.fields.totalAmount")} value={formatMoney(totals.totalAmount)} /></div>
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-2">
        <Button disabled={createMutation.isPending} type="submit">{t("purchases.actions.create")}</Button>
        <Button type="button" variant="outline" onClick={() => router.push("/purchases")}>{t("admin.actions.cancel")}</Button>
      </div>
    </form>
  );
}

function SelectField({ label, registration, options, error }: Readonly<{ label: string; registration: UseFormRegisterReturn; options: Array<{ value: string; label: string }>; error?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" {...registration}><option value="">-</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>{error ? <p className="text-xs text-destructive">{error}</p> : null}</div>;
}

function Field({ label, registration, type = "text", placeholder, error, className }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string; placeholder?: string; error?: string; className?: string }>) {
  return <div className={`space-y-2 ${className ?? ""}`}><Label>{label}</Label><Input type={type} step="0.000001" placeholder={placeholder} {...registration} />{error ? <p className="text-xs text-destructive">{error}</p> : null}</div>;
}

function SummaryRow({ label, value, strong }: Readonly<{ label: string; value: string; strong?: boolean }>) {
  return <div className={`flex items-center justify-between gap-3 ${strong ? "text-lg font-black" : ""}`}><span className="text-muted-foreground">{label}</span><span>{value}</span></div>;
}

function calculateDraftTotals(details: PurchaseFormInput["details"], discountAmount: number) {
  const subtotal = (details ?? []).reduce((total, detail) => total + Number(detail.quantityPresentation || 0) * Number(detail.unitCostPresentation || 0), 0);
  const taxAmount = (details ?? []).reduce((total, detail) => total + Number(detail.taxAmount || 0), 0);
  return { subtotal, taxAmount, totalAmount: Math.max(subtotal + taxAmount - Number(discountAmount || 0), 0) };
}

function lineError(errors: FieldErrors<PurchaseFormInput>, index: number, key: "presentationId" | "quantityPresentation" | "unitCostPresentation") {
  const message = errors.details?.[index]?.[key]?.message;
  return typeof message === "string" ? message : undefined;
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}
