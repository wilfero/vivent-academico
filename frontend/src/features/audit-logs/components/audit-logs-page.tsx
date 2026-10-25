"use client";

import { useTranslations } from "next-intl";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AuditLogsScreen } from "@/features/audit-logs/components/audit-logs-screen";

export function AuditLogsPageContent() {
  const t = useTranslations();
  return <AdminPage title={t("auditLogs.title")} description={t("auditLogs.description")} eyebrow={t("navigation.security")}><AuditLogsScreen /></AdminPage>;
}
