"use client";

import { useTranslations } from "next-intl";
import { useEffect } from "react";
import { toast } from "sonner";
import { formatApiErrorMessage } from "@/lib/api/errors";

export function AdminPage({
  title,
  description,
  eyebrow,
  isLoading,
  error,
  successMessage,
  children,
}: Readonly<{
  title: string;
  description: string;
  eyebrow?: string;
  isLoading?: boolean;
  error?: unknown;
  successMessage?: string | null;
  children: React.ReactNode;
}>) {
  const t = useTranslations();

  useEffect(() => {
    const toastId = `admin-loading-${title}`;

    if (isLoading) {
      toast.loading(t("admin.states.loading"), { id: toastId });
      return;
    }

    toast.dismiss(toastId);
  }, [isLoading, t, title]);

  useEffect(() => {
    if (!error) return;
    toast.error(formatErrorMessage(error, t));
  }, [error, t]);

  useEffect(() => {
    if (!successMessage) return;
    toast.success(successMessage);
  }, [successMessage]);

  return (
    <div className="fade-up space-y-6">
      <div className="max-w-3xl">
        <p className="font-mono text-xs font-bold uppercase tracking-[0.18em] text-muted-foreground">{eyebrow ?? "Admin"}</p>
        <h1 className="mt-3 text-4xl font-black tracking-[-0.05em] sm:text-5xl">{title}</h1>
        <p className="mt-4 text-sm leading-7 text-muted-foreground">{description}</p>
      </div>
      {children}
    </div>
  );
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  return formatApiErrorMessage(error, t);
}
