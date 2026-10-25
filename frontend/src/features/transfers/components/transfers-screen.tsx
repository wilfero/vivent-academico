"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Filter, Plus, Search } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { useAppLocale } from "@/components/providers/locale-provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { listBranches } from "@/features/admin/shared/admin-api";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { withSessionBranchFallback } from "@/features/auth/lib/session-branch";
import { approveTransfer, cancelTransfer, dispatchTransfer, listTransfers, rejectTransfer, transfersQueryKeys } from "@/features/transfers/api/transfers-api";
import { formatTransferDate, TransferStatusBadge } from "@/features/transfers/components/transfer-shared";
import { canApproveTransfers, canCancelTransfers, canDispatchTransfers, canReadTransfers, canRejectTransfers, canRequestTransfers, canReceiveTransfers } from "@/features/transfers/lib/transfer-permissions";
import type { Transfer, TransferFilters, TransferStatus } from "@/features/transfers/types/transfer-types";
import { formatPermissionError } from "@/lib/api/errors";
import { getStoredSession } from "@/features/auth/lib/session-storage";

const pageSize = 20;
const transferStatuses: TransferStatus[] = ["REQUESTED", "APPROVED", "REJECTED", "IN_TRANSIT", "PARTIALLY_RECEIVED", "RECEIVED", "CANCELLED"];
const defaultFilters: TransferFilters = { page: 1, pageSize, sourceBranchId: "", destinationBranchId: "", status: "", fromDate: "", toDate: "", query: "" };

type ActionState = { type: "approve" | "reject" | "dispatch" | "cancel"; transfer: Transfer } | null;

