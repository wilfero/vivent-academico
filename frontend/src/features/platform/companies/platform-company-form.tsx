"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { createPlatformCompany, listPlatformPlans } from "@/features/platform/lib/platform-api";
import { buildTenantPortalUrl, copyToClipboard, formatTenantCredentials } from "@/features/platform/lib/tenant-portal";
import type { ProvisionedCompany } from "@/lib/api/platform-contracts";

const formSchema = z.object({
  legalName: z.string().min(1, "El nombre legal es obligatorio"),
  commercialName: z.string().min(1, "El nombre comercial es obligatorio"),
  tenantSlug: z.string().regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, "Solo minúsculas, números y guiones"),
  planId: z.string().min(1, "Selecciona un plan"),
  branchName: z.string().min(1, "El nombre de la sucursal es obligatorio"),
  branchCode: z.string().regex(/^[A-Z0-9_-]+$/, "Solo mayúsculas, números, guiones y guiones bajos"),
  adminUsername: z.string().min(1, "El usuario administrador es obligatorio"),
  adminFirstName: z.string().min(1, "El nombre es obligatorio"),
  adminLastName: z.string().min(1, "El apellido es obligatorio"),
  adminEmail: z.string().email("Correo inválido"),
  currencyCode: z.string().min(3),
  timezone: z.string().min(1),
});

type FormData = z.infer<typeof formSchema>;

