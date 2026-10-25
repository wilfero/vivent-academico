"use client";

import { useTranslations } from "next-intl";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { PurchaseDetailScreen } from "@/features/purchases/components/purchase-detail-screen";

export function PurchaseDetailPageContent({ purchaseId }: Readonly<{ purchaseId: string }>) {
  const t = useTranslations();

  return (
    <AdminPage title={t("purchases.detailTitle")} description={t("purchases.detailDescription")} eyebrow={t("navigation.operation")}>
      <PurchaseDetailScreen purchaseId={purchaseId} />
    </AdminPage>
  );
}
