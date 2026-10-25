"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { getCompany, updateCompany, type CompanyPayload } from "@/features/admin/shared/admin-api";

const companySchema = z.object({
  legalName: z.string().min(1),
  commercialName: z.string().min(1),
  logoUrl: z.string().nullable(),
  supportedLocales: z.array(z.string()).min(1),
  defaultLocale: z.string().min(1),
  currencyCode: z.string().length(3),
  timezone: z.string().min(1),
  taxEnabled: z.boolean(),
  taxRate: z.number().min(0).max(100),
});

type CompanyForm = z.infer<typeof companySchema>;

const currencyOptions = ["GTQ", "USD", "MXN", "EUR", "CRC", "HNL", "NIO", "PAB"];
const localeOptions = ["es-GT", "en-US"];
const timezoneOptions = ["America/Guatemala", "America/Mexico_City", "America/Tegucigalpa", "America/Managua", "America/Costa_Rica", "America/Panama", "America/New_York"];

export function CompanyProfileScreen({ mode }: Readonly<{ mode: "profile" | "settings" }>) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const companyQuery = useQuery({ queryKey: ["admin", "company"], queryFn: getCompany });
  const form = useForm<CompanyForm>({ resolver: zodResolver(companySchema) });

  useEffect(() => {
    if (!companyQuery.data) return;
    form.reset({
      legalName: companyQuery.data.legalName,
      commercialName: companyQuery.data.commercialName,
      logoUrl: companyQuery.data.logoUrl ?? "",
      supportedLocales: companyQuery.data.supportedLocales,
      defaultLocale: companyQuery.data.defaultLocale,
      currencyCode: companyQuery.data.currencyCode,
      timezone: companyQuery.data.timezone,
      taxEnabled: companyQuery.data.taxEnabled,
      taxRate: Number(companyQuery.data.taxRate ?? 0),
    });
  }, [companyQuery.data, form]);

  const mutation = useMutation({
    mutationFn: (values: CompanyForm) => {
      const payload: CompanyPayload = {
        ...values,
        logoUrl: values.logoUrl || null,
        primaryColor: companyQuery.data?.primaryColor ?? "#2563eb",
        secondaryColor: companyQuery.data?.secondaryColor ?? "#64748b",
        defaultTheme: companyQuery.data?.defaultTheme ?? "light",
      };
      return updateCompany(payload);
    },
    onSuccess: () => {
      setSuccessMessage(t("admin.states.saved"));
      queryClient.invalidateQueries({ queryKey: ["admin", "company"] });
    },
  });

  return (
    <AdminPage title={mode === "profile" ? t("admin.company.profileTitle") : t("admin.company.settingsTitle")} description={t("admin.company.description")} isLoading={companyQuery.isLoading} error={companyQuery.error ?? mutation.error} successMessage={successMessage}>
      <Card>
        <CardContent className="grid gap-4 p-6 md:grid-cols-2">
          <ReadOnly label={t("admin.fields.tenantSlug")} value={companyQuery.data?.tenantSlug} />
          <ReadOnly label={t("admin.fields.status")} value={companyQuery.data?.status} />
        </CardContent>
      </Card>
      <Card>
        <CardContent className="p-6">
          <form className="grid gap-4 md:grid-cols-2" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
            <Field label={t("admin.fields.legalName")} registration={form.register("legalName")} />
            <Field label={t("admin.fields.commercialName")} registration={form.register("commercialName")} />
            <Field label={t("admin.fields.logoUrl")} registration={form.register("logoUrl")} />
            <Select label={t("admin.fields.currencyCode")} registration={form.register("currencyCode")} options={currencyOptions} />
            <Select label={t("admin.fields.defaultLocale")} registration={form.register("defaultLocale")} options={localeOptions} />
            <Select label={t("admin.fields.timezone")} registration={form.register("timezone")} options={timezoneOptions} />
            <LocaleChecklist
              label={t("admin.fields.supportedLocales")}
              // eslint-disable-next-line react-hooks/incompatible-library
              selected={form.watch("supportedLocales") ?? []}
              onChange={(locales) => form.setValue("supportedLocales", locales, { shouldDirty: true, shouldValidate: true })}
            />
            <Field label={t("admin.fields.taxRate")} registration={form.register("taxRate", { valueAsNumber: true })} type="number" />
            <label className="flex items-center gap-3 pt-6 text-sm"><input type="checkbox" {...form.register("taxEnabled")} />{t("admin.fields.taxEnabled")}</label>
            <div className="md:col-span-2"><Button disabled={mutation.isPending} type="submit">{t("admin.actions.save")}</Button></div>
          </form>
        </CardContent>
      </Card>
    </AdminPage>
  );
}

function ReadOnly({ label, value }: Readonly<{ label: string; value?: string }>) {
  return <div className="rounded-2xl border bg-background/60 p-4"><p className="text-xs font-bold uppercase tracking-[0.16em] text-muted-foreground">{label}</p><p className="mt-2 font-mono text-sm">{value ?? "-"}</p></div>;
}

function Field({ label, registration, type = "text" }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type={type} {...registration} /></div>;
}

function Select({ label, registration, options }: Readonly<{ label: string; registration: UseFormRegisterReturn; options: string[] }>) {
  return <div className="space-y-2"><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" {...registration}>{options.map((option) => <option key={option} value={option}>{option}</option>)}</select></div>;
}

function LocaleChecklist({ label, selected, onChange }: Readonly<{ label: string; selected: string[]; onChange: (locales: string[]) => void }>) {
  return <div className="space-y-2"><Label>{label}</Label><div className="flex flex-wrap gap-2 rounded-xl border bg-background/75 p-2">{localeOptions.map((locale) => {
    const checked = selected.includes(locale);
    return <button key={locale} className={`rounded-full border px-3 py-1 text-xs ${checked ? "bg-primary text-primary-foreground" : "bg-background"}`} type="button" onClick={() => onChange(checked ? selected.filter((item) => item !== locale) : [...selected, locale])}>{locale}</button>;
  })}</div></div>;
}