export function PlatformCompanyForm() {
  const router = useRouter();
  const [provisioned, setProvisioned] = useState<ProvisionedCompany | null>(null);

  const plansQuery = useQuery({
    queryKey: ["platform-plans"],
    queryFn: listPlatformPlans,
    staleTime: 5 * 60_000,
  });

  const form = useForm<FormData>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      legalName: "",
      commercialName: "",
      tenantSlug: "",
      planId: "",
      branchName: "",
      branchCode: "",
      adminUsername: "",
      adminFirstName: "",
      adminLastName: "",
      adminEmail: "",
      currencyCode: "GTQ",
      timezone: "America/Guatemala",
    },
  });

  const createMutation = useMutation({
    mutationFn: (values: FormData) =>
      createPlatformCompany({
        legalName: values.legalName,
        commercialName: values.commercialName,
        tenantSlug: values.tenantSlug,
        planId: values.planId,
        branchName: values.branchName,
        branchCode: values.branchCode,
        adminUsername: values.adminUsername,
        adminFirstName: values.adminFirstName,
        adminLastName: values.adminLastName,
        adminEmail: values.adminEmail,
        currencyCode: values.currencyCode,
        timezone: values.timezone,
        taxEnabled: false,
        taxRate: 0,
      }),
    onSuccess: (result) => setProvisioned(result),
    onError: (error) => toast.error(error instanceof Error ? error.message : "No se pudo crear la empresa"),
  });

  if (provisioned) {
    return <ProvisionedResult provisioned={provisioned} onDone={() => router.push(`/master/companies/${provisioned.companyId}`)} />;
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-3xl font-black tracking-tight">Nueva empresa</h1>
        <p className="text-sm text-muted-foreground">
          Crea la compañía, suscripción, sucursal, ubicación y usuario administrador inicial.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Datos de la empresa</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="legalName">Nombre legal</Label>
                <Input id="legalName" {...form.register("legalName")} />
                {form.formState.errors.legalName ? <p className="text-xs text-destructive">{form.formState.errors.legalName.message}</p> : null}
              </div>
              <div className="space-y-2">
                <Label htmlFor="commercialName">Nombre comercial</Label>
                <Input id="commercialName" {...form.register("commercialName")} />
                {form.formState.errors.commercialName ? <p className="text-xs text-destructive">{form.formState.errors.commercialName.message}</p> : null}
              </div>
              <div className="space-y-2">
                <Label htmlFor="tenantSlug">Subdominio</Label>
                <Input id="tenantSlug" placeholder="mi-empresa" {...form.register("tenantSlug")} />
                {form.formState.errors.tenantSlug ? <p className="text-xs text-destructive">{form.formState.errors.tenantSlug.message}</p> : null}
              </div>
              <div className="space-y-2">
                <Label htmlFor="planId">Plan</Label>
                <select
                  id="planId"
                  className="flex h-11 w-full rounded-xl border border-input bg-background/75 px-3.5 py-2 text-sm"
                  {...form.register("planId")}
                >
                  <option value="">Selecciona un plan</option>
                  {plansQuery.data?.map((plan) => (
                    <option key={plan.id} value={plan.id}>
                      {plan.name} (máx. {plan.maxUsers} usuarios, {plan.maxBranches} sucursales)
                    </option>
                  ))}
                </select>
                {form.formState.errors.planId ? <p className="text-xs text-destructive">{form.formState.errors.planId.message}</p> : null}
              </div>
              <div className="space-y-2">
                <Label htmlFor="currencyCode">Moneda</Label>
                <select
                  id="currencyCode"
                  className="flex h-11 w-full rounded-xl border border-input bg-background/75 px-3.5 py-2 text-sm"
                  {...form.register("currencyCode")}
                >
                  <option value="GTQ">GTQ — Quetzal</option>
                  <option value="USD">USD — Dólar</option>
                </select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="timezone">Zona horaria</Label>
                <Input id="timezone" {...form.register("timezone")} />
              </div>
            </div>

            <div className="border-t border-border pt-4">
              <h2 className="mb-4 text-sm font-bold uppercase tracking-wide text-muted-foreground">Sucursal inicial</h2>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="branchName">Nombre de sucursal</Label>
                  <Input id="branchName" {...form.register("branchName")} />
                  {form.formState.errors.branchName ? <p className="text-xs text-destructive">{form.formState.errors.branchName.message}</p> : null}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="branchCode">Código</Label>
                  <Input id="branchCode" placeholder="CENTRAL" {...form.register("branchCode")} />
                  {form.formState.errors.branchCode ? <p className="text-xs text-destructive">{form.formState.errors.branchCode.message}</p> : null}
                </div>
              </div>
            </div>

            <div className="border-t border-border pt-4">
              <h2 className="mb-4 text-sm font-bold uppercase tracking-wide text-muted-foreground">Administrador inicial</h2>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="adminUsername">Usuario</Label>
                  <Input id="adminUsername" placeholder="admin" {...form.register("adminUsername")} />
                  {form.formState.errors.adminUsername ? <p className="text-xs text-destructive">{form.formState.errors.adminUsername.message}</p> : null}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="adminEmail">Correo</Label>
                  <Input id="adminEmail" type="email" {...form.register("adminEmail")} />
                  {form.formState.errors.adminEmail ? <p className="text-xs text-destructive">{form.formState.errors.adminEmail.message}</p> : null}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="adminFirstName">Nombre</Label>
                  <Input id="adminFirstName" {...form.register("adminFirstName")} />
                  {form.formState.errors.adminFirstName ? <p className="text-xs text-destructive">{form.formState.errors.adminFirstName.message}</p> : null}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="adminLastName">Apellido</Label>
                  <Input id="adminLastName" {...form.register("adminLastName")} />
                  {form.formState.errors.adminLastName ? <p className="text-xs text-destructive">{form.formState.errors.adminLastName.message}</p> : null}
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <Link href="/master/companies">
                <Button type="button" variant="outline">
                  Cancelar
                </Button>
              </Link>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Creando…" : "Crear empresa"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

function ProvisionedResult({ provisioned, onDone }: { provisioned: ProvisionedCompany; onDone: () => void }) {
  async function copyValue(value: string, message: string) {
    try {
      await copyToClipboard(value);
      toast.success(message);
    } catch {
      toast.error("No se pudo copiar al portapapeles");
    }
  }

  function openTenantPortal() {
    window.open(buildTenantPortalUrl(provisioned.domain), "_blank", "noopener,noreferrer");
  }

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <div>
        <h1 className="text-3xl font-black tracking-tight">Empresa creada</h1>
        <p className="text-sm text-muted-foreground">
          La empresa <span className="font-semibold">{provisioned.domain}</span> quedó lista. Guarda las credenciales del
          administrador: solo se muestran una vez.
        </p>
      </div>
      <Card className="border-warning/40">
        <CardHeader>
          <CardTitle>Credenciales iniciales</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="rounded-lg bg-muted/40 p-4">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Subdominio</p>
            <p className="font-mono text-sm">{provisioned.domain}</p>
          </div>
          <div className="rounded-lg bg-muted/40 p-4">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Usuario</p>
            <p className="font-mono text-sm">{provisioned.adminUsername}</p>
          </div>
          <div className="rounded-lg bg-muted/40 p-4">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Contraseña</p>
            <p className="font-mono text-sm break-all">{provisioned.adminPassword}</p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <Button type="button" variant="outline" onClick={() => copyValue(provisioned.adminPassword, "Contraseña copiada")}>Copiar contraseña</Button>
            <Button type="button" variant="outline" onClick={() => copyValue(formatTenantCredentials(provisioned.domain, provisioned.adminUsername, provisioned.adminPassword), "Credenciales copiadas")}>Copiar credenciales</Button>
            <Button type="button" variant="secondary" onClick={openTenantPortal}>Abrir portal del cliente</Button>
            <Button type="button" onClick={onDone}>Ver detalle</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
