"use client";

import { useTranslations } from "next-intl";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { SaleDetailScreen } from "@/features/sales/components/sale-detail-screen";

export function SaleDetailPageContent({ saleId }: Readonly<{ saleId: string }>) {
  const t = useTranslations();
  return <AdminPage title={t("sales.detailTitle")} description={t("sales.detailDescription")} eyebrow={t("navigation.operation")}><SaleDetailScreen saleId={saleId} /></AdminPage>;
}
