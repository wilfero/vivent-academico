"use client";

import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Filter, Plus, Search } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { listBranches } from "@/features/admin/shared/admin-api";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { withSessionBranchFallback } from "@/features/auth/lib/session-branch";
import { listPurchases, listSuppliers, purchasesQueryKeys } from "@/features/purchases/api/purchases-api";
import { PurchaseFormScreen } from "@/features/purchases/components/purchase-form";
import { formatDate, formatMoney, PurchaseStatusBadge } from "@/features/purchases/components/purchase-shared";
import { canCreatePurchases, canReadPurchaseBranches, canReadPurchaseSuppliers, canReadPurchases } from "@/features/purchases/lib/purchase-permissions";
import type { Purchase, PurchaseFilters, PurchaseStatus } from "@/features/purchases/types/purchase-types";
import { formatPermissionError } from "@/lib/api/errors";

const pageSize = 20;
const purchaseStatuses: PurchaseStatus[] = ["DRAFT", "PENDING_APPROVAL", "CONFIRMED", "VOIDED"];
const defaultFilters: PurchaseFilters = { page: 1, pageSize, branchId: "", supplierId: "", status: "", fromDate: "", toDate: "", query: "" };

export function PurchasesScreen({ mode = "list" }: Readonly<{ mode?: "list" | "new" }>) {
  if (mode === "new") return <PurchaseFormScreen />;
  return <PurchasesList />;
}

