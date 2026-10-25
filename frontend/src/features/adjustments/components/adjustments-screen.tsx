"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Filter, Plus, Search } from "lucide-react";
import Link from "next/link";
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
import { listBranches } from "@/features/admin/shared/admin-api";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { AdjustmentFormScreen } from "@/features/adjustments/components/adjustment-form-screen";
import { AdjustmentStatusBadge, AdjustmentTypeBadge, displayUser, formatAdjustmentDate } from "@/features/adjustments/components/adjustment-shared";
import { adjustmentsQueryKeys, approveAdjustment, cancelAdjustment, getAdjustment, listAdjustments, rejectAdjustment, toSubmitAdjustmentPayload, updateAdjustment } from "@/features/adjustments/api/adjustments-api";
import { canApproveAdjustments, canCancelAdjustments, canCreateAdjustments, canReadAdjustmentBranches, canReadAdjustments, canRejectAdjustments, canUpdateAdjustments } from "@/features/adjustments/lib/adjustment-permissions";
import { adjustmentStatuses, reasonSchema, type ReasonForm, type ReasonInput } from "@/features/adjustments/schemas/adjustment-schema";
import type { Adjustment, AdjustmentFilters } from "@/features/adjustments/types/adjustment-types";
import { formatPermissionError } from "@/lib/api/errors";

const pageSize = 20;
const defaultFilters: AdjustmentFilters = { page: 1, pageSize, branchId: "", status: "", fromDate: "", toDate: "", query: "" };
type ActionType = "reject" | "cancel";
type ActionState = { type: ActionType; adjustment: Adjustment } | null;

export function AdjustmentsScreen({ mode = "list" }: Readonly<{ mode?: "list" | "new" | "approvals" }>) {
  if (mode === "new") return <AdjustmentFormScreen />;
  return <AdjustmentsList approvalsOnly={mode === "approvals"} />;
}

