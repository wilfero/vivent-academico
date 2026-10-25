"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { AlertTriangle, Boxes, ClipboardList, Filter, Layers, PackageCheck, PackageSearch, ReceiptText, Repeat2, Scale, Search, WalletCards } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAppLocale } from "@/components/providers/locale-provider";
import { listCategories, listProducts } from "@/features/admin/catalog/shared/catalog-api";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { listBranches } from "@/features/admin/shared/admin-api";
import { withSessionBranchFallback, type BranchOption } from "@/features/auth/lib/session-branch";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { listAdjustments } from "@/features/adjustments/api/adjustments-api";
import { AdjustmentStatusBadge, AdjustmentTypeBadge, formatAdjustmentNumber } from "@/features/adjustments/components/adjustment-shared";
import type { Adjustment, AdjustmentFilters, AdjustmentStatus } from "@/features/adjustments/types/adjustment-types";
import { getInventorySummary, listInventoryBalances, listInventoryMovements, listKardex } from "@/features/inventory/api/inventory-api";
import { InventorySourceLabel, MovementTypeBadge, StockStatusBadge } from "@/features/inventory/components/inventory-badges";
import type { InventoryBalance, InventoryFilters, StockMovement } from "@/features/inventory/types/inventory-types";
import { listPurchases, listSuppliers } from "@/features/purchases/api/purchases-api";
import { PurchaseStatusBadge } from "@/features/purchases/components/purchase-shared";
import type { Purchase, PurchaseFilters, PurchaseStatus } from "@/features/purchases/types/purchase-types";
import { canApproveCashClosures, canCreateCashClosures, canViewCashClosures, canViewInventoryReport, canViewKardexReport, canViewMovementsReport, canViewProductsReport, canViewPurchasesReport, canViewReports, canViewSalesReport } from "@/features/reports/lib/report-permissions";
import { approveCashClosure, closeCashClosure, createCashClosure, getAdjustmentsReportSummary, getCashClosure, getCashClosurePreview, getCashClosuresReportSummary, getProductsReport, getPurchasesReportSummary, getSalesReportSummary, getTransfersReportSummary, listCashClosures, reopenCashClosure } from "@/features/reports/api/reports-api";
import type { CashClosure, CashClosureFilters, CashClosureStatus, ProductReportItem } from "@/features/reports/types/report-types";
import { listSales } from "@/features/sales/api/sales-api";
import { SaleStatusBadge } from "@/features/sales/components/sale-shared";
import type { Sale, SaleFilters, SalePaymentMethod, SaleStatus } from "@/features/sales/types/sale-types";
import { listTransfers } from "@/features/transfers/api/transfers-api";
import { formatTransferNumber, TransferStatusBadge } from "@/features/transfers/components/transfer-shared";
import type { Transfer, TransferFilters, TransferStatus } from "@/features/transfers/types/transfer-types";
import { formatApiErrorMessage } from "@/lib/api/errors";

const pageSize = 20;
const saleStatuses: SaleStatus[] = ["DRAFT", "CONFIRMED", "CANCELLED"];
const salePaymentMethods: SalePaymentMethod[] = ["CASH", "CARD", "TRANSFER", "CREDIT", "OTHER"];
const purchaseStatuses: PurchaseStatus[] = ["DRAFT", "PENDING_APPROVAL", "CONFIRMED", "VOIDED"];
const transferStatuses: TransferStatus[] = ["REQUESTED", "APPROVED", "REJECTED", "IN_TRANSIT", "PARTIALLY_RECEIVED", "RECEIVED", "CANCELLED"];
const adjustmentStatuses: AdjustmentStatus[] = ["DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED"];
const movementTypes = ["PURCHASE_IN", "SALE_OUT", "SALE_CANCEL_IN", "PURCHASE_VOID_OUT", "TRANSFER_RESERVED", "TRANSFER_OUT", "TRANSFER_IN", "ADJUSTMENT_IN", "ADJUSTMENT_OUT", "STOCK_COUNT_IN", "STOCK_COUNT_OUT", "LOSS_OUT", "DAMAGE_OUT", "EXPIRED_OUT"] as const;

const defaultInventoryFilters: InventoryFilters = { branchId: "", locationId: "", productId: "", categoryId: "", query: "", movementType: "", fromDate: "", toDate: "", page: 1, pageSize };
const defaultSaleFilters: SaleFilters = { page: 1, pageSize, branchId: "", customerId: "", status: "", paymentMethod: "", fromDate: "", toDate: "", query: "" };
const defaultPurchaseFilters: PurchaseFilters = { page: 1, pageSize, branchId: "", supplierId: "", status: "", fromDate: "", toDate: "", query: "" };
const defaultTransferFilters: TransferFilters = { page: 1, pageSize, sourceBranchId: "", destinationBranchId: "", status: "", fromDate: "", toDate: "", query: "" };
const defaultAdjustmentFilters: AdjustmentFilters = { page: 1, pageSize, branchId: "", status: "", fromDate: "", toDate: "", query: "" };
const defaultClosureFilters: CashClosureFilters = { page: 1, pageSize, branchId: "", status: "", fromDate: "", toDate: "" };
const closureStatuses: CashClosureStatus[] = ["OPEN", "CLOSED", "APPROVED", "REOPENED"];

export function ReportsDashboardPageContent() {
  return <ReportPage titleKey="reports.dashboard.title" descriptionKey="reports.dashboard.description"><ReportsDashboardScreen /></ReportPage>;
}

export function InventoryReportPageContent() {
  return <ReportPage titleKey="reports.inventory.title" descriptionKey="reports.inventory.description"><InventoryReportScreen /></ReportPage>;
}

export function KardexReportPageContent() {
  return <ReportPage titleKey="reports.kardex.title" descriptionKey="reports.kardex.description"><KardexReportScreen /></ReportPage>;
}

export function MovementsReportPageContent() {
  return <ReportPage titleKey="reports.movements.title" descriptionKey="reports.movements.description"><MovementsReportScreen /></ReportPage>;
}

export function ProductsReportPageContent() {
  return <ReportPage titleKey="reports.products.title" descriptionKey="reports.products.description"><ProductsReportScreen /></ReportPage>;
}

export function SalesReportPageContent() {
  return <ReportPage titleKey="reports.sales.title" descriptionKey="reports.sales.description"><SalesReportScreen /></ReportPage>;
}

export function PurchasesReportPageContent() {
  return <ReportPage titleKey="reports.purchases.title" descriptionKey="reports.purchases.description"><PurchasesReportScreen /></ReportPage>;
}

export function TransfersReportPageContent() {
  return <ReportPage titleKey="reports.transfers.title" descriptionKey="reports.transfers.description"><TransfersReportScreen /></ReportPage>;
}

export function AdjustmentsReportPageContent() {
  return <ReportPage titleKey="reports.adjustments.title" descriptionKey="reports.adjustments.description"><AdjustmentsReportScreen /></ReportPage>;
}

export function CashClosingsReportPageContent() {
  const t = useTranslations();
  const canView = canViewCashClosures();

  return (
    <AdminPage title={t("reports.cashClosings.title")} description={t("reports.cashClosings.description")} eyebrow={t("navigation.reports")}>
      {canView ? <ClosuresListScreen /> : <AccessDenied />}
    </AdminPage>
  );
}

export function NewClosurePageContent() {
  const t = useTranslations();
  const canCreate = canCreateCashClosures();

  return (
    <AdminPage title={t("reports.cashClosings.newTitle")} description={t("reports.cashClosings.newDescription")} eyebrow={t("navigation.cashClosings")}>
      {canCreate ? <ClosureFormScreen /> : <AccessDenied />}
    </AdminPage>
  );
}

export function ClosureDetailPageContent({ closureId }: Readonly<{ closureId: string }>) {
  const t = useTranslations();
  const canView = canViewCashClosures();

  return (
    <AdminPage title={t("reports.cashClosings.detailTitle")} description={t("reports.cashClosings.detailDescription")} eyebrow={t("navigation.cashClosings")}>
      {canView ? <ClosureDetailScreen closureId={closureId} /> : <AccessDenied />}
    </AdminPage>
  );
}

function ReportPage({ titleKey, descriptionKey, children }: Readonly<{ titleKey: string; descriptionKey: string; children: React.ReactNode }>) {
  const t = useTranslations();

  return <AdminPage title={t(titleKey)} description={t(descriptionKey)} eyebrow={t("navigation.reports")}>{children}</AdminPage>;
}

