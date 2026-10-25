import { BadgeCheck, FileClock, XCircle } from "lucide-react";
import type { SaleStatus } from "@/features/sales/types/sale-types";

const statusStyles: Record<SaleStatus, string> = {
  DRAFT: "border-slate-300 bg-slate-100 text-slate-700 dark:border-slate-700 dark:bg-slate-900/60 dark:text-slate-200",
  CONFIRMED: "border-emerald-300 bg-emerald-100 text-emerald-800 dark:border-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-200",
  CANCELLED: "border-rose-300 bg-rose-100 text-rose-800 dark:border-rose-700 dark:bg-rose-950/50 dark:text-rose-200",
};

export function SaleStatusBadge({ status, label }: Readonly<{ status: SaleStatus; label: string }>) {
  const Icon = status === "CONFIRMED" ? BadgeCheck : status === "CANCELLED" ? XCircle : FileClock;

  return (
    <span className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-bold ${statusStyles[status]}`}>
      <Icon className="h-3.5 w-3.5" aria-hidden="true" />
      {label}
    </span>
  );
}

export function formatMoney(value: number) {
  return new Intl.NumberFormat("es-GT", { style: "currency", currency: "GTQ" }).format(value);
}

export function formatNumber(value: number) {
  return new Intl.NumberFormat("es-GT", { maximumFractionDigits: 6 }).format(value);
}

export function formatDateTime(value: string | null, timeZone: string, locale: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short", timeZone }).format(new Date(value));
}

export function saleErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string, formatPermissionError: (error: unknown, t: (key: string, values?: Record<string, string>) => string) => string | null) {
  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}