function AdjustmentsList({ approvalsOnly }: Readonly<{ approvalsOnly?: boolean }>) {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { locale, timeZone } = useAppLocale();
  const canView = canReadAdjustments();
  const canCreate = canCreateAdjustments();
  const canReadBranches = canReadAdjustmentBranches();
  const canUpdate = canUpdateAdjustments();
  const canApprove = canApproveAdjustments();
  const canReject = canRejectAdjustments();
  const canCancel = canCancelAdjustments();
  const [filters, setFilters] = useState<AdjustmentFilters>(approvalsOnly ? { ...defaultFilters, status: "PENDING_APPROVAL" } : defaultFilters);
  const [draftFilters, setDraftFilters] = useState<AdjustmentFilters>(filters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const [action, setAction] = useState<ActionState>(null);
  const reasonForm = useForm<ReasonInput, unknown, ReasonForm>({ resolver: zodResolver(reasonSchema), defaultValues: { reason: "" }, reValidateMode: "onBlur" });
  const effectiveFilters = approvalsOnly ? { ...filters, status: "PENDING_APPROVAL" as const } : filters;
  const branchesQuery = useQuery({ queryKey: ["adjustments", "branches"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const adjustmentsQuery = useQuery({ queryKey: approvalsOnly ? adjustmentsQueryKeys.approvals(effectiveFilters) : adjustmentsQueryKeys.list(effectiveFilters), queryFn: () => listAdjustments(effectiveFilters), enabled: canView });
  const activeAdvancedFilters = [effectiveFilters.branchId, approvalsOnly ? "" : effectiveFilters.status, effectiveFilters.fromDate, effectiveFilters.toDate].filter(Boolean).length;
  const actionMutation = useAdjustmentActionMutation(queryClient, () => { setAction(null); reasonForm.reset({ reason: "" }); });

  const columns = useMemo<ColumnDef<Adjustment>[]>(() => [
    { header: t("adjustments.fields.adjustmentNumber"), cell: ({ row }) => <Link className="font-mono text-xs font-bold text-primary hover:underline" href={`/adjustments/${row.original.id}`}>{row.original.adjustmentNumber}</Link> },
    { header: t("adjustments.fields.createdAt"), cell: ({ row }) => formatAdjustmentDate(row.original.createdAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("adjustments.fields.type"), cell: ({ row }) => <AdjustmentTypeBadge direction={row.original.direction} label={`${t(`adjustments.types.${row.original.adjustmentType}`)} · ${t(`adjustments.directions.${row.original.direction}`)}`} /> },
    { header: t("adjustments.fields.branch"), accessorKey: "branchName" },
    { header: t("adjustments.fields.status"), cell: ({ row }) => <AdjustmentStatusBadge status={row.original.status} label={t(`adjustments.status.${row.original.status}`)} /> },
    { header: t("adjustments.fields.createdBy"), cell: ({ row }) => displayUser(row.original.createdByUsername, row.original.createdBy) },
    { header: t("adjustments.fields.approvedBy"), cell: ({ row }) => displayUser(row.original.approvedByUsername, row.original.approvedBy) },
    { header: t("adjustments.fields.reason"), cell: ({ row }) => <span className="line-clamp-2 min-w-48 text-sm text-muted-foreground">{row.original.notes ?? row.original.rejectionReason ?? "-"}</span> },
    { header: t("admin.fields.actions"), cell: ({ row }) => <AdjustmentActions adjustment={row.original} onDetail={() => router.push(`/adjustments/${row.original.id}`)} onAction={setAction} onSimpleAction={(type, adjustment) => actionMutation.mutate({ type, adjustment })} canUpdate={canUpdate} canApprove={canApprove} canReject={canReject} canCancel={canCancel} isPending={actionMutation.isPending} /> },
  ], [actionMutation, canApprove, canCancel, canReject, canUpdate, locale, router, t, timeZone]);

  if (!canView) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  const commonError = adjustmentsQuery.error ?? branchesQuery.error ?? actionMutation.error;

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.10),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.34))] shadow-sm">
        <CardContent className="grid gap-3 p-5 md:grid-cols-[1fr_auto_auto_auto] md:items-end">
          <div className="space-y-2">
            <Label>{t("catalog.search")}</Label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
              <Input className="pl-9" value={filters.query} onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value, page: 1 }))} placeholder={t("adjustments.filters.searchPlaceholder")} />
            </div>
          </div>
          <Button className="gap-2" type="button" variant="outline" onClick={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }}>
            <Filter className="h-4 w-4" aria-hidden="true" />{t("adjustments.filters.button")}
            {activeAdvancedFilters > 0 ? <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-bold text-primary-foreground">{activeAdvancedFilters}</span> : null}
          </Button>
          {!approvalsOnly && canApprove ? <Button type="button" variant="outline" onClick={() => router.push("/adjustments/approvals")}>{t("adjustments.actions.openApprovals")}</Button> : null}
          {!approvalsOnly && canCreate ? <Button type="button" onClick={() => router.push("/adjustments/new")}><Plus className="mr-2 h-4 w-4" aria-hidden="true" />{t("adjustments.actions.new")}</Button> : null}
        </CardContent>
      </Card>

      {commonError ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(commonError, t)}</CardContent></Card> : null}

      <TableSection page={adjustmentsQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))}>
        <AdminTable columns={columns} data={adjustmentsQuery.data?.items ?? []} />
      </TableSection>

      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("adjustments.filters.title")} description={t("adjustments.filters.description")}>
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("adjustments.fields.branch")} value={draftFilters.branchId} onChange={(value) => setDraftFilters((current) => ({ ...current, branchId: value }))} options={(branchesQuery.data?.items ?? []).map((branch) => ({ value: branch.id, label: branch.name }))} disabled={!canReadBranches} />
          {!approvalsOnly ? <Select label={t("adjustments.fields.status")} value={draftFilters.status} onChange={(value) => setDraftFilters((current) => ({ ...current, status: value as AdjustmentFilters["status"] }))} options={adjustmentStatuses.map((status) => ({ value: status, label: t(`adjustments.status.${status}`) }))} /> : null}
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <div className="flex flex-wrap gap-2 md:col-span-2">
            <Button type="button" onClick={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }}>{t("adjustments.filters.apply")}</Button>
            <Button type="button" variant="outline" onClick={() => setDraftFilters({ ...defaultFilters, status: approvalsOnly ? "PENDING_APPROVAL" : "", query: filters.query })}>{t("adjustments.filters.clear")}</Button>
            <Button type="button" variant="ghost" onClick={() => setIsFiltersOpen(false)}>{t("admin.actions.cancel")}</Button>
          </div>
        </div>
      </FormDialog>

      <FormDialog open={action?.type === "reject" || action?.type === "cancel"} onOpenChange={(open) => { if (!open) { setAction(null); reasonForm.reset({ reason: "" }); } }} title={action ? t(`adjustments.dialogs.${action.type}.title`) : ""} description={action ? t(`adjustments.dialogs.${action.type}.description`) : ""}>
        <form className="space-y-4" onSubmit={reasonForm.handleSubmit((values) => actionMutation.mutate({ type: action?.type === "reject" ? "reject" : "cancel", adjustment: action?.adjustment as Adjustment, reason: values.reason }))}>
          <div className="space-y-2"><Label>{t("adjustments.fields.reason")}</Label><Input {...reasonForm.register("reason")} />{reasonForm.formState.errors.reason ? <p className="text-sm text-destructive">{reasonForm.formState.errors.reason.message}</p> : null}</div>
          <div className="flex flex-wrap justify-end gap-2"><Button type="button" variant="outline" onClick={() => setAction(null)}>{t("admin.actions.cancel")}</Button><Button type="submit" disabled={actionMutation.isPending}>{t("admin.actions.confirm")}</Button></div>
        </form>
      </FormDialog>
    </div>
  );
}

