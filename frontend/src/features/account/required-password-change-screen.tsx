"use client";

import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { ChangePasswordForm } from "@/features/account/change-password-form";
import { getCurrentSession, logout } from "@/features/auth/lib/auth-client";
import { useRequireSession } from "@/features/auth/lib/guards";
import { ApiError } from "@/lib/api/errors";

export function RequiredPasswordChangeScreen() {
  const router = useRouter();
  const { session, isChecking } = useRequireSession();
  const sessionQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: getCurrentSession,
    enabled: Boolean(session),
    retry: false,
  });

  useEffect(() => {
    if (sessionQuery.data && !sessionQuery.data.mustChangePassword) {
      router.replace("/dashboard");
    }
  }, [router, sessionQuery.data]);

  useEffect(() => {
    if (sessionQuery.error instanceof ApiError && sessionQuery.error.status === 401) {
      logout();
      router.replace("/login");
    }
  }, [router, sessionQuery.error]);

  if (isChecking || sessionQuery.isLoading || !sessionQuery.data?.mustChangePassword) {
    return <div className="rounded-full border bg-card px-4 py-2 text-sm text-muted-foreground">Validando sesión...</div>;
  }

  return <ChangePasswordForm forced />;
}
