"use client";

import { Moon, Sun } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAppLocale } from "@/components/providers/locale-provider";
import { useTheme } from "@/components/providers/theme-provider";

export function PreferenceControls() {
  const { locale, setLocale } = useAppLocale();
  const { resolvedTheme, setTheme } = useTheme();
  const isDark = resolvedTheme === "dark";

  return (
    <div className="flex items-center gap-2">
      <Button
        className="h-9 px-3 font-mono text-xs"
        type="button"
        variant="outline"
        aria-label="Cambiar idioma"
        onClick={() => setLocale(locale === "es-GT" ? "en-US" : "es-GT")}
      >
        {locale === "es-GT" ? "ES" : "EN"}
      </Button>
      <Button
        className="h-9 w-9 px-0"
        type="button"
        variant="outline"
        aria-label="Cambiar tema"
        onClick={() => setTheme(isDark ? "light" : "dark")}
      >
        {isDark ? <Sun className="h-4 w-4" aria-hidden="true" /> : <Moon className="h-4 w-4" aria-hidden="true" />}
      </Button>
    </div>
  );
}
