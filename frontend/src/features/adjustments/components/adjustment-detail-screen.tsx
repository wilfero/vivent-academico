"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { useAppLocale } from "@/components/providers/locale-provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { adjustmentsQueryKeys, approveAdjustment, cancelAdjustment, getAdjustment, rejectAdjustment, toSubmitAdjustmentPayload, updateAdjustment } from "@/features/adjustments/api/adjustments-api";
import { AdjustmentStatusBadge, AdjustmentTypeBadge, displayUser, formatAdjustmentDate, formatAdjustmentNumber } from "@/features/adjustments/components/adjustment-shared";
import { invalidateAdjustmentImpact } from "@/features/adjustments/components/adjustments-screen";
import { canApproveAdjustments, canCancelAdjustments, canReadAdjustments, canRejectAdjustments, canUpdateAdjustments } from "@/features/adjustments/lib/adjustment-permissions";
import { reasonSchema, type ReasonForm, type ReasonInput } from "@/features/adjustments/schemas/adjustment-schema";
import type { AdjustmentLine } from "@/features/adjustments/types/adjustment-types";
import { formatPermissionError } from "@/lib/api/errors";

type ActionType = "reject" | "cancel";

export function AdjustmentDetailScreen({ adjustmentId }: Readonly<{ adjustmentId: string }>) {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { locale, timeZone } = useAppLocale();
  const [action, setAction] = useState<ActionType | null>(null);
  const canView = canReadAdjustments();
  const canUpdate = canUpdateAdjustments();
  const canApprove = canApproveAdjustments();
  const canReject = canRejectAdjustments();
  const canCancel = canCancelAdjustments();
  const reasonForm = useForm<ReasonInput, unknown, ReasonForm>({ resolver: zodResolver(reasonSchema), defaultValues: { reason: "" }, reValidateMode: "onBlur" });
  const adjustmentQuery = useQuery({ queryKey: adjustmentsQueryKeys.detail(adjustmentId), queryFn: () => getAdjustment(adjustmentId), enabled: canView });
  const adjustment = adjustmentQuery.data;
  const actionMutation = useMutation({
    mutationFn: async (payload: { action: "submit" | "approve" } | { action: "reject" | "cancel"; reason: string }) => {
      if (payload.action === "submit") {
        const detail = adjustment ?? await getAdjustment(adjustmentId);
        return updateAdjustment(adjustmentId, toSubmitAdjustmentPayload(detail));
      }
      if (payload.action === "approve") return approveAdjustment(adjustmentId);
      if (payload.action === "reject") return rejectAdjustment(adjustmentId, payload.reason);
      if (payload.action === "cancel") return cancelAdjustment(adjustmentId, payload.reason);
      throw new Error("Unsupported adjustment action");
    },
    onSuccess: (updatedAdjustment) => {
      toast.success(t("adjustments.messages.actionCompleted"));
      invalidateAdjustmentImpact(queryClient, updatedAdjustment.id);
      reasonForm.reset({ reason: "" });
      setAction(null);
    },
  });
  const columns = useMemo<ColumnDef<AdjustmentLine>[]>(() => [
    { header: t("adjustments.fields.product"), accessorKey: "productName" },
    { header: t("adjustments.fields.presentation"), cell: ({ row }) => row.original.presentationName ?? "-" },
    { header: t("adjustments.fields.conversionFactor"), cell: ({ row }) => row.original.conversionFactor === null ? "-" : formatAdjustmentNumber(row.original.conversionFactor) },
    { header: t("adjustments.fields.quantityPresentation"), cell: ({ row }) => row.original.quantityPresentation === null ? "-" : formatAdjustmentNumber(row.original.quantityPresentation) },
    { header: t("adjustments.fields.quantityBase"), cell: ({ row }) => formatAdjustmentNumber(row.original.quantityBase) },
    { header: t("adjustments.fields.unitCost"), cell: ({ row }) => row.original.unitCost === null ? "-" : formatAdjustmentNumber(row.original.unitCost) },
    { header: t("adjustments.fields.lineNotes"), cell: ({ row }) => row.original.notes ?? "-" },
  ], [t]);

  if (!canView) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  if (adjustmentQuery.isLoading) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("admin.states.loading")}</CardContent></Card>;
  }

  if (adjustmentQuery.error || !adjustment) {
    return <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(adjustmentQuery.error, t)}</CardContent></Card>;
  }

  const recordTimeZone = adjustment.timezoneSnapshot ?? timeZone;
  const isWritable = adjustment.status === "DRAFT" || adjustment.status === "PENDING_APPROVAL";
  const commonError = actionMutation.error;

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.12),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.32))]">
        <CardContent className="grid gap-5 p-5 lg:grid-cols-[1fr_320px]">
          <div>
            <div className="mb-4 flex flex-wrap items-center gap-3">
              <p className="font-mono text-lg font-black tracking-[-0.04em]">{adjustment.adjustmentNumber}</p>
              <AdjustmentStatusBadge status={adjustment.status} label={t(`adjustments.status.${adjustment.status}`)} />
              <AdjustmentTypeBadge direction={adjustment.direction} label={`${t(`adjustments.types.${adjustment.adjustmentType}`)} · ${t(`adjustments.directions.${adjustment.direction}`)}`} />
            </div>
            <div className="grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
              <Info label={t("adjustments.fields.branch")} value={adjustment.branchName} />
              <Info label={t("adjustments.fields.location")} value={adjustment.locationName} />
              <Info label={t("adjustments.fields.createdAt")} value={formatAdjustmentDate(adjustment.createdAt, recordTimeZone, locale)} />
              <Info label={t("adjustments.fields.updatedAt")} value={formatAdjustmentDate(adjustment.updatedAt, recordTimeZone, locale)} />
              <Info label={t("adjustments.fields.approvedAt")} value={formatAdjustmentDate(adjustment.approvedAt, recordTimeZone, locale)} />
              <Info label={t("adjustments.fields.rejectedAt")} value={formatAdjustmentDate(adjustment.rejectedAt, recordTimeZone, locale)} />
              <Info label={t("adjustments.fields.createdBy")} value={displayUser(adjustment.createdByUsername, adjustment.createdBy)} />
              <Info label={t("adjustments.fields.approvedBy")} value={displayUser(adjustment.approvedByUsername, adjustment.approvedBy)} />
              <Info label={t("adjustments.fields.rejectedBy")} value={displayUser(adjustment.rejectedByUsername, adjustment.rejectedBy)} />
            </div>
            <p className="mt-4 rounded-2xl border bg-background/70 p-3 text-sm text-muted-foreground">{adjustment.notes ?? "-"}</p>
            {adjustment.rejectionReason ? <p className="mt-4 rounded-2xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-800 dark:border-rose-800 dark:bg-rose-950/40 dark:text-rose-100">{adjustment.rejectionReason}</p> : null}
          </div>
          <div className="rounded-3xl border bg-background/75 p-4 text-sm shadow-inner">
            <SummaryRow label={t("adjustments.summary.lines")} value={String(adjustment.lines.length)} />
            <SummaryRow label={t("adjustments.summary.quantityBase")} value={formatAdjustmentNumber(adjustment.lines.reduce((total, line) => total + Number(line.quantityBase || 0), 0))} />
            <SummaryRow label={t("adjustments.summary.inventoryImpact")} value={adjustment.status === "APPROVED" ? t("adjustments.summary.applied") : t("adjustments.summary.notApplied")} />
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" onClick={() => router.push("/adjustments")}>{t("adjustments.actions.back")}</Button>
        {canUpdate && adjustment.status === "DRAFT" ? <Button type="button" variant="outline" disabled={actionMutation.isPending} onClick={() => actionMutation.mutate({ action: "submit" })}>{t("adjustments.actions.submit")}</Button> : null}
        {canApprove && isWritable ? <Button type="button" disabled={actionMutation.isPending} onClick={() => actionMutation.mutate({ action: "approve" })}>{t("adjustments.actions.approve")}</Button> : null}
        {canReject && isWritable ? <Button type="button" variant="outline" onClick={() => setAction("reject")}>{t("adjustments.actions.reject")}</Button> : null}
        {canCancel && isWritable ? <Button type="button" variant="outline" onClick={() => setAction("cancel")}>{t("adjustments.actions.cancel")}</Button> : null}
        <Button type="button" variant="outline" onClick={() => router.push("/inventory")}>{t("adjustments.actions.viewInventory")}</Button>
      </div>

      {commonError ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(commonError, t)}</CardContent></Card> : null}

      <section className="space-y-3">
        <h2 className="text-xl font-black tracking-[-0.04em]">{t("adjustments.sections.lines")}</h2>
        <AdminTable columns={columns} data={adjustment.lines} />
      </section>

      <Card><CardContent className="p-5 text-sm text-muted-foreground">{t("adjustments.placeholders.generatedMovements")}</CardContent></Card>

      <FormDialog open={action === "reject" || action === "cancel"} onOpenChange={(open) => { if (!open) { setAction(null); reasonForm.reset({ reason: "" }); } }} title={action ? t(`adjustments.dialogs.${action}.title`) : ""} description={action ? t(`adjustments.dialogs.${action}.description`) : ""}>
        <form className="space-y-4" onSubmit={reasonForm.handleSubmit((values) => actionMutation.mutate({ action: action === "reject" ? "reject" : "cancel", reason: values.reason }))}>
          <div className="space-y-2"><Label>{t("adjustments.fields.reason")}</Label><Input {...reasonForm.register("reason")} />{reasonForm.formState.errors.reason ? <p className="text-sm text-destructive">{reasonForm.formState.errors.reason.message}</p> : null}</div>
          <div className="flex flex-wrap justify-end gap-2"><Button type="button" variant="outline" onClick={() => setAction(null)}>{t("admin.actions.cancel")}</Button><Button type="submit" disabled={actionMutation.isPending}>{t("admin.actions.confirm")}</Button></div>
        </form>
      </FormDialog>
    </div>
  );
}

function Info({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="rounded-2xl border bg-background/60 p-3"><p className="text-xs font-bold uppercase tracking-[0.14em] text-muted-foreground">{label}</p><p className="mt-1 font-semibold">{value}</p></div>;
}

function SummaryRow({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="mt-3 flex items-center justify-between gap-3"><span className="text-muted-foreground">{label}</span><span className="font-bold">{value}</span></div>;
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}
