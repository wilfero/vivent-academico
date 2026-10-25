import { AppShell } from "@/components/layout/app-shell";
import { SaleDetailPageContent } from "@/features/sales/components/sale-detail-page";

export default async function SaleDetailPage({ params }: Readonly<{ params: Promise<{ id: string }> }>) {
  const { id } = await params;
  return <AppShell><SaleDetailPageContent saleId={id} /></AppShell>;
}
