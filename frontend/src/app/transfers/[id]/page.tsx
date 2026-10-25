import { AppShell } from "@/components/layout/app-shell";
import { TransferDetailScreen } from "@/features/transfers/components/transfer-detail-screen";

export default async function TransferDetailPage({ params }: Readonly<{ params: Promise<{ id: string }> }>) {
  const { id } = await params;
  return <AppShell><TransferDetailScreen transferId={id} /></AppShell>;
}