function ReportsDashboardScreen() {
  const t = useTranslations();
  const canView = canViewReports();
  const canViewInventorySummary = canViewInventoryReport();
  const summaryQuery = useQuery({ queryKey: ["reports", "dashboard", "inventory-summary"], queryFn: getInventorySummary, enabled: canViewInventorySummary });
  const cards = [
    { href: "/reports/sales", icon: ReceiptText, title: t("reports.sales.title"), description: t("reports.dashboard.cards.sales"), enabled: canViewSalesReport() },
    { href: "/reports/purchases", icon: PackageCheck, title: t("reports.purchases.title"), description: t("reports.dashboard.cards.purchases"), enabled: canViewPurchasesReport() },
    { href: "/reports/inventory", icon: Boxes, title: t("reports.inventory.title"), description: t("reports.dashboard.cards.inventory"), enabled: canViewInventoryReport() },
    { href: "/reports/movements", icon: ClipboardList, title: t("reports.movements.title"), description: t("reports.dashboard.cards.movements"), enabled: canViewMovementsReport() },
    { href: "/reports/kardex", icon: Layers, title: t("reports.kardex.title"), description: t("reports.dashboard.cards.kardex"), enabled: canViewKardexReport() },
    { href: "/reports/products", icon: PackageSearch, title: t("reports.products.title"), description: t("reports.dashboard.cards.products"), enabled: canViewProductsReport() },
    { href: "/reports/closures", icon: WalletCards, title: t("reports.closures.title"), description: t("reports.dashboard.cards.closures"), enabled: canViewCashClosures() },
  ].filter((card) => card.enabled);

  if (!canView) return <AccessDenied />;

  return (
    <div className="space-y-6">
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={Boxes} label={t("inventory.summary.withStock")} value={summaryQuery.data?.productsWithStock ?? t("reports.dashboard.noOfficialTotal")} />
        <SummaryCard icon={Boxes} label={t("inventory.summary.withoutStock")} value={summaryQuery.data?.productsWithoutStock ?? t("reports.dashboard.noOfficialTotal")} />
        <SummaryCard icon={AlertTriangle} label={t("inventory.summary.lowStock")} value={summaryQuery.data?.lowStockProducts ?? t("reports.dashboard.noOfficialTotal")} />
        <SummaryCard icon={ClipboardList} label={t("inventory.summary.recentMovements")} value={summaryQuery.data?.totalMovements ?? t("reports.dashboard.noOfficialTotal")} />
      </section>
      <ErrorCard error={summaryQuery.error} />
      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {cards.map((card) => <ReportNavigationCard key={card.href} {...card} />)}
      </section>
      {cards.length === 0 ? <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card> : null}
    </div>
  );
}

function InventoryReportScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [draftFilters, setDraftFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canViewInventoryReport();
  const canReadBranches = canAny(["settings.view"]);
  const branchesQuery = useQuery({ queryKey: ["reports", "branches", "inventory"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const productsQuery = useQuery({ queryKey: ["reports", "products", "inventory"], queryFn: () => listProducts("", "", true), enabled: canView });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const singleBranchId = singleBranchOption(branchOptions);
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const summaryQuery = useQuery({ queryKey: ["reports", "inventory", "summary"], queryFn: getInventorySummary, enabled: canView });
  const balancesQuery = useQuery({ queryKey: ["reports", "inventory", "balances", effectiveFilters], queryFn: () => listInventoryBalances(effectiveFilters), enabled: canView });
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.productId].filter(Boolean).length;
  const columns = useMemo<ColumnDef<InventoryBalance>[]>(() => [
    { header: t("inventory.fields.product"), accessorKey: "productName" },
    { header: t("inventory.fields.branch"), accessorKey: "branchName" },
    { header: t("inventory.fields.onHand"), cell: ({ row }) => formatNumber(row.original.quantityOnHand, locale) },
    { header: t("inventory.fields.reserved"), cell: ({ row }) => formatNumber(row.original.quantityReserved, locale) },
    { header: t("inventory.fields.available"), cell: ({ row }) => formatNumber(row.original.quantityAvailable, locale) },
    { header: t("inventory.fields.unit"), accessorKey: "baseUnitCode" },
    { header: t("inventory.fields.lastUpdate"), cell: ({ row }) => formatDateTime(row.original.updatedAt, timeZone, locale) },
    { header: t("inventory.fields.status"), cell: ({ row }) => <StockStatusBadge quantity={row.original.quantityAvailable} minimumStock={row.original.minimumStock} /> },
  ], [locale, t, timeZone]);

  if (!canView) return <AccessDenied />;

  return (
    <div className="space-y-6">
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={Boxes} label={t("inventory.summary.withStock")} value={summaryQuery.data?.productsWithStock ?? "-"} />
        <SummaryCard icon={Boxes} label={t("inventory.summary.withoutStock")} value={summaryQuery.data?.productsWithoutStock ?? "-"} />
        <SummaryCard icon={AlertTriangle} label={t("inventory.summary.lowStock")} value={summaryQuery.data?.lowStockProducts ?? t("inventory.placeholder.noEndpoint")} />
        <SummaryCard icon={ClipboardList} label={t("inventory.summary.recentMovements")} value={summaryQuery.data?.totalMovements ?? "-"} />
      </section>
      <ReportToolbar query={filters.query} queryPlaceholder={t("inventory.filters.searchPlaceholder")} filterCount={activeAdvancedFilters} onQueryChange={(query) => setFilters((current) => ({ ...current, query, page: 1 }))} onOpenFilters={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }} />
      <ErrorCard error={firstError(summaryQuery.error, balancesQuery.error, canReadBranches ? branchesQuery.error : null, productsQuery.error)} />
      <TableSection title={t("reports.sections.records")} page={balancesQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))} exportFileName="inventory-report" exportRows={balancesQuery.data?.items ?? []}><AdminTable columns={columns} data={balancesQuery.data?.items ?? []} /></TableSection>
      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("reports.filters.title")} description={t("reports.filters.description")}> 
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("inventory.fields.branch")} value={draftFilters.branchId} onChange={(branchId) => setDraftFilters((current) => ({ ...current, branchId }))} options={branchOptions} />
          <Select label={t("inventory.fields.product")} value={draftFilters.productId} onChange={(productId) => setDraftFilters((current) => ({ ...current, productId }))} options={(productsQuery.data?.items ?? []).map((product) => ({ value: product.id, label: `${product.name} (${product.baseUnitCode})` }))} />
          <FilterActions onApply={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }} onClear={() => setDraftFilters({ ...defaultInventoryFilters, branchId: singleBranchId, query: filters.query })} onCancel={() => setIsFiltersOpen(false)} />
        </div>
      </FormDialog>
    </div>
  );
}

function ProductsReportScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [draftFilters, setDraftFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canViewProductsReport();
  const canReadBranches = canAny(["settings.view"]);
  const branchesQuery = useQuery({ queryKey: ["reports", "branches", "products"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const productsQuery = useQuery({ queryKey: ["reports", "products", "lookup"], queryFn: () => listProducts("", "", true), enabled: canView });
  const categoriesQuery = useQuery({ queryKey: ["reports", "categories", "products"], queryFn: () => listCategories("", true), enabled: canView });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const singleBranchId = singleBranchOption(branchOptions);
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const balancesQuery = useQuery({ queryKey: ["reports", "products", "balances", effectiveFilters], queryFn: () => listInventoryBalances(effectiveFilters), enabled: canView });
  const productsReportQuery = useQuery({ queryKey: ["reports", "products", "aggregate", effectiveFilters], queryFn: () => getProductsReport(effectiveFilters), enabled: canView });
  const balances = balancesQuery.data?.items ?? [];
  const productsReport = productsReportQuery.data;
  const visibleOut = balances.filter((balance) => balance.quantityAvailable <= 0).length;
  const visibleLow = balances.filter((balance) => balance.minimumStock !== null && balance.quantityAvailable > 0 && balance.quantityAvailable <= balance.minimumStock).length;
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.categoryId, effectiveFilters.productId, effectiveFilters.fromDate, effectiveFilters.toDate].filter(Boolean).length;
  const columns = useMemo<ColumnDef<InventoryBalance>[]>(() => [
    { header: t("inventory.fields.product"), accessorKey: "productName" },
    { header: t("navigation.categories"), cell: ({ row }) => row.original.categoryName ?? "-" },
    { header: t("inventory.fields.branch"), accessorKey: "branchName" },
    { header: t("inventory.fields.onHand"), cell: ({ row }) => formatNumber(row.original.quantityOnHand, locale) },
    { header: t("inventory.fields.available"), cell: ({ row }) => formatNumber(row.original.quantityAvailable, locale) },
    { header: t("inventory.fields.unit"), accessorKey: "baseUnitCode" },
    { header: t("inventory.fields.status"), cell: ({ row }) => <StockStatusBadge quantity={row.original.quantityAvailable} minimumStock={row.original.minimumStock} /> },
    { header: t("inventory.fields.lastUpdate"), cell: ({ row }) => formatDateTime(row.original.updatedAt, timeZone, locale) },
  ], [locale, t, timeZone]);

  if (!canView) return <AccessDenied />;

  return (
    <div className="space-y-6">
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={PackageSearch} label={t("reports.summary.visibleProducts")} value={balances.length} />
        <SummaryCard icon={AlertTriangle} label={t("reports.summary.visibleOutOfStock")} value={visibleOut} />
        <SummaryCard icon={AlertTriangle} label={t("reports.summary.visibleLowStock")} value={visibleLow} />
        <SummaryCard icon={ClipboardList} label={t("reports.summary.mostMovedProducts")} value={productsReport?.mostMoved.length ?? "-"} />
      </section>
      <ReportToolbar query={filters.query} queryPlaceholder={t("inventory.filters.searchPlaceholder")} filterCount={activeAdvancedFilters} onQueryChange={(query) => setFilters((current) => ({ ...current, query, page: 1 }))} onOpenFilters={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }} />
      <ErrorCard error={firstError(balancesQuery.error, productsReportQuery.error, canReadBranches ? branchesQuery.error : null, productsQuery.error, categoriesQuery.error)} />
      <div className="grid gap-4 xl:grid-cols-2">
        <ProductReportList title={t("reports.products.topSelling")} items={productsReport?.topSelling ?? []} locale={locale} />
        <ProductReportList title={t("reports.products.mostMoved")} items={productsReport?.mostMoved ?? []} locale={locale} />
        <ProductReportList title={t("reports.products.noMovement")} items={productsReport?.noMovement ?? []} locale={locale} />
        <ProductReportList title={t("reports.products.lowStock")} items={productsReport?.lowStock ?? []} locale={locale} />
        <ProductReportList title={t("reports.products.outOfStock")} items={productsReport?.outOfStock ?? []} locale={locale} />
      </div>
      <TableSection title={t("reports.sections.records")} page={balancesQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))} exportFileName="products-report" exportRows={balances}><AdminTable columns={columns} data={balances} /></TableSection>
      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("reports.filters.title")} description={t("reports.filters.description")}> 
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("inventory.fields.branch")} value={draftFilters.branchId} onChange={(branchId) => setDraftFilters((current) => ({ ...current, branchId }))} options={branchOptions} />
          <Select label={t("navigation.categories")} value={draftFilters.categoryId} onChange={(categoryId) => setDraftFilters((current) => ({ ...current, categoryId }))} options={(categoriesQuery.data?.items ?? []).map((category) => ({ value: category.id, label: category.name }))} />
          <Select label={t("inventory.fields.product")} value={draftFilters.productId} onChange={(productId) => setDraftFilters((current) => ({ ...current, productId }))} options={(productsQuery.data?.items ?? []).map((product) => ({ value: product.id, label: `${product.name} (${product.baseUnitCode})` }))} />
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <FilterActions onApply={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }} onClear={() => setDraftFilters({ ...defaultInventoryFilters, branchId: singleBranchId, query: filters.query })} onCancel={() => setIsFiltersOpen(false)} />
        </div>
      </FormDialog>
    </div>
  );
}

function KardexReportScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [draftFilters, setDraftFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canViewKardexReport();
  const canReadBranches = canAny(["settings.view"]);
  const branchesQuery = useQuery({ queryKey: ["reports", "branches", "kardex"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const productsQuery = useQuery({ queryKey: ["reports", "products", "kardex"], queryFn: () => listProducts("", "", true), enabled: canView });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const singleBranchId = singleBranchOption(branchOptions);
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const kardexQuery = useQuery({ queryKey: ["reports", "kardex", effectiveFilters], queryFn: () => listKardex(effectiveFilters), enabled: canView });
  const movements = kardexQuery.data?.items ?? [];
  const totals = summarizeMovements(movements);
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.productId, effectiveFilters.fromDate, effectiveFilters.toDate].filter(Boolean).length;
  const columns = useMemo<ColumnDef<StockMovement>[]>(() => [
    { header: t("inventory.fields.date"), cell: ({ row }) => formatDateTime(row.original.createdAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("inventory.fields.product"), accessorKey: "productName" },
    { header: t("inventory.fields.branch"), accessorKey: "branchName" },
    { header: t("inventory.fields.type"), cell: ({ row }) => <MovementTypeBadge type={row.original.movementType} /> },
    { header: t("inventory.fields.in"), cell: ({ row }) => formatNumber(row.original.quantityIn, locale) },
    { header: t("inventory.fields.out"), cell: ({ row }) => formatNumber(row.original.quantityOut, locale) },
    { header: t("inventory.fields.balance"), cell: ({ row }) => row.original.balanceAfter === null ? "-" : formatNumber(row.original.balanceAfter, locale) },
    { header: t("inventory.fields.unitCost"), cell: ({ row }) => row.original.unitCost === null ? "-" : formatMoney(row.original.unitCost, locale) },
    { header: t("inventory.fields.totalCost"), cell: ({ row }) => row.original.totalCost === null ? "-" : formatMoney(row.original.totalCost, locale) },
    { header: t("inventory.fields.reference"), cell: ({ row }) => <InventorySourceLabel sourceType={row.original.sourceType} sourceId={row.original.sourceId} /> },
    { header: t("inventory.fields.user"), cell: ({ row }) => row.original.createdByUsername ?? t("topbar.unknown") },
  ], [locale, t, timeZone]);

  if (!canView) return <AccessDenied />;

  return (
    <div className="space-y-6">
      <section className="grid gap-4 md:grid-cols-3">
        <SummaryCard icon={PackageCheck} label={t("reports.summary.visibleIn")} value={formatNumber(totals.quantityIn, locale)} />
        <SummaryCard icon={PackageCheck} label={t("reports.summary.visibleOut")} value={formatNumber(totals.quantityOut, locale)} />
        <SummaryCard icon={ClipboardList} label={t("reports.summary.visibleRecords")} value={movements.length} />
      </section>
      <ReportToolbar query={filters.query} queryPlaceholder={t("inventory.filters.searchPlaceholder")} filterCount={activeAdvancedFilters} onQueryChange={(query) => setFilters((current) => ({ ...current, query, page: 1 }))} onOpenFilters={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }} />
      <ErrorCard error={firstError(kardexQuery.error, canReadBranches ? branchesQuery.error : null, productsQuery.error)} />
      <TableSection title={t("reports.sections.records")} page={kardexQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))} exportFileName="kardex-report" exportRows={movements}><AdminTable columns={columns} data={movements} /></TableSection>
      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("reports.filters.title")} description={t("reports.filters.description")}>
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("inventory.fields.branch")} value={draftFilters.branchId} onChange={(branchId) => setDraftFilters((current) => ({ ...current, branchId }))} options={branchOptions} />
          <Select label={t("inventory.fields.product")} value={draftFilters.productId} onChange={(productId) => setDraftFilters((current) => ({ ...current, productId }))} options={(productsQuery.data?.items ?? []).map((product) => ({ value: product.id, label: `${product.name} (${product.baseUnitCode})` }))} />
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <FilterActions onApply={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }} onClear={() => setDraftFilters({ ...defaultInventoryFilters, branchId: singleBranchId, query: filters.query })} onCancel={() => setIsFiltersOpen(false)} />
        </div>
      </FormDialog>
    </div>
  );
}

function MovementsReportScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [draftFilters, setDraftFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canViewMovementsReport();
  const canReadBranches = canAny(["settings.view"]);
  const branchesQuery = useQuery({ queryKey: ["reports", "branches", "movements"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const productsQuery = useQuery({ queryKey: ["reports", "products", "movements"], queryFn: () => listProducts("", "", true), enabled: canView });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const singleBranchId = singleBranchOption(branchOptions);
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const movementsQuery = useQuery({ queryKey: ["reports", "movements", effectiveFilters], queryFn: () => listInventoryMovements(effectiveFilters), enabled: canView });
  const movements = movementsQuery.data?.items ?? [];
  const totals = summarizeMovements(movements);
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.productId, effectiveFilters.movementType, effectiveFilters.fromDate, effectiveFilters.toDate].filter(Boolean).length;
  const columns = useMemo<ColumnDef<StockMovement>[]>(() => [
    { header: t("inventory.fields.date"), cell: ({ row }) => formatDateTime(row.original.createdAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("inventory.fields.product"), accessorKey: "productName" },
    { header: t("inventory.fields.branch"), accessorKey: "branchName" },
    { header: t("inventory.fields.type"), cell: ({ row }) => <MovementTypeBadge type={row.original.movementType} /> },
    { header: t("inventory.fields.quantity"), cell: ({ row }) => formatNumber(row.original.quantityIn > 0 ? row.original.quantityIn : row.original.quantityOut, locale) },
    { header: t("inventory.fields.unit"), accessorKey: "baseUnitCode" },
    { header: t("inventory.fields.unitCost"), cell: ({ row }) => row.original.unitCost === null ? "-" : formatMoney(row.original.unitCost, locale) },
    { header: t("inventory.fields.reference"), cell: ({ row }) => <InventorySourceLabel sourceType={row.original.sourceType} sourceId={row.original.sourceId} /> },
    { header: t("inventory.fields.user"), cell: ({ row }) => row.original.createdByUsername ?? t("topbar.unknown") },
    { header: t("inventory.fields.notes"), cell: ({ row }) => row.original.notes ?? "-" },
  ], [locale, t, timeZone]);

  if (!canView) return <AccessDenied />;

  return (
    <div className="space-y-6">
      <section className="grid gap-4 md:grid-cols-3">
        <SummaryCard icon={PackageCheck} label={t("reports.summary.visibleIn")} value={formatNumber(totals.quantityIn, locale)} />
        <SummaryCard icon={PackageCheck} label={t("reports.summary.visibleOut")} value={formatNumber(totals.quantityOut, locale)} />
        <SummaryCard icon={ClipboardList} label={t("reports.summary.visibleRecords")} value={movements.length} />
      </section>
      <ReportToolbar query={filters.query} queryPlaceholder={t("inventory.filters.searchPlaceholder")} filterCount={activeAdvancedFilters} onQueryChange={(query) => setFilters((current) => ({ ...current, query, page: 1 }))} onOpenFilters={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }} />
      <ErrorCard error={firstError(movementsQuery.error, canReadBranches ? branchesQuery.error : null, productsQuery.error)} />
      <TableSection title={t("reports.sections.records")} page={movementsQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))} exportFileName="movements-report" exportRows={movements}><AdminTable columns={columns} data={movements} /></TableSection>
      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("reports.filters.title")} description={t("reports.filters.description")}>
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("inventory.fields.branch")} value={draftFilters.branchId} onChange={(branchId) => setDraftFilters((current) => ({ ...current, branchId }))} options={branchOptions} />
          <Select label={t("inventory.fields.product")} value={draftFilters.productId} onChange={(productId) => setDraftFilters((current) => ({ ...current, productId }))} options={(productsQuery.data?.items ?? []).map((product) => ({ value: product.id, label: `${product.name} (${product.baseUnitCode})` }))} />
          <Select label={t("inventory.fields.type")} value={draftFilters.movementType} onChange={(movementType) => setDraftFilters((current) => ({ ...current, movementType: movementType as InventoryFilters["movementType"] }))} options={movementTypes.map((type) => ({ value: type, label: t.has(`inventory.movementTypes.${type}`) ? t(`inventory.movementTypes.${type}`) : type }))} />
          <div />
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <FilterActions onApply={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }} onClear={() => setDraftFilters({ ...defaultInventoryFilters, branchId: singleBranchId, query: filters.query })} onCancel={() => setIsFiltersOpen(false)} />
        </div>
      </FormDialog>
    </div>
  );
}

function SalesReportScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<SaleFilters>(defaultSaleFilters);
  const [draftFilters, setDraftFilters] = useState<SaleFilters>(defaultSaleFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canViewSalesReport();
  const canReadBranches = canAny(["settings.view"]);
  const branchesQuery = useQuery({ queryKey: ["reports", "branches", "sales"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const singleBranchId = singleBranchOption(branchOptions);
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const salesQuery = useQuery({ queryKey: ["reports", "sales", effectiveFilters], queryFn: () => listSales(effectiveFilters), enabled: canView });
  const summaryQuery = useQuery({ queryKey: ["reports", "sales", "summary", effectiveFilters], queryFn: () => getSalesReportSummary(effectiveFilters), enabled: canView });
  const sales = salesQuery.data?.items ?? [];
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.status, effectiveFilters.paymentMethod, effectiveFilters.fromDate, effectiveFilters.toDate].filter(Boolean).length;
  const columns = useMemo<ColumnDef<Sale>[]>(() => [
    { header: t("sales.fields.saleNumber"), cell: ({ row }) => <Link className="font-mono text-xs font-bold text-primary hover:underline" href={`/sales/${row.original.id}`}>{row.original.saleNumber}</Link> },
    { header: t("sales.fields.saleDate"), cell: ({ row }) => formatDateOnly(row.original.saleDate, locale) },
    { header: t("sales.fields.branch"), accessorKey: "branchName" },
    { header: t("sales.fields.paymentMethod"), cell: ({ row }) => t(`sales.paymentMethods.${row.original.paymentMethod}`) },
    { header: t("sales.fields.totalAmount"), cell: ({ row }) => formatMoney(row.original.totalAmount, locale) },
    { header: t("sales.fields.createdAt"), cell: ({ row }) => formatDateTime(row.original.createdAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("sales.fields.status"), cell: ({ row }) => <SaleStatusBadge status={row.original.status} label={t(`sales.status.${row.original.status}`)} /> },
  ], [locale, t, timeZone]);

  if (!canView) return <AccessDenied />;

  return (
    <ListReportShell filters={filters} setFilters={setFilters} queryPlaceholder={t("sales.filters.searchPlaceholder")} filterCount={activeAdvancedFilters} onOpenFilters={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }} error={firstError(salesQuery.error, summaryQuery.error, canReadBranches ? branchesQuery.error : null)} page={salesQuery.data} columns={columns} data={sales}>
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={ReceiptText} label={t("reports.summary.officialSales")} value={summaryQuery.data?.totalSales ?? "-"} />
        <SummaryCard icon={WalletCards} label={t("reports.summary.officialConfirmedTotal")} value={summaryQuery.data ? formatMoney(summaryQuery.data.confirmedTotal, locale) : "-"} />
        <SummaryCard icon={ReceiptText} label={t("reports.summary.officialDiscounts")} value={summaryQuery.data ? formatMoney(summaryQuery.data.discountTotal, locale) : "-"} />
        <SummaryCard icon={AlertTriangle} label={t("reports.summary.officialCancelled")} value={summaryQuery.data?.cancelledSales ?? "-"} />
      </section>
      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("reports.filters.title")} description={t("reports.filters.description")}> 
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("sales.fields.branch")} value={draftFilters.branchId} onChange={(branchId) => setDraftFilters((current) => ({ ...current, branchId }))} options={branchOptions} />
          <Select label={t("sales.fields.status")} value={draftFilters.status} onChange={(status) => setDraftFilters((current) => ({ ...current, status: status as SaleStatus | "" }))} options={saleStatuses.map((status) => ({ value: status, label: t(`sales.status.${status}`) }))} />
          <Select label={t("sales.fields.paymentMethod")} value={draftFilters.paymentMethod} onChange={(paymentMethod) => setDraftFilters((current) => ({ ...current, paymentMethod: paymentMethod as SalePaymentMethod | "" }))} options={salePaymentMethods.map((paymentMethod) => ({ value: paymentMethod, label: t(`sales.paymentMethods.${paymentMethod}`) }))} />
          <div />
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <FilterActions onApply={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }} onClear={() => setDraftFilters({ ...defaultSaleFilters, branchId: singleBranchId, query: filters.query })} onCancel={() => setIsFiltersOpen(false)} />
        </div>
      </FormDialog>
    </ListReportShell>
  );
}

function PurchasesReportScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<PurchaseFilters>(defaultPurchaseFilters);
  const [draftFilters, setDraftFilters] = useState<PurchaseFilters>(defaultPurchaseFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canViewPurchasesReport();
  const canReadBranches = canAny(["settings.view"]);
  const branchesQuery = useQuery({ queryKey: ["reports", "branches", "purchases"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const suppliersQuery = useQuery({ queryKey: ["reports", "suppliers", "purchases"], queryFn: () => listSuppliers("", true), enabled: canView });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const singleBranchId = singleBranchOption(branchOptions);
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const purchasesQuery = useQuery({ queryKey: ["reports", "purchases", effectiveFilters], queryFn: () => listPurchases(effectiveFilters), enabled: canView });
  const summaryQuery = useQuery({ queryKey: ["reports", "purchases", "summary", effectiveFilters], queryFn: () => getPurchasesReportSummary(effectiveFilters), enabled: canView });
  const purchases = purchasesQuery.data?.items ?? [];
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.supplierId, effectiveFilters.status, effectiveFilters.fromDate, effectiveFilters.toDate].filter(Boolean).length;
  const columns = useMemo<ColumnDef<Purchase>[]>(() => [
    { header: t("purchases.fields.purchaseNumber"), cell: ({ row }) => <Link className="font-mono text-xs font-bold text-primary hover:underline" href={`/purchases/${row.original.id}`}>{row.original.purchaseNumber}</Link> },
    { header: t("purchases.fields.purchaseDate"), cell: ({ row }) => formatDateOnly(row.original.purchaseDate, locale) },
    { header: t("purchases.fields.supplier"), accessorKey: "supplierName" },
    { header: t("purchases.fields.branch"), accessorKey: "branchName" },
    { header: t("purchases.fields.totalAmount"), cell: ({ row }) => formatMoney(row.original.totalAmount, locale) },
    { header: t("purchases.fields.confirmedAt"), cell: ({ row }) => formatDateTime(row.original.confirmedAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("purchases.fields.status"), cell: ({ row }) => <PurchaseStatusBadge status={row.original.status} label={t(`purchases.status.${row.original.status}`)} /> },
  ], [locale, t, timeZone]);

  if (!canView) return <AccessDenied />;

  return (
    <ListReportShell filters={filters} setFilters={setFilters} queryPlaceholder={t("purchases.filters.searchPlaceholder")} filterCount={activeAdvancedFilters} onOpenFilters={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }} error={firstError(purchasesQuery.error, summaryQuery.error, canReadBranches ? branchesQuery.error : null, suppliersQuery.error)} page={purchasesQuery.data} columns={columns} data={purchases}>
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={PackageCheck} label={t("reports.summary.officialPurchases")} value={summaryQuery.data?.totalPurchases ?? "-"} />
        <SummaryCard icon={WalletCards} label={t("reports.summary.officialConfirmedTotal")} value={summaryQuery.data ? formatMoney(summaryQuery.data.confirmedTotal, locale) : "-"} />
        <SummaryCard icon={ReceiptText} label={t("reports.summary.officialTaxes" )} value={summaryQuery.data ? formatMoney(summaryQuery.data.taxTotal, locale) : "-"} />
        <SummaryCard icon={AlertTriangle} label={t("reports.summary.officialVoided")} value={summaryQuery.data?.voidedPurchases ?? "-"} />
      </section>
      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("reports.filters.title")} description={t("reports.filters.description")}> 
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("purchases.fields.branch")} value={draftFilters.branchId} onChange={(branchId) => setDraftFilters((current) => ({ ...current, branchId }))} options={branchOptions} />
          <Select label={t("purchases.fields.supplier")} value={draftFilters.supplierId} onChange={(supplierId) => setDraftFilters((current) => ({ ...current, supplierId }))} options={(suppliersQuery.data?.items ?? []).map((supplier) => ({ value: supplier.id, label: supplier.name }))} />
          <Select label={t("purchases.fields.status")} value={draftFilters.status} onChange={(status) => setDraftFilters((current) => ({ ...current, status: status as PurchaseStatus | "" }))} options={purchaseStatuses.map((status) => ({ value: status, label: t(`purchases.status.${status}`) }))} />
          <div />
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <FilterActions onApply={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }} onClear={() => setDraftFilters({ ...defaultPurchaseFilters, branchId: singleBranchId, query: filters.query })} onCancel={() => setIsFiltersOpen(false)} />
        </div>
      </FormDialog>
    </ListReportShell>
  );
}

function TransfersReportScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<TransferFilters>(defaultTransferFilters);
  const [draftFilters, setDraftFilters] = useState<TransferFilters>(defaultTransferFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canViewReports();
  const canReadBranches = canAny(["settings.view"]);
  const branchesQuery = useQuery({ queryKey: ["reports", "branches", "transfers"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const transfersQuery = useQuery({ queryKey: ["reports", "transfers", filters], queryFn: () => listTransfers(filters), enabled: canView });
  const summaryQuery = useQuery({ queryKey: ["reports", "transfers-summary", filters], queryFn: () => getTransfersReportSummary(filters), enabled: canView });
  const transfers = transfersQuery.data?.items ?? [];
  const activeAdvancedFilters = [filters.sourceBranchId, filters.destinationBranchId, filters.status, filters.fromDate, filters.toDate].filter(Boolean).length;
  const columns = useMemo<ColumnDef<Transfer>[]>(() => [
    { header: t("transfers.fields.transferNumber"), cell: ({ row }) => <Link className="font-mono text-xs font-bold text-primary hover:underline" href={`/transfers/${row.original.id}`}>{row.original.transferNumber}</Link> },
    { header: t("transfers.fields.sourceBranch"), accessorKey: "sourceBranchName" },
    { header: t("transfers.fields.destinationBranch"), accessorKey: "destinationBranchName" },
    { header: t("transfers.fields.requestedAt"), cell: ({ row }) => formatDateTime(row.original.requestedAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("transfers.fields.quantityRequestedBase"), cell: ({ row }) => formatNumber(totalTransferRequested(row.original), locale) },
    { header: t("transfers.fields.quantityReceivedBase"), cell: ({ row }) => formatNumber(totalTransferReceived(row.original), locale) },
    { header: t("transfers.fields.status"), cell: ({ row }) => <TransferStatusBadge status={row.original.status} label={t(`transfers.status.${row.original.status}`)} /> },
  ], [locale, t, timeZone]);

  if (!canView) return <AccessDenied />;

  return (
    <ListReportShell filters={filters} setFilters={setFilters} queryPlaceholder={t("transfers.filters.searchPlaceholder")} filterCount={activeAdvancedFilters} onOpenFilters={() => { setDraftFilters(filters); setIsFiltersOpen(true); }} error={firstError(transfersQuery.error, summaryQuery.error, canReadBranches ? branchesQuery.error : null)} page={transfersQuery.data} columns={columns} data={transfers}>
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={Repeat2} label={t("reports.summary.officialTransfers")} value={summaryQuery.data?.totalTransfers ?? "-"} />
        <SummaryCard icon={PackageCheck} label={t("reports.summary.officialRequestedBase")} value={summaryQuery.data ? formatTransferNumber(summaryQuery.data.requestedBaseTotal) : "-"} />
        <SummaryCard icon={PackageCheck} label={t("reports.summary.officialReceivedBase")} value={summaryQuery.data ? formatTransferNumber(summaryQuery.data.receivedBaseTotal) : "-"} />
        <SummaryCard icon={AlertTriangle} label={t("reports.summary.officialPendingTransfers")} value={summaryQuery.data?.pendingTransfers ?? "-"} />
      </section>
      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("reports.filters.title")} description={t("reports.filters.description")}> 
        <div className="grid gap-4 md:grid-cols-3">
          <Select label={t("transfers.fields.sourceBranch")} value={draftFilters.sourceBranchId} onChange={(sourceBranchId) => setDraftFilters((current) => ({ ...current, sourceBranchId }))} options={branchOptions} />
          <Select label={t("transfers.fields.destinationBranch")} value={draftFilters.destinationBranchId} onChange={(destinationBranchId) => setDraftFilters((current) => ({ ...current, destinationBranchId }))} options={branchOptions} />
          <Select label={t("transfers.fields.status")} value={draftFilters.status} onChange={(status) => setDraftFilters((current) => ({ ...current, status: status as TransferStatus | "" }))} options={transferStatuses.map((status) => ({ value: status, label: t(`transfers.status.${status}`) }))} />
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <FilterActions onApply={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }} onClear={() => setDraftFilters({ ...defaultTransferFilters, query: filters.query })} onCancel={() => setIsFiltersOpen(false)} />
        </div>
      </FormDialog>
    </ListReportShell>
  );
}

function AdjustmentsReportScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const [filters, setFilters] = useState<AdjustmentFilters>(defaultAdjustmentFilters);
  const [draftFilters, setDraftFilters] = useState<AdjustmentFilters>(defaultAdjustmentFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const canView = canViewReports();
  const canReadBranches = canAny(["settings.view"]);
  const branchesQuery = useQuery({ queryKey: ["reports", "branches", "adjustments"], queryFn: () => listBranches(), enabled: canView && canReadBranches });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const singleBranchId = singleBranchOption(branchOptions);
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const adjustmentsQuery = useQuery({ queryKey: ["reports", "adjustments", effectiveFilters], queryFn: () => listAdjustments(effectiveFilters), enabled: canView });
  const summaryQuery = useQuery({ queryKey: ["reports", "adjustments-summary", effectiveFilters], queryFn: () => getAdjustmentsReportSummary(effectiveFilters), enabled: canView });
  const adjustments = adjustmentsQuery.data?.items ?? [];
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.status, effectiveFilters.fromDate, effectiveFilters.toDate].filter(Boolean).length;
  const columns = useMemo<ColumnDef<Adjustment>[]>(() => [
    { header: t("adjustments.fields.adjustmentNumber"), cell: ({ row }) => <Link className="font-mono text-xs font-bold text-primary hover:underline" href={`/adjustments/${row.original.id}`}>{row.original.adjustmentNumber}</Link> },
    { header: t("adjustments.fields.branch"), accessorKey: "branchName" },
    { header: t("adjustments.fields.type"), cell: ({ row }) => <AdjustmentTypeBadge direction={row.original.direction} label={t(`adjustments.types.${row.original.adjustmentType}`)} /> },
    { header: t("adjustments.fields.direction"), cell: ({ row }) => t(`adjustments.directions.${row.original.direction}`) },
    { header: t("adjustments.fields.createdAt"), cell: ({ row }) => formatDateTime(row.original.createdAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("adjustments.fields.createdBy"), cell: ({ row }) => displayAdjustmentUser(row.original.createdByUsername, row.original.createdBy) },
    { header: t("adjustments.fields.quantityBase"), cell: ({ row }) => formatAdjustmentNumber(totalAdjustmentBase(row.original)) },
    { header: t("adjustments.fields.status"), cell: ({ row }) => <AdjustmentStatusBadge status={row.original.status} label={t(`adjustments.status.${row.original.status}`)} /> },
  ], [locale, t, timeZone]);

  if (!canView) return <AccessDenied />;

  return (
    <ListReportShell filters={filters} setFilters={setFilters} queryPlaceholder={t("adjustments.filters.searchPlaceholder")} filterCount={activeAdvancedFilters} onOpenFilters={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }} error={firstError(adjustmentsQuery.error, summaryQuery.error, canReadBranches ? branchesQuery.error : null)} page={adjustmentsQuery.data} columns={columns} data={adjustments}>
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={Scale} label={t("reports.summary.officialAdjustments")} value={summaryQuery.data?.totalAdjustments ?? "-"} />
        <SummaryCard icon={PackageCheck} label={t("reports.summary.officialIn")} value={summaryQuery.data ? formatNumber(summaryQuery.data.quantityInBase, locale) : "-"} />
        <SummaryCard icon={PackageCheck} label={t("reports.summary.officialOut")} value={summaryQuery.data ? formatNumber(summaryQuery.data.quantityOutBase, locale) : "-"} />
        <SummaryCard icon={AlertTriangle} label={t("reports.summary.officialPendingApprovals")} value={summaryQuery.data?.pendingApprovalAdjustments ?? "-"} />
      </section>
      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("reports.filters.title")} description={t("reports.filters.description")}> 
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("adjustments.fields.branch")} value={draftFilters.branchId} onChange={(branchId) => setDraftFilters((current) => ({ ...current, branchId }))} options={branchOptions} />
          <Select label={t("adjustments.fields.status")} value={draftFilters.status} onChange={(status) => setDraftFilters((current) => ({ ...current, status: status as AdjustmentStatus | "" }))} options={adjustmentStatuses.map((status) => ({ value: status, label: t(`adjustments.status.${status}`) }))} />
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <FilterActions onApply={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }} onClear={() => setDraftFilters({ ...defaultAdjustmentFilters, branchId: singleBranchId, query: filters.query })} onCancel={() => setIsFiltersOpen(false)} />
        </div>
      </FormDialog>
    </ListReportShell>
  );
}

