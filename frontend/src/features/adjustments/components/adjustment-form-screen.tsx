"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMemo } from "react";
import { useFieldArray, useForm, useWatch, type UseFormRegisterReturn } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { listBranches } from "@/features/admin/shared/admin-api";
import { listGlobalPresentations, listProducts } from "@/features/admin/catalog/shared/catalog-api";
import type { Product, ProductPresentation } from "@/features/admin/catalog/shared/catalog-types";
import { createAdjustment } from "@/features/adjustments/api/adjustments-api";
import { adjustmentCreateSchema, adjustmentDirections, adjustmentTypes, toAdjustmentPayload, type AdjustmentCreateForm, type AdjustmentCreateInput } from "@/features/adjustments/schemas/adjustment-schema";
import { formatAdjustmentNumber } from "@/features/adjustments/components/adjustment-shared";
import { canCreateAdjustments, canLookupAdjustmentPresentations, canLookupAdjustmentProducts, canReadAdjustmentBranches, canReadAdjustmentFifoLayers, canReadAdjustments } from "@/features/adjustments/lib/adjustment-permissions";
import { listInventoryBalances, listInventoryLayers } from "@/features/inventory/api/inventory-api";
import { getStoredSession } from "@/features/auth/lib/session-storage";
import { formatPermissionError } from "@/lib/api/errors";

const emptyLine = { productId: "", presentationId: "", quantityPresentation: 1, unitCost: "", notes: "" };

