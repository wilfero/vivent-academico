"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { CalendarDays, ListFilter, PackageSearch, ScrollText, SlidersHorizontal, X } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useTranslations } from "next-intl";
import { useMemo, useState } from "react";
import type { Dispatch, ReactNode, SetStateAction } from "react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useAppLocale } from "@/components/providers/locale-provider";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { listBranches } from "@/features/admin/shared/admin-api";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { listCategories, listProducts } from "@/features/admin/catalog/shared/catalog-api";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { withSessionBranchFallback } from "@/features/auth/lib/session-branch";
import { getInventorySummary, listInventoryBalances, listInventoryLayers, listInventoryMovements, listKardex, stockIn, stockOut } from "@/features/inventory/api/inventory-api";
import { InventorySourceLabel, LayerStatusBadge, MovementTypeBadge, StockStatusBadge } from "@/features/inventory/components/inventory-badges";
import { defaultStockManualValues, stockManualSchema, type StockManualForm, type StockManualInput } from "@/features/inventory/schemas/stock-manual-schema";
import type { InventoryBalance, InventoryFilters, InventoryLayer, InventoryLayerStatus, StockMovement } from "@/features/inventory/types/inventory-types";
import { formatPermissionError } from "@/lib/api/errors";

const pageSize = 20;
const movementTypes = ["PURCHASE_IN", "SALE_OUT", "SALE_CANCEL_IN", "PURCHASE_VOID_OUT", "TRANSFER_RESERVED", "TRANSFER_OUT", "TRANSFER_IN", "ADJUSTMENT_IN", "ADJUSTMENT_OUT", "STOCK_COUNT_IN", "STOCK_COUNT_OUT", "LOSS_OUT", "DAMAGE_OUT", "EXPIRED_OUT"] as const;
const defaultInventoryFilters: InventoryFilters = { branchId: "", locationId: "", productId: "", categoryId: "", query: "", movementType: "", fromDate: "", toDate: "", page: 1, pageSize };

