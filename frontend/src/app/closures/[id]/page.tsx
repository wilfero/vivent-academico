import { AppShell } from "@/components/layout/app-shell";
import { ClosureDetailPageContent } from "@/features/reports/components/reports-page";

export default async function ClosureDetailPage({ params }: Readonly<{ params: Promise<{ id: string }> }>) {
  const { id } = await params;
  return <AppShell><ClosureDetailPageContent closureId={id} /></AppShell>;
}
