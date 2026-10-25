"use client";

import { cn } from "@/lib/utils";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import type { InventoryLayerStatus, StockMovementType } from "@/features/inventory/types/inventory-types";

export function StockStatusBadge({ quantity, minimumStock = 0 }: Readonly<{ quantity: number; minimumStock?: number | null }>) {
  const t = useTranslations();
  const status = quantity <= 0 ? "out" : minimumStock && quantity <= minimumStock ? "low" : "available";
  const label = t(`inventory.stockStatus.${status}`);

  return <Badge className={status === "out" ? "border-destructive/30 bg-destructive/10 text-destructive" : status === "low" ? "border-warehouse-line/30 bg-warehouse-line/10 text-foreground" : "border-accent/40 bg-accent/15 text-accent-foreground"}>{label}</Badge>;
}

export function LayerStatusBadge({ status }: Readonly<{ status: InventoryLayerStatus }>) {
  const t = useTranslations();
  return <Badge className={status === "OPEN" ? "border-accent/40 bg-accent/15 text-accent-foreground" : status === "CONSUMED" ? "border-muted bg-muted text-muted-foreground" : "border-destructive/30 bg-destructive/10 text-destructive"}>{t(`inventory.layerStatus.${status}`)}</Badge>;
}

export function MovementTypeBadge({ type }: Readonly<{ type: StockMovementType }>) {
  const t = useTranslations();
  const isIn = type.endsWith("_IN");
  const isReserved = type === "TRANSFER_RESERVED";
  return <Badge className={isReserved ? "border-primary/30 bg-primary/10 text-primary" : isIn ? "border-accent/40 bg-accent/15 text-accent-foreground" : "border-destructive/25 bg-destructive/10 text-destructive"}>{translateInventoryCode(t, "movementTypes", type)}</Badge>;
}

export function InventorySourceLabel({ sourceType, sourceId }: Readonly<{ sourceType: string; sourceId: string }>) {
  const t = useTranslations();

  return <div><p className="text-xs font-bold">{translateInventoryCode(t, "sourceTypes", sourceType)}</p><p className="font-mono text-xs text-muted-foreground">{sourceId.slice(0, 8)}</p></div>;
}

function translateInventoryCode(t: ReturnType<typeof useTranslations>, namespace: "movementTypes" | "sourceTypes", value: string) {
  const key = `inventory.${namespace}.${value}`;
  return t.has(key) ? t(key) : humanizeCode(value);
}

function humanizeCode(value: string) {
  return value.toLowerCase().split("_").map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(" ");
}

function Badge({ className, children }: Readonly<{ className?: string; children: ReactNode }>) {
  return <span className={cn("inline-flex rounded-full border px-2.5 py-1 text-xs font-black uppercase tracking-[0.12em]", className)}>{children}</span>;
}