function ClosuresListScreen() {
  const t = useTranslations();
  const { locale } = useAppLocale();
  const canCreate = canCreateCashClosures();
  const canReadBranches = canAny(["settings.view"]);
  const [filters, setFilters] = useState<CashClosureFilters>(defaultClosureFilters);
  const [draftFilters, setDraftFilters] = useState<CashClosureFilters>(defaultClosureFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const branchesQuery = useQuery({ queryKey: ["closures", "branches"], queryFn: () => listBranches(), enabled: canReadBranches });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const singleBranchId = singleBranchOption(branchOptions);
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const closuresQuery = useQuery({ queryKey: ["closures", effectiveFilters], queryFn: () => listCashClosures(effectiveFilters) });
  const summaryQuery = useQuery({ queryKey: ["closures", "summary", effectiveFilters], queryFn: () => getCashClosuresReportSummary(effectiveFilters) });
  const activeAdvancedFilters = [effectiveFilters.branchId, effectiveFilters.status, effectiveFilters.fromDate, effectiveFilters.toDate].filter(Boolean).length;
  const columns = useMemo<ColumnDef<CashClosure>[]>(() => [
    { header: t("reports.cashClosings.fields.date"), cell: ({ row }) => formatDateOnly(row.original.closureDate, locale) },
    { header: t("reports.cashClosings.fields.branch"), accessorKey: "branchName" },
    { header: t("reports.cashClosings.fields.user"), accessorKey: "username" },
    { header: t("reports.cashClosings.fields.type"), cell: ({ row }) => t(`reports.cashClosings.types.${row.original.closureType}`) },
    { header: t("reports.cashClosings.fields.status"), cell: ({ row }) => <ClosureStatusBadge status={row.original.status} label={t(`reports.cashClosings.status.${row.original.status}`)} /> },
    { header: t("reports.cashClosings.fields.expectedTotal"), cell: ({ row }) => formatMoney(row.original.expectedTotal, locale) },
    { header: t("reports.cashClosings.fields.countedTotal"), cell: ({ row }) => formatMoney(row.original.countedTotal, locale) },
    { header: t("reports.cashClosings.fields.difference"), cell: ({ row }) => formatMoney(row.original.differenceAmount, locale) },
    { header: t("admin.fields.actions"), cell: ({ row }) => <Link className="font-bold text-primary hover:underline" href={`/closures/${row.original.id}`}>{t("auditLogs.actions.view")}</Link> },
  ], [locale, t]);

  return (
    <div className="space-y-6">
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={WalletCards} label={t("reports.cashClosings.summary.total")} value={summaryQuery.data?.totalClosures ?? "-"} />
        <SummaryCard icon={WalletCards} label={t("reports.cashClosings.summary.officialExpected")} value={summaryQuery.data ? formatMoney(summaryQuery.data.expectedTotal, locale) : "-"} />
        <SummaryCard icon={WalletCards} label={t("reports.cashClosings.summary.officialCounted")} value={summaryQuery.data ? formatMoney(summaryQuery.data.countedTotal, locale) : "-"} />
        <SummaryCard icon={AlertTriangle} label={t("reports.cashClosings.summary.officialDifference")} value={summaryQuery.data ? formatMoney(summaryQuery.data.differenceTotal, locale) : "-"} />
      </section>
      <Card><CardContent className="flex flex-wrap items-end justify-between gap-3 p-5"><div><p className="text-sm text-muted-foreground">{t("reports.cashClosings.supportedDescription")}</p></div><div className="flex flex-wrap gap-2"><Button type="button" variant="outline" onClick={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }}>{t("reports.filters.button")}{activeAdvancedFilters > 0 ? <span className="ml-2 rounded-full bg-primary px-2 py-0.5 text-xs font-bold text-primary-foreground">{activeAdvancedFilters}</span> : null}</Button>{canCreate ? <Link className="inline-flex h-10 items-center justify-center rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground" href="/closures/new">{t("reports.cashClosings.newClosure")}</Link> : null}</div></CardContent></Card>
      <ErrorCard error={firstError(closuresQuery.error, summaryQuery.error, canReadBranches ? branchesQuery.error : null)} />
      <TableSection title={t("reports.sections.records")} page={closuresQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))} exportFileName="cash-closures-report" exportRows={closuresQuery.data?.items ?? []}><AdminTable columns={columns} data={closuresQuery.data?.items ?? []} /></TableSection>
      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("reports.filters.title")} description={t("reports.filters.description")}> 
        <div className="grid gap-4 md:grid-cols-2">
          <Select label={t("reports.cashClosings.fields.branch")} value={draftFilters.branchId} onChange={(branchId) => setDraftFilters((current) => ({ ...current, branchId }))} options={branchOptions} />
          <Select label={t("reports.cashClosings.fields.status")} value={draftFilters.status} onChange={(status) => setDraftFilters((current) => ({ ...current, status: status as CashClosureStatus | "" }))} options={closureStatuses.map((status) => ({ value: status, label: t(`reports.cashClosings.status.${status}`) }))} />
          <DateFilter label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(fromDate) => setDraftFilters((current) => ({ ...current, fromDate }))} />
          <DateFilter label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(toDate) => setDraftFilters((current) => ({ ...current, toDate }))} />
          <FilterActions onApply={() => { setFilters({ ...draftFilters, page: 1, pageSize }); setIsFiltersOpen(false); }} onClear={() => setDraftFilters({ ...defaultClosureFilters, branchId: singleBranchId })} onCancel={() => setIsFiltersOpen(false)} />
        </div>
      </FormDialog>
    </div>
  );
}

function ClosureFormScreen() {
  const t = useTranslations();
  const router = useRouter();
  const { locale, timeZone } = useAppLocale();
  const canReadBranches = canAny(["settings.view"]);
  const [branchId, setBranchId] = useState("");
  const [closureDate, setClosureDate] = useState("");
  const [countedTotal, setCountedTotal] = useState("");
  const [notes, setNotes] = useState("");
  const branchesQuery = useQuery({ queryKey: ["closures", "branches", "create"], queryFn: () => listBranches(), enabled: canReadBranches });
  const branchOptions = useMemo(() => getBranchOptions(branchesQuery.data?.items ?? []), [branchesQuery.data?.items]);
  const effectiveBranchId = branchId || singleBranchOption(branchOptions);
  const maximumClosureDate = todayInTimeZone(timeZone);
  const futureDate = Boolean(closureDate && closureDate > maximumClosureDate);
  const countedTotalValue = parseNonNegativeAmount(countedTotal);
  const invalidCountedTotal = countedTotal.trim() !== "" && countedTotalValue === null;
  const canPreview = Boolean(effectiveBranchId && closureDate && !futureDate);
  const previewQuery = useQuery({
    queryKey: ["closures", "preview", effectiveBranchId, closureDate],
    queryFn: () => getCashClosurePreview(effectiveBranchId, closureDate),
    enabled: canPreview,
  });
  const preview = previewQuery.data;
  const createMutation = useMutation({
    mutationFn: () => {
      const amount = parseNonNegativeAmount(countedTotal);
      if (amount === null) throw new Error(t("reports.cashClosings.invalidAmount"));
      return createCashClosure({ branchId: effectiveBranchId, closureDate, countedTotal: amount, notes: notes.trim() || null });
    },
    onSuccess: (closure) => router.push(`/closures/${closure.id}`),
  });
  const canSubmit = Boolean(effectiveBranchId && closureDate && countedTotalValue !== null && !futureDate && !createMutation.isPending);

  return (
    <div className="space-y-6">
      <ErrorCard error={firstError(canReadBranches ? branchesQuery.error : null, previewQuery.error, createMutation.error)} />
      {futureDate ? <PlaceholderNotice title={t("reports.cashClosings.futureDate.title")} description={t("reports.cashClosings.futureDate.description")} /> : null}
      <Card><CardContent className="grid gap-4 p-5 md:grid-cols-2">
        <Select label={t("reports.cashClosings.fields.branch")} value={effectiveBranchId} onChange={setBranchId} options={branchOptions} />
        <DateFilter label={t("reports.cashClosings.fields.date")} value={closureDate} onChange={setClosureDate} max={maximumClosureDate} />
        <Field label={t("reports.cashClosings.fields.countedTotal")} value={countedTotal} onChange={setCountedTotal} type="number" min="0" step="0.01" required error={invalidCountedTotal ? t("reports.cashClosings.invalidAmount") : undefined} />
        <Field label={t("reports.cashClosings.fields.notes")} value={notes} onChange={setNotes} />
        <div className="flex flex-wrap gap-2 md:col-span-2"><Button type="button" disabled={!canSubmit} onClick={() => createMutation.mutate()}>{t("reports.cashClosings.create")}</Button><Link className="inline-flex h-10 items-center justify-center rounded-lg border border-input bg-background/70 px-4 py-2 text-sm font-semibold" href="/closures">{t("admin.actions.cancel")}</Link></div>
      </CardContent></Card>

      {canPreview && preview ? (
        <Card><CardContent className="p-5">
          <h3 className="text-lg font-black tracking-[-0.04em]">{t("reports.cashClosings.preview.title")}</h3>
          <p className="mt-1 text-sm text-muted-foreground">{t("reports.cashClosings.preview.description")}</p>
          <div className="mt-4 grid gap-4 md:grid-cols-4">
            <SummaryCard icon={WalletCards} label={t("reports.cashClosings.fields.confirmedSalesTotal")} value={formatMoney(preview.confirmedSalesTotal, locale)} />
            <SummaryCard icon={WalletCards} label={t("reports.cashClosings.fields.expectedTotal")} value={formatMoney(preview.expectedTotal, locale)} />
            <SummaryCard icon={ReceiptText} label={t("reports.cashClosings.fields.discountTotal")} value={formatMoney(preview.discountTotal, locale)} />
            <SummaryCard icon={Repeat2} label={t("reports.cashClosings.fields.cancelledSalesTotal")} value={formatMoney(preview.cancelledSalesTotal, locale)} />
          </div>
          <PaymentBreakdown payments={preview.paymentsByMethod} locale={locale} />
        </CardContent></Card>
      ) : null}
    </div>
  );
}

