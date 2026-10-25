"use client";

import { LocaleProvider } from "@/components/providers/locale-provider";
import { PwaServiceWorker } from "@/components/providers/pwa-service-worker";
import { QueryProvider } from "@/components/providers/query-provider";
import { ThemeProvider } from "@/components/providers/theme-provider";
import { ThemedToaster } from "@/components/providers/themed-toaster";
import type { AppLocale } from "@/lib/i18n/messages";

export function AppProviders({
  children,
  locale,
}: Readonly<{
  children: React.ReactNode;
  locale: string;
}>) {
  return (
    <ThemeProvider>
      <LocaleProvider initialLocale={locale as AppLocale}>
        <QueryProvider>{children}</QueryProvider>
        <ThemedToaster />
        <PwaServiceWorker />
      </LocaleProvider>
    </ThemeProvider>
  );
}
