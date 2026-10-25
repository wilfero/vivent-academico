"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { toast } from "sonner";
import { useAppLocale } from "@/components/providers/locale-provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { approveTransfer, cancelTransfer, dispatchTransfer, getTransfer, receiveTransfer, rejectTransfer, transfersQueryKeys } from "@/features/transfers/api/transfers-api";
import { formatTransferDate, formatTransferNumber, TransferStatusBadge } from "@/features/transfers/components/transfer-shared";
import { invalidateTransferImpact } from "@/features/transfers/components/transfers-screen";
import { canApproveTransfers, canCancelTransfers, canDispatchTransfers, canReadTransfers, canReceiveTransfers, canRejectTransfers } from "@/features/transfers/lib/transfer-permissions";
import { cancelTransferSchema, receiveTransferSchema, toReceiveTransferPayload, type CancelTransferForm, type CancelTransferInput, type ReceiveTransferForm, type ReceiveTransferInput } from "@/features/transfers/schemas/transfer-schema";
import type { Transfer, TransferDetail } from "@/features/transfers/types/transfer-types";
import { formatPermissionError } from "@/lib/api/errors";
import { getStoredSession } from "@/features/auth/lib/session-storage";

type ActionType = "approve" | "reject" | "dispatch" | "cancel" | "receive";

