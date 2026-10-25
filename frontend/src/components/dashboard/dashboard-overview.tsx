"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, Boxes, PackageCheck, ReceiptText } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import type { ElementType, ReactNode } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { getInventorySummary, listInventoryMovements } from "@/features/inventory/api/inventory-api";
import { MovementTypeBadge } from "@/features/inventory/components/inventory-badges";
import { listPurchases } from "@/features/purchases/api/purchases-api";
import { listSales } from "@/features/sales/api/sales-api";

export function DashboardOverview() {
  const t = useTranslations();
  const canViewInventory = canAny(["inventory.view"]);
  const canViewMovements = canAny(["inventory.view_movements"]);
  const canViewSales = canAny(["sales.view"]);
  const canViewPurchases = canAny(["purchases.view"]);
  const inventorySummaryQuery = useQuery({ queryKey: ["dashboard", "inventory-summary"], queryFn: getInventorySummary, enabled: canViewInventory });
  const movementsQuery = useQuery({ queryKey: ["dashboard", "movements"], queryFn: () => listInventoryMovements({ page: 1, pageSize: 5, branchId: "", locationId: "", productId: "", categoryId: "", query: "", movementType: "", fromDate: "", toDate: "" }), enabled: canViewMovements });
  const salesQuery = useQuery({ queryKey: ["dashboard", "sales"], queryFn: () => listSales({ page: 1, pageSize: 5, branchId: "", customerId: "", status: "", paymentMethod: "", fromDate: "", toDate: "", query: "" }), enabled: canViewSales });
  const purchasesQuery = useQuery({ queryKey: ["dashboard", "purchases"], queryFn: () => listPurchases({ page: 1, pageSize: 5, branchId: "", supplierId: "", status: "", fromDate: "", toDate: "", query: "" }), enabled: canViewPurchases });

  return (
    <div className="fade-up space-y-6">
      <div className="max-w-3xl">
        <p className="font-mono text-xs font-bold uppercase tracking-[0.18em] text-muted-foreground">Vivent</p>
        <h1 className="mt-3 text-4xl font-black tracking-[-0.05em] sm:text-5xl">{t("dashboard.title")}</h1>
        <p className="mt-4 text-sm leading-7 text-muted-foreground">{t("dashboard.description")}</p>
      </div>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {canViewInventory ? <MetricCard icon={Boxes} title={t("inventory.summary.withStock")} value={inventorySummaryQuery.data?.productsWithStock ?? "-"} description={t("dashboard.cards.stockDescription")} /> : null}
        {canViewInventory ? <MetricCard icon={AlertTriangle} title={t("inventory.summary.lowStock")} value={inventorySummaryQuery.data?.lowStockProducts ?? "-"} description={t("dashboard.cards.lowStockDescription")} /> : null}
        {canViewSales ? <MetricCard icon={ReceiptText} title={t("dashboard.cards.sales")} value={salesQuery.data?.totalItems ?? "-"} description={t("dashboard.cards.salesDescription")} href="/sales" /> : null}
        {canViewPurchases ? <MetricCard icon={PackageCheck} title={t("dashboard.cards.purchases")} value={purchasesQuery.data?.totalItems ?? "-"} description={t("dashboard.cards.purchasesDescription")} href="/purchases" /> : null}
      </div>
      {canViewMovements ? <Card><CardHeader><CardTitle>{t("dashboard.recentMovements")}</CardTitle><CardDescription>{t("dashboard.recentMovementsDescription")}</CardDescription></CardHeader><CardContent><div className="space-y-3">{movementsQuery.data?.items.length ? movementsQuery.data.items.map((movement) => <div key={movement.id} className="flex flex-col gap-2 rounded-2xl border bg-background/60 p-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="font-bold">{movement.productName}</p><p className="text-xs text-muted-foreground">{movement.branchName} · {new Date(movement.createdAt).toLocaleString()}</p></div><MovementTypeBadge type={movement.movementType} /></div>) : <p className="text-sm text-muted-foreground">{t("admin.states.empty")}</p>}</div></CardContent></Card> : null}
    </div>
  );
}

function MetricCard({ icon: Icon, title, value, description, href }: Readonly<{ icon: ElementType; title: string; value: ReactNode; description: string; href?: string }>) {
  const content = <Card className="overflow-hidden transition hover:border-primary/30"><CardHeader><div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-primary/12 text-primary"><Icon className="h-5 w-5" aria-hidden="true" /></div><CardTitle className="text-xl">{title}</CardTitle><CardDescription>{description}</CardDescription></CardHeader><CardContent><div className="rounded-2xl border bg-background/60 p-4 font-mono text-2xl font-black text-foreground">{value}</div></CardContent></Card>;
  return href ? <Link href={href}>{content}</Link> : content;
}
