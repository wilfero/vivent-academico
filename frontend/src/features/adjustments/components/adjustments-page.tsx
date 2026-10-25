"use client";

import { useTranslations } from "next-intl";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AdjustmentsScreen } from "@/features/adjustments/components/adjustments-screen";
import { AdjustmentDetailScreen } from "@/features/adjustments/components/adjustment-detail-screen";

export function AdjustmentsPageContent() {
  const t = useTranslations();
  return <AdminPage title={t("adjustments.title")} description={t("adjustments.description")} eyebrow={t("navigation.operation")}><AdjustmentsScreen /></AdminPage>;
}

export function NewAdjustmentPageContent() {
  const t = useTranslations();
  return <AdminPage title={t("adjustments.newTitle")} description={t("adjustments.newDescription")} eyebrow={t("navigation.operation")}><AdjustmentsScreen mode="new" /></AdminPage>;
}

export function AdjustmentDetailPageContent({ adjustmentId }: Readonly<{ adjustmentId: string }>) {
  const t = useTranslations();
  return <AdminPage title={t("adjustments.detailTitle")} description={t("adjustments.detailDescription")} eyebrow={t("navigation.operation")}><AdjustmentDetailScreen adjustmentId={adjustmentId} /></AdminPage>;
}

export function AdjustmentApprovalsPageContent() {
  const t = useTranslations();
  return <AdminPage title={t("adjustments.approvalsTitle")} description={t("adjustments.approvalsDescription")} eyebrow={t("navigation.operation")}><AdjustmentsScreen mode="approvals" /></AdminPage>;
}
