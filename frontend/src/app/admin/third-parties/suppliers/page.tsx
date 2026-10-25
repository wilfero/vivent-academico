import { AppShell } from "@/components/layout/app-shell";
import { ThirdPartyScreen } from "@/features/admin/third-parties/third-party-screen";

export default function SuppliersPage() {
  return <AppShell><ThirdPartyScreen type="suppliers" /></AppShell>;
}
