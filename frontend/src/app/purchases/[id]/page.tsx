import { AppShell } from "@/components/layout/app-shell";
import { PurchaseDetailPageContent } from "@/features/purchases/components/purchase-detail-page";

export default async function PurchaseDetailPage({ params }: Readonly<{ params: Promise<{ id: string }> }>) {
  const { id } = await params;
  return <AppShell><PurchaseDetailPageContent purchaseId={id} /></AppShell>;
}
