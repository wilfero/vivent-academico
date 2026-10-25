"use client";

import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Eye, Filter } from "lucide-react";
import { useTranslations } from "next-intl";
import { useMemo, useState } from "react";
import { useAppLocale } from "@/components/providers/locale-provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { can } from "@/features/admin/catalog/shared/catalog-permissions";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { auditLogQueryKeys, listAuditLogs } from "@/features/audit-logs/api/audit-logs-api";
import type { AuditLog, AuditLogFilters } from "@/features/audit-logs/types/audit-log-types";
import { formatPermissionError } from "@/lib/api/errors";

const pageSize = 20;
const defaultFilters: AuditLogFilters = { page: 1, pageSize, entityType: "", entityId: "" };
const auditEntityTypes = [
  "company",
  "branch",
  "location",
  "user",
  "role",
  "permission",
  "plan",
  "subscription",
  "product_unit",
  "product_category",
  "product",
  "product_presentation",
  "supplier",
  "customer",
  "purchase",
  "purchase_detail",
  "sale",
  "sale_detail",
  "transfer",
  "transfer_detail",
  "inventory_adjustment",
  "inventory_adjustment_line",
  "inventory_balance",
  "inventory_layer",
  "stock_movement",
  "cash_closure",
  "tenant_config",
  "auth_session",
];

export function AuditLogsScreen() {
  const t = useTranslations();
  const { locale, timeZone } = useAppLocale();
  const canView = can("settings.view");
  const [filters, setFilters] = useState<AuditLogFilters>(defaultFilters);
  const [draftFilters, setDraftFilters] = useState<AuditLogFilters>(defaultFilters);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);
  const auditLogsQuery = useQuery({ queryKey: auditLogQueryKeys.list(filters), queryFn: () => listAuditLogs(filters), enabled: canView });
  const columns = useMemo<ColumnDef<AuditLog>[]>(() => [
    { header: t("auditLogs.fields.createdAt"), cell: ({ row }) => formatAuditDate(row.original.createdAt, row.original.timezoneSnapshot ?? timeZone, locale) },
    { header: t("auditLogs.fields.user"), cell: ({ row }) => row.original.userId?.slice(0, 8) ?? "-" },
    { header: t("auditLogs.fields.action"), cell: ({ row }) => <span className="rounded-full bg-muted px-2.5 py-1 text-xs font-bold">{t(`auditLogs.actions.${row.original.action}`)}</span> },
    { header: t("auditLogs.fields.entityType"), cell: ({ row }) => formatAuditEntityType(t, row.original.entityType) },
    { header: t("auditLogs.fields.entityId"), cell: ({ row }) => <span className="font-mono text-xs">{row.original.entityId?.slice(0, 8) ?? "-"}</span> },
    { header: t("auditLogs.fields.ipAddress"), cell: ({ row }) => row.original.ipAddress ?? "-" },
    { header: t("auditLogs.fields.result"), cell: () => t("auditLogs.result.recorded") },
    { header: t("admin.fields.actions"), cell: ({ row }) => <Button type="button" variant="outline" onClick={() => setSelectedLog(row.original)}><Eye className="mr-2 h-4 w-4" aria-hidden="true" />{t("auditLogs.actions.view")}</Button> },
  ], [locale, t, timeZone]);

  if (!canView) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden border-primary/10 bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.10),transparent_34%),linear-gradient(135deg,hsl(var(--card)),hsl(var(--muted)/0.34))] shadow-sm">
        <CardContent className="flex flex-wrap items-end justify-between gap-3 p-5">
          <div>
            <p className="text-sm text-muted-foreground">{t("auditLogs.help.readOnly")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("auditLogs.help.supportedFilters")}</p>
          </div>
          <Button className="gap-2" type="button" variant="outline" onClick={() => { setDraftFilters(filters); setIsFiltersOpen(true); }}>
            <Filter className="h-4 w-4" aria-hidden="true" />{t("auditLogs.filters.button")}
            {[filters.entityType, filters.entityId].filter(Boolean).length > 0 ? <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-bold text-primary-foreground">{[filters.entityType, filters.entityId].filter(Boolean).length}</span> : null}
          </Button>
        </CardContent>
      </Card>

      {auditLogsQuery.error ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(auditLogsQuery.error, t)}</CardContent></Card> : null}

      <TableSection page={auditLogsQuery.data} onPageChange={(page) => setFilters((current) => ({ ...current, page }))}>
        <AdminTable columns={columns} data={auditLogsQuery.data?.items ?? []} />
      </TableSection>

      <FormDialog open={isFiltersOpen} onOpenChange={setIsFiltersOpen} title={t("auditLogs.filters.title")} description={t("auditLogs.filters.description")}>
        <div className="grid gap-4 md:grid-cols-2">
          <EntityTypeFilter value={draftFilters.entityType} onChange={(value) => setDraftFilters((current) => ({ ...current, entityType: value }))} />
          <Field label={t("auditLogs.fields.entityId")} value={draftFilters.entityId} onChange={(value) => setDraftFilters((current) => ({ ...current, entityId: value }))} placeholder="UUID" />
          <div className="flex flex-wrap gap-2 md:col-span-2">
            <Button type="button" onClick={() => { setFilters({ ...draftFilters, page: 1, pageSize }); setIsFiltersOpen(false); }}>{t("auditLogs.filters.apply")}</Button>
            <Button type="button" variant="outline" onClick={() => setDraftFilters(defaultFilters)}>{t("auditLogs.filters.clear")}</Button>
            <Button type="button" variant="ghost" onClick={() => setIsFiltersOpen(false)}>{t("admin.actions.cancel")}</Button>
          </div>
        </div>
      </FormDialog>

      <AuditLogDetailDialog log={selectedLog} onOpenChange={(open) => !open && setSelectedLog(null)} />
    </div>
  );
}

