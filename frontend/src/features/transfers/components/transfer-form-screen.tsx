"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useFieldArray, useForm, useWatch, type FieldErrors, type UseFormRegisterReturn } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { listGlobalPresentations, listProducts } from "@/features/admin/catalog/shared/catalog-api";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { listBranches } from "@/features/admin/shared/admin-api";
import { withSessionBranchFallback } from "@/features/auth/lib/session-branch";
import { listInventoryBalances } from "@/features/inventory/api/inventory-api";
import { createTransfer, listTransferSourceBranches, transfersQueryKeys } from "@/features/transfers/api/transfers-api";
import { formatTransferNumber } from "@/features/transfers/components/transfer-shared";
import { canLookupTransferPresentations, canLookupTransferProducts, canRequestTransfers } from "@/features/transfers/lib/transfer-permissions";
import { defaultTransferValues, toTransferPayload, transferCreateSchema, type TransferCreateForm, type TransferCreateInput } from "@/features/transfers/schemas/transfer-schema";
import { formatPermissionError } from "@/lib/api/errors";
import { getStoredSession } from "@/features/auth/lib/session-storage";

export function TransferFormScreen() {
  const t = useTranslations();
  const router = useRouter();
  const [presentationSearch, setPresentationSearch] = useState("");
  const canRequest = canRequestTransfers();
  const canLookupProducts = canLookupTransferProducts();
  const canLookupPresentations = canLookupTransferPresentations();
  const activeSession = getStoredSession();
  const canOperateAllBranches = activeSession?.roleCode === "COMPANY_ADMIN";
  const canReadBranches = canAny(["settings.view"]);

  const sourceBranchesQuery = useQuery({ queryKey: transfersQueryKeys.sourceBranches(), queryFn: listTransferSourceBranches, enabled: canRequest });
  const branchesQuery = useQuery({ queryKey: ["transfers", "branches"], queryFn: () => listBranches(), enabled: canRequest && canReadBranches });
  const productsQuery = useQuery({ queryKey: ["transfers", "products"], queryFn: () => listProducts("", "", true), enabled: canRequest && canLookupProducts });
  const presentationsQuery = useQuery({ queryKey: ["transfers", "presentations"], queryFn: () => listGlobalPresentations("", "", true), enabled: canRequest && canLookupPresentations });

  const form = useForm<TransferCreateInput, unknown, TransferCreateForm>({ resolver: zodResolver(transferCreateSchema), defaultValues: defaultTransferValues, reValidateMode: "onBlur" });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: "details" });
  const sourceBranchId = useWatch({ control: form.control, name: "sourceBranchId" }) ?? "";
  const watchedDetails = useWatch({ control: form.control, name: "details" });
  const details = useMemo(() => watchedDetails ?? [], [watchedDetails]);
  const productById = useMemo(() => new Map((productsQuery.data?.items ?? []).map((product) => [product.id, product])), [productsQuery.data?.items]);
  const presentationById = useMemo(() => new Map((presentationsQuery.data?.items ?? []).map((presentation) => [presentation.id, presentation])), [presentationsQuery.data?.items]);
  const filteredPresentations = useMemo(() => {
    const normalizedSearch = presentationSearch.trim().toLowerCase();
    const presentations = presentationsQuery.data?.items ?? [];
    if (!normalizedSearch) return presentations;
    return presentations.filter((presentation) => {
      const product = productById.get(presentation.productId);
      return [product?.name, product?.sku, presentation.name, presentation.barcode].filter(Boolean).some((value) => String(value).toLowerCase().includes(normalizedSearch));
    });
  }, [presentationSearch, presentationsQuery.data?.items, productById]);

  const balancesQuery = useQuery({
    queryKey: ["transfers", "inventory-balances", sourceBranchId],
    queryFn: () => listInventoryBalances({ branchId: sourceBranchId, locationId: "", productId: "", categoryId: "", query: "", movementType: "", fromDate: "", toDate: "", page: 1, pageSize: 100 }),
    enabled: canRequest && Boolean(sourceBranchId),
  });
  const availableByProductId = useMemo(() => new Map((balancesQuery.data?.items ?? []).map((balance) => [balance.productId, balance.quantityAvailable])), [balancesQuery.data?.items]);

  const createMutation = useMutation({
    mutationFn: (values: TransferCreateForm) => createTransfer(toTransferPayload(values)),
    onSuccess: (transfer) => {
      toast.success(t("transfers.messages.created"));
      router.push(`/transfers/${transfer.id}`);
    },
  });

  useEffect(() => {
    const sourceBranches = sourceBranchesQuery.data?.items ?? [];
    if (sourceBranches.length === 1 && !form.getValues("sourceBranchId")) {
      form.setValue("sourceBranchId", sourceBranches[0].id, { shouldValidate: true });
    }

    if (!canOperateAllBranches && activeSession?.branchId && form.getValues("destinationBranchId") !== activeSession.branchId) {
      form.setValue("destinationBranchId", activeSession.branchId, { shouldValidate: true });
    }
  }, [activeSession?.branchId, canOperateAllBranches, sourceBranchesQuery.data?.items, form]);

  if (!canRequest) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  const commonError = sourceBranchesQuery.error ?? (canReadBranches ? branchesQuery.error : null) ?? productsQuery.error ?? presentationsQuery.error ?? balancesQuery.error ?? createMutation.error;
  const sourceBranchOptions = sourceBranchesQuery.data?.items ?? [];
  const destinationBranchOptions = withSessionBranchFallback((branchesQuery.data?.items ?? []).map((branch) => ({ value: branch.id, label: `${branch.name} (${branch.code})` })));
  const destinationLocked = !canOperateAllBranches && Boolean(activeSession?.branchId);

  return (
    <form className="space-y-6" onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}>
      {commonError ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(commonError, t)}</CardContent></Card> : null}

      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.12),transparent_32%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.32))]">
        <CardContent className="grid gap-4 p-5 md:grid-cols-4">
          <SelectField label={t("transfers.fields.sourceBranch")} registration={form.register("sourceBranchId")} options={sourceBranchOptions.map((branch) => ({ value: branch.id, label: `${branch.name} (${branch.code})` }))} error={form.formState.errors.sourceBranchId?.message} />
          <SelectField label={t("transfers.fields.destinationBranch")} registration={form.register("destinationBranchId")} options={destinationBranchOptions} disabled={false} error={destinationLocked ? undefined : form.formState.errors.destinationBranchId?.message} />
          <Field label={t("transfers.fields.transferNumber")} registration={form.register("transferNumber")} placeholder={t("transfers.placeholders.autoNumber")} />
          <div className="space-y-2 md:col-span-4"><Label>{t("transfers.fields.notes")}</Label><Input {...form.register("notes")} /></div>
        </CardContent>
      </Card>

      <section className="space-y-3">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-xl font-black tracking-[-0.04em]">{t("transfers.sections.details")}</h2>
            <p className="text-sm text-muted-foreground">{t("transfers.help.stock")}</p>
          </div>
          <Button type="button" variant="outline" onClick={() => append({ presentationId: "", quantityRequestedPresentation: 1 })}><Plus className="mr-2 h-4 w-4" aria-hidden="true" />{t("transfers.actions.addLine")}</Button>
        </div>
        <div className="max-w-xl space-y-2"><Label>{t("transfers.fields.productSearch")}</Label><Input value={presentationSearch} onChange={(event) => setPresentationSearch(event.target.value)} placeholder={t("transfers.placeholders.searchPresentation")} /></div>
        {fields.length === 0 ? <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("transfers.emptyDetails")}</CardContent></Card> : null}
        <div className="grid gap-4">
          {fields.map((field, index) => {
            const detail = details[index];
            const presentation = presentationById.get(detail?.presentationId ?? "");
            const product = presentation ? productById.get(presentation.productId) : null;
            const available = product ? availableByProductId.get(product.id) : undefined;
            const requestedBase = Number(detail?.quantityRequestedPresentation || 0) * Number(presentation?.conversionFactor || 0);
            const lowStock = available !== undefined && requestedBase > available;
            return (
              <Card key={field.id} className={lowStock ? "border-amber-300 bg-amber-50/50 dark:border-amber-800 dark:bg-amber-950/20" : "border-dashed"}>
                <CardContent className="grid gap-4 p-4 md:grid-cols-12 md:items-end">
                  <div className="md:col-span-5">
                    <SelectField label={t("transfers.fields.presentation")} registration={form.register(`details.${index}.presentationId` as const)} options={filteredPresentations.map((item) => {
                      const itemProduct = productById.get(item.productId);
                      return { value: item.id, label: `${itemProduct?.name ?? item.productId.slice(0, 8)} · ${item.name}${item.barcode ? ` · ${item.barcode}` : ""}` };
                    })} error={lineError(form.formState.errors, index, "presentationId")} />
                  </div>
                  <Field className="md:col-span-2" label={t("transfers.fields.quantityRequestedPresentation")} registration={form.register(`details.${index}.quantityRequestedPresentation` as const, { valueAsNumber: true })} type="number" error={lineError(form.formState.errors, index, "quantityRequestedPresentation")} />
                  <div className="rounded-2xl border bg-muted/35 p-3 text-sm md:col-span-4">
                    <p className="font-semibold">{product?.name ?? t("transfers.placeholders.selectPresentation")}</p>
                    <p className="mt-1 text-muted-foreground">{t("transfers.fields.baseUnit")}: {product?.baseUnitCode ?? "-"}</p>
                    <p className="mt-1 text-muted-foreground">{t("transfers.fields.conversionFactor")}: {presentation ? formatTransferNumber(presentation.conversionFactor) : "-"}</p>
                    <p className="mt-1 text-muted-foreground">{t("transfers.fields.quantityRequestedBase")}: {presentation ? formatTransferNumber(requestedBase) : "-"}</p>
                    <p className={lowStock ? "mt-1 font-bold text-amber-700 dark:text-amber-300" : "mt-1 text-muted-foreground"}>{t("transfers.fields.available")}: {available === undefined ? t("transfers.placeholders.stockUnavailable") : formatTransferNumber(available)}</p>
                  </div>
                  <div className="md:col-span-1"><Button className="w-full" type="button" variant="outline" onClick={() => remove(index)}><Trash2 className="h-4 w-4" aria-hidden="true" /></Button></div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      <div className="flex flex-wrap gap-2">
        <Button disabled={createMutation.isPending} type="submit">{t("transfers.actions.create")}</Button>
        <Button type="button" variant="outline" onClick={() => router.push("/transfers")}>{t("admin.actions.cancel")}</Button>
      </div>
    </form>
  );
}

function SelectField({ label, registration, options, error, disabled = false }: Readonly<{ label: string; registration: UseFormRegisterReturn; options: Array<{ value: string; label: string }>; error?: string; disabled?: boolean }>) {
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm disabled:cursor-not-allowed disabled:opacity-70" disabled={disabled} {...registration}><option value="">-</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>{error ? <p className="text-xs text-destructive">{error}</p> : null}</div>;
}

function Field({ label, registration, type = "text", placeholder, error, className }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string; placeholder?: string; error?: string; className?: string }>) {
  return <div className={`space-y-2 ${className ?? ""}`}><Label>{label}</Label><Input type={type} step="0.000001" placeholder={placeholder} {...registration} />{error ? <p className="text-xs text-destructive">{error}</p> : null}</div>;
}

function lineError(errors: FieldErrors<TransferCreateInput>, index: number, key: "presentationId" | "quantityRequestedPresentation") {
  const message = errors.details?.[index]?.[key]?.message;
  return typeof message === "string" ? message : undefined;
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}
