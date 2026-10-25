"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import {
  getPlatformCompany,
  listPlatformPlans,
  resetCompanyAdminPassword,
  updateCompanyStatus,
  updateCompanySubscription,
} from "@/features/platform/lib/platform-api";
import { buildTenantPortalUrl, copyToClipboard, formatTenantCredentials } from "@/features/platform/lib/tenant-portal";
import type { ResetAdminPasswordResult } from "@/lib/api/platform-contracts";

const COMPANY_STATUSES = ["ACTIVE", "INACTIVE", "SUSPENDED"] as const;
const SUBSCRIPTION_STATUSES = ["TRIAL", "ACTIVE", "EXPIRED", "SUSPENDED", "CANCELLED"] as const;

function statusLabel(status: string) {
  const map: Record<string, string> = {
    ACTIVE: "Activa",
    INACTIVE: "Inactiva",
    SUSPENDED: "Suspendida",
    TRIAL: "Trial",
    EXPIRED: "Expirada",
    CANCELLED: "Cancelada",
  };
  return map[status] ?? status;
}

function formatDate(value: string | null | undefined) {
  if (!value) return "—";
  return new Date(value).toLocaleDateString("es-GT", { day: "2-digit", month: "short", year: "numeric" });
}

export function PlatformCompanyDetail() {
  const params = useParams<{ id: string }>();
  const companyId = params.id;
  const queryClient = useQueryClient();

  const [status, setStatus] = useState("");
  const [planId, setPlanId] = useState("");
  const [subscriptionStatus, setSubscriptionStatus] = useState("");
  const [resetResult, setResetResult] = useState<ResetAdminPasswordResult | null>(null);

  const companyQuery = useQuery({
    queryKey: ["platform-company", companyId],
    queryFn: () => getPlatformCompany(companyId),
  });

  const plansQuery = useQuery({
    queryKey: ["platform-plans"],
    queryFn: listPlatformPlans,
    staleTime: 5 * 60_000,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["platform-company", companyId] });

  const statusMutation = useMutation({
    mutationFn: (nextStatus: string) => updateCompanyStatus(companyId, nextStatus),
    onSuccess: () => {
      invalidate();
      toast.success("Estado actualizado");
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : "No se pudo actualizar el estado"),
  });

  const subscriptionMutation = useMutation({
    mutationFn: (input: { planId: string; status: string; endsAt: string | null; trialEndsAt: string | null }) =>
      updateCompanySubscription(companyId, {
        planId: input.planId,
        status: input.status,
        endsAt: input.endsAt,
        trialEndsAt: input.trialEndsAt,
      }),
    onSuccess: () => {
      invalidate();
      toast.success("Suscripción actualizada");
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : "No se pudo actualizar la suscripción"),
  });

  const resetMutation = useMutation({
    mutationFn: () => resetCompanyAdminPassword(companyId),
    onSuccess: (result) => setResetResult(result),
    onError: (error) => toast.error(error instanceof Error ? error.message : "No se pudo restablecer la contraseña"),
  });

  if (companyQuery.isLoading) {
    return <p className="text-sm text-muted-foreground">Cargando empresa…</p>;
  }

  if (companyQuery.isError || !companyQuery.data) {
    return <p className="text-sm text-muted-foreground">No se pudo cargar la empresa.</p>;
  }

  const detail = companyQuery.data;

  async function copyValue(value: string, message: string) {
    try {
      await copyToClipboard(value);
      toast.success(message);
    } catch {
      toast.error("No se pudo copiar al portapapeles");
    }
  }

  function openTenantPortal() {
    window.open(buildTenantPortalUrl(detail.company.domain), "_blank", "noopener,noreferrer");
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <Link href="/master/companies" className="text-xs text-muted-foreground hover:text-foreground">
            ← Volver a empresas
          </Link>
          <h1 className="text-3xl font-black tracking-tight">{detail.company.commercialName}</h1>
          <p className="text-sm text-muted-foreground">
            {detail.company.legalName} · <span className="font-mono">{detail.company.domain}</span>
          </p>
        </div>
        <span className="rounded-full border border-border px-3 py-1 text-xs font-bold uppercase tracking-wide">
          {statusLabel(detail.company.status)}
        </span>
      </div>

      <div className="grid grid-cols-3 gap-4">
        {[
          { label: "Sucursales", value: detail.usage.branches },
          { label: "Usuarios activos", value: detail.usage.users },
          { label: "Productos", value: detail.usage.products },
        ].map((item) => (
          <Card key={item.label}>
            <CardContent className="p-5">
              <p className="text-xs uppercase tracking-wide text-muted-foreground">{item.label}</p>
              <p className="mt-1 text-3xl font-black">{item.value}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Plan y suscripción</CardTitle>
            <CardDescription>
              {detail.subscription
                ? `${detail.subscription.plan.name} (${detail.subscription.plan.code}) · desde ${formatDate(detail.subscription.startsAt)}`
                : "Sin suscripción"}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="planId">Plan</Label>
              <select
                id="planId"
                className="flex h-11 w-full rounded-xl border border-input bg-background/75 px-3.5 py-2 text-sm"
                value={planId || detail.subscription?.plan.id || ""}
                onChange={(event) => setPlanId(event.target.value)}
              >
                {plansQuery.data?.map((plan) => (
                  <option key={plan.id} value={plan.id}>
                    {plan.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="subscriptionStatus">Estado de suscripción</Label>
              <select
                id="subscriptionStatus"
                className="flex h-11 w-full rounded-xl border border-input bg-background/75 px-3.5 py-2 text-sm"
                value={subscriptionStatus || detail.subscription?.status || ""}
                onChange={(event) => setSubscriptionStatus(event.target.value)}
              >
                {SUBSCRIPTION_STATUSES.map((value) => (
                  <option key={value} value={value}>
                    {statusLabel(value)}
                  </option>
                ))}
              </select>
            </div>
            <Button
              className="w-full"
              disabled={subscriptionMutation.isPending || !(planId || detail.subscription?.plan.id) || !(subscriptionStatus || detail.subscription?.status)}
              onClick={() => subscriptionMutation.mutate({
                planId: planId || detail.subscription?.plan.id || "",
                status: subscriptionStatus || detail.subscription?.status || "",
                endsAt: detail.subscription?.endsAt ?? null,
                trialEndsAt: detail.subscription?.trialEndsAt ?? null,
              })}
            >
              {subscriptionMutation.isPending ? "Guardando…" : "Guardar suscripción"}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Estado y acceso</CardTitle>
            <CardDescription>Activa o suspende el acceso a la empresa.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="companyStatus">Estado de la empresa</Label>
              <select
                id="companyStatus"
                className="flex h-11 w-full rounded-xl border border-input bg-background/75 px-3.5 py-2 text-sm"
                value={status || detail.company.status}
                onChange={(event) => setStatus(event.target.value)}
              >
                {COMPANY_STATUSES.map((value) => (
                  <option key={value} value={value}>
                    {statusLabel(value)}
                  </option>
                ))}
              </select>
            </div>
            <Button
              className="w-full"
              variant="outline"
              disabled={statusMutation.isPending || !status}
              onClick={() => statusMutation.mutate(status)}
            >
              {statusMutation.isPending ? "Guardando…" : "Guardar estado"}
            </Button>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Administrador inicial</CardTitle>
          <CardDescription>
            {detail.admin
              ? `${detail.admin.firstName} ${detail.admin.lastName} · ${detail.admin.email} (${detail.admin.username})`
              : "Sin administrador registrado"}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {resetResult ? (
            <div className="rounded-lg bg-muted/40 p-4">
              <p className="text-xs uppercase tracking-wide text-muted-foreground">Nueva contraseña para {resetResult.username}</p>
              <p className="mt-1 font-mono text-sm break-all">{resetResult.password}</p>
              <p className="mt-2 text-xs text-muted-foreground">Copia esta contraseña: solo se muestra una vez.</p>
            </div>
          ) : null}
          {resetResult ? (
            <div className="grid gap-3 sm:grid-cols-2">
              <Button type="button" variant="outline" onClick={() => copyValue(resetResult.password, "Contraseña copiada")}>Copiar contraseña</Button>
              <Button type="button" variant="outline" onClick={() => copyValue(formatTenantCredentials(detail.company.domain, resetResult.username, resetResult.password), "Credenciales copiadas")}>Copiar credenciales</Button>
            </div>
          ) : null}
          <Button className="w-full" type="button" variant="secondary" onClick={openTenantPortal}>Abrir portal del cliente</Button>
          <Button variant="outline" disabled={resetMutation.isPending} onClick={() => resetMutation.mutate()}>
            {resetMutation.isPending ? "Restableciendo…" : "Restablecer contraseña"}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
