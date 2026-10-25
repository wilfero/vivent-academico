import { AppShell } from "@/components/layout/app-shell";
import { CompanyProfileScreen } from "@/features/admin/company/company-profile-screen";

export default function CompanySettingsPage() {
  return <AppShell><CompanyProfileScreen mode="settings" /></AppShell>;
}
