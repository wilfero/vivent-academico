"use client";

import { Toaster } from "sonner";
import { useTheme } from "@/components/providers/theme-provider";

export function ThemedToaster() {
  const { resolvedTheme } = useTheme();

  return (
    <Toaster
      closeButton
      richColors={false}
      position="top-right"
      theme={resolvedTheme === "dark" ? "dark" : "light"}
      toastOptions={{
        classNames: {
          toast: "vivent-toast",
          title: "vivent-toast-title",
          description: "vivent-toast-description",
          actionButton: "vivent-toast-action",
          cancelButton: "vivent-toast-cancel",
          closeButton: "vivent-toast-close",
        },
      }}
    />
  );
}