function PaymentBreakdown({ payments, locale }: Readonly<{ payments: Record<string, number>; locale: string }>) {
  const t = useTranslations();
  return (
    <div className="mt-4">
      <p className="text-sm font-bold">{t("reports.cashClosings.fields.paymentsByMethod")}</p>
      <div className="mt-2 grid gap-2 md:grid-cols-2">
        {salePaymentMethods.map((method) => (
          <div key={method} className="flex items-center justify-between rounded-2xl border bg-background/70 px-3 py-2 text-sm">
            <span className="text-muted-foreground">{t(`sales.paymentMethods.${method}`)}</span>
            <span className="font-bold">{formatMoney(payments[method] ?? 0, locale)}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function ClosureDetailScreen({ closureId }: Readonly<{ closureId: string }>) {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const closureQuery = useQuery({ queryKey: ["closures", "detail", closureId], queryFn: () => getCashClosure(closureId) });
  const closure = closureQuery.data;
  const canApprove = canApproveCashClosures();
  const [correctedCounted, setCorrectedCounted] = useState<string | null>(null);
  const approveMutation = useMutation({
    mutationFn: () => approveCashClosure(closureId),
    onSuccess: () => { toast.success(t("reports.cashClosings.approved")); closureQuery.refetch(); },
  });
  const reopenMutation = useMutation({
    mutationFn: () => reopenCashClosure(closureId),
    onSuccess: () => { toast.success(t("reports.cashClosings.reopened")); closureQuery.refetch(); },
  });
  const closeMutation = useMutation({
    mutationFn: (countedTotal?: number) => closeCashClosure(closureId, countedTotal),
    onSuccess: () => { toast.success(t("reports.cashClosings.closed")); closureQuery.refetch(); },
  });

  if (closureQuery.error) return <ErrorCard error={closureQuery.error} />;
  if (!closure) return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("admin.states.loading")}</CardContent></Card>;

  const recordTimeZone = closure.timezoneSnapshot ?? timeZone;
  const countedInputValue = correctedCounted ?? String(closure.countedTotal);
  const correctedCountedValue = parseNonNegativeAmount(countedInputValue);
  return (
    <div className="space-y-6">
      <Card>
        <CardContent className="grid gap-3 p-5 md:grid-cols-3">
          <Info label={t("reports.cashClosings.fields.type")} value={t(`reports.cashClosings.types.${closure.closureType}`)} />
          <Info label={t("reports.cashClosings.fields.status")} value={t(`reports.cashClosings.status.${closure.status}`)} />
          <Info label={t("reports.cashClosings.fields.branch")} value={closure.branchName} />
          <Info label={t("reports.cashClosings.fields.user")} value={closure.username} />
          <Info label={t("reports.cashClosings.fields.date")} value={formatDateOnly(closure.closureDate, locale)} />
          <Info label={t("reports.cashClosings.fields.createdAt")} value={formatDateTime(closure.createdAt, recordTimeZone, locale)} />
          <Info label={t("reports.cashClosings.fields.approvedAt")} value={formatDateTime(closure.approvedAt, recordTimeZone, locale)} />
        </CardContent>
      </Card>
      <ErrorCard error={approveMutation.error ?? reopenMutation.error ?? closeMutation.error} />
      {closure.outdated ? <PlaceholderNotice title={t("reports.cashClosings.outdated.title")} description={t("reports.cashClosings.outdated.description", { count: String(closure.salesAfterClosureCount) })} /> : null}
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={WalletCards} label={t("reports.cashClosings.fields.expectedTotal")} value={formatMoney(closure.expectedTotal, locale)} />
        <SummaryCard icon={WalletCards} label={t("reports.cashClosings.fields.countedTotal")} value={formatMoney(closure.countedTotal, locale)} />
        <SummaryCard icon={AlertTriangle} label={t("reports.cashClosings.fields.difference")} value={formatMoney(closure.differenceAmount, locale)} />
        <SummaryCard icon={ReceiptText} label={t("reports.cashClosings.fields.discountTotal")} value={formatMoney(closure.discountTotal, locale)} />
      </section>
      <Card>
        <CardContent className="p-5">
          <PaymentBreakdown payments={closure.paymentsByMethod ?? {}} locale={locale} />
        </CardContent>
      </Card>
      <Card>
        <CardContent className="p-5">
          <p className="text-sm font-bold">{t("reports.cashClosings.fields.notes")}</p>
          <p className="mt-2 text-sm text-muted-foreground">{closure.notes ?? "-"}</p>
          {canApprove && closure.status === "REOPENED" ? (
            <div className="mt-4 max-w-xs">
              <Field label={t("reports.cashClosings.fields.countedTotal")} value={countedInputValue} onChange={setCorrectedCounted} type="number" min="0" step="0.01" required error={correctedCountedValue === null ? t("reports.cashClosings.invalidAmount") : undefined} />
            </div>
          ) : null}
          <div className="mt-4 flex flex-wrap gap-2">
            <Link className="inline-flex h-10 items-center justify-center rounded-lg border border-input bg-background/70 px-4 py-2 text-sm font-semibold" href="/closures">{t("purchases.actions.back")}</Link>
            <Link className="inline-flex h-10 items-center justify-center rounded-lg border border-input bg-background/70 px-4 py-2 text-sm font-semibold" href="/reports/sales">{t("reports.sales.title")}</Link>
            {canApprove && closure.status === "CLOSED" ? <Button type="button" disabled={approveMutation.isPending} onClick={() => approveMutation.mutate()}>{t("reports.cashClosings.approve")}</Button> : null}
            {canApprove && closure.status === "APPROVED" ? <Button type="button" variant="outline" disabled={reopenMutation.isPending} onClick={() => reopenMutation.mutate()}>{t("reports.cashClosings.reopen")}</Button> : null}
            {canApprove && closure.status === "REOPENED" ? <Button type="button" disabled={closeMutation.isPending || correctedCountedValue === null} onClick={() => closeMutation.mutate(correctedCounted === null ? undefined : correctedCountedValue ?? undefined)}>{t("reports.cashClosings.close")}</Button> : null}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function todayInTimeZone(timeZone: string) {
  const parts = new Intl.DateTimeFormat("en-US", { timeZone, year: "numeric", month: "2-digit", day: "2-digit" }).formatToParts(new Date());
  const part = (type: "year" | "month" | "day") => parts.find((item) => item.type === type)?.value ?? "";
  return `${part("year")}-${part("month")}-${part("day")}`;
}

function parseNonNegativeAmount(value: string): number | null {
  const normalized = value.trim();
  if (!/^\d+(?:\.\d{1,2})?$/.test(normalized)) return null;
  const amount = Number(normalized);
  return Number.isFinite(amount) && amount >= 0 ? amount : null;
}

function ListReportShell<T, TFilters extends { page: number; query: string }>(props: Readonly<{ filters: TFilters; setFilters: React.Dispatch<React.SetStateAction<TFilters>>; queryPlaceholder: string; filterCount: number; onOpenFilters: () => void; error: unknown; page?: { page: number; totalPages: number; totalItems: number }; columns: ColumnDef<T>[]; data: T[]; children: React.ReactNode }>) {
  const t = useTranslations();
  const { filters, setFilters, queryPlaceholder, filterCount, onOpenFilters, error, page, columns, data, children } = props;

  return (
    <div className="space-y-6">
      {children}
      <ReportToolbar query={filters.query} queryPlaceholder={queryPlaceholder} filterCount={filterCount} onQueryChange={(query) => setFilters((current) => ({ ...current, query, page: 1 }))} onOpenFilters={onOpenFilters} />
      <ErrorCard error={error} />
      <TableSection title={t("reports.sections.records")} page={page} onPageChange={(pageNumber) => setFilters((current) => ({ ...current, page: pageNumber }))} exportFileName="vivent-report" exportRows={data}><AdminTable columns={columns} data={data} /></TableSection>
    </div>
  );
}

function ReportToolbar({ query, queryPlaceholder, filterCount, onQueryChange, onOpenFilters }: Readonly<{ query: string; queryPlaceholder: string; filterCount: number; onQueryChange: (query: string) => void; onOpenFilters: () => void }>) {
  const t = useTranslations();

  return (
    <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.10),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.34))] shadow-sm">
      <CardContent className="grid gap-3 p-5 md:grid-cols-[1fr_auto] md:items-end">
        <div className="space-y-2">
          <Label>{t("catalog.search")}</Label>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input className="pl-9" value={query} onChange={(event) => onQueryChange(event.target.value)} placeholder={queryPlaceholder} />
          </div>
        </div>
        <Button className="gap-2" type="button" variant="outline" onClick={onOpenFilters}>
          <Filter className="h-4 w-4" aria-hidden="true" />
          {t("reports.filters.button")}
          {filterCount > 0 ? <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-bold text-primary-foreground">{filterCount}</span> : null}
        </Button>
      </CardContent>
    </Card>
  );
}

function SummaryCard({ icon: Icon, label, value }: Readonly<{ icon: LucideIcon; label: string; value: string | number }>) {
  return <Card><CardContent className="p-5"><div className="mb-4 flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/10 text-primary"><Icon className="h-5 w-5" aria-hidden="true" /></div><p className="text-sm text-muted-foreground">{label}</p><p className="mt-2 text-2xl font-black tracking-[-0.05em]">{value}</p></CardContent></Card>;
}

function ReportNavigationCard({ href, icon: Icon, title, description }: Readonly<{ href: string; icon: LucideIcon; title: string; description: string }>) {
  const t = useTranslations();
  return <Card className="overflow-hidden transition hover:border-primary/30 hover:shadow-md"><CardContent className="flex h-full flex-col gap-4 p-5"><div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/10 text-primary"><Icon className="h-5 w-5" aria-hidden="true" /></div><div className="flex-1"><p className="text-lg font-black tracking-[-0.04em]">{title}</p><p className="mt-2 text-sm leading-6 text-muted-foreground">{description}</p></div><Link className="inline-flex h-10 items-center justify-center rounded-lg border border-input bg-background/70 px-4 py-2 text-sm font-semibold transition-all hover:-translate-y-0.5 hover:bg-accent hover:text-accent-foreground" href={href}>{t("reports.dashboard.openReport")}</Link></CardContent></Card>;
}

function PlaceholderNotice({ title, description }: Readonly<{ title: string; description: string }>) {
  return <Card className="border-dashed"><CardContent className="flex gap-3 p-5 text-sm text-muted-foreground"><AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" aria-hidden="true" /><div><p className="font-bold text-foreground">{title}</p><p className="mt-1 leading-6">{description}</p></div></CardContent></Card>;
}

function TableSection({ title, page, onPageChange, exportFileName, exportRows, children }: Readonly<{ title: string; page?: { page: number; totalPages: number; totalItems: number }; onPageChange: (page: number) => void; exportFileName?: string; exportRows?: unknown[]; children: React.ReactNode }>) {
  const t = useTranslations();
  const hasExportRows = Boolean(exportRows?.length);
  return <section className="space-y-3"><div className="flex flex-wrap items-center justify-between gap-2"><div><h2 className="text-xl font-black tracking-[-0.04em]">{title}</h2><p className="mt-1 text-xs text-muted-foreground">{t("reports.summary.visiblePageOnly")}</p></div><div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">{exportFileName ? <Button type="button" variant="outline" disabled={!hasExportRows} onClick={() => exportRowsAsExcel(exportFileName, exportRows ?? [])}>{t("reports.export.excel")}</Button> : null}{page ? <><span>{page.totalItems} items</span><Button type="button" variant="outline" disabled={page.page <= 1} onClick={() => onPageChange(page.page - 1)}>Prev</Button><span>{page.page}/{Math.max(page.totalPages, 1)}</span><Button type="button" variant="outline" disabled={page.page >= page.totalPages} onClick={() => onPageChange(page.page + 1)}>Next</Button></> : null}</div></div>{children}</section>;
}

function FilterActions({ onApply, onClear, onCancel }: Readonly<{ onApply: () => void; onClear: () => void; onCancel: () => void }>) {
  const t = useTranslations();
  return <div className="flex flex-wrap gap-2 md:col-span-full"><Button type="button" onClick={onApply}>{t("reports.filters.apply")}</Button><Button type="button" variant="outline" onClick={onClear}>{t("reports.filters.clear")}</Button><Button type="button" variant="ghost" onClick={onCancel}>{t("admin.actions.cancel")}</Button></div>;
}

function Select({ label, value, onChange, options }: Readonly<{ label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }> }>) {
  const t = useTranslations();
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)}><option value="">{t("reports.filters.all")}</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>;
}

