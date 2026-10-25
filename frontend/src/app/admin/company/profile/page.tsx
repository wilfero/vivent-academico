import { AppShell } from "@/components/layout/app-shell";
import { CompanyProfileScreen } from "@/features/admin/company/company-profile-screen";

export default function CompanyProfilePage() {
  return <AppShell><CompanyProfileScreen mode="profile" /></AppShell>;
}
