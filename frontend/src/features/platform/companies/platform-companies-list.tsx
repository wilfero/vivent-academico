"use client";

import { useQuery, keepPreviousData } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { listPlatformCompanies } from "@/features/platform/lib/platform-api";
import type { PlatformCompanySummary } from "@/lib/api/platform-contracts";

const STATUS_OPTIONS = [
  { value: "", label: "Todos" },
  { value: "ACTIVE", label: "Activas" },
  { value: "INACTIVE", label: "Inactivas" },
  { value: "SUSPENDED", label: "Suspendidas" },
];

function statusLabel(status: string) {
  const map: Record<string, string> = {
    ACTIVE: "Activa",
    INACTIVE: "Inactiva",
    SUSPENDED: "Suspendida",
    DELETED: "Eliminada",
  };
  return map[status] ?? status;
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString("es-GT", { day: "2-digit", month: "short", year: "numeric" });
}

export function PlatformCompaniesList() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [page, setPage] = useState(() => Number(searchParams.get("page") ?? 1));
  const [pageSize] = useState(20);
  const [query, setQuery] = useState(searchParams.get("query") ?? "");
  const [status, setStatus] = useState(searchParams.get("status") ?? "");

  const companiesQuery = useQuery({
    queryKey: ["platform-companies", { page, pageSize, query, status }],
    queryFn: () => listPlatformCompanies({ page, pageSize, query: query || undefined, status: status || undefined }),
    placeholderData: keepPreviousData,
  });

  useEffect(() => {
    const params = new URLSearchParams();
    if (page > 1) params.set("page", String(page));
    if (query) params.set("query", query);
    if (status) params.set("status", status);
    router.replace(`/master/companies${params.size > 0 ? `?${params.toString()}` : ""}`);
  }, [page, query, status, router]);

  const data = companiesQuery.data;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-black tracking-tight">Empresas</h1>
          <p className="text-sm text-muted-foreground">Gestiona las compañías de la plataforma.</p>
        </div>
        <Link href="/master/companies/new">
          <Button className="gap-2">
            <Plus className="h-4 w-4" />
            Nueva empresa
          </Button>
        </Link>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-4 sm:flex-row sm:items-end">
          <div className="flex-1 space-y-2">
            <Label htmlFor="search">Buscar</Label>
            <Input
              id="search"
              placeholder="Nombre, nombre comercial o subdominio"
              value={query}
              onChange={(event) => {
                setPage(1);
                setQuery(event.target.value);
              }}
            />
          </div>
          <div className="w-full sm:w-52">
            <Label htmlFor="status">Estado</Label>
            <select
              id="status"
              className="mt-2 flex h-11 w-full rounded-xl border border-input bg-background/75 px-3.5 py-2 text-sm"
              value={status}
              onChange={(event) => {
                setPage(1);
                setStatus(event.target.value);
              }}
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        </CardContent>
        <CardContent>
          {companiesQuery.isLoading ? (
            <p className="py-8 text-center text-sm text-muted-foreground">Cargando empresas…</p>
          ) : companiesQuery.isError ? (
            <p className="py-8 text-center text-sm text-muted-foreground">No se pudieron cargar las empresas.</p>
          ) : data && data.items.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">No hay empresas que coincidan.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
                  <th className="py-3 pr-4 font-semibold">Empresa</th>
                  <th className="py-3 pr-4 font-semibold">Subdominio</th>
                  <th className="py-3 pr-4 font-semibold">Plan</th>
                  <th className="py-3 pr-4 font-semibold">Suscripción</th>
                  <th className="py-3 pr-4 font-semibold">Estado</th>
                  <th className="py-3 font-semibold">Creada</th>
                </tr>
              </thead>
              <tbody>
                {data?.items.map((company: PlatformCompanySummary) => (
                  <tr
                    key={company.id}
                    className="cursor-pointer border-b border-border/60 transition-colors hover:bg-accent/50"
                    onClick={() => router.push(`/master/companies/${company.id}`)}
                  >
                    <td className="py-3 pr-4">
                      <p className="font-semibold">{company.commercialName}</p>
                      <p className="text-xs text-muted-foreground">{company.legalName}</p>
                    </td>
                    <td className="py-3 pr-4 font-mono text-xs">{company.domain}</td>
                    <td className="py-3 pr-4">{company.planName ?? "—"}</td>
                    <td className="py-3 pr-4">{company.subscriptionStatus ?? "—"}</td>
                    <td className="py-3 pr-4">{statusLabel(company.status)}</td>
                    <td className="py-3 text-xs text-muted-foreground">{formatDate(company.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {data && data.totalPages > 1 ? (
            <div className="mt-4 flex items-center justify-between">
              <p className="text-xs text-muted-foreground">
                {data.totalItems} empresa(s) · Página {data.page} de {data.totalPages}
              </p>
              <div className="flex gap-2">
                <Button variant="outline" className="h-8 px-3 text-xs" disabled={page <= 1} onClick={() => setPage((value) => value - 1)}>
                  Anterior
                </Button>
                <Button
                  variant="outline"
                  className="h-8 px-3 text-xs"
                  disabled={page >= data.totalPages}
                  onClick={() => setPage((value) => value + 1)}
                >
                  Siguiente
                </Button>
              </div>
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}