export function TransfersScreen() {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<TransferFilters>(defaultFilters);
  const [draftFilters, setDraftFilters] = useState<TransferFilters>(defaultFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const [action, setAction] = useState<ActionState>(null);
  const [cancelReason, setCancelReason] = useState("");
  const activeSession = getStoredSession();
  const activeBranchId = activeSession?.branchId ?? null;
  const canOperateAllBranches = activeSession?.roleCode === "COMPANY_ADMIN";
  const canView = canReadTransfers();
  const canRequest = canRequestTransfers();
  const canApprove = canApproveTransfers();
  const canReject = canRejectTransfers();
  const canDispatch = canDispatchTransfers();
  const canReceive = canReceiveTransfers();
  const canCancel = canCancelTransfers();
  const canReadBranches = canAny(["settings.view"]);

  const transfersQuery = useQuery({ queryKey: transfersQueryKeys.list(filters), queryFn: () => listTransfers(filters), enabled: canView });
  const branchesQuery = useQuery({ queryKey: ["transfers", "branches"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const branchOptions = useMemo(() => withSessionBranchFallback((branchesQuery.data?.items ?? []).map((branch) => ({ value: branch.id, label: `${branch.name} (${branch.code})` }))), [branchesQuery.data?.items]);
  const activeAdvancedFilters = [filters.sourceBranchId, filters.destinationBranchId, filters.status, filters.fromDate, filters.toDate].filter(Boolean).length;

  const approveMutation = useTransferActionMutation(() => action?.transfer.id ? approveTransfer(action.transfer.id) : Promise.reject(new Error("Transfer not selected")), t("transfers.messages.approved"), queryClient, () => setAction(null));
  const rejectMutation = useTransferActionMutation(() => action?.transfer.id ? rejectTransfer(action.transfer.id) : Promise.reject(new Error("Transfer not selected")), t("transfers.messages.rejected"), queryClient, () => setAction(null));
  const dispatchMutation = useTransferActionMutation(() => action?.transfer.id ? dispatchTransfer(action.transfer.id) : Promise.reject(new Error("Transfer not selected")), t("transfers.messages.dispatched"), queryClient, () => setAction(null));
  const cancelMutation = useTransferActionMutation(() => action?.transfer.id ? cancelTransfer(action.transfer.id, cancelReason) : Promise.reject(new Error("Transfer not selected")), t("transfers.messages.cancelled"), queryClient, () => { setCancelReason(""); setAction(null); });

  const columns = useMemo<ColumnDef<Transfer>[]>(() => [
    { header: t("transfers.fields.requestedAt"), cell: ({ row }) => formatTransferDate(row.original.requestedAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("transfers.fields.transferNumber"), cell: ({ row }) => <Link className="font-mono text-xs font-bold text-primary hover:underline" href={`/transfers/${row.original.id}`}>{row.original.transferNumber}</Link> },
    { header: t("transfers.fields.sourceBranch"), accessorKey: "sourceBranchName" },
    { header: t("transfers.fields.destinationBranch"), accessorKey: "destinationBranchName" },
    { header: t("transfers.fields.status"), cell: ({ row }) => <TransferStatusBadge status={row.original.status} label={t(`transfers.status.${row.original.status}`)} /> },
    { header: t("transfers.fields.totalItems"), cell: ({ row }) => row.original.details.length || "-" },
    { header: t("transfers.fields.requestedBy"), cell: ({ row }) => displayUser(row.original.requestedByUsername, row.original.requestedBy) },
    { header: t("admin.fields.actions"), cell: ({ row }) => <TransferRowActions transfer={row.original} activeBranchId={activeBranchId} canOperateAllBranches={canOperateAllBranches} canApprove={canApprove} canReject={canReject} canDispatch={canDispatch} canReceive={canReceive} canCancel={canCancel} onAction={setAction} onDetail={() => router.push(`/transfers/${row.original.id}`)} /> },
  ], [activeBranchId, canApprove, canCancel, canDispatch, canOperateAllBranches, canReceive, canReject, locale, router, t, timeZone]);

  if (!canView) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  const currentError = transfersQuery.error ?? (canReadBranches ? branchesQuery.error : null) ?? approveMutation.error ?? rejectMutation.error ?? dispatchMutation.error ?? cancelMutation.error;

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.14),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.34))] shadow-sm">
        <CardContent className="grid gap-3 p-5 md:grid-cols-[1fr_auto_auto] md:items-end">
          <div className="space-y-2">
            <Label>{t("catalog.search")}</Label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
              <Input className="pl-9" value={filters.query} onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value, page: 1 }))} placeholder={t("transfers.filters.searchPlaceholder")} />
            </div>
          </div>
          <Button className="gap-2" type="button" variant="outline" onClick={() => { setDraftFilters(filters); setIsFiltersOpen(true); }}>
            <Filter className="h-4 w-4" aria-hidden="true" />{t("transfers.filters.button")}
            {activeAdvancedFilters > 0 ? <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-bold text-primary-foreground">{activeAdvancedFilters}</span> : null}
          </Button>
          {canRequest ? <Button type="button" onClick={() => router.push("/transfers/new")}><Plus className="mr-2 h-4 w-4" aria-hidden="true" />{t("transfers.actions.new")}</Button> : null}
        </CardContent>
      </Card>

      {currentError ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(currentError, t)}</CardContent></Card> : null}

      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-xl font-black tracking-[-0.04em]">{t("transfers.sections.records")}</h2>
          {transfersQuery.data ? <div className="flex items-center gap-2 text-sm text-muted-foreground"><span>{transfersQuery.data.totalItems} items</span><Button type="button" variant="outline" disabled={transfersQuery.data.page <= 1} onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))}>Prev</Button><span>{transfersQuery.data.page}/{Math.max(transfersQuery.data.totalPages, 1)}</span><Button type="button" variant="outline" disabled={transfersQuery.data.page >= transfersQuery.data.totalPages} onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}>Next</Button></div> : null}
        </div>
        <AdminTable columns={columns} data={transfersQuery.data?.items ?? []} />
      </section>

      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("transfers.filters.title")} description={t("transfers.filters.description")}>
        <div className="grid gap-4 md:grid-cols-3">
          <Select label={t("transfers.fields.sourceBranch")} value={draftFilters.sourceBranchId} onChange={(value) => setDraftFilters((current) => ({ ...current, sourceBranchId: value }))} options={branchOptions} />
          <Select label={t("transfers.fields.destinationBranch")} value={draftFilters.destinationBranchId} onChange={(value) => setDraftFilters((current) => ({ ...current, destinationBranchId: value }))} options={branchOptions} />
          <Select label={t("transfers.fields.status")} value={draftFilters.status} onChange={(value) => setDraftFilters((current) => ({ ...current, status: value as TransferStatus | "" }))} options={transferStatuses.map((status) => ({ value: status, label: t(`transfers.status.${status}`) }))} />
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <div className="flex flex-wrap gap-2 md:col-span-3">
            <Button type="button" onClick={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }}>{t("transfers.filters.apply")}</Button>
            <Button type="button" variant="outline" onClick={() => setDraftFilters({ ...defaultFilters, query: filters.query })}>{t("transfers.filters.clear")}</Button>
            <Button type="button" variant="ghost" onClick={() => setIsFiltersOpen(false)}>{t("admin.actions.cancel")}</Button>
          </div>
        </div>
      </FormDialog>

      <TransferActionDialog action={action} cancelReason={cancelReason} setCancelReason={setCancelReason} onOpenChange={(open) => { if (!open) { setAction(null); setCancelReason(""); } }} onConfirm={() => {
        if (action?.type === "approve") approveMutation.mutate();
        if (action?.type === "reject") rejectMutation.mutate();
        if (action?.type === "dispatch") dispatchMutation.mutate();
        if (action?.type === "cancel") cancelMutation.mutate();
      }} isPending={approveMutation.isPending || rejectMutation.isPending || dispatchMutation.isPending || cancelMutation.isPending} />
    </div>
  );
}

