import { AppShell } from "@/components/layout/app-shell";
import { AdjustmentDetailPageContent } from "@/features/adjustments/components/adjustments-page";

export default async function AdjustmentDetailPage({ params }: Readonly<{ params: Promise<{ id: string }> }>) {
  const { id } = await params;
  return <AppShell><AdjustmentDetailPageContent adjustmentId={id} /></AppShell>;
}
