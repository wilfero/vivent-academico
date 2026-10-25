import { PublicShell } from "@/components/layout/public-shell";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default function TenantNotFoundPage() {
  return (
    <PublicShell>
      <Card className="fade-up w-full max-w-lg overflow-hidden">
        <CardHeader>
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-destructive text-xl font-black text-destructive-foreground">?</div>
          <CardTitle>Compañía no encontrada</CardTitle>
          <CardDescription>Verifica el subdominio o contacta al administrador de Vivent.</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          En ambiente local se usa el tenant configurado en <code>NEXT_PUBLIC_DEFAULT_TENANT</code>.
        </CardContent>
      </Card>
    </PublicShell>
  );
}