export function TransferDetailScreen({ transferId }: Readonly<{ transferId: string }>) {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { locale, timeZone } = useAppLocale();
  const [action, setAction] = useState<ActionType | null>(null);
  const activeSession = getStoredSession();
  const activeBranchId = activeSession?.branchId ?? null;
  const canOperateAllBranches = activeSession?.roleCode === "COMPANY_ADMIN";
  const canView = canReadTransfers();
  const canApprove = canApproveTransfers();
  const canReject = canRejectTransfers();
  const canDispatch = canDispatchTransfers();
  const canReceive = canReceiveTransfers();
  const canCancel = canCancelTransfers();
  const transferQuery = useQuery({ queryKey: transfersQueryKeys.detail(transferId), queryFn: () => getTransfer(transferId), enabled: canView });
  const transfer = transferQuery.data;
  const receiveForm = useForm<ReceiveTransferInput, unknown, ReceiveTransferForm>({ resolver: zodResolver(receiveTransferSchema), defaultValues: { details: [] }, reValidateMode: "onBlur" });
  const cancelForm = useForm<CancelTransferInput, unknown, CancelTransferForm>({ resolver: zodResolver(cancelTransferSchema), defaultValues: { reason: "" }, reValidateMode: "onBlur" });

  const approveMutation = useActionMutation(() => approveTransfer(transferId), t("transfers.messages.approved"), queryClient, () => setAction(null));
  const rejectMutation = useActionMutation(() => rejectTransfer(transferId), t("transfers.messages.rejected"), queryClient, () => setAction(null));
  const dispatchMutation = useActionMutation(() => dispatchTransfer(transferId), t("transfers.messages.dispatched"), queryClient, () => setAction(null));
  const receiveMutation = useActionMutation((values: ReceiveTransferForm) => receiveTransfer(transferId, toReceiveTransferPayload(values)), t("transfers.messages.received"), queryClient, () => setAction(null));
  const cancelMutation = useActionMutation((values: CancelTransferForm) => cancelTransfer(transferId, values.reason), t("transfers.messages.cancelled"), queryClient, () => { cancelForm.reset({ reason: "" }); setAction(null); });

  const columns = useMemo<ColumnDef<TransferDetail>[]>(() => [
    { header: t("transfers.fields.product"), accessorKey: "productName" },
    { header: t("transfers.fields.presentation"), accessorKey: "presentationName" },
    { header: t("transfers.fields.conversionFactor"), cell: ({ row }) => formatTransferNumber(row.original.conversionFactor) },
    { header: t("transfers.fields.quantityRequestedPresentation"), cell: ({ row }) => formatTransferNumber(row.original.quantityRequestedPresentation) },
    { header: t("transfers.fields.quantityRequestedBase"), cell: ({ row }) => formatTransferNumber(row.original.quantityRequestedBase) },
    { header: t("transfers.fields.quantityApprovedBase"), cell: ({ row }) => formatTransferNumber(row.original.quantityApprovedBase) },
    { header: t("transfers.fields.quantityReceivedBase"), cell: ({ row }) => formatTransferNumber(row.original.quantityReceivedBase) },
  ], [t]);

  if (!canView) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  if (transferQuery.isLoading) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("admin.states.loading")}</CardContent></Card>;
  }

  if (transferQuery.error || !transfer) {
    return <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(transferQuery.error, t)}</CardContent></Card>;
  }

  const recordTimeZone = transfer.timezoneSnapshot ?? timeZone;
  const commonError = approveMutation.error ?? rejectMutation.error ?? dispatchMutation.error ?? receiveMutation.error ?? cancelMutation.error;
  const canOperateSource = canOperateAllBranches || transfer.sourceBranchId === activeBranchId;
  const canOperateDestination = canOperateAllBranches || transfer.destinationBranchId === activeBranchId;

  function openReceiveDialog() {
    if (!transfer) return;
    receiveForm.reset({ details: transfer.details.map((detail) => ({ detailId: detail.id, quantityReceivedBase: Math.max(detail.quantityApprovedBase - detail.quantityReceivedBase, 0) })) });
    setAction("receive");
  }

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.12),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.32))]">
        <CardContent className="grid gap-5 p-5 lg:grid-cols-[1fr_320px]">
          <div>
            <div className="mb-4 flex flex-wrap items-center gap-3">
              <p className="font-mono text-lg font-black tracking-[-0.04em]">{transfer.transferNumber}</p>
              <TransferStatusBadge status={transfer.status} label={t(`transfers.status.${transfer.status}`)} />
            </div>
            <div className="grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
              <Info label={t("transfers.fields.sourceBranch")} value={transfer.sourceBranchName} />
              <Info label={t("transfers.fields.destinationBranch")} value={transfer.destinationBranchName} />
              <Info label={t("transfers.fields.requestedAt")} value={formatTransferDate(transfer.requestedAt, recordTimeZone, locale)} />
              <Info label={t("transfers.fields.approvedAt")} value={formatTransferDate(transfer.approvedAt, recordTimeZone, locale)} />
              <Info label={t("transfers.fields.dispatchedAt")} value={formatTransferDate(transfer.dispatchedAt, recordTimeZone, locale)} />
              <Info label={t("transfers.fields.receivedAt")} value={formatTransferDate(transfer.receivedAt, recordTimeZone, locale)} />
              <Info label={t("transfers.fields.requestedBy")} value={displayUser(transfer.requestedByUsername, transfer.requestedBy)} />
              <Info label={t("transfers.fields.approvedBy")} value={displayUser(transfer.approvedByUsername, transfer.approvedBy)} />
              <Info label={t("transfers.fields.dispatchedBy")} value={displayUser(transfer.dispatchedByUsername, transfer.dispatchedBy)} />
              <Info label={t("transfers.fields.receivedBy")} value={displayUser(transfer.receivedByUsername, transfer.receivedBy)} />
              <Info label={t("transfers.fields.cancelledBy")} value={displayUser(transfer.cancelledByUsername, transfer.cancelledBy)} />
            </div>
            {transfer.notes ? <p className="mt-4 rounded-2xl border bg-background/70 p-3 text-sm text-muted-foreground">{transfer.notes}</p> : null}
            {transfer.cancelReason ? <p className="mt-4 rounded-2xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-800 dark:border-rose-800 dark:bg-rose-950/40 dark:text-rose-100">{transfer.cancelReason}</p> : null}
          </div>
          <div className="rounded-3xl border bg-background/75 p-4 text-sm shadow-inner">
            <SummaryRow label={t("transfers.summary.lines")} value={String(transfer.details.length)} />
            <SummaryRow label={t("transfers.summary.requestedBase")} value={formatTransferNumber(transfer.details.reduce((total, detail) => total + detail.quantityRequestedBase, 0))} />
            <SummaryRow label={t("transfers.summary.approvedBase")} value={formatTransferNumber(transfer.details.reduce((total, detail) => total + detail.quantityApprovedBase, 0))} />
            <SummaryRow label={t("transfers.summary.receivedBase")} value={formatTransferNumber(transfer.details.reduce((total, detail) => total + detail.quantityReceivedBase, 0))} />
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" onClick={() => router.push("/transfers")}>{t("transfers.actions.back")}</Button>
        {canApprove && canOperateSource && transfer.status === "REQUESTED" ? <ActionButton action="approve" setAction={setAction} /> : null}
        {canReject && canOperateSource && transfer.status === "REQUESTED" ? <ActionButton action="reject" setAction={setAction} /> : null}
        {canDispatch && canOperateSource && transfer.status === "APPROVED" ? <ActionButton action="dispatch" setAction={setAction} /> : null}
        {canReceive && canOperateDestination && ["APPROVED", "IN_TRANSIT", "PARTIALLY_RECEIVED"].includes(transfer.status) ? <Button type="button" onClick={openReceiveDialog}>{t("transfers.actions.receive")}</Button> : null}
        {canCancel && (canOperateSource || canOperateDestination) && !["CANCELLED", "RECEIVED", "REJECTED"].includes(transfer.status) ? <ActionButton action="cancel" setAction={setAction} /> : null}
      </div>

      {commonError ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(commonError, t)}</CardContent></Card> : null}

      <section className="space-y-3">
        <h2 className="text-xl font-black tracking-[-0.04em]">{t("transfers.sections.details")}</h2>
        <AdminTable columns={columns} data={transfer.details} />
      </section>

      <Card><CardContent className="p-5 text-sm text-muted-foreground">{t("transfers.placeholders.generatedMovements")}</CardContent></Card>

      <SimpleActionDialog action={action} onOpenChange={(open) => !open && setAction(null)} onConfirm={() => {
        if (action === "approve") approveMutation.mutate(undefined);
        if (action === "reject") rejectMutation.mutate(undefined);
        if (action === "dispatch") dispatchMutation.mutate(undefined);
      }} isPending={approveMutation.isPending || rejectMutation.isPending || dispatchMutation.isPending} />

      <FormDialog open={action === "receive"} onOpenChange={(open) => !open && setAction(null)} title={t("transfers.dialogs.receive.title")} description={t("transfers.dialogs.receive.description")}>
        <form className="space-y-4" onSubmit={receiveForm.handleSubmit((values) => receiveMutation.mutate(values))}>
          <div className="grid gap-3">
            {transfer.details.map((detail, index) => {
              const remaining = Math.max(detail.quantityApprovedBase - detail.quantityReceivedBase, 0);
              return <div key={detail.id} className="grid gap-3 rounded-2xl border bg-background/75 p-3 md:grid-cols-[1fr_220px] md:items-end"><div><p className="font-bold">{detail.productName}</p><p className="text-sm text-muted-foreground">{detail.presentationName} · {t("transfers.fields.remaining")}: {formatTransferNumber(remaining)}</p></div><input type="hidden" {...receiveForm.register(`details.${index}.detailId` as const)} /><Field label={t("transfers.fields.quantityReceivedBase")} registration={receiveForm.register(`details.${index}.quantityReceivedBase` as const, { valueAsNumber: true })} type="number" /></div>;
            })}
          </div>
          <div className="flex flex-wrap justify-end gap-2"><Button type="button" variant="outline" onClick={() => setAction(null)}>{t("admin.actions.cancel")}</Button><Button type="submit" disabled={receiveMutation.isPending}>{t("transfers.actions.receive")}</Button></div>
        </form>
      </FormDialog>

      <FormDialog open={action === "cancel"} onOpenChange={(open) => !open && setAction(null)} title={t("transfers.dialogs.cancel.title")} description={t("transfers.dialogs.cancel.description")}>
        <form className="space-y-4" onSubmit={cancelForm.handleSubmit((values) => cancelMutation.mutate(values))}>
          <Field label={t("transfers.fields.reason")} registration={cancelForm.register("reason")} />
          <div className="flex flex-wrap justify-end gap-2"><Button type="button" variant="outline" onClick={() => setAction(null)}>{t("admin.actions.cancel")}</Button><Button type="submit" disabled={cancelMutation.isPending}>{t("admin.actions.confirm")}</Button></div>
        </form>
      </FormDialog>
    </div>
  );
}

