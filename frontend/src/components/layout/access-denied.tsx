"use client";

import Link from "next/link";
import { ShieldAlert } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export function AccessDenied() {
  const t = useTranslations();

  return (
    <Card className="fade-up max-w-3xl overflow-hidden">
      <CardHeader className="border-b bg-gradient-to-br from-card to-muted/50">
        <div className="mb-6 flex h-14 w-14 items-center justify-center rounded-2xl bg-destructive text-destructive-foreground">
          <ShieldAlert className="h-7 w-7" aria-hidden="true" />
        </div>
        <CardTitle>{t("accessDenied.title")}</CardTitle>
        <CardDescription>{t("accessDenied.description")}</CardDescription>
      </CardHeader>
      <CardContent className="p-6">
        <Link href="/dashboard">
          <Button variant="outline">{t("accessDenied.backToDashboard")}</Button>
        </Link>
      </CardContent>
    </Card>
  );
}
