"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { KeyRound } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { changeOwnPassword } from "@/features/account/account-api";
import { passwordSchema, type PasswordFormData } from "@/features/account/password-schema";
import { storeSession } from "@/features/auth/lib/session-storage";
import { formatApiErrorMessage } from "@/lib/api/errors";

export function ChangePasswordForm({ forced = false }: Readonly<{ forced?: boolean }>) {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const form = useForm<PasswordFormData>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  const mutation = useMutation({
    mutationFn: (values: PasswordFormData) =>
      changeOwnPassword({ currentPassword: values.currentPassword, newPassword: values.newPassword }),
    onSuccess: (session) => {
      storeSession(session);
      queryClient.clear();
      form.reset();
      toast.success(t("account.passwordChanged"));
      router.replace(forced ? "/dashboard" : "/account");
    },
    onError: (error) => toast.error(formatApiErrorMessage(error, t)),
  });

  return (
    <Card className={forced ? "w-full max-w-lg border-primary/30 shadow-2xl shadow-primary/10" : undefined}>
      <CardHeader>
        <div className="mb-2 flex h-11 w-11 items-center justify-center rounded-xl bg-primary text-primary-foreground">
          <KeyRound className="h-5 w-5" aria-hidden="true" />
        </div>
        <CardTitle>{forced ? t("account.requiredTitle") : t("account.securityTitle")}</CardTitle>
        <CardDescription>{forced ? t("account.requiredDescription") : t("account.securityDescription")}</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
          <PasswordField
            id="currentPassword"
            label={t("account.currentPassword")}
            errorKey={form.formState.errors.currentPassword?.message}
            registration={form.register("currentPassword")}
            t={t}
          />
          <PasswordField
            id="newPassword"
            label={t("account.newPassword")}
            errorKey={form.formState.errors.newPassword?.message}
            registration={form.register("newPassword")}
            t={t}
          />
          <PasswordField
            id="confirmPassword"
            label={t("account.confirmPassword")}
            errorKey={form.formState.errors.confirmPassword?.message}
            registration={form.register("confirmPassword")}
            t={t}
          />
          <Button className="w-full" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? t("account.changingPassword") : t("account.changePassword")}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

function PasswordField({
  id,
  label,
  errorKey,
  registration,
  t,
}: Readonly<{
  id: string;
  label: string;
  errorKey?: string;
  registration: UseFormRegisterReturn;
  t: (key: string) => string;
}>) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} type="password" autoComplete={id === "currentPassword" ? "current-password" : "new-password"} {...registration} />
      {errorKey ? <p className="text-xs text-destructive">{t(errorKey)}</p> : null}
    </div>
  );
}