function AuditLogDetailDialog({ log, onOpenChange }: Readonly<{ log: AuditLog | null; onOpenChange: (open: boolean) => void }>) {
  const t = useTranslations();
  if (!log) return null;
  return <FormDialog open={Boolean(log)} onOpenChange={onOpenChange} title={t("auditLogs.detail.title")} description={t("auditLogs.detail.description")}><div className="space-y-4"><div className="grid gap-3 text-sm md:grid-cols-2"><Info label={t("auditLogs.fields.action")} value={t(`auditLogs.actions.${log.action}`)} /><Info label={t("auditLogs.fields.entityType")} value={formatAuditEntityType(t, log.entityType)} /><Info label={t("auditLogs.fields.entityId")} value={log.entityId} /><Info label={t("auditLogs.fields.user")} value={log.userId ?? "-"} /><Info label={t("auditLogs.fields.ipAddress")} value={log.ipAddress ?? "-"} /><Info label={t("auditLogs.fields.userAgent")} value={log.userAgent ?? "-"} /></div><JsonBlock title={t("auditLogs.fields.oldValue")} value={log.oldValue} /><JsonBlock title={t("auditLogs.fields.newValue")} value={log.newValue} /><div className="flex justify-end"><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>{t("admin.actions.cancel")}</Button></div></div></FormDialog>;
}

function JsonBlock({ title, value }: Readonly<{ title: string; value: string | null }>) {
  return <div className="space-y-2"><Label>{title}</Label><pre className="max-h-56 overflow-auto rounded-2xl border bg-muted/35 p-3 text-xs">{prettyJson(value)}</pre></div>;
}

function Info({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="rounded-2xl border bg-background/60 p-3"><p className="text-xs font-bold uppercase tracking-[0.14em] text-muted-foreground">{label}</p><p className="mt-1 break-all font-semibold">{value}</p></div>;
}

function Field({ label, value, onChange, placeholder }: Readonly<{ label: string; value: string; onChange: (value: string) => void; placeholder?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input value={value} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} /></div>;
}

function EntityTypeFilter({ value, onChange }: Readonly<{ value: string; onChange: (value: string) => void }>) {
  const t = useTranslations();
  const query = value.trim().toLowerCase();
  const options = useMemo(() => auditEntityTypes.map((code) => ({ code, label: formatAuditEntityType(t, code) })).filter((option) => !query || option.code.includes(query) || option.label.toLowerCase().includes(query)), [query, t]);

  return <div className="space-y-2"><Label>{t("auditLogs.fields.entityType")}</Label><Input value={value} placeholder={t("auditLogs.filters.entityPlaceholder")} onChange={(event) => onChange(event.target.value)} /><div className="max-h-48 overflow-auto rounded-2xl border bg-background p-1">{options.length > 0 ? options.map((option) => <button key={option.code} type="button" className="flex w-full items-center justify-between gap-3 rounded-xl px-3 py-2 text-left text-sm hover:bg-muted" onClick={() => onChange(option.code)}><span className="font-semibold">{option.label}</span><span className="font-mono text-xs text-muted-foreground">{option.code}</span></button>) : <p className="px-3 py-2 text-sm text-muted-foreground">{t("auditLogs.filters.noEntities")}</p>}</div></div>;
}

function TableSection({ page, onPageChange, children }: Readonly<{ page?: { page: number; totalPages: number; totalItems: number }; onPageChange: (page: number) => void; children: React.ReactNode }>) {
  return <section className="space-y-3"><div className="flex flex-wrap items-center justify-between gap-2"><h2 className="text-xl font-black tracking-[-0.04em]">Audit logs</h2>{page ? <div className="flex items-center gap-2 text-sm text-muted-foreground"><span>{page.totalItems} items</span><Button type="button" variant="outline" disabled={page.page <= 1} onClick={() => onPageChange(page.page - 1)}>Prev</Button><span>{page.page}/{Math.max(page.totalPages, 1)}</span><Button type="button" variant="outline" disabled={page.page >= page.totalPages} onClick={() => onPageChange(page.page + 1)}>Next</Button></div> : null}</div>{children}</section>;
}

function formatAuditDate(value: string | null, timeZone: string, locale: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short", timeZone }).format(new Date(value));
}

function prettyJson(value: string | null) {
  if (!value) return "-";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function formatAuditEntityType(t: ReturnType<typeof useTranslations>, value: string) {
  const key = `auditLogs.entities.${value}`;
  return t.has(key) ? t(key) : humanizeCode(value);
}

function humanizeCode(value: string) {
  return value.split("_").map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()).join(" ");
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}
