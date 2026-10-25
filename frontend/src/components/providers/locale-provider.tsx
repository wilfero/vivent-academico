"use client";

import { createContext, useContext, useMemo, useState } from "react";
import { NextIntlClientProvider, type AbstractIntlMessages } from "next-intl";
import { clientMessages, type AppLocale } from "@/lib/i18n/messages";

type LocaleContextValue = {
  locale: AppLocale;
  setLocale: (locale: AppLocale) => void;
  timeZone: string;
  setTimeZone: (timeZone: string) => void;
};

const LocaleContext = createContext<LocaleContextValue | null>(null);

export function LocaleProvider({ children, initialLocale }: Readonly<{ children: React.ReactNode; initialLocale: AppLocale }>) {
  const [locale, setLocale] = useState<AppLocale>(initialLocale);
  const [timeZone, setTimeZone] = useState("UTC");
  const value = useMemo(() => ({ locale, setLocale, timeZone, setTimeZone }), [locale, timeZone]);

  return (
    <LocaleContext.Provider value={value}>
      <NextIntlClientProvider key={`${locale}-${timeZone}`} locale={locale} messages={clientMessages[locale] as AbstractIntlMessages} timeZone={timeZone}>
        {children}
      </NextIntlClientProvider>
    </LocaleContext.Provider>
  );
}

export function useAppLocale() {
  const context = useContext(LocaleContext);

  if (!context) {
    throw new Error("useAppLocale must be used within LocaleProvider");
  }

  return context;
}
