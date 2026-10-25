"use client";

import { QueryClient, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useAppLocale } from "@/components/providers/locale-provider";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { confirmPurchase, getPurchase, purchasesQueryKeys, voidPurchase } from "@/features/purchases/api/purchases-api";
import { formatDate, formatDateTime, formatMoney, formatNumber, PurchaseStatusBadge } from "@/features/purchases/components/purchase-shared";
import { canReadPurchases } from "@/features/purchases/lib/purchase-permissions";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import type { PurchaseDetail } from "@/features/purchases/types/purchase-types";
import { formatApiErrorMessage } from "@/lib/api/errors";

export function PurchaseDetailScreen({ purchaseId }: Readonly<{ purchaseId: string }>) {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [isVoidOpen, setIsVoidOpen] = useState(false);
  const [voidReason, setVoidReason] = useState("");
  const canView = canReadPurchases();
  const purchaseQuery = useQuery({ queryKey: purchasesQueryKeys.detail(purchaseId), queryFn: () => getPurchase(purchaseId), enabled: canView });
  const purchase = purchaseQuery.data;
  const canConfirm = canAny(["purchases.confirm"]);
  const canVoid = canAny(["purchases.void"]);

  const confirmMutation = useMutation({
    mutationFn: () => confirmPurchase(purchaseId),
    onSuccess: () => {
      toast.success(t("purchases.messages.confirmed"));
      invalidatePurchaseImpact(queryClient);
    },
  });
  const voidMutation = useMutation({
    mutationFn: () => voidPurchase(purchaseId, voidReason),
    onSuccess: () => {
      toast.success(t("purchases.messages.voided"));
      setIsVoidOpen(false);
      setVoidReason("");
      invalidatePurchaseImpact(queryClient);
    },
  });

  const columns = useMemo<ColumnDef<PurchaseDetail>[]>(() => [
    { header: t("purchases.fields.product"), accessorKey: "productName" },
    { header: t("purchases.fields.presentation"), accessorKey: "presentationName" },
    { header: t("purchases.fields.quantityPresentation"), cell: ({ row }) => formatNumber(row.original.quantityPresentation) },
    { header: t("purchases.fields.conversionFactor"), cell: ({ row }) => formatNumber(row.original.conversionFactor) },
    { header: t("purchases.fields.quantityBase"), cell: ({ row }) => formatNumber(row.original.quantityBase) },
    { header: t("purchases.fields.unitCostPresentation"), cell: ({ row }) => formatMoney(row.original.unitCostPresentation) },
    { header: t("purchases.fields.unitCostBase"), cell: ({ row }) => formatMoney(row.original.unitCostBase) },
    { header: t("purchases.fields.taxAmount"), cell: ({ row }) => formatMoney(row.original.taxAmount) },
    { header: t("purchases.fields.totalAmount"), cell: ({ row }) => formatMoney(row.original.totalAmount) },
    { header: t("purchases.fields.lotNumber"), cell: ({ row }) => row.original.lotNumber ?? "-" },
    { header: t("purchases.fields.expirationDate"), cell: ({ row }) => row.original.expirationDate ? formatDate(row.original.expirationDate) : "-" },
  ], [t]);

  if (!canView) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  if (purchaseQuery.isLoading) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("admin.states.loading")}</CardContent></Card>;
  }

  if (purchaseQuery.error || !purchase) {
    return <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(purchaseQuery.error, t)}</CardContent></Card>;
  }

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.12),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.32))]">
        <CardContent className="grid gap-5 p-5 lg:grid-cols-[1fr_320px]">
          <div>
            <div className="mb-4 flex flex-wrap items-center gap-3">
              <p className="font-mono text-lg font-black tracking-[-0.04em]">{purchase.purchaseNumber}</p>
              <PurchaseStatusBadge status={purchase.status} label={t(`purchases.status.${purchase.status}`)} />
            </div>
            <div className="grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
              <Info label={t("purchases.fields.supplier")} value={purchase.supplierName} />
              <Info label={t("purchases.fields.branch")} value={purchase.branchName} />
              <Info label={t("purchases.fields.purchaseDate")} value={formatDate(purchase.purchaseDate)} />
              <Info label={t("purchases.fields.supplierInvoiceNumber")} value={purchase.supplierInvoiceNumber ?? "-"} />
              <Info label={t("purchases.fields.paymentMethod")} value={t(`purchases.paymentMethods.${purchase.paymentMethod}`)} />
              <Info label={t("purchases.fields.paymentType")} value={t(`purchases.paymentTypes.${purchase.paymentType}`)} />
              <Info label={t("purchases.fields.createdAt")} value={formatDateTime(purchase.createdAt, purchase.timezoneSnapshot ?? timeZone, locale)} />
              <Info label={t("purchases.fields.confirmedAt")} value={formatDateTime(purchase.confirmedAt, purchase.timezoneSnapshot ?? timeZone, locale)} />
              <Info label={t("purchases.fields.voidedAt")} value={formatDateTime(purchase.voidedAt, purchase.timezoneSnapshot ?? timeZone, locale)} />
              <Info label={t("purchases.fields.createdBy")} value={displayUser(purchase.createdByUsername, purchase.createdBy)} />
              <Info label={t("purchases.fields.confirmedBy")} value={displayUser(purchase.confirmedByUsername, purchase.confirmedBy)} />
              <Info label={t("purchases.fields.voidedBy")} value={displayUser(purchase.voidedByUsername, purchase.voidedBy)} />
            </div>
            {purchase.notes ? <p className="mt-4 rounded-2xl border bg-background/70 p-3 text-sm text-muted-foreground">{purchase.notes}</p> : null}
            {purchase.voidReason ? <p className="mt-4 rounded-2xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-800 dark:border-rose-800 dark:bg-rose-950/40 dark:text-rose-100">{purchase.voidReason}</p> : null}
          </div>
          <div className="rounded-3xl border bg-background/75 p-4 text-sm shadow-inner">
            <SummaryRow label={t("purchases.fields.subtotal")} value={formatMoney(purchase.subtotal)} />
            <SummaryRow label={t("purchases.fields.taxAmount")} value={formatMoney(purchase.taxAmount)} />
            <SummaryRow label={t("purchases.fields.discountAmount")} value={formatMoney(purchase.discountAmount)} />
            <div className="mt-3 border-t pt-3"><SummaryRow strong label={t("purchases.fields.totalAmount")} value={formatMoney(purchase.totalAmount)} /></div>
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" onClick={() => router.push("/purchases")}>{t("purchases.actions.back")}</Button>
        {canConfirm && purchase.status !== "CONFIRMED" && purchase.status !== "VOIDED" ? <ConfirmDialog trigger={<Button disabled={confirmMutation.isPending}>{t("purchases.actions.confirm")}</Button>} title={t("purchases.confirm.title")} description={t("purchases.confirm.description")} confirmLabel={t("purchases.actions.confirm")} cancelLabel={t("admin.actions.cancel")} onConfirm={() => confirmMutation.mutate()} /> : null}
        {canVoid && purchase.status !== "VOIDED" ? <Button className="border border-rose-300 bg-rose-600 text-white hover:bg-rose-700" type="button" disabled={voidMutation.isPending} onClick={() => setIsVoidOpen(true)}>{t("purchases.actions.void")}</Button> : null}
      </div>

      {confirmMutation.error || voidMutation.error ? <Card><CardContent className="p-6 text-sm text-destructive">{firstErrorMessage(t, confirmMutation.error, voidMutation.error) ?? t("admin.states.error")}</CardContent></Card> : null}

      <section className="space-y-3">
        <h2 className="text-xl font-black tracking-[-0.04em]">{t("purchases.sections.details")}</h2>
        <AdminTable columns={columns} data={purchase.details} />
      </section>

      <FormDialog open={isVoidOpen} onOpenChange={setIsVoidOpen} title={t("purchases.void.title")} description={t("purchases.void.description")}>
        <div className="space-y-4">
          <div className="space-y-2"><Label>{t("purchases.fields.voidReason")}</Label><Input value={voidReason} onChange={(event) => setVoidReason(event.target.value)} /></div>
          <div className="flex flex-wrap gap-2">
            <Button className="border border-rose-300 bg-rose-600 text-white hover:bg-rose-700" type="button" disabled={voidReason.trim().length === 0 || voidMutation.isPending} onClick={() => voidMutation.mutate()}>{t("purchases.actions.void")}</Button>
            <Button type="button" variant="outline" onClick={() => setIsVoidOpen(false)}>{t("admin.actions.cancel")}</Button>
          </div>
        </div>
      </FormDialog>
    </div>
  );
}

function invalidatePurchaseImpact(queryClient: QueryClient) {
  queryClient.invalidateQueries({ queryKey: ["purchases"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "balances"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "layers"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "movements"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "kardex"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "summary"] });
}

function Info({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="rounded-2xl border bg-background/60 p-3"><p className="text-xs font-bold uppercase tracking-[0.14em] text-muted-foreground">{label}</p><p className="mt-1 font-semibold">{value}</p></div>;
}

function SummaryRow({ label, value, strong }: Readonly<{ label: string; value: string; strong?: boolean }>) {
  return <div className={`flex items-center justify-between gap-3 ${strong ? "text-lg font-black" : ""}`}><span className="text-muted-foreground">{label}</span><span>{value}</span></div>;
}

function firstErrorMessage(t: (key: string, values?: Record<string, string>) => string, ...errors: unknown[]) {
  const error = errors.find(Boolean);
  return formatErrorMessage(error, t);
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatApiErrorMessage(error, t);
}

function displayUser(username: string | null | undefined, userId: string | null | undefined) {
  return username ?? userId?.slice(0, 8) ?? "-";
}
