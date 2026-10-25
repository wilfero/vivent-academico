"use client";

import { QueryClient, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { useAppLocale } from "@/components/providers/locale-provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { cancelSale, confirmSale, deleteDraftSale, getSale, salesQueryKeys } from "@/features/sales/api/sales-api";
import { SaleStatusBadge, formatDateTime, formatMoney, formatNumber } from "@/features/sales/components/sale-shared";
import { canCancelSales, canConfirmSales, canReadSales } from "@/features/sales/lib/sale-permissions";
import type { SaleDetail } from "@/features/sales/types/sale-types";
import { formatApiErrorMessage } from "@/lib/api/errors";

export function SaleDetailScreen({ saleId }: Readonly<{ saleId: string }>) {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { locale, timeZone } = useAppLocale();
  const [isCancelOpen, setIsCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelDisposition, setCancelDisposition] = useState("RETURN_TO_STOCK");
  const canView = canReadSales();
  const canConfirm = canConfirmSales();
  const canCancel = canCancelSales();
  const saleQuery = useQuery({ queryKey: salesQueryKeys.detail(saleId), queryFn: () => getSale(saleId), enabled: canView });
  const sale = saleQuery.data;

  const confirmMutation = useMutation({
    mutationFn: () => confirmSale(saleId),
    onSuccess: (confirmedSale) => {
      toast.success(t("sales.messages.confirmed"));
      invalidateSaleImpact(queryClient, confirmedSale.id);
    },
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelSale(saleId, cancelReason, cancelDisposition),
    onSuccess: (cancelledSale) => {
      toast.success(t("sales.messages.cancelled"));
      setIsCancelOpen(false);
      setCancelReason("");
      setCancelDisposition("RETURN_TO_STOCK");
      invalidateSaleImpact(queryClient, cancelledSale.id);
    },
  });

  const deleteDraftMutation = useMutation({
    mutationFn: () => deleteDraftSale(saleId),
    onSuccess: () => {
      toast.success(t("sales.messages.draftDeleted"));
      queryClient.invalidateQueries({ queryKey: salesQueryKeys.all });
      router.push("/sales");
    },
  });

  const columns = useMemo<ColumnDef<SaleDetail>[]>(() => [
    { header: t("sales.fields.product"), accessorKey: "productName" },
    { header: t("sales.fields.presentation"), accessorKey: "presentationName" },
    { header: t("sales.fields.barcode"), cell: ({ row }) => row.original.barcode ?? "-" },
    { header: t("sales.fields.quantityPresentation"), cell: ({ row }) => formatNumber(row.original.quantityPresentation) },
    { header: t("sales.fields.conversionFactor"), cell: ({ row }) => formatNumber(row.original.conversionFactor) },
    { header: t("sales.fields.quantityBase"), cell: ({ row }) => formatNumber(row.original.quantityBase) },
    { header: t("sales.fields.unitPrice"), cell: ({ row }) => formatMoney(row.original.unitPrice) },
    { header: t("sales.fields.discountAmount"), cell: ({ row }) => formatMoney(row.original.discountAmount) },
    { header: t("sales.fields.taxAmount"), cell: ({ row }) => formatMoney(row.original.taxAmount) },
    { header: t("sales.fields.totalAmount"), cell: ({ row }) => formatMoney(row.original.totalAmount) },
    { header: t("sales.fields.fifoCostTotal"), cell: ({ row }) => formatMoney(row.original.fifoCostTotal) },
  ], [t]);

  if (!canView) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  if (saleQuery.isLoading) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("admin.states.loading")}</CardContent></Card>;
  }

  if (saleQuery.error || !sale) {
    return <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(saleQuery.error, t)}</CardContent></Card>;
  }

  const recordTimeZone = sale.timezoneSnapshot ?? timeZone;

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.12),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.32))]">
        <CardContent className="grid gap-5 p-5 lg:grid-cols-[1fr_320px]">
          <div>
            <div className="mb-4 flex flex-wrap items-center gap-3">
              <p className="font-mono text-lg font-black tracking-[-0.04em]">{sale.saleNumber}</p>
              <SaleStatusBadge status={sale.status} label={t(`sales.status.${sale.status}`)} />
            </div>
            <div className="grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
              <Info label={t("sales.fields.customer")} value={sale.customerName ?? t("sales.finalConsumer")} />
              <Info label={t("sales.fields.branch")} value={sale.branchName} />
              <Info label={t("sales.fields.saleDate")} value={formatDateTime(sale.saleDate, recordTimeZone, locale)} />
              <Info label={t("sales.fields.paymentMethod")} value={t(`sales.paymentMethods.${sale.paymentMethod}`)} />
              <Info label={t("sales.fields.createdBy")} value={displayUser(sale.createdByUsername, sale.createdBy)} />
              <Info label={t("sales.fields.cancelledBy")} value={displayUser(sale.cancelledByUsername, sale.cancelledBy)} />
              <Info label={t("sales.fields.cancelledAt")} value={formatDateTime(sale.cancelledAt, recordTimeZone, locale)} />
            </div>
            {sale.notes ? <p className="mt-4 rounded-2xl border bg-background/70 p-3 text-sm text-muted-foreground">{sale.notes}</p> : null}
            {sale.cancelReason ? <p className="mt-4 rounded-2xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-800 dark:border-rose-800 dark:bg-rose-950/40 dark:text-rose-100">{sale.cancelReason}</p> : null}
          </div>
          <div className="rounded-3xl border bg-background/75 p-4 text-sm shadow-inner">
            <SummaryRow label={t("sales.fields.subtotal")} value={formatMoney(sale.subtotal)} />
            <SummaryRow label={t("sales.fields.taxAmount")} value={formatMoney(sale.taxAmount)} />
            <SummaryRow label={t("sales.fields.discountAmount")} value={formatMoney(sale.discountAmount)} />
            <div className="mt-3 border-t pt-3"><SummaryRow strong label={t("sales.fields.totalAmount")} value={formatMoney(sale.totalAmount)} /></div>
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" onClick={() => router.push("/sales")}>{t("sales.actions.back")}</Button>
        {canConfirm && sale.status === "DRAFT" ? <ConfirmDialog trigger={<Button disabled={confirmMutation.isPending}>{t("sales.actions.confirm")}</Button>} title={t("sales.confirm.title")} description={t("sales.confirm.description")} confirmLabel={t("sales.actions.confirm")} cancelLabel={t("admin.actions.cancel")} onConfirm={() => confirmMutation.mutate()} /> : null}
        {canConfirm && sale.status === "DRAFT" ? <ConfirmDialog trigger={<Button className="border border-rose-300 bg-rose-600 text-white hover:bg-rose-700" type="button" disabled={deleteDraftMutation.isPending}>{t("sales.actions.deleteDraft")}</Button>} title={t("sales.deleteDraft.title")} description={t("sales.deleteDraft.description")} confirmLabel={t("sales.actions.deleteDraft")} cancelLabel={t("admin.actions.cancel")} onConfirm={() => deleteDraftMutation.mutate()} /> : null}
        {canCancel && sale.status === "CONFIRMED" ? <Button className="border border-rose-300 bg-rose-600 text-white hover:bg-rose-700" type="button" disabled={cancelMutation.isPending} onClick={() => setIsCancelOpen(true)}>{t("sales.actions.cancelSale")}</Button> : null}
      </div>

      {confirmMutation.error || cancelMutation.error || deleteDraftMutation.error ? <Card><CardContent className="p-6 text-sm text-destructive">{firstErrorMessage(t, confirmMutation.error, cancelMutation.error, deleteDraftMutation.error)}</CardContent></Card> : null}

      <section className="space-y-3">
        <h2 className="text-xl font-black tracking-[-0.04em]">{t("sales.sections.items")}</h2>
        <AdminTable columns={columns} data={sale.details.filter((detail) => detail.sourceType !== "COMBO_LINE")} />
      </section>

      {sale.comboLines && sale.comboLines.length > 0 ? (
        <section className="space-y-3">
          <h2 className="text-xl font-black tracking-[-0.04em]">{t("sales.sections.combos")}</h2>
          <div className="grid gap-2">
            {sale.comboLines.map((line) => (
              <Card key={line.id}>
                <CardContent className="grid gap-2 p-4 md:grid-cols-4 md:items-center">
                  <div>
                    <p className="font-bold">{line.comboName}</p>
                    <p className="text-xs text-muted-foreground">{line.comboBarcode ?? "-"}</p>
                  </div>
                  <p className="text-sm">{t("sales.fields.quantityPresentation")}: <span className="font-bold">{line.quantity}</span></p>
                  <p className="text-sm">{t("sales.fields.unitPrice")}: <span className="font-bold">{formatMoney(line.comboUnitPrice)}</span></p>
                  <p className="text-sm">{t("sales.fields.totalAmount")}: <span className="font-bold">{formatMoney(line.totalAmount)}</span></p>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>
      ) : null}

      <Card>
        <CardContent className="p-5 text-sm text-muted-foreground">
          {t("sales.placeholders.generatedMovements")}
        </CardContent>
      </Card>

      <FormDialog open={isCancelOpen} onOpenChange={setIsCancelOpen} title={t("sales.cancel.title")} description={t("sales.cancel.description")}>
        <div className="space-y-4">
          <div className="space-y-2"><Label>{t("sales.fields.cancelReason")}</Label><Input value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} /></div>
          <div className="space-y-2">
            <Label>{t("sales.fields.cancelDisposition")}</Label>
            <select
              value={cancelDisposition}
              onChange={(event) => setCancelDisposition(event.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <option value="RETURN_TO_STOCK">{t("sales.cancelDispositions.RETURN_TO_STOCK")}</option>
              <option value="DAMAGED">{t("sales.cancelDispositions.DAMAGED")}</option>
              <option value="LOSS">{t("sales.cancelDispositions.LOSS")}</option>
              <option value="DO_NOT_RETURN">{t("sales.cancelDispositions.DO_NOT_RETURN")}</option>
            </select>
          </div>
          <div className="rounded-2xl border bg-muted/35 p-3 text-xs text-muted-foreground">{t("sales.placeholders.cancelOptions")}</div>
          <div className="flex flex-wrap gap-2">
            <Button className="border border-rose-300 bg-rose-600 text-white hover:bg-rose-700" type="button" disabled={cancelReason.trim().length === 0 || cancelMutation.isPending} onClick={() => cancelMutation.mutate()}>{t("sales.actions.cancelSale")}</Button>
            <Button type="button" variant="outline" onClick={() => setIsCancelOpen(false)}>{t("admin.actions.cancel")}</Button>
          </div>
        </div>
      </FormDialog>
    </div>
  );
}

function invalidateSaleImpact(queryClient: QueryClient, saleId: string) {
  queryClient.invalidateQueries({ queryKey: ["sales"] });
  queryClient.invalidateQueries({ queryKey: salesQueryKeys.detail(saleId) });
  queryClient.invalidateQueries({ queryKey: ["inventory", "balances"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "layers"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "movements"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "kardex"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "summary"] });
  queryClient.invalidateQueries({ queryKey: ["combos"] });
}

function Info({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="rounded-2xl border bg-background/60 p-3"><p className="text-xs font-bold uppercase tracking-[0.14em] text-muted-foreground">{label}</p><p className="mt-1 font-semibold">{value}</p></div>;
}

function SummaryRow({ label, value, strong }: Readonly<{ label: string; value: string; strong?: boolean }>) {
  return <div className={`flex items-center justify-between gap-3 ${strong ? "text-lg font-black" : ""}`}><span className="text-muted-foreground">{label}</span><span>{value}</span></div>;
}

function displayUser(username: string | null | undefined, userId: string | null | undefined) {
  return username ?? userId?.slice(0, 8) ?? "-";
}

function firstErrorMessage(t: (key: string, values?: Record<string, string>) => string, ...errors: unknown[]) {
  const error = errors.find(Boolean);
  return formatErrorMessage(error, t);
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatApiErrorMessage(error, t);
}