export function AdjustmentFormScreen() {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const activeSession = getStoredSession();
  const canView = canReadAdjustments();
  const canCreate = canCreateAdjustments();
  const canReadBranches = canReadAdjustmentBranches();
  const canReadProducts = canLookupAdjustmentProducts();
  const canReadPresentations = canLookupAdjustmentPresentations();
  const canReadFifoLayers = canReadAdjustmentFifoLayers();
  const defaultBranchId = activeSession?.branchId ?? "";
  const form = useForm<AdjustmentCreateInput, unknown, AdjustmentCreateForm>({
    resolver: zodResolver(adjustmentCreateSchema),
    defaultValues: { branchId: defaultBranchId, adjustmentNumber: "", adjustmentType: "REGISTER_ERROR", direction: "IN", status: "DRAFT", notes: "", lines: [emptyLine] },
    reValidateMode: "onBlur",
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: "lines" });
  const branchId = useWatch({ control: form.control, name: "branchId" }) ?? "";
  const adjustmentType = useWatch({ control: form.control, name: "adjustmentType" }) ?? "REGISTER_ERROR";
  const direction = useWatch({ control: form.control, name: "direction" }) ?? "IN";
  const watchedLines = useWatch({ control: form.control, name: "lines" });
  const branchesQuery = useQuery({ queryKey: ["adjustments", "branches"], queryFn: () => listBranches(), enabled: canReadBranches && canCreate });
  const productsQuery = useQuery({ queryKey: ["adjustments", "products"], queryFn: () => listProducts("", "", true), enabled: canCreate && canReadProducts });
  const presentationsQuery = useQuery({ queryKey: ["adjustments", "presentations"], queryFn: () => listGlobalPresentations("", "", true), enabled: canCreate && canReadPresentations });
  const balancesQuery = useQuery({
    queryKey: ["adjustments", "balances", branchId],
    queryFn: () => listInventoryBalances({ branchId, locationId: "", productId: "", categoryId: "", query: "", movementType: "", fromDate: "", toDate: "", page: 1, pageSize: 100 }),
    enabled: canView && canCreate && Boolean(branchId),
  });
  const fifoLayersQuery = useQuery({
    queryKey: ["adjustments", "fifo-layers", branchId],
    queryFn: () => listInventoryLayers({ branchId, locationId: "", productId: "", categoryId: "", query: "", movementType: "", fromDate: "", toDate: "", page: 1, pageSize: 100, status: "OPEN" }),
    enabled: canCreate && canReadFifoLayers && Boolean(branchId),
  });
  const products = productsQuery.data?.items ?? [];
  const presentations = presentationsQuery.data?.items ?? [];
  const stockByProductId = useMemo(() => new Map((balancesQuery.data?.items ?? []).map((balance) => [balance.productId, balance.quantityAvailable])), [balancesQuery.data?.items]);
  const fifoCostByProductId = useMemo(() => {
    const layers = [...(fifoLayersQuery.data?.items ?? [])].sort((left, right) => new Date(left.receivedAt).getTime() - new Date(right.receivedAt).getTime());
    const costs = new Map<string, number>();
    for (const layer of layers) {
      if (layer.quantityRemaining > 0 && !costs.has(layer.productId)) {
        costs.set(layer.productId, layer.unitCost);
      }
    }
    return costs;
  }, [fifoLayersQuery.data?.items]);
  const mutation = useMutation({
    mutationFn: (values: AdjustmentCreateForm) => createAdjustment(toAdjustmentPayload(values)),
    onSuccess: (adjustment) => {
      toast.success(t("adjustments.messages.created"));
      queryClient.invalidateQueries({ queryKey: ["adjustments"] });
      queryClient.invalidateQueries({ queryKey: ["auditLogs"] });
      router.push(`/adjustments/${adjustment.id}`);
    },
  });

  if (!canCreate) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  const branchOptions = canReadBranches ? (branchesQuery.data?.items ?? []) : defaultBranchId ? [{ id: defaultBranchId, name: activeSession?.branchName ?? defaultBranchId, code: "", companyId: activeSession?.companyId ?? "", address: null, phone: null, email: null, active: true }] : [];
  const commonError = mutation.error ?? branchesQuery.error ?? productsQuery.error ?? presentationsQuery.error ?? balancesQuery.error ?? fifoLayersQuery.error;

  function submitWithStatus(status: "DRAFT" | "PENDING_APPROVAL") {
    form.setValue("status", status);
    void form.handleSubmit((values) => mutation.mutate(values))();
  }

  return (
    <form className="space-y-6" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.12),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.32))]">
        <CardContent className="grid gap-4 p-5 md:grid-cols-2">
          <Select label={t("adjustments.fields.branch")} value={branchId} disabled={!canReadBranches || branchOptions.length <= 1} onChange={(value) => form.setValue("branchId", value, { shouldValidate: true })} options={branchOptions.map((branch) => ({ value: branch.id, label: branch.name }))} placeholder={t("adjustments.filters.selectBranch")} />
          <Field label={t("adjustments.fields.adjustmentNumber")} registration={form.register("adjustmentNumber")} placeholder={t("adjustments.placeholders.autoNumber")} />
          <Select label={t("adjustments.fields.adjustmentType")} value={adjustmentType} onChange={(value) => form.setValue("adjustmentType", value as AdjustmentCreateForm["adjustmentType"], { shouldValidate: true })} options={adjustmentTypes.map((type) => ({ value: type, label: t(`adjustments.types.${type}`) }))} />
          <Select label={t("adjustments.fields.direction")} value={direction} onChange={(value) => form.setValue("direction", value as AdjustmentCreateForm["direction"], { shouldValidate: true })} options={adjustmentDirections.map((item) => ({ value: item, label: t(`adjustments.directions.${item}`) }))} />
          <div className="space-y-2 md:col-span-2">
            <Label>{t("adjustments.fields.reason")}</Label>
            <textarea className="min-h-24 w-full rounded-xl border bg-background/75 px-3 py-2 text-sm" {...form.register("notes")} />
            {form.formState.errors.notes ? <p className="text-sm text-destructive">{form.formState.errors.notes.message}</p> : null}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="space-y-4 p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div><h2 className="text-xl font-black tracking-[-0.04em]">{t("adjustments.sections.lines")}</h2><p className="text-sm text-muted-foreground">{t("adjustments.help.backendOwnsStock")}</p></div>
            <Button type="button" variant="outline" onClick={() => append(emptyLine)}><Plus className="mr-2 h-4 w-4" aria-hidden="true" />{t("adjustments.actions.addLine")}</Button>
          </div>
          {form.formState.errors.lines?.message ? <p className="text-sm text-destructive">{form.formState.errors.lines.message}</p> : null}
          <div className="grid gap-3">
            {fields.map((field, index) => {
              const productId = watchedLines?.[index]?.productId ?? "";
              const product = products.find((item) => item.id === productId) ?? null;
              const productPresentations = presentations.filter((presentation) => presentation.productId === productId);
              const selectedPresentation = presentations.find((presentation) => presentation.id === watchedLines?.[index]?.presentationId) ?? null;
              return <AdjustmentLineRow key={field.id} index={index} product={product} products={products} presentations={productPresentations} selectedPresentation={selectedPresentation} availableStock={stockByProductId.get(productId) ?? null} fifoUnitCost={fifoCostByProductId.get(productId) ?? null} canRemove={fields.length > 1} remove={remove} form={form} />;
            })}
          </div>
        </CardContent>
      </Card>

      {commonError ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(commonError, t)}</CardContent></Card> : null}

      <div className="flex flex-wrap justify-end gap-2">
        <Button type="button" variant="outline" onClick={() => router.push("/adjustments")}>{t("adjustments.actions.back")}</Button>
        <Button type="button" variant="outline" disabled={mutation.isPending} onClick={() => submitWithStatus("DRAFT")}>{t("adjustments.actions.saveDraft")}</Button>
        <Button type="button" disabled={mutation.isPending} onClick={() => submitWithStatus("PENDING_APPROVAL")}>{t("adjustments.actions.submit")}</Button>
      </div>
    </form>
  );
}

