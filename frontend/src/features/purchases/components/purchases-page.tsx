"use client";

import { useTranslations } from "next-intl";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { PurchasesScreen } from "@/features/purchases/components/purchases-screen";

export function PurchasesPageContent() {
  const t = useTranslations();

  return (
    <AdminPage title={t("purchases.title")} description={t("purchases.description")} eyebrow={t("navigation.operation")}>
      <PurchasesScreen />
    </AdminPage>
  );
}

export function NewPurchasePageContent() {
  const t = useTranslations();

  return (
    <AdminPage title={t("purchases.newTitle")} description={t("purchases.newDescription")} eyebrow={t("navigation.operation")}>
      <PurchasesScreen mode="new" />
    </AdminPage>
  );
}