export function InventoryScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const queryClient = useQueryClient();
  const [activeView, setActiveView] = useState<"balances" | "layers" | "kardex" | "movements">("balances");
  const [filters, setFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [draftFilters, setDraftFilters] = useState<InventoryFilters>(defaultInventoryFilters);
  const [layerStatus, setLayerStatus] = useState<InventoryLayerStatus | "">("OPEN");
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const [isManualOpen, setIsManualOpen] = useState(false);
  const canReadBranches = canAny(["settings.view"]);
  const canViewInventory = canAny(["inventory.view"]);
  const canViewLayers = canAny(["inventory.view_layers"]);
  const canViewKardex = canAny(["inventory.view_kardex"]);
  const canViewMovements = canAny(["inventory.view_movements"]);
  const canAdjustInventory = canAny(["inventory.adjust"]);

  const branchesQuery = useQuery({ queryKey: ["inventory", "branches"], queryFn: () => listBranches(), enabled: canReadBranches });
  const productsQuery = useQuery({ queryKey: ["inventory", "products"], queryFn: () => listProducts("", "", true) });
  const categoriesQuery = useQuery({ queryKey: ["inventory", "categories"], queryFn: () => listCategories("", true) });
  const branchOptions = useMemo(() => withSessionBranchFallback((branchesQuery.data?.items ?? []).map((branch) => ({ value: branch.id, label: branch.name }))), [branchesQuery.data?.items]);
  const singleBranchId = branchOptions.length === 1 ? branchOptions[0].value : "";
  const effectiveFilters = singleBranchId && !filters.branchId ? { ...filters, branchId: singleBranchId } : filters;
  const summaryQuery = useQuery({ queryKey: ["inventory", "summary"], queryFn: getInventorySummary, enabled: canViewInventory });
  const balancesQuery = useQuery({ queryKey: ["inventory", "balances", effectiveFilters], queryFn: () => listInventoryBalances(effectiveFilters), enabled: canViewInventory });
  const layersQuery = useQuery({ queryKey: ["inventory", "layers", effectiveFilters, layerStatus], queryFn: () => listInventoryLayers({ ...effectiveFilters, status: layerStatus }), enabled: activeView === "layers" && canViewLayers });
  const kardexQuery = useQuery({ queryKey: ["inventory", "kardex", effectiveFilters], queryFn: () => listKardex(effectiveFilters), enabled: activeView === "kardex" && canViewKardex });
  const movementsQuery = useQuery({ queryKey: ["inventory", "movements", effectiveFilters], queryFn: () => listInventoryMovements(effectiveFilters), enabled: activeView === "movements" && canViewMovements });

  const form = useForm<StockManualInput, unknown, StockManualForm>({ resolver: zodResolver(stockManualSchema), defaultValues: defaultStockManualValues, reValidateMode: "onBlur" });
  const manualMutation = useMutation({
    mutationFn: (values: StockManualForm) => {
      const payload = { branchId: values.branchId, productId: values.productId, quantity: values.quantity, locationId: null, notes: values.notes.trim() };
      return values.operation === "IN"
        ? stockIn({ ...payload, unitCost: values.unitCost ?? 0, expirationDate: values.expirationDate || null, lotNumber: values.lotNumber || null })
        : stockOut(payload);
    },
    onSuccess: () => {
      toast.success(t("inventory.manual.success"));
      form.reset(defaultStockManualValues);
      setIsManualOpen(false);
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
    },
  });

  const productById = useMemo(() => new Map((productsQuery.data?.items ?? []).map((product) => [product.id, product])), [productsQuery.data?.items]);
  const selectedProduct = effectiveFilters.productId ? productById.get(effectiveFilters.productId) : null;
  const activeAdvancedFilters = countAdvancedFilters(effectiveFilters);

  const commonError = (canReadBranches ? branchesQuery.error : null) ?? productsQuery.error ?? categoriesQuery.error ?? summaryQuery.error ?? balancesQuery.error ?? layersQuery.error ?? kardexQuery.error ?? movementsQuery.error ?? manualMutation.error;

  const balanceColumns = useMemo<ColumnDef<InventoryBalance>[]>(() => [
    { header: t("inventory.fields.product"), accessorKey: "productName" },
    { header: t("navigation.categories"), cell: ({ row }) => row.original.categoryName ?? "-" },
    { header: t("inventory.fields.branch"), accessorKey: "branchName" },
    { header: t("inventory.fields.location"), accessorKey: "locationCode" },
    { header: t("inventory.fields.onHand"), cell: ({ row }) => formatNumber(row.original.quantityOnHand) },
    { header: t("inventory.fields.reserved"), cell: ({ row }) => formatNumber(row.original.quantityReserved) },
    { header: t("inventory.fields.available"), cell: ({ row }) => formatNumber(row.original.quantityAvailable) },
    { header: t("inventory.fields.unit"), accessorKey: "baseUnitCode" },
    { header: t("inventory.fields.lastUpdate"), cell: ({ row }) => formatDate(row.original.updatedAt, timeZone, locale) },
      { header: t("inventory.fields.status"), cell: ({ row }) => <StockStatusBadge quantity={row.original.quantityAvailable} minimumStock={row.original.minimumStock} /> },
    { header: t("admin.fields.actions"), cell: ({ row }) => <QuickActions productId={row.original.productId} canViewLayers={canViewLayers} canViewKardex={canViewKardex} canViewMovements={canViewMovements} setActiveView={setActiveView} setFilters={setFilters} /> },
  ], [canViewKardex, canViewLayers, canViewMovements, locale, t, timeZone]);

  const layerColumns = useMemo<ColumnDef<InventoryLayer>[]>(() => [
    { header: t("inventory.fields.product"), accessorKey: "productName" },
    { header: t("inventory.fields.branch"), accessorKey: "branchName" },
    { header: t("inventory.fields.location"), accessorKey: "locationCode" },
    { header: t("inventory.fields.receivedAt"), cell: ({ row }) => formatDate(row.original.receivedAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("inventory.fields.quantityReceived"), cell: ({ row }) => formatNumber(row.original.quantityReceived) },
    { header: t("inventory.fields.quantityRemaining"), cell: ({ row }) => formatNumber(row.original.quantityRemaining) },
    { header: t("inventory.fields.unitCost"), cell: ({ row }) => formatMoney(row.original.unitCost) },
    { header: t("inventory.fields.remainingCost"), cell: ({ row }) => formatMoney(row.original.quantityRemaining * row.original.unitCost) },
    { header: t("inventory.fields.source"), cell: ({ row }) => <InventorySourceLabel sourceType={row.original.sourceType} sourceId={row.original.sourceId} /> },
    { header: t("inventory.fields.status"), cell: ({ row }) => <LayerStatusBadge status={row.original.status} /> },
  ], [locale, t, timeZone]);

  const movementColumns = useMemo<ColumnDef<StockMovement>[]>(() => [
    { header: t("inventory.fields.date"), cell: ({ row }) => formatDate(row.original.createdAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("inventory.fields.product"), accessorKey: "productName" },
    { header: t("inventory.fields.branch"), accessorKey: "branchName" },
    { header: t("inventory.fields.location"), accessorKey: "locationCode" },
    { header: t("inventory.fields.type"), cell: ({ row }) => <MovementTypeBadge type={row.original.movementType} /> },
    { header: t("inventory.fields.in"), cell: ({ row }) => formatNumber(row.original.quantityIn) },
    { header: t("inventory.fields.out"), cell: ({ row }) => formatNumber(row.original.quantityOut) },
    { header: t("inventory.fields.balance"), cell: ({ row }) => row.original.balanceAfter === null ? "-" : formatNumber(row.original.balanceAfter) },
    { header: t("inventory.fields.unitCost"), cell: ({ row }) => row.original.unitCost === null ? "-" : formatMoney(row.original.unitCost) },
    { header: t("inventory.fields.totalCost"), cell: ({ row }) => row.original.totalCost === null ? "-" : formatMoney(row.original.totalCost) },
    { header: t("inventory.fields.reference"), cell: ({ row }) => <InventorySourceLabel sourceType={row.original.sourceType} sourceId={row.original.sourceId} /> },
    { header: t("inventory.fields.user"), cell: ({ row }) => row.original.createdByUsername ?? t("topbar.unknown") },
    { header: t("inventory.fields.notes"), cell: ({ row }) => row.original.notes ?? "-" },
  ], [locale, t, timeZone]);

  if (!canViewInventory) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  return (
    <div className="space-y-6">
      <section className="grid gap-4 md:grid-cols-4">
        <SummaryCard icon={PackageSearch} label={t("inventory.summary.withStock")} value={summaryQuery.data?.productsWithStock ?? "-"} />
        <SummaryCard icon={PackageSearch} label={t("inventory.summary.withoutStock")} value={summaryQuery.data?.productsWithoutStock ?? "-"} />
        <SummaryCard icon={ListFilter} label={t("inventory.summary.lowStock")} value={summaryQuery.data?.lowStockProducts ?? t("inventory.placeholder.noEndpoint")} />
        <SummaryCard icon={ScrollText} label={t("inventory.summary.recentMovements")} value={summaryQuery.data?.totalMovements ?? "-"} />
      </section>

      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.10),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.34))] shadow-sm dark:bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.16),transparent_36%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.18))]">
        <CardContent className="grid gap-3 p-5 md:grid-cols-[1fr_auto_auto] md:items-end">
          <div className="space-y-2">
            <Label>{t("catalog.search")}</Label>
            <Input value={filters.query} onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value, page: 1 }))} placeholder={t("inventory.filters.searchPlaceholder")} />
          </div>
          <Button className="gap-2" type="button" variant="outline" onClick={() => { setDraftFilters(effectiveFilters); setIsFiltersOpen(true); }}>
            <SlidersHorizontal className="h-4 w-4" aria-hidden="true" />
            {t("inventory.filters.button")}
            {activeAdvancedFilters > 0 ? <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-bold text-primary-foreground">{activeAdvancedFilters}</span> : null}
          </Button>
          {canAdjustInventory ? <Button type="button" onClick={() => { form.reset({ ...defaultStockManualValues, branchId: effectiveFilters.branchId || singleBranchId, productId: effectiveFilters.productId }); setIsManualOpen(true); }}>{t("inventory.manual.open")}</Button> : null}
        </CardContent>
      </Card>

      {commonError ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(commonError, t)}</CardContent></Card> : null}

      <div className="flex flex-wrap gap-2">
        <ViewButton active={activeView === "balances"} onClick={() => setActiveView("balances")}>{t("inventory.views.balances")}</ViewButton>
        {canViewLayers ? <ViewButton active={activeView === "layers"} onClick={() => setActiveView("layers")}>{t("inventory.views.layers")}</ViewButton> : null}
        {canViewKardex ? <ViewButton active={activeView === "kardex"} onClick={() => setActiveView("kardex")}>{t("inventory.views.kardex")}</ViewButton> : null}
        {canViewMovements ? <ViewButton active={activeView === "movements"} onClick={() => setActiveView("movements")}>{t("inventory.views.movements")}</ViewButton> : null}
      </div>

      {activeView === "balances" ? <TableSection title={t("inventory.views.balances")} page={balancesQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))}><AdminTable columns={balanceColumns} data={balancesQuery.data?.items ?? []} /></TableSection> : null}

      {activeView === "layers" ? <div className="space-y-4"><div className="max-w-xs"><Select label={t("inventory.fields.status")} value={layerStatus} onChange={(value) => setLayerStatus(value as InventoryLayerStatus | "")} options={["OPEN", "CONSUMED", "VOIDED"].map((status) => ({ value: status, label: t.has(`inventory.layerStatus.${status}`) ? t(`inventory.layerStatus.${status}`) : status }))} includeAll /></div><TableSection title={t("inventory.views.layers")} page={layersQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))}><AdminTable columns={layerColumns} data={layersQuery.data?.items ?? []} /></TableSection></div> : null}

      {activeView === "kardex" ? <TableSection title={selectedProduct ? `${t("inventory.views.kardex")} · ${selectedProduct.name}` : t("inventory.views.kardex")} page={kardexQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))}><AdminTable columns={movementColumns} data={kardexQuery.data?.items ?? []} /></TableSection> : null}

      {activeView === "movements" ? <TableSection title={t("inventory.views.movements")} page={movementsQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))}><AdminTable columns={movementColumns} data={movementsQuery.data?.items ?? []} /></TableSection> : null}

      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("inventory.filters.title")} description={t("inventory.filters.description")}>
        <div className="grid gap-4 md:grid-cols-4">
          <Select label={t("inventory.fields.branch")} value={draftFilters.branchId} onChange={(value) => setDraftFilters((current) => ({ ...current, branchId: value }))} options={branchOptions} />
          <Select label={t("inventory.fields.product")} value={draftFilters.productId} onChange={(value) => setDraftFilters((current) => ({ ...current, productId: value }))} options={(productsQuery.data?.items ?? []).map((product) => ({ value: product.id, label: `${product.name} (${product.baseUnitCode})` }))} />
          <Select label={t("navigation.categories")} value={draftFilters.categoryId} onChange={(value) => setDraftFilters((current) => ({ ...current, categoryId: value }))} options={(categoriesQuery.data?.items ?? []).map((category) => ({ value: category.id, label: category.name }))} />
          <Select label={t("inventory.fields.type")} value={draftFilters.movementType} onChange={(value) => setDraftFilters((current) => ({ ...current, movementType: value as InventoryFilters["movementType"] }))} options={movementTypes.map((type) => ({ value: type, label: t.has(`inventory.movementTypes.${type}`) ? t(`inventory.movementTypes.${type}`) : type }))} />
          <div className="rounded-3xl border border-primary/15 bg-background/70 p-4 shadow-inner shadow-primary/5 md:col-span-4">
            <div className="mb-3 flex items-center gap-2 text-xs font-bold uppercase tracking-[0.18em] text-muted-foreground">
              <CalendarDays className="h-4 w-4 text-primary" aria-hidden="true" />
              {t("inventory.filters.dateRange")}
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <DateFilterField label={t("inventory.fields.fromDate")} value={draftFilters.fromDate} onChange={(value) => setDraftFilters((current) => ({ ...current, fromDate: value }))} />
              <DateFilterField label={t("inventory.fields.toDate")} value={draftFilters.toDate} onChange={(value) => setDraftFilters((current) => ({ ...current, toDate: value }))} />
            </div>
            <p className="mt-3 text-xs leading-5 text-muted-foreground">{t("inventory.filters.dateHelp")}</p>
          </div>
          <div className="flex flex-wrap gap-2 md:col-span-4">
            <Button type="button" onClick={() => { setFilters({ ...draftFilters, query: filters.query, page: 1, pageSize }); setIsFiltersOpen(false); }}>{t("inventory.filters.apply")}</Button>
            <Button type="button" variant="outline" onClick={() => setDraftFilters({ ...defaultInventoryFilters, branchId: singleBranchId, query: filters.query })}><X className="mr-2 h-4 w-4" aria-hidden="true" />{t("inventory.filters.clear")}</Button>
            <Button type="button" variant="ghost" onClick={() => setIsFiltersOpen(false)}>{t("admin.actions.cancel")}</Button>
          </div>
        </div>
      </FormDialog>

      <FormDialog open={isManualOpen} onOpenChange={setIsManualOpen} title={t("inventory.manual.title")} description={t("inventory.manual.description")}>
        <form className="grid gap-4 md:grid-cols-2" onSubmit={form.handleSubmit((values) => manualMutation.mutate(values))}>
          <SelectField label={t("inventory.manual.operation")} registration={form.register("operation")} options={[{ value: "IN", label: t("inventory.manual.stockIn") }, { value: "OUT", label: t("inventory.manual.stockOut") }]} />
          <SelectField label={t("inventory.fields.branch")} registration={form.register("branchId")} options={branchOptions} />
          <SelectField label={t("inventory.fields.product")} registration={form.register("productId")} options={(productsQuery.data?.items ?? []).map((product) => ({ value: product.id, label: `${product.name} (${product.baseUnitCode})` }))} />
          <Field label={t("inventory.fields.quantity")} registration={form.register("quantity", { valueAsNumber: true })} type="number" />
          <Field label={t("inventory.fields.unitCost")} registration={form.register("unitCost", { valueAsNumber: true })} type="number" />
          <Field label={t("inventory.fields.expirationDate")} registration={form.register("expirationDate")} type="date" />
          <Field label={t("inventory.fields.lotNumber")} registration={form.register("lotNumber")} />
          <div className="space-y-2 md:col-span-2"><Label>{t("inventory.fields.notes")}</Label><Input {...form.register("notes")} /></div>
          <div className="flex gap-2 md:col-span-2"><ConfirmDialog trigger={<Button disabled={manualMutation.isPending} type="button">{t("inventory.manual.submit")}</Button>} title={t("inventory.manual.confirmTitle")} description={t("inventory.manual.confirmDescription")} confirmLabel={t("inventory.manual.submit")} cancelLabel={t("admin.actions.cancel")} onConfirm={form.handleSubmit((values) => manualMutation.mutate(values))} /><Button type="button" variant="outline" onClick={() => setIsManualOpen(false)}>{t("admin.actions.cancel")}</Button></div>
        </form>
      </FormDialog>
    </div>
  );
}