function AdjustmentLineRow({ index, product, products, presentations, selectedPresentation, availableStock, fifoUnitCost, canRemove, remove, form }: Readonly<{ index: number; product: Product | null; products: Product[]; presentations: ProductPresentation[]; selectedPresentation: ProductPresentation | null; availableStock: number | null; fifoUnitCost: number | null; canRemove: boolean; remove: (index: number) => void; form: ReturnType<typeof useForm<AdjustmentCreateInput, unknown, AdjustmentCreateForm>> }>) {
  const t = useTranslations();
  const line = useWatch({ control: form.control, name: `lines.${index}` });
  const suggestedUnitCost = fifoUnitCost;
  return (
    <div className="grid gap-3 rounded-2xl border bg-background/70 p-4 lg:grid-cols-[1.2fr_1fr_130px_130px_1fr_auto] lg:items-end">
      <Select label={t("adjustments.fields.product")} value={line?.productId ?? ""} onChange={(value) => { form.setValue(`lines.${index}.productId`, value, { shouldValidate: true }); form.setValue(`lines.${index}.presentationId`, "", { shouldValidate: true }); form.setValue(`lines.${index}.unitCost`, "", { shouldValidate: true }); }} options={products.map((item) => ({ value: item.id, label: `${item.name}${item.sku ? ` · ${item.sku}` : ""}` }))} placeholder={t("adjustments.filters.selectProduct")} />
      <Select label={t("adjustments.fields.presentation")} value={line?.presentationId ?? ""} onChange={(value) => { form.setValue(`lines.${index}.presentationId`, value, { shouldValidate: true }); if (fifoUnitCost !== null) form.setValue(`lines.${index}.unitCost`, String(fifoUnitCost), { shouldValidate: true }); }} options={presentations.map((presentation) => ({ value: presentation.id, label: `${presentation.name}${presentation.barcode ? ` · ${presentation.barcode}` : ""}` }))} placeholder={t("adjustments.filters.selectPresentation")} />
      <Field label={t("adjustments.fields.quantityPresentation")} registration={form.register(`lines.${index}.quantityPresentation`, { valueAsNumber: true })} type="number" />
      <Field label={t("adjustments.fields.unitCost")} registration={form.register(`lines.${index}.unitCost`)} type="number" />
      <div className="rounded-xl border bg-muted/35 px-3 py-2 text-sm">
        <p className="font-bold">{product?.baseUnitCode ?? "-"}</p>
        <p className="text-muted-foreground">{t("adjustments.fields.conversionFactor")}: {selectedPresentation ? formatAdjustmentNumber(selectedPresentation.conversionFactor) : "-"}</p>
        <p className="text-muted-foreground">{t("adjustments.fields.availableStock")}: {availableStock === null ? "-" : formatAdjustmentNumber(availableStock)}</p>
        <p className="text-muted-foreground">{t("adjustments.fields.suggestedUnitCost")}: {suggestedUnitCost === null ? "-" : formatAdjustmentNumber(suggestedUnitCost)}</p>
      </div>
      <Button className="h-11" type="button" variant="outline" disabled={!canRemove} onClick={() => remove(index)} aria-label={t("adjustments.actions.removeLine")}><Trash2 className="h-4 w-4" aria-hidden="true" /></Button>
      <div className="lg:col-span-6"><Field label={t("adjustments.fields.lineNotes")} registration={form.register(`lines.${index}.notes`)} /></div>
    </div>
  );
}

function Field({ label, registration, type = "text", placeholder }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string; placeholder?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type={type} step="0.000001" placeholder={placeholder} {...registration} /></div>;
}

function Select({ label, value, onChange, options, placeholder, disabled = false }: Readonly<{ label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }>; placeholder?: string; disabled?: boolean }>) {
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm disabled:cursor-not-allowed disabled:opacity-70" value={value} disabled={disabled} onChange={(event) => onChange(event.target.value)}><option value="">{placeholder ?? "-"}</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>;
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}