function AdjustmentActions({ adjustment, onDetail, onAction, onSimpleAction, canUpdate, canApprove, canReject, canCancel, isPending }: Readonly<{ adjustment: Adjustment; onDetail: () => void; onAction: (action: ActionState) => void; onSimpleAction: (type: "submit" | "approve", adjustment: Adjustment) => void; canUpdate: boolean; canApprove: boolean; canReject: boolean; canCancel: boolean; isPending: boolean }>) {
  const t = useTranslations();
  const isWritable = adjustment.status === "DRAFT" || adjustment.status === "PENDING_APPROVAL";
  return <div className="flex flex-wrap gap-2"><Button variant="outline" onClick={onDetail}>{t("adjustments.actions.view")}</Button>{canUpdate && adjustment.status === "DRAFT" ? <Button variant="outline" disabled={isPending} onClick={() => onSimpleAction("submit", adjustment)}>{t("adjustments.actions.submit")}</Button> : null}{canApprove && isWritable ? <Button variant="outline" disabled={isPending} onClick={() => onSimpleAction("approve", adjustment)}>{t("adjustments.actions.approve")}</Button> : null}{canReject && isWritable ? <Button variant="outline" onClick={() => onAction({ type: "reject", adjustment })}>{t("adjustments.actions.reject")}</Button> : null}{canCancel && isWritable ? <Button variant="outline" onClick={() => onAction({ type: "cancel", adjustment })}>{t("adjustments.actions.cancel")}</Button> : null}</div>;
}

function TableSection({ page, onPageChange, children }: Readonly<{ page?: { page: number; totalPages: number; totalItems: number }; onPageChange: (page: number) => void; children: React.ReactNode }>) {
  return <section className="space-y-3"><div className="flex flex-wrap items-center justify-between gap-2"><h2 className="text-xl font-black tracking-[-0.04em]">Ajustes</h2>{page ? <div className="flex items-center gap-2 text-sm text-muted-foreground"><span>{page.totalItems} items</span><Button type="button" variant="outline" disabled={page.page <= 1} onClick={() => onPageChange(page.page - 1)}>Prev</Button><span>{page.page}/{Math.max(page.totalPages, 1)}</span><Button type="button" variant="outline" disabled={page.page >= page.totalPages} onClick={() => onPageChange(page.page + 1)}>Next</Button></div> : null}</div>{children}</section>;
}

function Select({ label, value, onChange, options, disabled = false }: Readonly<{ label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }>; disabled?: boolean }>) {
  const t = useTranslations();
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm disabled:cursor-not-allowed disabled:opacity-70" value={value} disabled={disabled} onChange={(event) => onChange(event.target.value)}><option value="">{t("adjustments.filters.all")}</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>;
}

function DateFilter({ label, value, onChange }: Readonly<{ label: string; value: string; onChange: (value: string) => void }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type="date" value={value} onChange={(event) => onChange(event.target.value)} /></div>;
}

function useAdjustmentActionMutation(queryClient: ReturnType<typeof useQueryClient>, onSuccess: () => void) {
  const t = useTranslations();
  return useMutation({
    mutationFn: async (action: { type: "submit" | "approve"; adjustment: Adjustment } | { type: "reject" | "cancel"; adjustment: Adjustment; reason: string }) => {
      if (action.type === "submit") {
        const detail = await getAdjustment(action.adjustment.id);
        return updateAdjustment(action.adjustment.id, toSubmitAdjustmentPayload(detail));
      }
      if (action.type === "approve") return approveAdjustment(action.adjustment.id);
      if (action.type === "reject") return rejectAdjustment(action.adjustment.id, action.reason);
      if (action.type === "cancel") return cancelAdjustment(action.adjustment.id, action.reason);
      throw new Error("Unsupported adjustment action");
    },
    onSuccess: (adjustment) => {
      toast.success(t("adjustments.messages.actionCompleted"));
      invalidateAdjustmentImpact(queryClient, adjustment.id);
      onSuccess();
    },
  });
}

export function invalidateAdjustmentImpact(queryClient: ReturnType<typeof useQueryClient>, adjustmentId?: string) {
  queryClient.invalidateQueries({ queryKey: adjustmentsQueryKeys.all });
  if (adjustmentId) queryClient.invalidateQueries({ queryKey: adjustmentsQueryKeys.detail(adjustmentId) });
  queryClient.invalidateQueries({ queryKey: ["inventory"] });
  queryClient.invalidateQueries({ queryKey: ["auditLogs"] });
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}