function SummaryCard({ icon: Icon, label, value }: Readonly<{ icon: LucideIcon; label: string; value: string | number }>) {
  return <Card><CardContent className="p-5"><div className="mb-4 flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/10 text-primary"><Icon className="h-5 w-5" aria-hidden="true" /></div><p className="text-sm text-muted-foreground">{label}</p><p className="mt-2 text-2xl font-black tracking-[-0.05em]">{value}</p></CardContent></Card>;
}

function ViewButton({ active, disabled, onClick, children }: Readonly<{ active: boolean; disabled?: boolean; onClick: () => void; children: ReactNode }>) {
  return <Button type="button" variant={active ? "default" : "outline"} disabled={disabled} onClick={onClick}>{children}</Button>;
}

function TableSection({ title, page, onPageChange, children }: Readonly<{ title: string; page?: { page: number; totalPages: number; totalItems: number }; onPageChange: (page: number) => void; children: ReactNode }>) {
  return <section className="space-y-3"><div className="flex flex-wrap items-center justify-between gap-2"><h2 className="text-xl font-black tracking-[-0.04em]">{title}</h2>{page ? <div className="flex items-center gap-2 text-sm text-muted-foreground"><span>{page.totalItems} items</span><Button type="button" variant="outline" disabled={page.page <= 1} onClick={() => onPageChange(page.page - 1)}>Prev</Button><span>{page.page}/{Math.max(page.totalPages, 1)}</span><Button type="button" variant="outline" disabled={page.page >= page.totalPages} onClick={() => onPageChange(page.page + 1)}>Next</Button></div> : null}</div>{children}</section>;
}