function ActionButton({ action, setAction }: Readonly<{ action: ActionType; setAction: (action: ActionType) => void }>) {
  const t = useTranslations();
  return <Button type="button" variant={action === "cancel" || action === "reject" ? "outline" : "default"} onClick={() => setAction(action)}>{t(`transfers.actions.${action}`)}</Button>;
}

function SimpleActionDialog({ action, onOpenChange, onConfirm, isPending }: Readonly<{ action: ActionType | null; onOpenChange: (open: boolean) => void; onConfirm: () => void; isPending: boolean }>) {
  const t = useTranslations();
  if (!action || action === "receive" || action === "cancel") return null;
  return <FormDialog open={Boolean(action)} onOpenChange={onOpenChange} title={t(`transfers.dialogs.${action}.title`)} description={t(`transfers.dialogs.${action}.description`)}><div className="flex flex-wrap justify-end gap-2"><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>{t("admin.actions.cancel")}</Button><Button type="button" disabled={isPending} onClick={onConfirm}>{t(`transfers.actions.${action}`)}</Button></div></FormDialog>;
}

function useActionMutation<TValues>(mutationFn: (values: TValues) => Promise<Transfer>, successMessage: string, queryClient: ReturnType<typeof useQueryClient>, onSuccess: () => void) {
  return useMutation({
    mutationFn,
    onSuccess: (transfer) => {
      toast.success(successMessage);
      invalidateTransferImpact(queryClient, transfer.id);
      onSuccess();
    },
  });
}

function Info({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="rounded-2xl border bg-background/60 p-3"><p className="text-xs font-bold uppercase tracking-[0.14em] text-muted-foreground">{label}</p><p className="mt-1 font-semibold">{value}</p></div>;
}

function SummaryRow({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="mt-3 flex items-center justify-between gap-3"><span className="text-muted-foreground">{label}</span><span className="font-bold">{value}</span></div>;
}

function Field({ label, registration, type = "text" }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type={type} step="0.000001" {...registration} /></div>;
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}

function displayUser(username: string | null | undefined, userId: string | null | undefined) {
  return username ?? userId?.slice(0, 8) ?? "-";
}
