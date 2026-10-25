"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Building2, ShieldCheck, UserRound, type LucideIcon } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getOwnProfile, updateOwnProfile } from "@/features/account/account-api";
import { ChangePasswordForm } from "@/features/account/change-password-form";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { formatApiErrorMessage } from "@/lib/api/errors";

const profileSchema = z.object({
  firstName: z.string().min(1, "account.validation.firstNameRequired"),
  lastName: z.string().min(1, "account.validation.lastNameRequired"),
  email: z.string().email("account.validation.invalidEmail"),
  phone: z.string(),
  birthDate: z.string(),
});

type ProfileFormData = z.infer<typeof profileSchema>;

export function AccountScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const profileQuery = useQuery({ queryKey: ["account", "profile"], queryFn: getOwnProfile });
  const form = useForm<ProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: { firstName: "", lastName: "", email: "", phone: "", birthDate: "" },
  });
  const { reset } = form;

  useEffect(() => {
    if (!profileQuery.data) return;
    reset({
      firstName: profileQuery.data.firstName,
      lastName: profileQuery.data.lastName,
      email: profileQuery.data.email,
      phone: profileQuery.data.phone ?? "",
      birthDate: profileQuery.data.birthDate ?? "",
    });
  }, [profileQuery.data, reset]);

  const updateMutation = useMutation({
    mutationFn: (values: ProfileFormData) =>
      updateOwnProfile({
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        phone: values.phone.trim() || null,
        birthDate: values.birthDate || null,
      }),
    onSuccess: (profile) => {
      queryClient.setQueryData(["account", "profile"], profile);
      toast.success(t("account.profileSaved"));
    },
    onError: (error) => toast.error(formatApiErrorMessage(error, t)),
  });

  const profile = profileQuery.data;

  return (
    <AdminPage
      title={t("account.title")}
      description={t("account.description")}
      eyebrow={t("account.eyebrow")}
      isLoading={profileQuery.isLoading}
      error={profileQuery.error}
    >
      {profile ? (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(340px,0.65fr)]">
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <div className="mb-2 flex h-11 w-11 items-center justify-center rounded-xl bg-secondary text-secondary-foreground">
                  <UserRound className="h-5 w-5" aria-hidden="true" />
                </div>
                <CardTitle>{t("account.personalTitle")}</CardTitle>
                <CardDescription>{t("account.personalDescription")}</CardDescription>
              </CardHeader>
              <CardContent>
                <form className="grid gap-4 sm:grid-cols-2" onSubmit={form.handleSubmit((values) => updateMutation.mutate(values))}>
                  <Field id="firstName" label={t("admin.fields.firstName")} error={form.formState.errors.firstName?.message} t={t}>
                    <Input id="firstName" autoComplete="given-name" {...form.register("firstName")} />
                  </Field>
                  <Field id="lastName" label={t("admin.fields.lastName")} error={form.formState.errors.lastName?.message} t={t}>
                    <Input id="lastName" autoComplete="family-name" {...form.register("lastName")} />
                  </Field>
                  <Field id="email" label={t("admin.fields.email")} error={form.formState.errors.email?.message} t={t}>
                    <Input id="email" type="email" autoComplete="email" {...form.register("email")} />
                  </Field>
                  <Field id="phone" label={t("admin.fields.phone")} t={t}>
                    <Input id="phone" type="tel" autoComplete="tel" {...form.register("phone")} />
                  </Field>
                  <Field id="birthDate" label={t("admin.fields.birthDate")} t={t}>
                    <Input id="birthDate" type="date" {...form.register("birthDate")} />
                  </Field>
                  <div className="flex items-end sm:justify-end">
                    <Button className="w-full sm:w-auto" type="submit" disabled={updateMutation.isPending}>
                      {updateMutation.isPending ? t("account.saving") : t("admin.actions.save")}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>

            <ChangePasswordForm />
          </div>

          <div className="space-y-6">
            <ReadOnlyCard
              icon={ShieldCheck}
              title={t("account.accessTitle")}
              description={t("account.accessDescription")}
              rows={[
                [t("admin.fields.username"), profile.username],
                [t("admin.fields.role"), profile.role.name],
              ]}
            />
            <ReadOnlyCard
              icon={Building2}
              title={t("account.assignmentTitle")}
              description={t("account.assignmentDescription")}
              rows={[
                [t("admin.fields.branch"), profile.branch.name],
                [t("admin.fields.code"), profile.branch.code],
              ]}
            />
          </div>
        </div>
      ) : null}
    </AdminPage>
  );
}

function Field({ id, label, error, t, children }: Readonly<{ id: string; label: string; error?: string; t: (key: string) => string; children: React.ReactNode }>) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      {children}
      {error ? <p className="text-xs text-destructive">{t(error)}</p> : null}
    </div>
  );
}

function ReadOnlyCard({ icon: Icon, title, description, rows }: Readonly<{ icon: LucideIcon; title: string; description: string; rows: Array<[string, string]> }>) {
  return (
    <Card>
      <CardHeader>
        <div className="mb-2 flex h-11 w-11 items-center justify-center rounded-xl border bg-muted/50">
          <Icon className="h-5 w-5" aria-hidden="true" />
        </div>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {rows.map(([label, value]) => (
          <div key={label} className="rounded-lg border bg-muted/25 px-4 py-3">
            <p className="text-xs font-bold uppercase tracking-wide text-muted-foreground">{label}</p>
            <p className="mt-1 text-sm font-semibold">{value}</p>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
