import { Suspense } from "react";
import { PlatformCompaniesList } from "@/features/platform/companies/platform-companies-list";

export default function PlatformCompaniesPage() {
  return (
    <Suspense fallback={<p className="text-sm text-muted-foreground">Cargando empresas...</p>}>
      <PlatformCompaniesList />
    </Suspense>
  );
}
