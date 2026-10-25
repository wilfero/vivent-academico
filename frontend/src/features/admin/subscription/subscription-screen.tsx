"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getCurrentSubscription } from "@/features/admin/shared/admin-api";
import { AdminPage } from "@/features/admin/shared/admin-page";

export function SubscriptionScreen() {
  const t = useTranslations();
  const subscriptionQuery = useQuery({ queryKey: ["admin", "subscription", "current"], queryFn: getCurrentSubscription });
  const subscription = subscriptionQuery.data;

  return (
    <AdminPage title={t("admin.subscription.planAndLimitsTitle")} description={t("admin.subscription.description")} isLoading={subscriptionQuery.isLoading} error={subscriptionQuery.error}>
      <section className="space-y-3">
        <div>
          <h2 className="text-xl font-black tracking-tight">{t("admin.subscription.planSummary")}</h2>
          <p className="text-sm text-muted-foreground">{subscription?.plan.description}</p>
        </div>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Info title={t("admin.fields.plan")} value={subscription?.plan.name} />
          <Info title={t("admin.fields.status")} value={subscription?.status} />
          <Info title={t("admin.fields.startsAt")} value={subscription?.startsAt} />
          <Info title={t("admin.fields.endsAt")} value={subscription?.endsAt ?? subscription?.trialEndsAt ?? "-"} />
        </div>
      </section>
      <section className="space-y-3">
        <h2 className="text-xl font-black tracking-tight">{t("admin.subscription.limitsSummary")}</h2>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Info title={t("admin.fields.maxUsers")} value={subscription?.plan.maxUsers?.toString()} />
          <Info title={t("admin.fields.maxBranches")} value={subscription?.plan.maxBranches?.toString()} />
          <Info title={t("admin.fields.maxProducts")} value={subscription?.plan.maxProducts?.toString()} />
          <Info title={t("admin.fields.modules")} value={subscription?.plan.enabledModules.join(", ")} />
        </div>
      </section>
    </AdminPage>
  );
}

function Info({ title, value }: Readonly<{ title: string; value?: string }>) {
  return <Card><CardHeader><CardTitle className="text-lg">{title}</CardTitle></CardHeader><CardContent className="font-mono text-sm text-muted-foreground">{value ?? "-"}</CardContent></Card>;
}
