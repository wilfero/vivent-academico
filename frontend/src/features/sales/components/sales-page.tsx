"use client";

import { useTranslations } from "next-intl";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { SalesScreen } from "@/features/sales/components/sales-screen";
import { SalePosScreen } from "@/features/sales/components/sale-pos-screen";

export function SalesPageContent() {
  const t = useTranslations();
  return <AdminPage title={t("sales.title")} description={t("sales.description")} eyebrow={t("navigation.operation")}><SalesScreen /></AdminPage>;
}

export function NewSalePageContent() {
  const t = useTranslations();
  return <AdminPage title={t("sales.newTitle")} description={t("sales.newDescription")} eyebrow={t("navigation.operation")}><SalePosScreen /></AdminPage>;
}
