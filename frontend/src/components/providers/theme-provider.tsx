"use client";

import { createContext, useContext, useEffect, useSyncExternalStore } from "react";

type AppTheme = "light" | "dark";

const storageKey = "vivent-theme";
const listeners = new Set<() => void>();

const ThemeContext = createContext<{
  theme: AppTheme;
  resolvedTheme: AppTheme;
  setTheme: (theme: AppTheme) => void;
} | null>(null);

function getStoredTheme(): AppTheme {
  if (typeof window === "undefined") return "light";

  const storedTheme = window.localStorage.getItem(storageKey);
  return storedTheme === "dark" || storedTheme === "light" ? storedTheme : "light";
}

function subscribeToThemeChanges(listener: () => void) {
  listeners.add(listener);

  function handleStorage(event: StorageEvent) {
    if (event.key === storageKey) listener();
  }

  window.addEventListener("storage", handleStorage);

  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", handleStorage);
  };
}

function notifyThemeChanges() {
  for (const listener of listeners) listener();
}

export function ThemeProvider({ children }: Readonly<{ children: React.ReactNode }>) {
  const theme = useSyncExternalStore<AppTheme>(subscribeToThemeChanges, getStoredTheme, () => "light");

  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
    window.localStorage.setItem(storageKey, theme);
  }, [theme]);

  function setTheme(nextTheme: AppTheme) {
    window.localStorage.setItem(storageKey, nextTheme);
    notifyThemeChanges();
  }

  return <ThemeContext.Provider value={{ theme, resolvedTheme: theme, setTheme }}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);

  if (!context) {
    throw new Error("useTheme must be used within ThemeProvider");
  }

  return context;
}