function Field({ label, value, onChange, type = "text", min, step, required, error }: Readonly<{ label: string; value: string; onChange: (value: string) => void; type?: string; min?: string; step?: string; required?: boolean; error?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type={type} min={min} step={step} required={required} value={value} aria-invalid={Boolean(error)} onChange={(event) => onChange(event.target.value)} />{error ? <p className="text-xs text-destructive">{error}</p> : null}</div>;
}

function Info({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="rounded-2xl border bg-background/70 p-3"><p className="text-xs font-bold uppercase tracking-[0.14em] text-muted-foreground">{label}</p><p className="mt-1 break-words font-semibold">{value}</p></div>;
}

function ClosureStatusBadge({ status, label }: Readonly<{ status: CashClosureStatus; label: string }>) {
  const className = status === "APPROVED" ? "border-emerald-300 bg-emerald-100 text-emerald-800" : status === "CLOSED" ? "border-blue-300 bg-blue-100 text-blue-800" : status === "REOPENED" ? "border-amber-300 bg-amber-100 text-amber-800" : "border-slate-300 bg-slate-100 text-slate-800";
  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-bold ${className}`}>{label}</span>;
}

function ProductReportList({ title, items, locale }: Readonly<{ title: string; items: ProductReportItem[]; locale: string }>) {
  const t = useTranslations();
  return <Card><CardContent className="p-5"><h3 className="text-lg font-black tracking-[-0.04em]">{title}</h3><div className="mt-4 space-y-2">{items.length > 0 ? items.map((item) => <div key={`${title}-${item.productId}-${item.branchId ?? "all"}`} className="rounded-2xl border bg-background/70 p-3 text-sm"><div className="flex flex-wrap items-center justify-between gap-2"><p className="font-bold">{item.productName}</p><p className="font-mono text-xs text-muted-foreground">{formatNumber(item.quantity, locale)} {item.baseUnitCode}</p></div><p className="mt-1 text-xs text-muted-foreground">{item.branchName ?? t("reports.products.allBranches")} · {item.categoryName ?? "-"} · {t("reports.products.movements")}: {item.movementCount}</p></div>) : <p className="text-sm text-muted-foreground">{t("admin.states.empty")}</p>}</div></CardContent></Card>;
}

function DateFilter({ label, value, onChange, max }: Readonly<{ label: string; value: string; onChange: (value: string) => void; max?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type="date" value={value} max={max} onChange={(event) => onChange(event.target.value)} /></div>;
}

function ErrorCard({ error }: Readonly<{ error: unknown }>) {
  const t = useTranslations();
  if (!error) return null;
  return <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(error, t)}</CardContent></Card>;
}

function AccessDenied() {
  const t = useTranslations();
  return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
}

function formatMoney(value: number, locale: string) {
  return new Intl.NumberFormat(locale, { style: "currency", currency: "GTQ" }).format(value);
}

function formatNumber(value: number, locale: string) {
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 6 }).format(Number(value || 0));
}

function formatDateTime(value: string | null, timeZone: string, locale: string) {
  const date = parseReportDate(value);
  if (!date) return "-";
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short", timeZone }).format(date);
}

function formatDateOnly(value: string | null, locale: string) {
  const date = parseReportDate(value);
  if (!date) return "-";
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(date);
}

function parseReportDate(value: string | null) {
  if (!value) return null;
  const date = new Date(/^\d{4}-\d{2}-\d{2}$/.test(value) ? `${value}T00:00:00` : value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatApiErrorMessage(error, t);
}

function firstError(...errors: unknown[]) {
  return errors.find(Boolean) ?? null;
}

function exportRowsAsExcel(fileName: string, rows: unknown[]) {
  if (rows.length === 0) return;
  const normalizedRows = rows.map(flattenExportRow);
  const headers = Array.from(new Set(normalizedRows.flatMap((row) => Object.keys(row))));
  const headerHtml = headers.map((header) => `<th>${escapeHtml(humanizeExportHeader(header))}</th>`).join("");
  const bodyHtml = normalizedRows.map((row) => `<tr>${headers.map((header) => `<td>${escapeHtml(row[header] ?? "")}</td>`).join("")}</tr>`).join("");
  const html = `<html><head><meta charset="UTF-8" /></head><body><table><thead><tr>${headerHtml}</tr></thead><tbody>${bodyHtml}</tbody></table></body></html>`;
  const blob = new Blob([html], { type: "application/vnd.ms-excel;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${fileName}-${new Date().toISOString().slice(0, 10)}.xls`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function flattenExportRow(row: unknown) {
  if (!row || typeof row !== "object") return { value: String(row ?? "") };
  return Object.entries(row).reduce<Record<string, string>>((accumulator, [key, value]) => {
    accumulator[key] = exportValue(value);
    return accumulator;
  }, {});
}

function exportValue(value: unknown): string {
  if (value === null || value === undefined) return "";
  if (Array.isArray(value)) return JSON.stringify(value);
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function escapeHtml(value: string) {
  return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;");
}

function humanizeExportHeader(value: string) {
  return value.replace(/([A-Z])/g, " $1").replace(/^./, (letter) => letter.toUpperCase());
}

function getBranchOptions(branches: Array<{ id: string; name: string }>) {
  return withSessionBranchFallback(branches.map((branch) => ({ value: branch.id, label: branch.name })));
}

function singleBranchOption(branches: BranchOption[]) {
  return branches.length === 1 ? branches[0].value : "";
}

function summarizeMovements(movements: StockMovement[]) {
  return movements.reduce((totals, movement) => ({ quantityIn: totals.quantityIn + movement.quantityIn, quantityOut: totals.quantityOut + movement.quantityOut }), { quantityIn: 0, quantityOut: 0 });
}

function totalTransferRequested(transfer: Transfer) {
  return transfer.details.reduce((total, detail) => total + detail.quantityRequestedBase, 0);
}

function totalTransferReceived(transfer: Transfer) {
  return transfer.details.reduce((total, detail) => total + detail.quantityReceivedBase, 0);
}

function totalAdjustmentBase(adjustment: Adjustment) {
  return adjustment.lines.reduce((total, line) => total + line.quantityBase, 0);
}

function displayAdjustmentUser(username: string | null | undefined, userId: string | null | undefined) {
  return username ?? (userId ? "-" : "-");
}
