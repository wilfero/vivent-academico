"use client";

import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Filter, Plus, Search } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useAppLocale } from "@/components/providers/locale-provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { listBranches } from "@/features/admin/shared/admin-api";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { withSessionBranchFallback } from "@/features/auth/lib/session-branch";
import { listSales, salesQueryKeys } from "@/features/sales/api/sales-api";
import { SaleStatusBadge, formatDateTime, formatMoney } from "@/features/sales/components/sale-shared";
import { canCreateSales, canReadSales } from "@/features/sales/lib/sale-permissions";
import type { Sale, SaleFilters, SaleStatus } from "@/features/sales/types/sale-types";
import { formatPermissionError } from "@/lib/api/errors";

const pageSize = 20;
const saleStatuses: SaleStatus[] = ["DRAFT", "CONFIRMED", "CANCELLED"];
const defaultFilters: SaleFilters = { page: 1, pageSize, branchId: "", customerId: "", status: "", paymentMethod: "", fromDate: "", toDate: "", query: "" };

export function SalesScreen() {
  const t = useTranslations();
  const router = useRouter();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<SaleFilters>(defaultFilters);
  const [draftFilters, setDraftFilters] = useState<SaleFilters>(defaultFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canReadSales();
  const canCreate = canCreateSales();
  const canReadBranches = canAny(["settings.view"]);
  const branchesQuery = useQuery({ queryKey: ["sales", "branches"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const branchOptions = useMemo(() => withSessionBranchFallback((branchesQuery.data?.items ?? []).map((branch) => ({ value: branch.id, label: branch.name }))), [branchesQuery.data?.items]);
  const singleBranchId = branchOptions.length === 1 ? branchOptions[0].value : "";
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const salesQuery = useQuery({ queryKey: salesQueryKeys.list(effectiveFilters), queryFn: () => listSales(effectiveFilters), enabled: canView });
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.status].filter(Boolean).length;

  const columns = useMemo<ColumnDef<Sale>[]>(() => [
    { header: t("sales.fields.saleDate"), cell: ({ row }) => formatDateTime(row.original.saleDate, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("sales.fields.saleNumber"), cell: ({ row }) => <Link className="font-mono text-xs font-bold text-primary hover:underline" href={`/sales/${row.original.id}`}>{row.original.saleNumber}</Link> },
    { header: t("sales.fields.customer"), cell: ({ row }) => row.original.customerName ?? t("sales.finalConsumer") },
    { header: t("sales.fields.branch"), accessorKey: "branchName" },
    { header: t("sales.fields.status"), cell: ({ row }) => <SaleStatusBadge status={row.original.status} label={t(`sales.status.${row.original.status}`)} /> },
    { header: t("sales.fields.totalAmount"), cell: ({ row }) => formatMoney(row.original.totalAmount) },
    { header: t("sales.fields.paymentMethod"), cell: ({ row }) => t(`sales.paymentMethods.${row.original.paymentMethod}`) },
    { header: t("sales.fields.createdBy"), cell: ({ row }) => displayUser(row.original.createdByUsername, row.original.createdBy) },
    { header: t("admin.fields.actions"), cell: ({ row }) => <Button variant="outline" onClick={() => router.push(`/sales/${row.original.id}`)}>{t("sales.actions.view")}</Button> },
  ], [locale, router, t, timeZone]);

  if (!canView) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.14),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.34))] shadow-sm">
        <CardContent className="grid gap-3 p-5 md:grid-cols-[1fr_auto_auto] md:items-end">
          <div className="space-y-2">
            <Label>{t("catalog.search")}</Label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
              <Input className="pl-9" value={filters.query} onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value, page: 1 }))} placeholder={t("sales.filters.searchPlaceholder")} />
            </div>
          </div>
          <Button className="gap-2" type="button" variant="outline" onClick={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }}>
            <Filter className="h-4 w-4" aria-hidden="true" />{t("sales.filters.button")}
            {activeAdvancedFilters > 0 ? <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-bold text-primary-foreground">{activeAdvancedFilters}</span> : null}
          </Button>
          {canCreate ? <Button type="button" onClick={() => router.push("/sales/new")}><Plus className="mr-2 h-4 w-4" aria-hidden="true" />{t("sales.actions.new")}</Button> : null}
        </CardContent>
      </Card>

      {salesQuery.error || (canReadBranches && branchesQuery.error) ? <Card><CardContent className="p-6 text-sm text-destructive">{firstErrorMessage(t, salesQuery.error, canReadBranches ? branchesQuery.error : null) ?? t("admin.states.error")}</CardContent></Card> : null}

      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-xl font-black tracking-[-0.04em]">{t("sales.sections.records")}</h2>
          {salesQuery.data ? <div className="flex items-center gap-2 text-sm text-muted-foreground"><span>{salesQuery.data.totalItems} items</span><Button type="button" variant="outline" disabled={salesQuery.data.page <= 1} onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))}>Prev</Button><span>{salesQuery.data.page}/{Math.max(salesQuery.data.totalPages, 1)}</span><Button type="button" variant="outline" disabled={salesQuery.data.page >= salesQuery.data.totalPages} onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}>Next</Button></div> : null}
        </div>
        <AdminTable columns={columns} data={salesQuery.data?.items ?? []} />
      </section>

      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("sales.filters.title")} description={t("sales.filters.description")}>
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("sales.fields.branch")} value={draftFilters.branchId} onChange={(value) => setDraftFilters((current) => ({ ...current, branchId: value }))} options={branchOptions} />
          <Select label={t("sales.fields.status")} value={draftFilters.status} onChange={(value) => setDraftFilters((current) => ({ ...current, status: value as SaleStatus | "" }))} options={saleStatuses.map((status) => ({ value: status, label: t(`sales.status.${status}`) }))} />
          <div className="rounded-2xl border bg-muted/35 p-3 text-xs text-muted-foreground md:col-span-2">{t("sales.placeholders.customerFilter")}</div>
          <div className="flex flex-wrap gap-2 md:col-span-2">
            <Button type="button" onClick={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }}>{t("sales.filters.apply")}</Button>
            <Button type="button" variant="outline" onClick={() => setDraftFilters({ ...defaultFilters, branchId: singleBranchId, query: filters.query })}>{t("sales.filters.clear")}</Button>
            <Button type="button" variant="ghost" onClick={() => setIsFiltersOpen(false)}>{t("admin.actions.cancel")}</Button>
          </div>
        </div>
      </FormDialog>
    </div>
  );
}

function Select({ label, value, onChange, options }: Readonly<{ label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }> }>) {
  const t = useTranslations();
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)}><option value="">{t("sales.filters.all")}</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>;
}

function firstErrorMessage(t: (key: string, values?: Record<string, string>) => string, ...errors: unknown[]) {
  const error = errors.find(Boolean);
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : null);
}

function displayUser(username: string | null | undefined, userId: string | null | undefined) {
  return username ?? userId?.slice(0, 8) ?? "-";
}
