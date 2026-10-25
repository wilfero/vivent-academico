"use client";

import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { listPlatformAuditLogs } from "@/features/platform/lib/platform-api";

const ACTION_LABELS: Record<string, string> = {
  LOGIN: "Inicio de sesión",
  LOGOUT: "Cierre de sesión",
  CREATE: "Creación",
  UPDATE: "Actualización",
  STATUS_CHANGE: "Cambio de estado",
  SUBSCRIPTION_CHANGE: "Cambio de suscripción",
  RESET_ADMIN_PASSWORD: "Restablecer contraseña",
  PROVISION: "Alta de empresa",
};

function formatDate(value: string) {
  return new Date(value).toLocaleString("es-GT", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function PlatformAuditLogs() {
  const [page, setPage] = useState(1);
  const pageSize = 25;

  const logsQuery = useQuery({
    queryKey: ["platform-audit-logs", page],
    queryFn: () => listPlatformAuditLogs(page, pageSize),
    placeholderData: keepPreviousData,
  });

  const data = logsQuery.data;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-black tracking-tight">Auditoría</h1>
        <p className="text-sm text-muted-foreground">Bitácora de acciones del panel de administración.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Registros</CardTitle>
        </CardHeader>
        <CardContent>
          {logsQuery.isLoading ? (
            <p className="py-8 text-center text-sm text-muted-foreground">Cargando bitácora…</p>
          ) : logsQuery.isError ? (
            <p className="py-8 text-center text-sm text-muted-foreground">No se pudo cargar la bitácora.</p>
          ) : data && data.items.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">Sin registros.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
                  <th className="py-3 pr-4 font-semibold">Fecha</th>
                  <th className="py-3 pr-4 font-semibold">Acción</th>
                  <th className="py-3 pr-4 font-semibold">Entidad</th>
                  <th className="py-3 pr-4 font-semibold">IP</th>
                </tr>
              </thead>
              <tbody>
                {data?.items.map((log) => (
                  <tr key={log.id} className="border-b border-border/60">
                    <td className="py-3 pr-4 text-xs text-muted-foreground">{formatDate(log.createdAt)}</td>
                    <td className="py-3 pr-4 font-semibold">{ACTION_LABELS[log.action] ?? log.action}</td>
                    <td className="py-3 pr-4">
                      {log.entityType}
                      {log.entityId ? <span className="ml-1 font-mono text-xs text-muted-foreground">{log.entityId.slice(0, 8)}</span> : null}
                    </td>
                    <td className="py-3 pr-4 font-mono text-xs">{log.ipAddress ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {data && data.totalPages > 1 ? (
            <div className="mt-4 flex items-center justify-between">
              <p className="text-xs text-muted-foreground">
                {data.totalItems} registro(s) · Página {data.page} de {data.totalPages}
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
