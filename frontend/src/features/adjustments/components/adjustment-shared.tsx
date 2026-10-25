import { BadgeCheck, CircleDashed, Clock3, PackageMinus, PackagePlus, Scale, XCircle, type LucideIcon } from "lucide-react";
import type { AdjustmentDirection, AdjustmentStatus } from "@/features/adjustments/types/adjustment-types";

const statusIcons: Record<AdjustmentStatus, LucideIcon> = {
  DRAFT: CircleDashed,
  PENDING_APPROVAL: Clock3,
  APPROVED: BadgeCheck,
  REJECTED: XCircle,
  CANCELLED: XCircle,
};

export function AdjustmentStatusBadge({ status, label }: Readonly<{ status: AdjustmentStatus; label: string }>) {
  const Icon = statusIcons[status];
  return <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold ${statusClass(status)}`}><Icon className="h-3.5 w-3.5" aria-hidden="true" />{label}</span>;
}

export function AdjustmentTypeBadge({ direction, label }: Readonly<{ direction: AdjustmentDirection; label: string }>) {
  const Icon = direction === "IN" ? PackagePlus : direction === "OUT" ? PackageMinus : Scale;
  return <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold ${direction === "IN" ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-950/45 dark:text-emerald-200" : "bg-amber-100 text-amber-800 dark:bg-amber-950/45 dark:text-amber-200"}`}><Icon className="h-3.5 w-3.5" aria-hidden="true" />{label}</span>;
}

export function formatAdjustmentDate(value: string | null, timeZone: string, locale: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short", timeZone }).format(new Date(value));
}

export function formatAdjustmentNumber(value: number | null | undefined) {
  return new Intl.NumberFormat("es-GT", { maximumFractionDigits: 6 }).format(Number(value || 0));
}

export function displayUser(username: string | null | undefined, userId?: string | null | undefined) {
  return username ?? (userId ? "-" : "-");
}

function statusClass(status: AdjustmentStatus) {
  switch (status) {
    case "DRAFT":
      return "bg-slate-100 text-slate-800 dark:bg-slate-900/70 dark:text-slate-200";
    case "PENDING_APPROVAL":
      return "bg-sky-100 text-sky-800 dark:bg-sky-950/45 dark:text-sky-200";
    case "APPROVED":
      return "bg-emerald-100 text-emerald-800 dark:bg-emerald-950/45 dark:text-emerald-200";
    case "REJECTED":
    case "CANCELLED":
      return "bg-rose-100 text-rose-800 dark:bg-rose-950/45 dark:text-rose-200";
    default:
      return "bg-muted text-muted-foreground";
  }
}
