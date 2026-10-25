"use client";

import { useTranslations } from "next-intl";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { InventoryScreen } from "@/features/inventory/components/inventory-screen";

export function InventoryPageContent() {
  const t = useTranslations();

  return (
    <AdminPage title={t("inventory.title")} description={t("inventory.description")} eyebrow={t("navigation.operation")}>
      <InventoryScreen />
    </AdminPage>
  );
}