function QuickActions({ productId, canViewLayers, canViewKardex, canViewMovements, setActiveView, setFilters }: Readonly<{ productId: string; canViewLayers: boolean; canViewKardex: boolean; canViewMovements: boolean; setActiveView: (view: "balances" | "layers" | "kardex" | "movements") => void; setFilters: Dispatch<SetStateAction<InventoryFilters>> }>) {
  return <div className="flex flex-wrap gap-2">{canViewLayers ? <Button variant="outline" onClick={() => { setFilters((current) => ({ ...current, productId, page: 1 })); setActiveView("layers"); }}>FIFO</Button> : null}{canViewKardex ? <Button variant="outline" onClick={() => { setFilters((current) => ({ ...current, productId, page: 1 })); setActiveView("kardex"); }}>Kardex</Button> : null}{canViewMovements ? <Button variant="outline" onClick={() => { setFilters((current) => ({ ...current, productId, page: 1 })); setActiveView("movements"); }}>Mov.</Button> : null}</div>;
}

function Select({ label, value, onChange, options, includeAll = true }: Readonly<{ label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }>; includeAll?: boolean }>) {
  const t = useTranslations();

  return <div className="space-y-2 md:col-span-1"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm shadow-sm transition-colors focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 dark:bg-background/60" value={value} onChange={(event) => onChange(event.target.value)}>{includeAll ? <option value="">{t("inventory.filters.all")}</option> : null}{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>;
}

function DateFilterField({ label, value, onChange }: Readonly<{ label: string; value: string; onChange: (value: string) => void }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input className="bg-background/85 font-mono text-sm dark:bg-background/60" type="date" value={value} onChange={(event) => onChange(event.target.value)} /></div>;
}

function countAdvancedFilters(filters: InventoryFilters) {
  return [filters.branchId, filters.productId, filters.categoryId, filters.movementType, filters.fromDate, filters.toDate].filter(Boolean).length;
}

function SelectField({ label, registration, options }: Readonly<{ label: string; registration: UseFormRegisterReturn; options: Array<{ value: string; label: string }> }>) {
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" {...registration}><option value="">-</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></div>;
}

function Field({ label, registration, type = "text" }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type={type} step="0.000001" {...registration} /></div>;
}

function formatNumber(value: number) {
  return new Intl.NumberFormat("es-GT", { maximumFractionDigits: 6 }).format(value);
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("es-GT", { style: "currency", currency: "GTQ" }).format(value);
}

function formatDate(value: string, timeZone: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short", timeZone }).format(new Date(value));
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}
