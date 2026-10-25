import { BadgeCheck, CircleDashed, Clock3, PackageCheck, Truck, XCircle, type LucideIcon } from "lucide-react";
import type { TransferStatus } from "@/features/transfers/types/transfer-types";

const statusIcons: Record<TransferStatus, LucideIcon> = {
  REQUESTED: Clock3,
  APPROVED: BadgeCheck,
  IN_TRANSIT: Truck,
  PARTIALLY_RECEIVED: CircleDashed,
  RECEIVED: PackageCheck,
  REJECTED: XCircle,
  CANCELLED: XCircle,
};

export function TransferStatusBadge({ status, label }: Readonly<{ status: TransferStatus; label: string }>) {
  const className = statusClass(status);
  const Icon = statusIcons[status];
  return <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold ${className}`}><Icon className="h-3.5 w-3.5" aria-hidden="true" />{label}</span>;
}

export function formatTransferDate(value: string | null, timeZone: string, locale: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short", timeZone }).format(new Date(value));
}

export function formatTransferNumber(value: number) {
  return new Intl.NumberFormat("es-GT", { maximumFractionDigits: 6 }).format(Number(value || 0));
}

function statusClass(status: TransferStatus) {
  switch (status) {
    case "REQUESTED":
      return "bg-sky-100 text-sky-800 dark:bg-sky-950/45 dark:text-sky-200";
    case "APPROVED":
      return "bg-indigo-100 text-indigo-800 dark:bg-indigo-950/45 dark:text-indigo-200";
    case "IN_TRANSIT":
      return "bg-amber-100 text-amber-800 dark:bg-amber-950/45 dark:text-amber-200";
    case "PARTIALLY_RECEIVED":
      return "bg-violet-100 text-violet-800 dark:bg-violet-950/45 dark:text-violet-200";
    case "RECEIVED":
      return "bg-emerald-100 text-emerald-800 dark:bg-emerald-950/45 dark:text-emerald-200";
    case "REJECTED":
    case "CANCELLED":
      return "bg-rose-100 text-rose-800 dark:bg-rose-950/45 dark:text-rose-200";
    default:
      return "bg-muted text-muted-foreground";
  }
}
