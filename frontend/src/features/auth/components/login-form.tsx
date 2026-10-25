"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { useRouter, useSearchParams } from "next/navigation";
import { Eye, EyeOff } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { login } from "@/features/auth/lib/auth-client";
import { getTenantConfig } from "@/features/tenant/lib/get-tenant-config";
import { resolveTenantSlug } from "@/features/tenant/lib/resolve-tenant";
import { ApiError } from "@/lib/api/errors";

const loginSchema = z.object({
  username: z.string().min(1),
  password: z.string().min(1),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginForm() {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  const tenantSlug = typeof window === "undefined" ? "demo" : resolveTenantSlug(window.location.hostname);
  const [showPassword, setShowPassword] = useState(false);

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  const tenantQuery = useQuery({
    queryKey: ["tenant-config", tenantSlug],
    queryFn: () => getTenantConfig(tenantSlug),
    retry: false,
  });

  const loginMutation = useMutation({
    mutationFn: (values: LoginFormData) => login({ tenantSlug, ...values }),
    onSuccess: (session) => {
      queryClient.clear();
      router.replace(session.mustChangePassword ? "/change-password" : "/dashboard");
    },
    onError: (error) => {
      if (error instanceof ApiError && error.errorCode === "INVALID_CREDENTIALS") {
        toast.error(t("auth.invalidCredentials"));
        return;
      }

      toast.error(error instanceof Error ? error.message : t("auth.invalidCredentials"));
    },
  });

  useEffect(() => {
    if (tenantQuery.error instanceof ApiError && tenantQuery.error.errorCode === "TENANT_NOT_FOUND") {
      router.replace("/tenant-not-found");
    }
  }, [router, tenantQuery.error]);

  useEffect(() => {
    if (searchParams.get("expired") === "1") {
      toast.warning(t("common.sessionExpired"));
    }
  }, [searchParams, t]);

  useEffect(() => {
    if (tenantQuery.isError) {
      toast.error(t("auth.tenantUnavailable"));
    }
  }, [tenantQuery.isError, t]);

  return (
    <div className="grid w-full items-stretch gap-5 lg:grid-cols-[1.1fr_0.9fr]">
      <section className="fade-up hidden min-h-[640px] overflow-hidden rounded-[2rem] bg-panel text-panel-foreground steel-panel lg:block">
        <div className="relative flex h-full flex-col justify-between p-8">
          <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-primary/30 blur-3xl" />
          <div className="absolute bottom-20 left-8 h-24 w-24 rounded-full border border-panel-foreground/15" />
          <div className="relative">
            <div className="mb-10 inline-flex items-center rounded-full border border-panel-foreground/15 bg-panel-foreground/8 px-3 py-1 text-xs font-bold uppercase tracking-[0.22em] text-panel-foreground/70">
              {t("auth.heroEyebrow")}
            </div>
            <h1 className="max-w-xl text-6xl font-black leading-[0.9] tracking-[-0.07em]">
              {t("auth.heroTitle")}
            </h1>
            <p className="mt-6 max-w-md text-sm leading-7 text-panel-foreground/68">
              {t("auth.heroDescription")}
            </p>
          </div>
          <div className="relative grid grid-cols-3 gap-3">
            {["FIFO", "KARDEX", "GTQ"].map((item) => (
              <div key={item} className="rounded-2xl border border-panel-foreground/12 bg-panel-foreground/[0.06] p-4">
                <p className="font-mono text-xs text-panel-foreground/50">{t("auth.heroReady")}</p>
                <p className="mt-2 text-lg font-black tracking-[-0.04em]">{item}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <Card className="fade-up w-full self-center border-border bg-card/88 lg:max-w-md lg:justify-self-end">
        <CardHeader>
          <div className="mb-4 flex items-center gap-3">
            <div className="scanline flex h-14 w-14 items-center justify-center overflow-hidden rounded-2xl bg-primary shadow-lg shadow-primary/25">
              <Image src="/brand/vivent-icon.png" alt="Vivent" width={56} height={56} priority className="h-14 w-14 object-cover" />
            </div>
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground">{t("auth.tenantLabel")}</p>
              <p className="font-mono text-sm text-foreground/80">{tenantSlug}.corpoviva.com</p>
            </div>
          </div>
          <CardTitle>{tenantQuery.data?.commercialName ?? t("app.name")}</CardTitle>
          <CardDescription>{t("auth.loginDescription")}</CardDescription>
        </CardHeader>
        <CardContent>
        {tenantQuery.isLoading ? <p className="mb-4 text-sm text-muted-foreground">{t("auth.loadingTenant")}</p> : null}

        <form className="space-y-4" onSubmit={form.handleSubmit((values) => loginMutation.mutate(values))}>
          <div className="space-y-2">
            <Label htmlFor="username">{t("auth.username")}</Label>
            <Input id="username" autoComplete="username" disabled={loginMutation.isPending} {...form.register("username")} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">{t("auth.password")}</Label>
            <div className="relative">
              <Input id="password" type={showPassword ? "text" : "password"} autoComplete="current-password" disabled={loginMutation.isPending} className="pr-10" {...form.register("password")} />
              <button type="button" className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground" onClick={() => setShowPassword((showPassword) => !showPassword)} tabIndex={-1} aria-label={showPassword ? t("auth.hidePassword") : t("auth.showPassword")}>
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>
          <Button className="mt-2 w-full" type="submit" disabled={tenantQuery.isLoading || loginMutation.isPending}>
            {loginMutation.isPending ? "Validando..." : t("auth.submit")}
          </Button>
        </form>
        </CardContent>
      </Card>
    </div>
  );
}