function PurchasesList() {
  const t = useTranslations();
  const router = useRouter();
  const [filters, setFilters] = useState<PurchaseFilters>(defaultFilters);
  const [draftFilters, setDraftFilters] = useState<PurchaseFilters>(defaultFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canReadPurchases();
  const canCreate = canCreatePurchases();
  const canReadBranches = canReadPurchaseBranches();
  const canReadSuppliers = canReadPurchaseSuppliers();
  const branchesQuery = useQuery({ queryKey: ["purchases", "branches"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const suppliersQuery = useQuery({ queryKey: purchasesQueryKeys.suppliers, queryFn: () => listSuppliers("", true), enabled: canView && canReadSuppliers });
  const branchOptions = useMemo(() => withSessionBranchFallback((branchesQuery.data?.items ?? []).map((branch) => ({ value: branch.id, label: branch.name }))), [branchesQuery.data?.items]);
  const singleBranchId = branchOptions.length === 1 ? branchOptions[0].value : "";
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const purchasesQuery = useQuery({ queryKey: purchasesQueryKeys.list(effectiveFilters), queryFn: () => listPurchases(effectiveFilters), enabled: canView });
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.supplierId, effectiveFilters.status].filter(Boolean).length;

  const columns = useMemo<ColumnDef<Purchase>[]>(() => [
    { header: t("purchases.fields.purchaseNumber"), cell: ({ row }) => <Link className="font-mono text-xs font-bold text-primary hover:underline" href={`/purchases/${row.original.id}`}>{row.original.purchaseNumber}</Link> },
    { header: t("purchases.fields.purchaseDate"), cell: ({ row }) => formatDate(row.original.purchaseDate) },
    { header: t("purchases.fields.supplier"), accessorKey: "supplierName" },
    { header: t("purchases.fields.branch"), accessorKey: "branchName" },
    { header: t("purchases.fields.paymentType"), cell: ({ row }) => t(`purchases.paymentTypes.${row.original.paymentType}`) },
    { header: t("purchases.fields.createdBy"), cell: ({ row }) => displayUser(row.original.createdByUsername, row.original.createdBy) },
    { header: t("purchases.fields.totalAmount"), cell: ({ row }) => formatMoney(row.original.totalAmount) },
    { header: t("purchases.fields.status"), cell: ({ row }) => <PurchaseStatusBadge status={row.original.status} label={t(`purchases.status.${row.original.status}`)} /> },
    { header: t("admin.fields.actions"), cell: ({ row }) => <Button variant="outline" onClick={() => router.push(`/purchases/${row.original.id}`)}>{t("purchases.actions.view")}</Button> },
  ], [router, t]);

  if (!canView) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.10),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.34))] shadow-sm">
        <CardContent className="grid gap-3 p-5 md:grid-cols-[1fr_auto_auto] md:items-end">
          <div className="space-y-2">
            <Label>{t("catalog.search")}</Label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
              <Input className="pl-9" value={filters.query} onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value, page: 1 }))} placeholder={t("purchases.filters.searchPlaceholder")} />
            </div>
          </div>
          <Button className="gap-2" type="button" variant="outline" onClick={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }}>
            <Filter className="h-4 w-4" aria-hidden="true" />{t("purchases.filters.button")}
            {activeAdvancedFilters > 0 ? <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-bold text-primary-foreground">{activeAdvancedFilters}</span> : null}
          </Button>
          {canCreate ? <Button type="button" onClick={() => router.push("/purchases/new")}><Plus className="mr-2 h-4 w-4" aria-hidden="true" />{t("purchases.actions.new")}</Button> : null}
        </CardContent>
      </Card>

      {purchasesQuery.error || (canReadBranches && branchesQuery.error) || (canReadSuppliers && suppliersQuery.error) ? <Card><CardContent className="p-6 text-sm text-destructive">{firstErrorMessage(t, purchasesQuery.error, canReadBranches ? branchesQuery.error : null, canReadSuppliers ? suppliersQuery.error : null) ?? t("admin.states.error")}</CardContent></Card> : null}

      <TableSection page={purchasesQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))}>
        <AdminTable columns={columns} data={purchasesQuery.data?.items ?? []} />
      </TableSection>

      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("purchases.filters.title")} description={t("purchases.filters.description")}>
        <div className="grid gap-4 md:grid-cols-3">
          <Select label={t("purchases.fields.branch")} value={draftFilters.branchId} onChange={(value) => setDraftFilters((current) => ({ ...current, branchId: value }))} options={branchOptions} />
          <Select label={t("purchases.fields.supplier")} value={draftFilters.supplierId} onChange={(value) => setDraftFilters((current) => ({ ...current, supplierId: value }))} options={(suppliersQuery.data?.items ?? []).map((supplier) => ({ value: supplier.id, label: supplier.name }))} />
          <Select label={t("purchases.fields.status")} value={draftFilters.status} onChange={(value) => setDraftFilters((current) => ({ ...current, status: value as PurchaseStatus | "" }))} options={purchaseStatuses.map((status) => ({ value: status, label: t(`purchases.status.${status}`) }))} />
          <div className="flex flex-wrap gap-2 md:col-span-3">
            <Button type="button" onClick={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }}>{t("purchases.filters.apply")}</Button>
            <Button type="button" variant="outline" onClick={() => setDraftFilters({ ...defaultFilters, branchId: singleBranchId, query: filters.query })}>{t("purchases.filters.clear")}</Button>
            <Button type="button" variant="ghost" onClick={() => setIsFiltersOpen(false)}>{t("admin.actions.cancel")}</Button>
          </div>
        </div>
      </FormDialog>
    </div>
  );
}

function TableSection({ page, onPageChange, children }: Readonly<{ page?: { page: number; totalPages: number; totalItems: number }; onPageChange: (page: number) => void; children: React.ReactNode }>) {
  const t = useTranslations();
  return <section className="space-y-3"><div className="flex flex-wrap items-center justify-between gap-2"><h2 className="text-xl font-black tracking-[-0.04em]">{t("purchases.sections.records")}</h2>{page ? <div className="flex items-center gap-2 text-sm text-muted-foreground"><span>{page.totalItems} items</span><Button type="button" variant="outline" disabled={page.page <= 1} onClick={() => onPageChange(page.page - 1)}>Prev</Button><span>{page.page}/{Math.max(page.totalPages, 1)}</span><Button type="button" variant="outline" disabled={page.page >= page.totalPages} onClick={() => onPageChange(page.page + 1)}>Next</Button></div> : null}</div>{children}</section>;
}

function Select({ label, value, onChange, options }: Readonly<{ label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }> }>) {
  const t = useTranslations();
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)}><option value="">{t("purchases.filters.all")}</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>;
}

function firstErrorMessage(t: (key: string, values?: Record<string, string>) => string, ...errors: unknown[]) {
  const error = errors.find(Boolean);
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : null);
}

function displayUser(username: string | null | undefined, userId: string | null | undefined) {
  return username ?? userId?.slice(0, 8) ?? "-";
}