function TransferRowActions({ transfer, activeBranchId, canOperateAllBranches, canApprove, canReject, canDispatch, canReceive, canCancel, onAction, onDetail }: Readonly<{ transfer: Transfer; activeBranchId: string | null; canOperateAllBranches: boolean; canApprove: boolean; canReject: boolean; canDispatch: boolean; canReceive: boolean; canCancel: boolean; onAction: (action: ActionState) => void; onDetail: () => void }>) {
  const t = useTranslations();
  const canOperateSource = canOperateAllBranches || transfer.sourceBranchId === activeBranchId;
  const canOperateDestination = canOperateAllBranches || transfer.destinationBranchId === activeBranchId;
  return <div className="flex flex-wrap gap-2"><Button variant="outline" onClick={onDetail}>{t("transfers.actions.view")}</Button>{canApprove && canOperateSource && transfer.status === "REQUESTED" ? <Button variant="outline" onClick={() => onAction({ type: "approve", transfer })}>{t("transfers.actions.approve")}</Button> : null}{canReject && canOperateSource && transfer.status === "REQUESTED" ? <Button variant="outline" onClick={() => onAction({ type: "reject", transfer })}>{t("transfers.actions.reject")}</Button> : null}{canDispatch && canOperateSource && transfer.status === "APPROVED" ? <Button variant="outline" onClick={() => onAction({ type: "dispatch", transfer })}>{t("transfers.actions.dispatch")}</Button> : null}{canReceive && canOperateDestination && ["APPROVED", "IN_TRANSIT", "PARTIALLY_RECEIVED"].includes(transfer.status) ? <Button variant="outline" onClick={onDetail}>{t("transfers.actions.receive")}</Button> : null}{canCancel && (canOperateSource || canOperateDestination) && !["CANCELLED", "RECEIVED", "REJECTED"].includes(transfer.status) ? <Button variant="outline" onClick={() => onAction({ type: "cancel", transfer })}>{t("transfers.actions.cancelTransfer")}</Button> : null}</div>;
}

function TransferActionDialog({ action, cancelReason, setCancelReason, onOpenChange, onConfirm, isPending }: Readonly<{ action: ActionState; cancelReason: string; setCancelReason: (value: string) => void; onOpenChange: (open: boolean) => void; onConfirm: () => void; isPending: boolean }>) {
  const t = useTranslations();
  if (!action) return null;
  const requiresReason = action.type === "cancel";
  return <FormDialog open={Boolean(action)} onOpenChange={onOpenChange} title={t(`transfers.dialogs.${action.type}.title`)} description={t(`transfers.dialogs.${action.type}.description`)}><div className="space-y-4">{requiresReason ? <div className="space-y-2"><Label>{t("transfers.fields.reason")}</Label><Input value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} /></div> : null}<div className="flex flex-wrap justify-end gap-2"><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>{t("admin.actions.cancel")}</Button><Button type="button" disabled={isPending || (requiresReason && cancelReason.trim().length === 0)} onClick={onConfirm}>{requiresReason ? t("admin.actions.confirm") : t(`transfers.actions.${action.type}`)}</Button></div></div></FormDialog>;
}

function Select({ label, value, onChange, options }: Readonly<{ label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }> }>) {
  const t = useTranslations();
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)}><option value="">{t("transfers.filters.all")}</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>;
}

function DateFilter({ label, value, onChange }: Readonly<{ label: string; value: string; onChange: (value: string) => void }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type="date" value={value} onChange={(event) => onChange(event.target.value)} /></div>;
}

function useTransferActionMutation(mutationFn: () => Promise<Transfer>, successMessage: string, queryClient: ReturnType<typeof useQueryClient>, onSuccess: () => void) {
  return useMutation({
    mutationFn,
    onSuccess: (transfer) => {
      toast.success(successMessage);
      invalidateTransferImpact(queryClient, transfer.id);
      onSuccess();
    },
  });
}

export function invalidateTransferImpact(queryClient: ReturnType<typeof useQueryClient>, transferId: string) {
  queryClient.invalidateQueries({ queryKey: transfersQueryKeys.all });
  queryClient.invalidateQueries({ queryKey: transfersQueryKeys.detail(transferId) });
  queryClient.invalidateQueries({ queryKey: ["inventory", "balances"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "layers"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "movements"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "kardex"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "summary"] });
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}

function displayUser(username: string | null | undefined, userId: string | null | undefined) {
  return username ?? userId?.slice(0, 8) ?? "-";
}
