"use client";

import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";
import { formatPermissionError, getMissingPermissions } from "@/lib/api/errors";

export function QueryProvider({ children }: Readonly<{ children: React.ReactNode }>) {
  const t = useTranslations();
  const [queryClient] = useState(
    () =>
      new QueryClient({
        queryCache: new QueryCache({
          onError: (error) => notifyPermissionError(error, t),
        }),
        mutationCache: new MutationCache({
          onError: (error) => notifyPermissionError(error, t),
        }),
        defaultOptions: {
          queries: {
            retry: 1,
            staleTime: 30_000,
          },
        },
      }),
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function notifyPermissionError(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  const message = formatPermissionError(error, t);

  if (!message) {
    return;
  }

  toast.error(message, { id: `missing-permission-${getMissingPermissions(error).join("-")}` });
}
