import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export function PlaceholderPage({ title, description }: Readonly<{ title: string; description: string }>) {
  return (
    <Card className="fade-up overflow-hidden">
      <CardHeader className="border-b bg-gradient-to-br from-card to-muted/50">
        <div className="mb-8 flex items-center justify-between gap-4">
          <div className="rounded-full border bg-background/70 px-3 py-1 font-mono text-xs text-muted-foreground">PHASE FRONT 1</div>
          <div className="h-2 w-28 rounded-full bg-gradient-to-r from-primary via-warehouse-line to-accent" />
        </div>
        <CardTitle className="text-4xl sm:text-5xl">{title}</CardTitle>
        <CardDescription className="max-w-2xl">{description}</CardDescription>
      </CardHeader>
      <CardContent className="grid gap-4 p-6 text-sm text-muted-foreground sm:grid-cols-3">
        <div className="rounded-2xl border bg-background/60 p-4">
          <p className="font-mono text-xs uppercase tracking-[0.18em]">Scope</p>
          <p className="mt-3 leading-6">Esta ruta queda preparada para una fase posterior.</p>
        </div>
        <div className="rounded-2xl border bg-background/60 p-4">
          <p className="font-mono text-xs uppercase tracking-[0.18em]">Status</p>
          <p className="mt-3 leading-6">Sin lógica de negocio ni llamadas funcionales todavía.</p>
        </div>
        <div className="rounded-2xl border bg-background/60 p-4">
          <p className="font-mono text-xs uppercase tracking-[0.18em]">Next</p>
          <p className="mt-3 leading-6">Se conectará cuando se apruebe la fase del módulo.</p>
        </div>
      </CardContent>
    </Card>
  );
}
