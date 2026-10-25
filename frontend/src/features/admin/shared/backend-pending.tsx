"use client";

import { ServerOff } from "lucide-react";
import { useTranslations } from "next-intl";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export function BackendPending({ moduleName }: Readonly<{ moduleName: string }>) {
  const t = useTranslations();

  return (
    <Card className="max-w-3xl">
      <CardHeader>
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-muted text-muted-foreground">
          <ServerOff className="h-6 w-6" aria-hidden="true" />
        </div>
        <CardTitle>{moduleName}</CardTitle>
        <CardDescription>{t("admin.states.backendPending")}</CardDescription>
      </CardHeader>
      <CardContent className="text-sm text-muted-foreground">{t("admin.states.backendPendingDetail")}</CardContent>
    </Card>
  );
}
